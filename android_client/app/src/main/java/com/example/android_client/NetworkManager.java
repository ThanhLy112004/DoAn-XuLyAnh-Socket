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

public class NetworkManager {
    private Context context;
    private static final int TCP_PORT = 8888;
    private static final int UDP_PORT = 8889;
    private static final int UDP_CHUNK_SIZE = 16384; 

    public NetworkManager(Context context) {
        this.context = context;
    }

    public byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public Bitmap sendViaTCP(String serverIp, String userId, int cmd, byte[] imgData) {
        try (Socket socket = new Socket(serverIp, TCP_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            // BẮT BUỘC: Gửi chuỗi Header bằng writeUTF theo đúng yêu cầu Backend
            String headerStr = "HEADER:" + userId + ":" + cmd + ":" + imgData.length;
            dos.writeUTF(headerStr);

            // Gửi dữ liệu ảnh
            dos.write(imgData);
            dos.flush();
            Log.d("TCP_CLIENT", "Đã gửi lên Server: " + imgData.length + " bytes");

            // Nhận kết quả
            int resultLen = dis.readInt();
            byte[] resultData = new byte[resultLen];
            dis.readFully(resultData);

            return BitmapFactory.decodeByteArray(resultData, 0, resultData.length);

        } catch (Exception e) {
            Log.e("TCP_CLIENT", "Lỗi TCP: " + e.getMessage());
            return null;
        }
    }

    public Bitmap sendViaUDP(String serverIp, String userId, int cmd, byte[] imgData, int totalChunks) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(5000); // Rút ngắn Timeout xuống 5s để show tính năng lỗi cho thầy cô xem
            InetAddress address = InetAddress.getByName(serverIp);

            // Gửi Header
            String headerStr = "HEADER:" + userId + ":" + cmd + ":" + imgData.length + ":" + totalChunks;
            byte[] header = headerStr.getBytes();
            socket.send(new DatagramPacket(header, header.length, address, UDP_PORT));

            // Bơm mảnh UDP
            int offset = 0;
            for (int i = 0; i < totalChunks; i++) {
                int length = Math.min(UDP_CHUNK_SIZE, imgData.length - offset);
                byte[] chunk = new byte[length];
                System.arraycopy(imgData, offset, chunk, 0, length);
                socket.send(new DatagramPacket(chunk, length, address, UDP_PORT));
                offset += length;
                Thread.sleep(1);
            }

            // Nhận phản hồi
            byte[] rcvBuffer = new byte[1024];
            DatagramPacket rcvPacket = new DatagramPacket(rcvBuffer, rcvBuffer.length);
            socket.receive(rcvPacket);
            String[] rcvHeaderInfo = new String(rcvPacket.getData(), 0, rcvPacket.getLength()).split(":");
            
            int resultLen = Integer.parseInt(rcvHeaderInfo[1]);
            int resultTotalChunks = Integer.parseInt(rcvHeaderInfo[2]);

            byte[] resultData = new byte[resultLen];
            int resultOffset = 0;

            // Vòng lặp nhận mảnh ảnh (Có bọc Try-Catch để chống Crash khi rớt mạng)
            try {
                for(int i = 0; i < resultTotalChunks; i++) {
                    byte[] chunkBuffer = new byte[UDP_CHUNK_SIZE];
                    DatagramPacket chunkPacket = new DatagramPacket(chunkBuffer, chunkBuffer.length);
                    socket.receive(chunkPacket);
                    
                    int copyLength = Math.min(chunkPacket.getLength(), resultData.length - resultOffset);
                    System.arraycopy(chunkPacket.getData(), 0, resultData, resultOffset, copyLength);
                    resultOffset += copyLength;
                }
                Log.d("UDP_CLIENT", "Đã nhận kết quả đủ " + resultTotalChunks + " mảnh.");
            } catch (SocketTimeoutException timeout) {
                Log.e("UDP_CLIENT", "Mất kết nối mạng! Chỉ lấy được một phần dữ liệu để phô diễn lỗi UDP.");
                // Không return null, cứ giải mã mảng byte bị thiếu để tạo ra ảnh lỗi xước/xám
            }

            return BitmapFactory.decodeByteArray(resultData, 0, resultData.length);

        } catch (Exception e) {
            Log.e("UDP_CLIENT", "Lỗi UDP toàn cục: " + e.getMessage());
            return null;
        }
    }
}