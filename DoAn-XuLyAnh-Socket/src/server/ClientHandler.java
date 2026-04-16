import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
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

            // 3. Đọc dữ liệu ảnh
            byte[] buffer = new byte[L];
            int totalBytesRead = 0;
            while (totalBytesRead < L) {
                int bytesRead = dis.read(buffer, totalBytesRead, L - totalBytesRead);
                if (bytesRead == -1) {
                    throw new Exception("Mất kết nối giữa chừng khi đang đọc ảnh!");
                }
                totalBytesRead += bytesRead;
            }

            // 4. Dịch mảng byte thành ảnh
            BufferedImage originalImage = ImageProcessor.readImage(buffer);
            if (originalImage == null) {
                throw new Exception("Dữ liệu nhận được không thể giải mã thành ảnh.");
            }

            // Thực hiện chức năng
            BufferedImage processedImage = applyCommand(originalImage, commandCode);

            // Đóng gói ảnh thành mảng byte trả về
            byte[] resultData = ImageProcessor.writeImage(processedImage, "png");

            // 5. Gửi kết quả về Client
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

    // Hàm phụ trợ để điều phối các chức năng
    private BufferedImage applyCommand(BufferedImage img, byte command) {
        int w = img.getWidth();
        int h = img.getHeight();

        // Chuẩn hóa ảnh về ARGB để tránh lỗi hệ màu khi xử lý (đặc biệt là xoay và làm mờ)
        BufferedImage convertedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dInit = convertedImg.createGraphics();
        g2dInit.drawImage(img, 0, 0, null);
        g2dInit.dispose();
        img = convertedImg;

        switch (command) {
            case 1: // Nén ảnh
                BufferedImage compressed = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED);
                Graphics2D gComp = compressed.createGraphics();
                gComp.drawImage(img, 0, 0, null);
                gComp.dispose();
                return compressed;
                
            case 2: // Phóng to (Cắt vùng trung tâm 80% rồi phóng to lên kích thước cũ)
                int zoomInW = (int)(w * 0.8);
                int zoomInH = (int)(h * 0.8);
                int x = (w - zoomInW) / 2;
                int y = (h - zoomInH) / 2;
                BufferedImage cropped = img.getSubimage(x, y, zoomInW, zoomInH);
                return ImageProcessor.resize(cropped, w, h);
                
            case 3: // Thu nhỏ (Tạo nền trong suốt, vẽ ảnh nhỏ lại 80% ở giữa)
                BufferedImage zoomOutImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2dOut = zoomOutImg.createGraphics();
                int smallW = (int)(w * 0.8);
                int smallH = (int)(h * 0.8);
                g2dOut.drawImage(img, (w - smallW) / 2, (h - smallH) / 2, smallW, smallH, null);
                g2dOut.dispose();
                return zoomOutImg;
                
            case 4: // Xoay ảnh 90 độ (Sửa lại logic tịnh tiến tọa độ chuẩn)
                BufferedImage rotated = new BufferedImage(h, w, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2dRot = rotated.createGraphics();
                g2dRot.translate(h, 0); 
                g2dRot.rotate(Math.PI / 2); 
                g2dRot.drawImage(img, 0, 0, null);
                g2dRot.dispose();
                return rotated;
                
            case 5: // Đen trắng
                return ImageProcessor.toGrayscale(img);
                
            case 6: // Đảo màu
                return ImageProcessor.invertColors(img);
                
            case 7: // Làm mờ che mặt (Hiệu ứng Pixelate / Mosaic)
                int pixelSize = 15; 
                
                int wOriginal = img.getWidth();
                int hOriginal = img.getHeight();

                // Bước 1: Thu nhỏ ảnh (Đã đổi tên biến thành mosaicW và mosaicH để không bị trùng)
                int mosaicW = Math.max(1, wOriginal / pixelSize);
                int mosaicH = Math.max(1, hOriginal / pixelSize);
                BufferedImage smallImg = new BufferedImage(mosaicW, mosaicH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gSmall = smallImg.createGraphics();
                gSmall.drawImage(img, 0, 0, mosaicW, mosaicH, null);
                gSmall.dispose();

                // Bước 2: Phóng to lại bằng Nearest Neighbor để lộ ra các khối pixel vuông
                BufferedImage pixelatedImg = new BufferedImage(wOriginal, hOriginal, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gPixel = pixelatedImg.createGraphics();
                gPixel.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                gPixel.drawImage(smallImg, 0, 0, wOriginal, hOriginal, null);
                gPixel.dispose();

                return pixelatedImg;
                
            default:
                LogWindow.log("Mã lệnh không xác định: " + command);
                return img;
        }
    }
}