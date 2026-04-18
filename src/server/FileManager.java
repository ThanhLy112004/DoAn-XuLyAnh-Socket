package server;

import core.LogWindow;
import java.io.File;
import java.io.FileOutputStream;

public class FileManager {
    // Thư mục gốc lưu trữ dữ liệu của Server
    private static final String BASE_DIR = "ServerData";

    /**
     * Hàm lưu mảng byte ảnh xuống ổ cứng vật lý
     * @param imgData mảng byte của ảnh
     * @param folderName tên thư mục con ("original" hoặc "result")
     * @param prefix tiền tố tên file (ví dụ: "SV001_goc")
     * @return Đường dẫn tuyệt đối của file đã lưu, hoặc null nếu lưu thất bại
     */
    public static String saveImageToDisk(byte[] imgData, String folderName, String prefix) {
        try {
            // 1. Tạo thư mục gốc (ServerData) nếu chưa tồn tại
            File baseDir = new File(BASE_DIR);
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }

            // 2. Tạo thư mục con (ServerData/original hoặc ServerData/result)
            File subDir = new File(baseDir, folderName);
            if (!subDir.exists()) {
                subDir.mkdirs();
            }

            // 3. Tạo tên file duy nhất (để nhiều người gửi cùng lúc không bị đè file)
            String fileName = prefix + "_" + System.currentTimeMillis() + ".png";
            File imageFile = new File(subDir, fileName);

            // 4. Ghi mảng byte ra thành file ảnh .png trên ổ cứng
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                fos.write(imgData);
                fos.flush();
            }

            // 5. Trả về đường dẫn tuyệt đối để ClientHandler lấy mang đi lưu Database
            return imageFile.getAbsolutePath();

        } catch (Exception e) {
            LogWindow.log("Lỗi khi lưu ảnh vào đĩa (" + folderName + "): " + e.getMessage());
            return null; // Báo lỗi cho hàm gọi nó
        }
    }
}