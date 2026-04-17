import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.*;

public class ImageProcessor {

    public static BufferedImage readImage(byte[] data) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            return ImageIO.read(bais);
        }
    }

    public static byte[] writeImage(BufferedImage img, String format) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, format, baos);
            return baos.toByteArray();
        }
    }

    // Hàm giao tiếp chuẩn để Server gọi vào (như yêu cầu trong README)
    public static byte[] processRequest(int command, byte[] inputImageData) {
        try {
            BufferedImage img = readImage(inputImageData);
            if (img == null) return null;

            if (command == 1) { // 1. Nén ảnh (Compress)
                return compressImage(img);
            }

            BufferedImage result = img;
            switch (command) {
                case 2: // 2. Phóng to
                    result = resize(img, img.getWidth() * 2, img.getHeight() * 2);
                    break;
                case 3: // 3. Thu nhỏ
                    result = resize(img, Math.max(1, img.getWidth() / 2), Math.max(1, img.getHeight() / 2));
                    break;
                case 4: // 4. Xoay ảnh (90 độ)
                    result = rotate(img);
                    break;
                case 5: // 5. Đen trắng
                    result = toGrayscale(img);
                    break;
                case 6: // 6. Đảo màu
                    result = invertColors(img);
                    break;
                case 7: // 7. Làm mờ
                    result = blur(img);
                    break;
                case 8: // 8. Color Splash (Giữ tone Đỏ/Hồng)
                    result = colorSplash(img);
                    break;
                case 9: // 9. Màu phim hoài cổ (Vintage Sepia)
                    result = sepia(img);
                    break;
                case 10: // 10. Phác họa Bút chì (Pencil Sketch)
                    result = pencilSketch(img);
                    break;
            }
            return writeImage(result, "png"); // Mặc định trả về định dạng png để giữ nguyên chất lượng sau xử lý
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] compressImage(BufferedImage src) throws IOException {
        BufferedImage rgbImage = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgbImage.createGraphics();
        g.drawImage(src, 0, 0, Color.WHITE, null);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        writer.setOutput(ios);

        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.1f);
        }

        writer.write(null, new IIOImage(rgbImage, null, null), param);
        writer.dispose();
        ios.close();
        return baos.toByteArray();
    }

    public static BufferedImage toGrayscale(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = dst.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return dst;
    }

    public static BufferedImage invertColors(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int rgba = src.getRGB(x, y);
                int a = (rgba >> 24) & 0xff;
                int r = 255 - ((rgba >> 16) & 0xff);
                int g = 255 - ((rgba >> 8) & 0xff);
                int b = 255 - (rgba & 0xff);
                int nrgb = (a << 24) | (r << 16) | (g << 8) | b;
                dst.setRGB(x, y, nrgb);
            }
        }
        return dst;
    }

    public static BufferedImage resize(BufferedImage src, int targetW, int targetH) {
        Image scaled = src.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
        BufferedImage dst = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dst.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();
        return dst;
    }

    public static BufferedImage rotate(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dest = new BufferedImage(h, w, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dest.createGraphics();
        g2.translate(h, 0); 
        g2.rotate(Math.PI / 2); 
        g2.drawImage(src, 0, 0, null);
        g2.dispose();
        return dest;
    }

    public static BufferedImage blur(BufferedImage src) {
        float weight = 1.0f / 9.0f;
        float[] data = new float[9];
        for (int i = 0; i < 9; i++) {
            data[i] = weight;
        }
        Kernel kernel = new Kernel(3, 3, data);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);

        BufferedImage dest = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dest.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();

        return op.filter(dest, null);
    }

    // =====================================================================
    // 3 TÍNH NĂNG NÂNG CAO MỚI THÊM
    // =====================================================================

    // 8. Color Splash (Giữ lại tone Đỏ/Hồng)
    public static BufferedImage colorSplash(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int rgb = src.getRGB(x, y);
                Color c = new Color(rgb);
                
                float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
                
                if (hsb[0] < 0.08f || hsb[0] > 0.92f) {
                    dst.setRGB(x, y, rgb); 
                } else {
                    int gray = (int) (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue());
                    dst.setRGB(x, y, new Color(gray, gray, gray).getRGB());
                }
            }
        }
        return dst;
    }

    // 9. Hiệu ứng Phim cũ (Sepia)
    public static BufferedImage sepia(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                Color c = new Color(src.getRGB(x, y));
                int r = c.getRed();
                int g = c.getGreen();
                int b = c.getBlue();

                int tr = (int) (0.393 * r + 0.769 * g + 0.189 * b);
                int tg = (int) (0.349 * r + 0.686 * g + 0.168 * b);
                int tb = (int) (0.272 * r + 0.534 * g + 0.131 * b);

                r = Math.min(255, tr);
                g = Math.min(255, tg);
                b = Math.min(255, tb);

                dst.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        return dst;
    }

    // 10. Hiệu ứng Tranh vẽ phác thảo (Pencil Sketch)
    public static BufferedImage pencilSketch(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h - 1; y++) {
            for (int x = 0; x < w - 1; x++) {
                int p1 = src.getRGB(x, y);
                int p2 = src.getRGB(x + 1, y + 1); 

                int lum1 = (((p1 >> 16) & 0xFF) * 77 + ((p1 >> 8) & 0xFF) * 150 + (p1 & 0xFF) * 29) >> 8;
                int lum2 = (((p2 >> 16) & 0xFF) * 77 + ((p2 >> 8) & 0xFF) * 150 + (p2 & 0xFF) * 29) >> 8;

                int diff = Math.abs(lum1 - lum2);
                int val = 255 - Math.min(255, diff * 6); 
                
                dst.setRGB(x, y, (255 << 24) | (val << 16) | (val << 8) | val);
            }
        }
        return dst;
    }

    // =====================================================================

    // Convenience: process by name
    public static BufferedImage process(BufferedImage src, String operation) {
        switch (operation.toLowerCase()) {
            case "grayscale":
            case "gray":
                return toGrayscale(src);
            case "invert":
                return invertColors(src);
            default:
                return src;
        }
    }

    // Quick CLI demo
    public static void main(String[] args) {
        File inDir = new File("test_images/input");
        File outDir = new File("test_images/output");
        outDir.mkdirs();
        File[] files = inDir.listFiles((d, name) -> {
            String l = name.toLowerCase();
            return l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".png") || l.endsWith(".bmp");
        });
        if (files == null || files.length == 0) {
            System.out.println("No input images found in test_images/input");
            return;
        }
        File src = files[0];
        System.out.println("Processing: " + src.getName());
        try {
            BufferedImage img = ImageIO.read(src);
            
            // Test thử 3 tính năng mới ngay tại đây
            BufferedImage splash = colorSplash(img);
            BufferedImage vintage = sepia(img);
            BufferedImage sketch = pencilSketch(img);

            ImageIO.write(splash, "png", new File(outDir, "splash_" + src.getName() + ".png"));
            ImageIO.write(vintage, "png", new File(outDir, "vintage_" + src.getName() + ".png"));
            ImageIO.write(sketch, "png", new File(outDir, "sketch_" + src.getName() + ".png"));

            System.out.println("Wrote outputs to test_images/output. Hãy mở ra xem độ WOW nhé!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}