package client;

import share.Constants;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import javax.imageio.ImageIO;

public class UdpImageClient {
    // Đã thêm tham số String userId vào hàm
    public static BufferedImage sendAndReceiveImage(File file, int commandCode, String userId) throws Exception {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(5000);
            InetAddress serverAddr = InetAddress.getByName("localhost");
            byte[] imageData = Files.readAllBytes(file.toPath());

            int totalChunks = (int) Math.ceil((double) imageData.length / Constants.UDP_CHUNK_SIZE);
            
            // Đóng gói Header UDP đúng quy ước
            String header = "HEADER:" + userId + ":" + commandCode + ":" + imageData.length + ":" + totalChunks;
            byte[] headerBytes = header.getBytes();
            socket.send(new DatagramPacket(headerBytes, headerBytes.length, serverAddr, Constants.SERVER_PORT_UDP));

            int offset = 0;
            for (int i = 0; i < totalChunks; i++) {
                int length = Math.min(Constants.UDP_CHUNK_SIZE, imageData.length - offset);
                socket.send(new DatagramPacket(imageData, offset, length, serverAddr, Constants.SERVER_PORT_UDP));
                offset += length;
            }

            // Nhận Header phản hồi
            byte[] respBuf = new byte[1024];
            DatagramPacket respPacket = new DatagramPacket(respBuf, respBuf.length);
            socket.receive(respPacket);
            String[] respInfo = new String(respPacket.getData(), 0, respPacket.getLength()).split(":");
            if (!respInfo[0].equals("RESP")) throw new Exception("Server trả về sai định dạng.");

            int resultLen = Integer.parseInt(respInfo[1]);
            int resTotalChunks = Integer.parseInt(respInfo[2]);

            // Nhận mảng byte kết quả
            byte[] resultData = new byte[resultLen];
            int resOffset = 0;
            for (int i = 0; i < resTotalChunks; i++) {
                byte[] chunkBuf = new byte[Constants.UDP_CHUNK_SIZE];
                DatagramPacket chunkPacket = new DatagramPacket(chunkBuf, chunkBuf.length);
                socket.receive(chunkPacket);
                System.arraycopy(chunkPacket.getData(), 0, resultData, resOffset, chunkPacket.getLength());
                resOffset += chunkPacket.getLength();
            }

            return ImageIO.read(new ByteArrayInputStream(resultData));
        }
    }
}