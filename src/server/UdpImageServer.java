package server;

import core.ImageProcessor;
import share.Constants;
import java.net.*;
import java.nio.ByteBuffer;

public class UdpImageServer {
    
    public static void startServer() throws Exception {
        ServerUI.log("Khoi dong UDP Server tren cong " + Constants.SERVER_PORT_UDP);

        // Su dung try-with-resources de quan ly socket an toan
        try (DatagramSocket udpSocket = new DatagramSocket(Constants.SERVER_PORT_UDP)) {
            
            // Mo rong bo dem (Buffer) len 5MB de hung "con mua" goi tin tu Android gui len
            udpSocket.setSendBufferSize(5 * 1024 * 1024);
            udpSocket.setReceiveBufferSize(5 * 1024 * 1024);
            
            // Vong lap vo han lang nghe request cua khach hang
            while (true) {
                try {
                    // ==========================================================
                    // 1. NHAN KHOI TIN HEADER (Dung im cho vo tan)
                    // ==========================================================
                    udpSocket.setSoTimeout(0); 
                    byte[] headerBuffer = new byte[1024];
                    DatagramPacket headerPacket = new DatagramPacket(headerBuffer, headerBuffer.length);
                    udpSocket.receive(headerPacket);
                    
                    String headerMessage = new String(headerPacket.getData(), 0, headerPacket.getLength());
                    String[] headerParts = headerMessage.split(":");
                    
                    if(!headerParts[0].equals("HEADER") || headerParts.length < 5) {
                        continue; // Neu nhan rac thi bo qua, lang nghe tiep
                    }
                    
                    String userId = headerParts[1];
                    int commandCode = Integer.parseInt(headerParts[2]);
                    int expectedImageSize = Integer.parseInt(headerParts[3]);
                    int expectedTotalChunks = Integer.parseInt(headerParts[4]); 
                    
                    InetAddress clientAddress = headerPacket.getAddress();
                    int clientPort = headerPacket.getPort();

                    ServerUI.log("--- BAT DAU PHIEN UDP TU CLIENT: " + userId + " ---");

                    // ==========================================================
                    // 2. NHAN CAC MANH ANH (Phat Timeout sau 300ms)
                    // ==========================================================
                    udpSocket.setSoTimeout(300); 
                    byte[] originalImageData = new byte[expectedImageSize];
                    int currentReceiveOffset = 0;
                    int actualReceivedChunks = 0; 
                    
                    try {
                        for (int i = 0; i < expectedTotalChunks; i++) {
                            byte[] chunkBuffer = new byte[Constants.UDP_CHUNK_SIZE];
                            DatagramPacket chunkPacket = new DatagramPacket(chunkBuffer, chunkBuffer.length);
                            udpSocket.receive(chunkPacket);
                            
                            // Ghep noi manh vao mang chinh
                            System.arraycopy(chunkPacket.getData(), 0, originalImageData, currentReceiveOffset, chunkPacket.getLength());
                            currentReceiveOffset += chunkPacket.getLength();
                            actualReceivedChunks++; 
                        }
                    } catch (SocketTimeoutException timeoutException) {
                        // Bo qua loi, chap nhan viec anh bi thieu manh de di den buoc xu ly tiep theo
                    }

                    // Tinh toan ty le rot goi tin tu Client gui len
                    String packetLossReport = "Da nhan: " + actualReceivedChunks + "/" + expectedTotalChunks + " manh.";
                    if (actualReceivedChunks < expectedTotalChunks) {
                        ServerUI.log("=> CANH BAO UDP: Rot goi tin tren duong len! " + packetLossReport);
                    } else {
                        ServerUI.log("=> UDP: Tuyet voi! " + packetLossReport + " (Mang hoan hao)");
                    }

                    // ==========================================================
                    // 3. XU LY ANH, LUU FILE VA GHI DATABASE
                    // ==========================================================
                    long startTimeMs = System.currentTimeMillis();
                    byte[] processedImageData = null;
                    
                    try {
                        processedImageData = ImageProcessor.processRequest(commandCode, originalImageData);
                    } catch (Exception exception) {
                        ServerUI.log("=> Bep truong tu choi vi anh bi rach nang. Tra nguyen ban loi ve Client!");
                    }

                    // Co che an toan: Neu xu ly that bai, tra luon anh loi ve de Android ve hinh ranh (Datamoshing)
                    if (processedImageData == null) {
                        processedImageData = originalImageData; 
                    }
                    
                    long processingTimeMs = System.currentTimeMillis() - startTimeMs;
                    
                    // Ghi Log ngam de khong anh huong den toc do tra ket qua
                    try {
                        String originalFilePath = FileManager.saveImageToDisk(originalImageData, "original", userId + "_udp_goc");
                        String resultFilePath = FileManager.saveImageToDisk(processedImageData, "result", userId + "_udp_xong");
                        
                        // Ghi chu vao Database so manh bi rot de sau nay de danh gia
                        String protocolLog = "UDP (" + actualReceivedChunks + "/" + expectedTotalChunks + ")";
                        HistoryDAO.saveRecord(userId, commandCode, protocolLog, processingTimeMs, originalFilePath, resultFilePath);
                    } catch (Exception dbException) {
                        ServerUI.log("Loi ghi log UDP (Khong anh huong truyen tai): " + dbException.getMessage());
                    }
                    
                    ServerUI.updateImages(originalImageData, processedImageData);          
                    
                    // ==========================================================
                    // 4. GUI TRA CLIENT (FLOW CONTROL VA DANH INDEX)
                    // ==========================================================
                    int responseTotalChunks = (int) Math.ceil((double) processedImageData.length / Constants.UDP_CHUNK_SIZE);
                    String responseHeader = "RESP:" + processedImageData.length + ":" + responseTotalChunks;
                    byte[] responseHeaderData = responseHeader.getBytes();
                    
                    // Ban vien đan dau tien la Header thong bao dung luong
                    udpSocket.send(new DatagramPacket(responseHeaderData, responseHeaderData.length, clientAddress, clientPort));

                    int currentSendOffset = 0;
                    int simulatedDroppedCount = 0; 
                    
                    // Cap phat bo dem du chua kich thuoc Chunk + 4 bytes de luu so Index
                    ByteBuffer chunkWrapper = ByteBuffer.allocate(Constants.UDP_CHUNK_SIZE + 4);

                    for (int chunkIndex = 0; chunkIndex < responseTotalChunks; chunkIndex++) {
                        int chunkDataLength = Math.min(Constants.UDP_CHUNK_SIZE, processedImageData.length - currentSendOffset);
                        
                        chunkWrapper.clear();
                        chunkWrapper.putInt(chunkIndex); // 1. Danh so thu tu vao dau goi tin
                        chunkWrapper.put(processedImageData, currentSendOffset, chunkDataLength); // 2. Nhet du lieu anh vao sau
                        
                        byte[] finalPayload = new byte[chunkDataLength + 4];
                        System.arraycopy(chunkWrapper.array(), 0, finalPayload, 0, finalPayload.length);
                        
                        // 3. CHIEN THUAT BAO VE THONG TIN COT LOI (DATAMOSHING)
                        // Kiem tra: Neu la 2 goi tin dau tien (chua thong tin header cua JPEG) thi cam tuyet doi khong duoc xoa
                        if (chunkIndex < 2 || Math.random() >= Constants.SIMULATE_DROP_RATE) {
                            udpSocket.send(new DatagramPacket(finalPayload, finalPayload.length, clientAddress, clientPort));
                        } else {
                            simulatedDroppedCount++; 
                        }
                        
                        currentSendOffset += chunkDataLength;
                        
                        // 4. FLOW CONTROL: Ngu 1ms de tranh tinh trang card mang cua Android bi ngop
                        Thread.sleep(1); 
                    }
                    
                    if (simulatedDroppedCount > 0) {
                        ServerUI.log("=> Tra Client: Mo phong mang ruc rac, DA CO TINH DANH ROT " + simulatedDroppedCount + "/" + responseTotalChunks + " manh!\n");
                    } else {
                        ServerUI.log("=> Hoan tat gui tra Client (" + processingTimeMs + "ms) - Mang on dinh.\n");
                    }

                } catch (Exception exception) {
                    ServerUI.log("Loi trong phien giao dich UDP: " + exception.getMessage());
                }
            }
        }
    }
}