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
                    // 1. Chờ nhận Header (Chờ vô tận không timeout)
                    socket.setSoTimeout(0); 
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    String[] headerInfo = new String(packet.getData(), 0, packet.getLength()).split(":");
                    if(!headerInfo[0].equals("HEADER")) continue;
                    
                    int cmd = Integer.parseInt(headerInfo[1]);
                    int imgLen = Integer.parseInt(headerInfo[2]);
                    int totalChunks = Integer.parseInt(headerInfo[3]);
                    InetAddress clientAddr = packet.getAddress();
                    int clientPort = packet.getPort();

                    LogWindow.log("UDP: Nhận yêu cầu cmd " + cmd + ". Đang chờ " + totalChunks + " mảnh...");

                    // 2. Nhận các mảnh (Cài timeout 3 giây, nếu 3s không nhận được mảnh -> rớt mạng -> Hủy)
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

                    // 3. Xử lý ảnh
                    byte[] resultData = ImageProcessor.processRequest(cmd, imgData);
                    if (resultData == null) throw new Exception("Core không xử lý được ảnh bị vỡ.");
                    
                    // 4. Gửi ngược lại cho Client
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