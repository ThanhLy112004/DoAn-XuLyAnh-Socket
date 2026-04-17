import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            // 1. Lấy mã lệnh
            byte commandCode = dis.readByte();
            
            // 2. Kích thước ảnh L
            int L = dis.readInt();
            LogWindow.log("Yêu cầu mã: " + commandCode + ". Kích thước ảnh: " + L + " bytes");

            // 3. Đọc dữ liệu ảnh từ Client gửi lên
            byte[] buffer = new byte[L];
            int totalBytesRead = 0;
            while (totalBytesRead < L) {
                int bytesRead = dis.read(buffer, totalBytesRead, L - totalBytesRead);
                if (bytesRead == -1) {
                    throw new Exception("Mất kết nối giữa chừng khi đang đọc ảnh!");
                }
                totalBytesRead += bytesRead;
            }

            // ==========================================================
            // 4. CHUYỂN GIAO TOÀN QUYỀN CHO BẾP TRƯỞNG (IMAGE PROCESSOR)
            // ==========================================================
            LogWindow.log("Đang chuyển dữ liệu vào Core để xử lý...");
            
            // Chỉ 1 dòng lệnh duy nhất! Tự đọc, tự xử lý, tự đóng gói.
            byte[] resultData = ImageProcessor.processRequest(commandCode, buffer);

            if (resultData == null) {
                throw new Exception("Core xử lý ảnh thất bại hoặc trả về dữ liệu rỗng.");
            }

            // 5. Gửi kết quả (mảng byte) về lại cho Client
            dos.writeInt(resultData.length);
            dos.write(resultData);
            dos.flush();
            LogWindow.log("Đã xử lý xong và gửi trả Client " + resultData.length + " bytes.");

        } catch (Exception e) {
            LogWindow.log("Lỗi ở ClientHandler: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                // Bỏ qua lỗi đóng socket
            }
        }
    }
}