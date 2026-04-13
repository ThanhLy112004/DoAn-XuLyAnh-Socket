import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
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

	// Quick CLI demo: reads first image in test_images/input and writes examples to test_images/output
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
			BufferedImage gray = toGrayscale(img);
			BufferedImage inv = invertColors(img);
			BufferedImage small = resize(img, Math.max(1, img.getWidth()/2), Math.max(1, img.getHeight()/2));

			ImageIO.write(gray, "png", new File(outDir, "gray_" + src.getName() + ".png"));
			ImageIO.write(inv, "png", new File(outDir, "inv_" + src.getName() + ".png"));
			ImageIO.write(small, "png", new File(outDir, "small_" + src.getName() + ".png"));

			System.out.println("Wrote outputs to test_images/output");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
