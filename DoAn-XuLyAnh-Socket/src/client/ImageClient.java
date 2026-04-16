package client;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import javax.imageio.ImageIO;

public class ImageClient {
    // Địa chỉ IP và Port của Server (Người 2 làm)
    private static final String SERVER_IP = "127.0.0.1"; // localhost để test trên cùng 1 máy
    private static final int PORT = 5000;

    // Hàm này nhận vào File ảnh và Mã lệnh, trả về Ảnh đã xử lý
    public static BufferedImage sendAndReceiveImage(File imageFile, int commandCode) throws Exception {
        try (Socket socket = new Socket(SERVER_IP, PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            // 1. Chuyển file ảnh thành mảng byte[] (imageData)
            BufferedImage img = ImageIO.read(imageFile);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Lấy đuôi file (jpg, png) để ghi cho chuẩn
            String format = imageFile.getName().substring(imageFile.getName().lastIndexOf(".") + 1);
            ImageIO.write(img, format, baos);
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
            // 5. Đứng đợi nhận 4 byte đầu tiên để biết độ dài ảnh trả về
            int resultLength = dis.readInt();
            byte[] resultData = new byte[resultLength];
            
            // 6. Đọc đủ số byte dữ liệu ảnh
            dis.readFully(resultData);
            
            // 7. Chuyển mảng byte ngược lại thành BufferedImage để hiển thị
            ByteArrayInputStream bais = new ByteArrayInputStream(resultData);
            BufferedImage resultImage = ImageIO.read(bais);

            System.out.println("Đã nhận ảnh kết quả thành công!");
            return resultImage;
        }
    }
}