package server;

import java.io.File;
import java.io.FileOutputStream;

public class FileManager {
    // Thu muc goc luu tru du lieu cua Server tren o cung
    private static final String BASE_DIRECTORY = "ServerData";

    /**
     * Ham luu mang byte anh xuong o cung vat ly
     * @param imageData Mang byte cua anh can luu
     * @param folderName Ten thu muc con (Vi du: "original" hoac "result")
     * @param filePrefix Tien to ten file (Vi du: "SV001_goc")
     * @return Duong dan tuyet doi cua file da luu, hoac null neu luu that bai
     */
    public static String saveImageToDisk(byte[] imageData, String folderName, String filePrefix) {
        try {
            // 1. Tao thu muc goc (ServerData) neu chua ton tai
            File baseDirectory = new File(BASE_DIRECTORY);
            if (!baseDirectory.exists()) {
                baseDirectory.mkdirs();
            }

            // 2. Tao thu muc con (Vi du: ServerData/original hoac ServerData/result)
            File subDirectory = new File(baseDirectory, folderName);
            if (!subDirectory.exists()) {
                subDirectory.mkdirs();
            }

            // 3. Tao ten file duy nhat (Dung Timestamp de nhieu Client gui cung luc khong bi de file)
            String fileName = filePrefix + "_" + System.currentTimeMillis() + ".png";
            File imageFile = new File(subDirectory, fileName);

            // 4. Ghi mang byte ra thanh file anh tren o cung
            // Su dung try-with-resources de tu dong giai phong bo nho luong ghi
            try (FileOutputStream fileOutputStream = new FileOutputStream(imageFile)) {
                fileOutputStream.write(imageData);
                fileOutputStream.flush(); // Ep du lieu ghi thang vao o dia
            }

            // 5. Tra ve duong dan tuyet doi de ClientHandler lay mang di luu Database
            return imageFile.getAbsolutePath();

        } catch (Exception exception) {
            // Ghi nhan loi vao bang dieu khien cua Server thay vi chi in ra Console an
            ServerUI.log("Loi khi luu anh vao dia (" + folderName + "): " + exception.getMessage());
            return null; 
        }
    }
}