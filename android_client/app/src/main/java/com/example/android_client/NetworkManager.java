package com.example.android_client;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Locale;

import share.Constants;

public class NetworkManager {
    
    public interface NetworkCallback {
        void onProgressUpdate(String sentStats, String receivedStats);
    }

    private final NetworkCallback callback;

    public NetworkManager(NetworkCallback callback) {
        this.callback = callback;
    }

    // Ep dung luong anh tu Android xuong muc an toan truoc khi gui
    public byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        return stream.toByteArray();
    }

    // =====================================================================
    // GIAO THUC TCP (An toan nhung cham neu mang yeu)
    // =====================================================================
    public Bitmap sendViaTCP(String serverIp, String userId, int commandCode, byte[] imageData) {
        try (Socket tcpSocket = new Socket(serverIp, Constants.SERVER_PORT_TCP);
             DataOutputStream dataOut = new DataOutputStream(tcpSocket.getOutputStream());
             DataInputStream dataIn = new DataInputStream(tcpSocket.getInputStream())) {

            // TANG TIMEOUT LEN 30 GIAY: Vi Server v2 co the phat delay RTO len toi 7-10 giay
            tcpSocket.setSoTimeout(30000); 
            
            // 1. GUI DU LIEU CHO SERVER
            long startSendTime = System.currentTimeMillis();
            String headerMessage = "HEADER:" + userId + ":" + commandCode + ":" + imageData.length;
            dataOut.writeUTF(headerMessage);
            dataOut.write(imageData);
            dataOut.flush();
            
            long sendDuration = System.currentTimeMillis() - startSendTime;
            double sendSpeedKbps = (imageData.length / 1024.0) / (sendDuration / 1000.0 + 0.001);
            
            if (callback != null) {
                callback.onProgressUpdate(
                    String.format(Locale.US, "Gui: Xong - %dms - %.1f KB/s", sendDuration, sendSpeedKbps),
                    "Dang doi Server xu ly (Co the lau do mo phong mang)..."
                );
            }

            // 2. NHAN KET QUA TU SERVER
            long startReceiveTime = System.currentTimeMillis();
            int resultDataLength = dataIn.readInt(); // Doc do dai anh tu Server v2
            byte[] resultImageData = new byte[resultDataLength];
            dataIn.readFully(resultImageData); // Doc du 100% data moi dung lai
            
            long receiveDuration = System.currentTimeMillis() - startReceiveTime;
            double receiveSpeedKbps = (resultDataLength / 1024.0) / (receiveDuration / 1000.0 + 0.001);

            if (callback != null) {
                callback.onProgressUpdate(null, 
                    String.format(Locale.US, "Nhan: Xong - %dms - %.1f KB/s", receiveDuration, receiveSpeedKbps));
            }

            return BitmapFactory.decodeByteArray(resultImageData, 0, resultImageData.length);
            
        } catch (Exception exception) {
            Log.e("TCP_CLIENT", "Loi TCP: " + exception.getMessage());
            return null;
        }
    }

    // =====================================================================
    // GIAO THUC UDP (Nhanh, chap nhan rot goi, co co che Datamoshing)
    // =====================================================================
    public Bitmap sendViaUDP(String serverIp, String userId, int commandCode, byte[] imageData, int totalChunks) {
        // TOI UU RAM: Anh tra ve luon duoc Server nen, nen kich thuoc anh goc + 2 byte du phong la an toan 100%
        byte[] resultImageData = new byte[imageData.length + 1024]; 
        int expectedImageLength = 0;
        int expectedTotalChunks = 0;
        int maxPositionReached = 0;
        int actualReceivedChunks = 0;

        try (DatagramSocket udpSocket = new DatagramSocket()) {
            udpSocket.setReceiveBufferSize(5 * 1024 * 1024);
            udpSocket.setSendBufferSize(5 * 1024 * 1024);
            
            InetAddress serverAddress = InetAddress.getByName(serverIp);

            // 1. GUI HEADER
            String headerStr = "HEADER:" + userId + ":" + commandCode + ":" + imageData.length + ":" + totalChunks;
            byte[] headerBytes = headerStr.getBytes();
            udpSocket.send(new DatagramPacket(headerBytes, headerBytes.length, serverAddress, Constants.SERVER_PORT_UDP));

            // 2. GUI DU LIEU ANH (Cat manh)
            long startSendTime = System.currentTimeMillis();
            int currentSendOffset = 0;
            
            for (int i = 0; i < totalChunks; i++) {
                int sendLength = Math.min(Constants.UDP_CHUNK_SIZE, imageData.length - currentSendOffset);
                byte[] chunkData = new byte[sendLength];
                System.arraycopy(imageData, currentSendOffset, chunkData, 0, sendLength);
                
                udpSocket.send(new DatagramPacket(chunkData, sendLength, serverAddress, Constants.SERVER_PORT_UDP));
                currentSendOffset += sendLength;
                
                // Ngu 1ms de tranh tran bo dem Server
                if (i % 15 == 0) {
                    Thread.sleep(1); 
                }
                                
                
                if (callback != null && (i % 50 == 0 || i == totalChunks - 1)) {
                    callback.onProgressUpdate(String.format(Locale.US, "Dang gui: %d/%d manh...", (i+1), totalChunks), "...");
                }
            }
            
            long sendDuration = System.currentTimeMillis() - startSendTime;
            double sendSpeedKbps = (imageData.length / 1024.0) / (sendDuration / 1000.0 + 0.001);
            if (callback != null) {
                callback.onProgressUpdate(String.format(Locale.US, "Gui: %d manh - %dms - %.1f KB/s", totalChunks, sendDuration, sendSpeedKbps), "Dang lang nghe Server...");
            }

            // 3. NHAN DU LIEU ANH TU SERVER
            long startReceiveTime = System.currentTimeMillis();
            udpSocket.setSoTimeout(3000); // Cho toi da 3 giay cho goi tin dau tien

            // Khung don du lieu du chua 16KB + 4 byte Index cua Server V2
            byte[] receiveBuffer = new byte[Constants.UDP_CHUNK_SIZE + 8];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

            try {
                while (true) {
                    udpSocket.receive(receivePacket);
                    udpSocket.setSoTimeout(300); // Sau khi goi dau tien ve, chi cho cac goi sau 300ms

                    int packetLength = receivePacket.getLength();
                    byte[] packetData = receivePacket.getData();

                    // Kiem tra chuoi Header bao hieu dung luong "RESP:..."
                    if (packetLength > 5 && packetData[0] == 'R' && packetData[1] == 'E' && packetData[2] == 'S' && packetData[3] == 'P') {
                        String fullHeader = new String(packetData, 0, packetLength);
                        String[] headerParts = fullHeader.split(":");
                        expectedImageLength = Integer.parseInt(headerParts[1]);
                        expectedTotalChunks = Integer.parseInt(headerParts[2]);
                        
                        // Mo rong mang neu anh ket qua bong dung to hon anh goc (Riem khi xay ra)
                        if (expectedImageLength > resultImageData.length - 2) {
                            resultImageData = new byte[expectedImageLength + 2];
                        }
                        continue;
                    }

                    // Xu ly lap rap manh anh (Doc 4 bytes dau tien de lay Index)
                    if (packetLength >= 4) {
                        actualReceivedChunks++;
                        
                        int chunkIndex = ((packetData[0] & 0xFF) << 24) | 
                                         ((packetData[1] & 0xFF) << 16) |
                                         ((packetData[2] & 0xFF) << 8)  | 
                                          (packetData[3] & 0xFF);
                        
                        int payloadLength = packetLength - 4;
                        int targetPosition = chunkIndex * Constants.UDP_CHUNK_SIZE;

                        // Lap rap vao mang chinh neu hop le
                        if (targetPosition + payloadLength <= resultImageData.length - 2) {
                            System.arraycopy(packetData, 4, resultImageData, targetPosition, payloadLength);
                            if (targetPosition + payloadLength > maxPositionReached) {
                                maxPositionReached = targetPosition + payloadLength;
                            }
                        }

                        if (callback != null && (actualReceivedChunks % 20 == 0)) {
                            callback.onProgressUpdate(null, String.format(Locale.US, "Dang nhan: %d/%d manh...", actualReceivedChunks, expectedTotalChunks));
                        }
                    }
                }
            } catch (SocketTimeoutException timeoutException) {
                // Tinh toan toc do khau hao 300ms do cho doi Timeout
                long receiveDuration = System.currentTimeMillis() - startReceiveTime - 300; 
                double receiveSpeedKbps = (maxPositionReached / 1024.0) / (Math.max(1, receiveDuration) / 1000.0);
                if (callback != null) {
                    callback.onProgressUpdate(null, String.format(Locale.US, "Nhan: %d/%d manh - %dms - %.1f KB/s", actualReceivedChunks, expectedTotalChunks, Math.max(0, receiveDuration), receiveSpeedKbps));
                }
            }

            // BUOC CHOT DATAMOSHING: Khoa duoi file bang ma EOI (End of Image) cua JPEG
            int finalValidSize = (expectedImageLength > 0) ? expectedImageLength : maxPositionReached;
            if (finalValidSize > 0) {
                resultImageData[finalValidSize] = (byte) 0xFF;
                resultImageData[finalValidSize + 1] = (byte) 0xD9;
                return BitmapFactory.decodeByteArray(resultImageData, 0, finalValidSize + 2);
            }
            
        } catch (Exception exception) {
            Log.e("UDP_CLIENT", "Loi UDP: " + exception.getMessage());
        }
        return null;
    }
}
