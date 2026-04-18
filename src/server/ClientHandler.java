package server;

import core.LogWindow;
import core.ImageProcessor;
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

            // ==========================================================
            // 1. ĐỌC VÀ CẮT CHUỖI HEADER
            // ==========================================================
            // Android sẽ gửi chuỗi Header bằng phương thức writeUTF()
            String headerString = dis.readUTF(); 
            LogWindow.log("Đã nhận Packet Header: " + headerString);

            // Cắt chuỗi theo dấu hai chấm ":"
            String[] parts = headerString.split(":");
            if (parts.length < 4 || !parts[0].equals("HEADER")) {
                throw new Exception("Sai định dạng Header. Đã ngắt kết nối!");
            }

            // Bóc tách thông tin từ mảng đã cắt
            String userId = parts[1];
            int commandCode = Integer.parseInt(parts[2]);
            int L = Integer.parseInt(parts[3]); // Đây chính là kích thước ảnh (Size)

            // ==========================================================
            // 2. ĐỌC DỮ LIỆU ẢNH TỪ CLIENT
            // ==========================================================
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
            // 3. CHUYỂN GIAO TOÀN QUYỀN CHO BẾP TRƯỞNG & LƯU LỊCH SỬ
            // ==========================================================

            // Lưu ảnh gốc xuống ổ cứng
            String originalPath = FileManager.saveImageToDisk(buffer, "original", userId + "_goc");

            // Bấm đồng hồ đo thời gian
            long startTime = System.currentTimeMillis();

            LogWindow.log("Đang chuyển dữ liệu vào Core để xử lý...");
            byte[] resultData = ImageProcessor.processRequest(commandCode, buffer); 

            // Kiểm tra lỗi ngay lập tức
            if (resultData == null) {
                throw new Exception("Core xử lý ảnh thất bại hoặc trả về dữ liệu rỗng.");
            }

            // Dừng đồng hồ, tính thời gian xử lý
            long processTime = System.currentTimeMillis() - startTime;

            // Lưu ảnh kết quả xuống ổ cứng
            String resultPath = FileManager.saveImageToDisk(resultData, "result", userId + "_xong");

            // Ghi nhật ký vào Database
            boolean dbSuccess = HistoryDAO.saveRecord(userId, commandCode, "TCP", processTime, originalPath, resultPath);
            if (dbSuccess) {
                LogWindow.log("=> Đã lưu lịch sử vào Database thành công.");
            }

            // ==========================================================
            // 4. GỬI KẾT QUẢ VỀ LẠI CHO CLIENT
            // ==========================================================
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