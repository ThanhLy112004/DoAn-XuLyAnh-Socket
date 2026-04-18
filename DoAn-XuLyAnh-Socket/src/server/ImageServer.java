import java.net.ServerSocket;
import java.net.Socket;

public class ImageServer {
    private static final int PORT = 5000;

    public static void main(String[] args) {
        LogWindow.log("Khởi động Server xử lý ảnh trên cổng " + PORT + "...");

        // 1. Tạo ServerSocket(port)
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            LogWindow.log("Đang chờ khách kết nối...");

            // 2. Trong vòng lặp while(true)
            while (true) {
                // Khi có khách kết nối
                Socket clientSocket = serverSocket.accept();
                LogWindow.log("Có khách mới kết nối từ IP: " + clientSocket.getInetAddress().getHostAddress());

                // Tạo một Thread mới (ClientHandler) và chạy
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            LogWindow.log("Lỗi Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}