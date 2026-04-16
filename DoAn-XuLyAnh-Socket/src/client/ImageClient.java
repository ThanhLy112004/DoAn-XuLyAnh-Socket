package client;

import share.Constants; // Import file cấu hình chung
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import javax.imageio.ImageIO;

public class ImageClient {

    // Hàm này nhận vào File ảnh và Mã lệnh, trả về Ảnh đã xử lý từ Server
    public static BufferedImage sendAndReceiveImage(File imageFile, int commandCode) throws Exception {
        
        // Tạo kết nối bằng IP và Port lấy từ file Constants dùng chung
        try (Socket socket = new Socket(Constants.SERVER_IP, Constants.SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            // 1. Đọc file ảnh và băm thành mảng byte[]
            BufferedImage img = ImageIO.read(imageFile);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // Ép tất cả ảnh thành định dạng "png" trước khi truyền để tránh lỗi file không đuôi
            ImageIO.write(img, "png", baos); 
            byte[] imageData = baos.toByteArray();

            // ================= GIAI ĐOẠN GỬI =================
            // 2. Gửi 1 byte (Mã chức năng)
            dos.writeByte(commandCode);
            
            // 3. Gửi 4 byte (Độ dài mảng byte)
            dos.writeInt(imageData.length);
            
            // 4. Gửi Payload (Toàn bộ mảng imageData)
            dos.write(imageData);
            dos.flush();
            System.out.println("Đã gửi gói tin: Lệnh " + commandCode + ", Kích thước: " + imageData.length + " bytes.");

            // ================= GIAI ĐOẠN NHẬN =================
            // 5. Đợi nhận 4 byte đầu tiên để biết độ dài ảnh trả về
            int resultLength = dis.readInt();
            byte[] resultData = new byte[resultLength];
            
            // 6. Đọc đủ số byte dữ liệu ảnh
            dis.readFully(resultData);
            
            // 7. Chuyển mảng byte ngược lại thành BufferedImage để trả về UI
            ByteArrayInputStream bais = new ByteArrayInputStream(resultData);
            BufferedImage resultImage = ImageIO.read(bais);

            System.out.println("Đã nhận ảnh kết quả thành công!");
            return resultImage;
        }
    }
}