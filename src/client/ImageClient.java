package client;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import javax.imageio.ImageIO;

public class ImageClient {
    // Đã thêm tham số String userId vào hàm
    public static BufferedImage sendAndReceiveImage(File file, int commandCode, String userId) throws Exception {
        try (Socket socket = new Socket("localhost", 8888);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            byte[] imageData = Files.readAllBytes(file.toPath());
            
            // Đóng gói Header đúng quy ước Giai đoạn 2
            String header = "HEADER:" + userId + ":" + commandCode + ":" + imageData.length;
            dos.writeUTF(header);
            dos.write(imageData);
            dos.flush();

            // Nhận kết quả
            int resultLength = dis.readInt();
            byte[] resultData = new byte[resultLength];
            dis.readFully(resultData);

            return ImageIO.read(new ByteArrayInputStream(resultData));
        }
    }
}