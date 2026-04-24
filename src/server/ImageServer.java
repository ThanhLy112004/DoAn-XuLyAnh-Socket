package server;

import core.LogWindow;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import share.Constants;

public class ImageServer {
    // Tối ưu Đa luồng: Pool giới hạn 10 luồng xử lý đồng thời
    private static final ExecutorService tcpPool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        // Khởi chạy UDP Server song song ngầm
        HistoryDAO.initializeDatabase();
        
        new Thread(() -> {
            try {
                UdpImageServer.startServer();
            } catch (Exception e) {
                LogWindow.log("Lỗi UDP Server: " + e.getMessage());
            }
        }).start();

        // Khởi chạy TCP Server ở luồng chính
        LogWindow.log("Khởi động TCP Server cổng " + Constants.SERVER_PORT_TCP);
        try (ServerSocket serverSocket = new ServerSocket(Constants.SERVER_PORT_TCP)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                LogWindow.log("TCP: Có khách kết nối từ " + clientSocket.getInetAddress().getHostAddress());
                
                // Giao việc cho ThreadPool thay vì tạo Thread mới
                tcpPool.execute(new ClientHandler(clientSocket));
            }
        } catch (Exception e) {
            LogWindow.log("Lỗi TCP Server: " + e.getMessage());
        }
    }
}