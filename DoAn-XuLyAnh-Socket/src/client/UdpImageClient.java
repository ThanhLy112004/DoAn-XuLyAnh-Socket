package client;

import share.Constants;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;

public class UdpImageClient {

    public static BufferedImage sendAndReceiveImage(File file, int cmd) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(5000); // Đợi tối đa 5s nếu rớt mạng
        
        // --- 1. MỞ RỘNG BỘ ĐỆM ĐỂ CHẠY TỐC ĐỘ TỐI ĐA (Thay cho lệnh sleep) ---
        socket.setSendBufferSize(5 * 1024 * 1024);
        socket.setReceiveBufferSize(5 * 1024 * 1024);

        InetAddress address = InetAddress.getByName(Constants.SERVER_IP);

        // 2. Đọc và băm ảnh
        BufferedImage img = ImageIO.read(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] imgData = baos.toByteArray();

        // 3. Tính toán số mảnh
        int totalChunks = (int) Math.ceil((double) imgData.length / Constants.UDP_CHUNK_SIZE);
        
        // 4. Gửi Header (Báo cho Server biết)
        String headerStr = "HEADER:" + cmd + ":" + imgData.length + ":" + totalChunks;
        byte[] header = headerStr.getBytes();
        socket.send(new DatagramPacket(header, header.length, address, Constants.SERVER_PORT_UDP));

        // 5. Bơm các mảnh ảnh qua mạng
        int offset = 0;
        for (int i = 0; i < totalChunks; i++) {
            
            // --- CẬP NHẬT: GIẢ LẬP RỚT MẠNG 5% (SIMULATE DROP) ---
            if (Math.random() < Constants.SIMULATE_DROP_RATE) {
                offset += Constants.UDP_CHUNK_SIZE; // Vẫn phải cộng offset để nhích tới mảnh tiếp theo
                continue; // Bỏ qua lệnh send() phía dưới -> Mảnh này bị bốc hơi!
            }
            // ----------------------------------------------------

            int length = Math.min(Constants.UDP_CHUNK_SIZE, imgData.length - offset);
            byte[] chunk = new byte[length];
            System.arraycopy(imgData, offset, chunk, 0, length);
            socket.send(new DatagramPacket(chunk, length, address, Constants.SERVER_PORT_UDP));
            offset += length;
        }

        // 6. Nhận kết quả từ Server
        byte[] rcvBuffer = new byte[1024];
        DatagramPacket rcvPacket = new DatagramPacket(rcvBuffer, rcvBuffer.length);
        socket.receive(rcvPacket);
        
        String[] rcvHeaderInfo = new String(rcvPacket.getData(), 0, rcvPacket.getLength()).split(":");
        int resultLen = Integer.parseInt(rcvHeaderInfo[1]);
        int resultTotalChunks = Integer.parseInt(rcvHeaderInfo[2]);

        byte[] resultData = new byte[resultLen];
        int resultOffset = 0;
        
        for(int i = 0; i < resultTotalChunks; i++) {
            byte[] chunkBuffer = new byte[Constants.UDP_CHUNK_SIZE];
            DatagramPacket chunkPacket = new DatagramPacket(chunkBuffer, chunkBuffer.length);
            socket.receive(chunkPacket);
            
            // Chống lỗi ArrayOutOfBounds nếu Server gửi mảnh cuối bị dư byte
            int copyLength = Math.min(chunkPacket.getLength(), resultData.length - resultOffset);
            System.arraycopy(chunkPacket.getData(), 0, resultData, resultOffset, copyLength);
            resultOffset += copyLength;
        }

        socket.close();
        return ImageIO.read(new ByteArrayInputStream(resultData));
    }
}