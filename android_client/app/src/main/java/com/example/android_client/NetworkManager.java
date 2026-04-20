package com.example.android_client;

import android.content.Context;
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
import java.nio.ByteBuffer;
import java.util.Locale;

import share.Constants;

public class NetworkManager {
    
    public interface NetworkCallback {
        void onProgressUpdate(String sentStats, String receivedStats);
    }

    private NetworkCallback callback;

    public NetworkManager(NetworkCallback callback) {
        this.callback = callback;
    }

    public byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        return stream.toByteArray();
    }

    public Bitmap sendViaTCP(String serverIp, String userId, int cmd, byte[] imgData) {
        try (Socket socket = new Socket(serverIp, Constants.SERVER_PORT_TCP);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            socket.setSoTimeout(15000);
            
            long startSend = System.currentTimeMillis();
            String headerStr = "HEADER:" + userId + ":" + cmd + ":" + imgData.length;
            dos.writeUTF(headerStr);
            dos.write(imgData);
            dos.flush();
            long sendTime = System.currentTimeMillis() - startSend;
            double sendSpeed = (imgData.length / 1024.0) / (sendTime / 1000.0 + 0.001);
            
            if (callback != null) {
                callback.onProgressUpdate(
                    String.format(Locale.US, "Gửi: Xong - %dms - %.1f KB/s", sendTime, sendSpeed),
                    "Đang đợi Server xử lý..."
                );
            }

            long startRecv = System.currentTimeMillis();
            int resultLen = dis.readInt();
            byte[] resultData = new byte[resultLen];
            dis.readFully(resultData);
            long recvTime = System.currentTimeMillis() - startRecv;
            double recvSpeed = (resultLen / 1024.0) / (recvTime / 1000.0 + 0.001);

            if (callback != null) {
                callback.onProgressUpdate(null, 
                    String.format(Locale.US, "Nhận: Xong - %dms - %.1f KB/s", recvTime, recvSpeed));
            }

            return BitmapFactory.decodeByteArray(resultData, 0, resultData.length);
        } catch (Exception e) {
            Log.e("TCP_CLIENT", "Lỗi TCP: " + e.getMessage());
            return null;
        }
    }

    public Bitmap sendViaUDP(String serverIp, String userId, int cmd, byte[] imgData, int totalChunks) {
        byte[] resultData = new byte[15 * 1024 * 1024]; 
        int expectedLen = 0, expectedChunks = 0;
        int maxPosReached = 0, receivedChunks = 0;

        try (DatagramSocket socket = new DatagramSocket()) {
            // Tối ưu bộ đệm hệ thống lên mức tối đa
            socket.setReceiveBufferSize(8 * 1024 * 1024);
            socket.setSendBufferSize(8 * 1024 * 1024);
            
            InetAddress address = InetAddress.getByName(serverIp);

            // 1. Gửi Header
            byte[] hBytes = ("HEADER:" + userId + ":" + cmd + ":" + imgData.length + ":" + totalChunks).getBytes();
            socket.send(new DatagramPacket(hBytes, hBytes.length, address, Constants.SERVER_PORT_UDP));

            // 2. Gửi Ảnh
            long startSend = System.currentTimeMillis();
            int offset = 0;
            for (int i = 0; i < totalChunks; i++) {
                int length = Math.min(Constants.UDP_CHUNK_SIZE, imgData.length - offset);
                byte[] chunk = new byte[length];
                System.arraycopy(imgData, offset, chunk, 0, length);
                socket.send(new DatagramPacket(chunk, length, address, Constants.SERVER_PORT_UDP));
                offset += length;
                Thread.sleep(1); // Khoảng nghỉ cần thiết để Server không bị sập
                
                if (callback != null && (i % 100 == 0 || i == totalChunks - 1)) {
                    callback.onProgressUpdate(String.format(Locale.US, "Đang gửi: %d/%d mảnh...", (i+1), totalChunks), "...");
                }
            }
            long sendTime = System.currentTimeMillis() - startSend;
            double sendSpeed = (imgData.length / 1024.0) / (sendTime / 1000.0 + 0.001);
            if (callback != null) {
                callback.onProgressUpdate(String.format(Locale.US, "Gửi: %d mảnh - %dms - %.1f KB/s", totalChunks, sendTime, sendSpeed), "Đang nhận...");
            }

            // 3. Nhận Ảnh
            long startRecv = System.currentTimeMillis();
            socket.setSoTimeout(3000); 

            // Khởi tạo buffer ngoài vòng lặp để tránh Garbage Collector làm chậm máy
            byte[] receiveBuffer = new byte[Constants.UDP_CHUNK_SIZE + 10];
            DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);

            try {
                while (true) {
                    socket.receive(packet);
                    socket.setSoTimeout(300); // Sau khi gói đầu đến, chờ 300ms

                    int len = packet.getLength();
                    byte[] data = packet.getData();

                    // Kiểm tra nhanh Header RESP
                    if (len > 5 && data[0] == 'R' && data[1] == 'E' && data[2] == 'S' && data[3] == 'P') {
                        String fullHdr = new String(data, 0, len);
                        String[] pts = fullHdr.split(":");
                        expectedLen = Integer.parseInt(pts[1]);
                        expectedChunks = Integer.parseInt(pts[2]);
                        continue;
                    }

                    // Xử lý Xếp hình mảnh ảnh
                    if (len >= 4) {
                        receivedChunks++;
                        // Dùng ByteBuffer nhanh để lấy Index
                        int chunkIndex = ((data[0] & 0xFF) << 24) | ((data[1] & 0xFF) << 16) |
                                         ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
                        
                        int payloadLen = len - 4;
                        int targetPos = chunkIndex * Constants.UDP_CHUNK_SIZE;

                        if (targetPos + payloadLen <= resultData.length - 2) {
                            System.arraycopy(data, 4, resultData, targetPos, payloadLen);
                            if (targetPos + payloadLen > maxPosReached) maxPosReached = targetPos + payloadLen;
                        }

                        // Giảm tần suất cập nhật UI để không làm nghẽn luồng nhận tin
                        if (callback != null && (receivedChunks % 50 == 0)) {
                            callback.onProgressUpdate(null, String.format(Locale.US, "Đang nhận: %d/%d mảnh...", receivedChunks, expectedChunks));
                        }
                    }
                }
            } catch (SocketTimeoutException e) {
                // Trừ khấu hao 300ms cho đúng tốc độ thật
                long recvDuration = System.currentTimeMillis() - startRecv - 300; 
                double recvSpeed = (maxPosReached / 1024.0) / (Math.max(1, recvDuration) / 1000.0);
                if (callback != null) {
                    callback.onProgressUpdate(null, String.format(Locale.US, "Nhận: %d/%d mảnh - %dms - %.1f KB/s", receivedChunks, expectedChunks, Math.max(0, recvDuration), recvSpeed));
                }
            }

            int finalSize = (expectedLen > 0) ? expectedLen : maxPosReached;
            if (finalSize > 0) {
                resultData[finalSize] = (byte) 0xFF;
                resultData[finalSize + 1] = (byte) 0xD9;
                return BitmapFactory.decodeByteArray(resultData, 0, finalSize + 2);
            }
        } catch (Exception e) {
            Log.e("UDP_CLIENT", "Lỗi: " + e.getMessage());
        }
        return null;
    }
}