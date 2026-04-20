package server;

import core.ImageProcessor;
import share.Constants;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        // Su dung try-with-resources de tu dong dong luong (Stream) khi hoan tat hoac co loi
        try (DataInputStream dataInStream = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream dataOutStream = new DataOutputStream(clientSocket.getOutputStream())) {

            // ==========================================================
            // 1. DOC VA BOC TACH CHUOI HEADER
            // ==========================================================
            // Client se gui chuoi Header thong qua phuong thuc writeUTF()
            String headerMessage = dataInStream.readUTF(); 
            ServerUI.log("Da nhan Packet Header: " + headerMessage);

            // Phan tich chuoi theo cu phap chuan: "HEADER:userId:commandCode:imageSize"
            String[] headerParts = headerMessage.split(":");
            if (headerParts.length < 4 || !headerParts[0].equals("HEADER")) {
                throw new Exception("Sai dinh dang Header. Da ngat ket noi!");
            }

            // Trich xuat cac thong so quan trong
            String userId = headerParts[1];
            int commandCode = Integer.parseInt(headerParts[2]);
            int imageSize = Integer.parseInt(headerParts[3]); 

            // ==========================================================
            // 2. NHAN DU LIEU ANH TU CLIENT (READ BYTES)
            // ==========================================================
            byte[] originalImageData = new byte[imageSize];
            int totalBytesRead = 0;
            
            // Vong lap dam bao doc du 100% dung luong anh moi thoat
            while (totalBytesRead < imageSize) {
                int bytesRead = dataInStream.read(originalImageData, totalBytesRead, imageSize - totalBytesRead);
                if (bytesRead == -1) {
                    throw new Exception("Mat ket noi mang giua chung khi dang nhan anh!");
                }
                totalBytesRead += bytesRead;
            }

            // ==========================================================
            // 3. XU LY ANH, LUU FILE VA GHI NHAN LOG DATABASE
            // ==========================================================
            String originalImagePath = FileManager.saveImageToDisk(originalImageData, "original", userId + "_tcp_goc");
            long startTime = System.currentTimeMillis();

            ServerUI.log("Dang chuyen du lieu vao Core de xu ly...");
            byte[] processedImageData = null;
            
            try {
                processedImageData = ImageProcessor.processRequest(commandCode, originalImageData); 
            } catch (Exception e) {
                ServerUI.log("=> Loi Core TCP: Khong the xu ly anh.");
            }

            // CO CHE BAO VE: Neu bep truong tu choi xu ly (tra ve null), lay luon anh goc tra ve 
            // de dam bao App Android khong bi crash vi thieu du lieu
            if (processedImageData == null) {
                ServerUI.log("=> Canh bao TCP: Anh loi dinh dang. Tra nguyen ban goc ve cho Client!");
                processedImageData = originalImageData; 
            }

            long processingDuration = System.currentTimeMillis() - startTime;
            String resultImagePath = FileManager.saveImageToDisk(processedImageData, "result", userId + "_tcp_xong");

            // Luu lich su vao co so du lieu (SQLite)
            boolean isSavedToDb = HistoryDAO.saveRecord(userId, commandCode, "TCP", processingDuration, originalImagePath, resultImagePath);
            if (isSavedToDb) {
                ServerUI.log("=> Da luu lich su TCP vao Database thanh cong.");
            }
            ServerUI.updateImages(originalImageData, processedImageData);            
            
            // ==========================================================
            // 4. TRUYEN KET QUA VE CLIENT (CO MO PHONG DO TRE MANG)
            // ==========================================================
            // Buoc tu lenh: Gui kich thuoc file tra ve truoc de Client chuan bi bo nho RAM
            dataOutStream.writeInt(processedImageData.length);
            
            // Bam nho du lieu de truyen y het nhu co che UDP
            int maxChunkSize = Constants.UDP_CHUNK_SIZE; 
            int currentOffset = 0;
            int retransmissionCount = 0; 
            int currentPenaltyMs = 200; // An phat thoi gian bat dau tu 200ms

            while (currentOffset < processedImageData.length) {
                int sendLength = Math.min(maxChunkSize, processedImageData.length - currentOffset);
                
                dataOutStream.write(processedImageData, currentOffset, sendLength);
                dataOutStream.flush(); // Ep day du lieu vao card mang ngay lap tuc

                // LOGIC MO PHONG TCP RTO (Retransmission TimeOut)
                if (Constants.SIMULATE_DROP_RATE > 0 && Math.random() < Constants.SIMULATE_DROP_RATE) {
                    retransmissionCount++;
                    try {
                        // Dung hinh (Sleep) de mo phong khoang thoi gian TCP cho ACK
                        Thread.sleep(currentPenaltyMs); 
                        // TCP Congestion Control: Mang cang ruc rac, an phat thoi gian cang tang
                        currentPenaltyMs += 100; 
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                currentOffset += sendLength;
            }

            if (retransmissionCount > 0) {
                ServerUI.log("=> TCP phat hien rot goi " + retransmissionCount + " lan. RTO tang dan lam cham he thong!");
            }
            ServerUI.log("Da xu ly xong va gui tra Client " + processedImageData.length + " bytes.\n");
            
        } catch (Exception e) {
            ServerUI.log("Loi o luong xu ly Client (TCP): " + e.getMessage());
        } finally {
            try {
                // Dam bao Socket luon duoc don dep du co loi hay khong
                clientSocket.close();
            } catch (Exception e) {
                // Bo qua loi khi dong socket
            }
        }
    }
}