package server;

import core.LogWindow;
import core.ImageProcessor;
import share.Constants;
import java.net.*;

public class UdpImageServer {
    public static void startServer() throws Exception {
        LogWindow.log("Khởi động UDP Server cổng " + Constants.SERVER_PORT_UDP);

        try (DatagramSocket socket = new DatagramSocket(Constants.SERVER_PORT_UDP)) {
            socket.setSendBufferSize(5 * 1024 * 1024);
            socket.setReceiveBufferSize(5 * 1024 * 1024);
            while (true) {
                try {
                    // ==========================================================
                    // 1. CHỜ NHẬN VÀ CẮT HEADER (Chờ vô tận không timeout)
                    // ==========================================================
                    socket.setSoTimeout(0); 
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    String headerStr = new String(packet.getData(), 0, packet.getLength());
                    String[] headerInfo = headerStr.split(":");
                    
                    // Quy ước UDP: HEADER:UserId:Cmd:ImgLen:TotalChunks
                    if(!headerInfo[0].equals("HEADER") || headerInfo.length < 5) {
                        continue; // Bỏ qua gói tin rác
                    }
                    
                    String userId = headerInfo[1];
                    int cmd = Integer.parseInt(headerInfo[2]);
                    int imgLen = Integer.parseInt(headerInfo[3]);
                    int totalChunks = Integer.parseInt(headerInfo[4]);
                    InetAddress clientAddr = packet.getAddress();
                    int clientPort = packet.getPort();

                    LogWindow.log("UDP: Nhận yêu cầu từ " + userId + " (cmd: " + cmd + "). Chờ " + totalChunks + " mảnh...");

                    // ==========================================================
                    // 2. NHẬN CÁC MẢNH ẢNH (Timeout 3s)
                    // ==========================================================
                    socket.setSoTimeout(3000); 
                    byte[] imgData = new byte[imgLen];
                    int offset = 0;
                    for (int i = 0; i < totalChunks; i++) {
                        byte[] chunkBuffer = new byte[Constants.UDP_CHUNK_SIZE];
                        DatagramPacket chunkPacket = new DatagramPacket(chunkBuffer, chunkBuffer.length);
                        socket.receive(chunkPacket);
                        System.arraycopy(chunkPacket.getData(), 0, imgData, offset, chunkPacket.getLength());
                        offset += chunkPacket.getLength();
                    }

                    // ==========================================================
                    // 3. LƯU Ổ CỨNG, XỬ LÝ VÀ GHI DATABASE
                    // ==========================================================
                    // Lưu ảnh gốc
                    String originalPath = FileManager.saveImageToDisk(imgData, "original", userId + "_udp_goc");
                    
                    // Bấm giờ xử lý
                    long startTime = System.currentTimeMillis();
                    byte[] resultData = ImageProcessor.processRequest(cmd, imgData);
                    if (resultData == null) throw new Exception("Core không xử lý được ảnh bị vỡ.");
                    long processTime = System.currentTimeMillis() - startTime;
                    
                    // Lưu ảnh kết quả
                    String resultPath = FileManager.saveImageToDisk(resultData, "result", userId + "_udp_xong");
                    
                    // Ghi Database
                    boolean dbSuccess = HistoryDAO.saveRecord(userId, cmd, "UDP", processTime, originalPath, resultPath);
                    if (dbSuccess) {
                        LogWindow.log("=> UDP: Đã lưu lịch sử vào Database thành công.");
                    }

                    // ==========================================================
                    // 4. GỬI KẾT QUẢ TRẢ LẠI CLIENT
                    // ==========================================================
                    int resultTotalChunks = (int) Math.ceil((double) resultData.length / Constants.UDP_CHUNK_SIZE);
                    String respHeader = "RESP:" + resultData.length + ":" + resultTotalChunks;
                    byte[] respHdrData = respHeader.getBytes();
                    socket.send(new DatagramPacket(respHdrData, respHdrData.length, clientAddr, clientPort));

                    int resOffset = 0;
                    for (int i = 0; i < resultTotalChunks; i++) {
                        int length = Math.min(Constants.UDP_CHUNK_SIZE, resultData.length - resOffset);
                        byte[] chunk = new byte[length];
                        System.arraycopy(resultData, resOffset, chunk, 0, length);
                        socket.send(new DatagramPacket(chunk, length, clientAddr, clientPort));
                        resOffset += length;
                    }
                    
                    LogWindow.log("UDP: Đã xử lý và trả về " + resultData.length + " bytes.");
                    
                } catch (SocketTimeoutException timeout) {
                    LogWindow.log("UDP Lỗi: Khách hàng bị rớt mạng giữa chừng, không nhận đủ mảnh!");
                } catch (Exception ex) {
                    LogWindow.log("Lỗi xử lý UDP: " + ex.getMessage());
                }
            }
        }
    }
}