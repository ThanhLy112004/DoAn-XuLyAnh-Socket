package server;

import core.ImageProcessor;
import share.Constants;

import java.awt.image.BufferedImage;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageServer {
    
    // Toi uu Da luong: Quan ly toi da 10 luong TCP xu ly dong thoi tranh tran bo nho RAM
    private static final ExecutorService tcpThreadPool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        // ==========================================================
        // 1. KHOI CHAY CO SO DU LIEU VA GIAO DIEN (UI)
        // ==========================================================
        HistoryDAO.initializeDatabase();
        ServerUI.startUI();
        
        // ==========================================================
        // 2. KHOI DONG NONG MAY AO JAVA (JVM WARM-UP)
        // ==========================================================
        ServerUI.log("Dang Warm-up JVM (Khoi dong nong CPU)...");
        try {
            // Tao mot anh sieu nho (10x10) de Warm-up cuc nhanh ma khong nghen RAM
            BufferedImage dummyImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            
            // Chay luot qua toan bo cac thuat toan de JIT Compiler bien dich san ra ma may
            ImageProcessor.compressImage(dummyImage);
            ImageProcessor.zoomIn(dummyImage);
            ImageProcessor.zoomOut(dummyImage);
            ImageProcessor.rotate(dummyImage);
            ImageProcessor.toGrayscale(dummyImage);
            ImageProcessor.invertColors(dummyImage);
            ImageProcessor.blur(dummyImage);
            ImageProcessor.colorSplash(dummyImage);
            ImageProcessor.sepia(dummyImage);
            ImageProcessor.pencilSketch(dummyImage);
            
            ServerUI.log("=> JVM da khoi dong xong toan dien. San sang chay toc do toi da!");
        } catch (Exception exception) {
            ServerUI.log("=> Canh bao Warm-up: " + exception.getMessage());
        }

        // ==========================================================
        // 3. KHOI CHAY MAY CHU UDP (CHAY NGAM SONG SONG)
        // ==========================================================
        new Thread(() -> {
            try {
                UdpImageServer.startServer();
            } catch (Exception exception) {
                ServerUI.log("Loi UDP Server: " + exception.getMessage()); 
            }
        }).start();

        // ==========================================================
        // 4. KHOI CHAY MAY CHU TCP (LUONG CHINH)
        // ==========================================================
        ServerUI.log("Khoi dong TCP Server tren cong " + Constants.SERVER_PORT_TCP); 
        try (ServerSocket tcpServerSocket = new ServerSocket(Constants.SERVER_PORT_TCP)) {
            
            // Vong lap vo han de don khach lien tuc
            while (true) {
                Socket tcpClientSocket = tcpServerSocket.accept();
                ServerUI.log("TCP: Co khach ket noi tu " + tcpClientSocket.getInetAddress().getHostAddress()); 
                
                // Giao viec xu ly cho ThreadPool thay vi tao Thread moi de toi uu tai nguyen
                tcpThreadPool.execute(new ClientHandler(tcpClientSocket));
            }
            
        } catch (Exception exception) {
            ServerUI.log("Loi TCP Server: " + exception.getMessage()); 
        }
    }
}