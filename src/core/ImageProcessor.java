package core;

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

    // =====================================================================
    // KHOI DOC GHI DU LIEU CO BAN
    // =====================================================================
    
    public static BufferedImage readImage(byte[] imageData) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData)) {
            return ImageIO.read(inputStream);
        }
    }

    public static byte[] writeImage(BufferedImage sourceImage, String formatType) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(sourceImage, formatType, outputStream);
            return outputStream.toByteArray();
        }
    }

    // =====================================================================
    // HAM EP CAN JPEG (DAC NHIEM CHO UDP)
    // =====================================================================
    // Bien qualityRate dung de dieu chinh muc do nen anh (tu 0.0 den 1.0)
    public static byte[] saveAsJpeg(BufferedImage sourceImage, float qualityRate) throws IOException {
        // Chuyen doi sang chuan RGB de tranh loi khi luu anh co kenh Alpha (trong suot)
        BufferedImage rgbFormatImage = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgbFormatImage.createGraphics();
        graphics.drawImage(sourceImage, 0, 0, Color.WHITE, null);
        graphics.dispose();

        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(byteOutputStream);
        ImageWriter jpegWriter = ImageIO.getImageWritersByFormatName("jpg").next();
        jpegWriter.setOutput(imageOutputStream);

        ImageWriteParam writeParameters = jpegWriter.getDefaultWriteParam();
        if (writeParameters.canWriteCompressed()) {
            writeParameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParameters.setCompressionQuality(qualityRate); 
        }

        jpegWriter.write(null, new IIOImage(rgbFormatImage, null, null), writeParameters);
        jpegWriter.dispose();
        imageOutputStream.close();
        
        return byteOutputStream.toByteArray();
    }

    // =====================================================================
    // BO DINH TUYEN XU LY YEU CAU (MAIN CONTROLLER)
    // =====================================================================
    
    public static byte[] processRequest(int commandCode, byte[] inputImageData) {
        try {
            BufferedImage sourceImage = readImage(inputImageData);
            if (sourceImage == null) {
                return null;
            }

            // Chuc nang nen anh (1) thi ep dung luong xuong muc thap nhat
            if (commandCode == 1) { 
                return saveAsJpeg(sourceImage, 0.10f); 
            }

            BufferedImage resultImage = sourceImage;
            switch (commandCode) {
                case 2: resultImage = zoomIn(sourceImage); break;
                case 3: resultImage = zoomOut(sourceImage); break;
                case 4: resultImage = rotate(sourceImage); break;
                case 5: resultImage = toGrayscale(sourceImage); break;
                case 6: resultImage = invertColors(sourceImage); break;
                case 7: resultImage = blur(sourceImage); break;
                case 8: resultImage = colorSplash(sourceImage); break;
                case 9: resultImage = sepia(sourceImage); break;
                case 10: resultImage = pencilSketch(sourceImage); break;
                default: break; // Truong hop khong xac dinh thi giu nguyen anh goc
            }
            
            // CHIEN THUAT TOI UU DUNG LUONG BANG BANG THONG:
            float currentQuality = 0.75f; // Mac dinh nen 75%
            
            // Cac bo loc tao nhieu (8, 9, 10) co the nen xuong 40% ma mat thuong van thay net
            // Viec nay giup UDP giam duoc mot nua so luong goi tin bi rot
            if (commandCode == 8 || commandCode == 9 || commandCode == 10) {
                currentQuality = 0.40f; 
            }

            return saveAsJpeg(resultImage, currentQuality); 
            
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static byte[] compressImage(BufferedImage sourceImage) throws IOException {
        // Bao cho ham saveAsJpeg: "Hay ep dung luong xuong con 10%"
        return saveAsJpeg(sourceImage, 0.1f); 
    }

    // =====================================================================
    // DAN THUAT TOAN XU LY ANH CO BAN
    // =====================================================================

    public static BufferedImage toGrayscale(BufferedImage sourceImage) {
        BufferedImage grayImage = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = grayImage.createGraphics();
        graphics.drawImage(sourceImage, 0, 0, null);
        graphics.dispose();
        return grayImage;
    }

    public static BufferedImage invertColors(BufferedImage sourceImage) {
        int imageWidth = sourceImage.getWidth();
        int imageHeight = sourceImage.getHeight();
        BufferedImage invertedImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                int pixelRgba = sourceImage.getRGB(x, y);
                int alpha = (pixelRgba >> 24) & 0xff;
                int red = 255 - ((pixelRgba >> 16) & 0xff);
                int green = 255 - ((pixelRgba >> 8) & 0xff);
                int blue = 255 - (pixelRgba & 0xff);
                
                invertedImage.setRGB(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
            }
        }
        return invertedImage;
    }

    // Zoom In: Cat lay phan trung tam roi phong to len (Giu nguyen kich thuoc khung hinh)
    public static BufferedImage zoomIn(BufferedImage sourceImage) {
        int imageWidth = sourceImage.getWidth();
        int imageHeight = sourceImage.getHeight();
        
        BufferedImage croppedCenter = sourceImage.getSubimage(imageWidth / 4, imageHeight / 4, imageWidth / 2, imageHeight / 2);
        Image scaledImage = croppedCenter.getScaledInstance(imageWidth, imageHeight, Image.SCALE_SMOOTH);
        
        BufferedImage zoomedImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = zoomedImage.createGraphics();
        graphics.drawImage(scaledImage, 0, 0, null);
        graphics.dispose();
        return zoomedImage;
    }

    // Zoom Out: Bop nho anh lai va dat vao nen den (Giu nguyen kich thuoc khung hinh)
    public static BufferedImage zoomOut(BufferedImage sourceImage) {
        int imageWidth = sourceImage.getWidth();
        int imageHeight = sourceImage.getHeight();
        
        Image scaledImage = sourceImage.getScaledInstance(imageWidth / 2, imageHeight / 2, Image.SCALE_SMOOTH);
        BufferedImage zoomedOutImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        
        Graphics2D graphics = zoomedOutImage.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, imageWidth, imageHeight);
        graphics.drawImage(scaledImage, imageWidth / 4, imageHeight / 4, null);
        graphics.dispose();
        
        return zoomedOutImage;
    }

    public static BufferedImage rotate(BufferedImage sourceImage) {
        int imageWidth = sourceImage.getWidth();
        int imageHeight = sourceImage.getHeight();
        
        // Dao nguoc chieu dai va chieu rong cho khung hinh moi
        BufferedImage rotatedImage = new BufferedImage(imageHeight, imageWidth, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = rotatedImage.createGraphics();
        
        graphics.translate(imageHeight, 0); 
        graphics.rotate(Math.PI / 2); 
        graphics.drawImage(sourceImage, 0, 0, null);
        graphics.dispose();
        
        return rotatedImage;
    }

    // Blur: Lam mo anh bang ma tran tich chap (Convolution Matrix) 5x5
    public static BufferedImage blur(BufferedImage sourceImage) {
        int matrixSize = 25; // 5x5
        float blurWeight = 1.0f / matrixSize;
        float[] blurData = new float[matrixSize];
        
        for (int i = 0; i < matrixSize; i++) {
            blurData[i] = blurWeight;
        }
        
        Kernel blurKernel = new Kernel(5, 5, blurData);
        ConvolveOp blurOperation = new ConvolveOp(blurKernel, ConvolveOp.EDGE_NO_OP, null);
        
        BufferedImage tempImage = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = tempImage.createGraphics();
        graphics.drawImage(sourceImage, 0, 0, null);
        graphics.dispose();
        
        return blurOperation.filter(tempImage, null);
    }

    // =====================================================================
    // DAN THUAT TOAN XU LY ANH NANG CAO (TON CPU)
    // =====================================================================

    public static BufferedImage colorSplash(BufferedImage sourceImage) {
        int imageWidth = sourceImage.getWidth();
        int imageHeight = sourceImage.getHeight();
        BufferedImage splashImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                int currentPixel = sourceImage.getRGB(x, y);
                Color pixelColor = new Color(currentPixel);
                float[] hueSatBright = Color.RGBtoHSB(pixelColor.getRed(), pixelColor.getGreen(), pixelColor.getBlue(), null);
                
                // Giu lai mau do (Hue nam o hai dau quang pho)
                if (hueSatBright[0] < 0.08f || hueSatBright[0] > 0.92f) {
                    splashImage.setRGB(x, y, currentPixel); 
                } else {
                    // Cac mau khac chuyen thanh trang den (Grayscale)
                    int grayValue = (int) (0.299 * pixelColor.getRed() + 0.587 * pixelColor.getGreen() + 0.114 * pixelColor.getBlue());
                    splashImage.setRGB(x, y, new Color(grayValue, grayValue, grayValue).getRGB());
                }
            }
        }
        return splashImage;
    }

    public static BufferedImage sepia(BufferedImage sourceImage) {
        int imageWidth = sourceImage.getWidth();
        int imageHeight = sourceImage.getHeight();
        BufferedImage sepiaImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                Color pixelColor = new Color(sourceImage.getRGB(x, y));
                int currentRed = pixelColor.getRed();
                int currentGreen = pixelColor.getGreen();
                int currentBlue = pixelColor.getBlue();
                
                // Cong thuc tinh mau Sepia co dien
                int targetRed = (int) (0.393 * currentRed + 0.769 * currentGreen + 0.189 * currentBlue);
                int targetGreen = (int) (0.349 * currentRed + 0.686 * currentGreen + 0.168 * currentBlue);
                int targetBlue = (int) (0.272 * currentRed + 0.534 * currentGreen + 0.131 * currentBlue);
                
                // Dam bao gia tri khong vuot qua 255
                sepiaImage.setRGB(x, y, new Color(Math.min(255, targetRed), Math.min(255, targetGreen), Math.min(255, targetBlue)).getRGB());
            }
        }
        return sepiaImage;
    }

    // Pencil Sketch: Thuat toan tim bien (Edge Detection) dua tren do sang
    public static BufferedImage pencilSketch(BufferedImage sourceImage) {
        int imageWidth = sourceImage.getWidth();
        int imageHeight = sourceImage.getHeight();
        BufferedImage sketchImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < imageHeight - 1; y++) {
            for (int x = 0; x < imageWidth - 1; x++) {
                int pixel1 = sourceImage.getRGB(x, y);
                int pixel2 = sourceImage.getRGB(x + 1, y + 1); 
                
                // Tinh do sang (Luminance) cua 2 pixel ke tiep nhau
                int luminance1 = (((pixel1 >> 16) & 0xFF) * 77 + ((pixel1 >> 8) & 0xFF) * 150 + (pixel1 & 0xFF) * 29) >> 8;
                int luminance2 = (((pixel2 >> 16) & 0xFF) * 77 + ((pixel2 >> 8) & 0xFF) * 150 + (pixel2 & 0xFF) * 29) >> 8;
                
                // Tinh do lech sang de ve net but chi
                int colorValue = 255 - Math.min(255, Math.abs(luminance1 - luminance2) * 6); 
                sketchImage.setRGB(x, y, (255 << 24) | (colorValue << 16) | (colorValue << 8) | colorValue);
            }
        }
        return sketchImage;
    }
}