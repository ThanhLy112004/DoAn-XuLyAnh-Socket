package share;

public class Constants {
    public static final String SERVER_IP = "127.0.0.1"; // Sẽ đổi ở Bước 4
    public static final int SERVER_PORT_TCP = 8888;
    public static final int SERVER_PORT_UDP = 8889;
    
    // Giới hạn an toàn của gói tin UDP (khoảng 16KB)
    public static final int UDP_CHUNK_SIZE = 16384; 
    // Tỉ lệ cố tình làm rớt gói tin để Demo (0.05 = rớt 5%)
public static final double SIMULATE_DROP_RATE = 0.05;
    // Mã lệnh
    public static final int CMD_COMPRESS = 1;
    public static final int CMD_ZOOM_IN = 2;
    public static final int CMD_ZOOM_OUT = 3;
    public static final int CMD_ROTATE = 4;
    public static final int CMD_GRAYSCALE = 5;
    public static final int CMD_INVERT = 6;
    public static final int CMD_BLUR = 7;
    public static final int CMD_COLOR_SPLASH = 8;
    public static final int CMD_SEPIA = 9;
    public static final int CMD_PENCIL_SKETCH = 10;
}