package share;

public class Constants {
    
    // =====================================================================
    // CAU HINH MANG (NETWORK CONFIGURATION)
    // =====================================================================
    // IP may chu (Thay doi thanh IP LAN cua may tinh khi mang ra chay thuc te)
    public static final String SERVER_IP = "127.0.0.1"; 
    
    // Cong (Port) lang nghe cua cac giao thuc
    public static final int SERVER_PORT_TCP = 8888;
    public static final int SERVER_PORT_UDP = 8889;
    
    // =====================================================================
    // CAU HINH UDP VA MO PHONG MANG (UDP & NETWORK SIMULATION)
    // =====================================================================
    // Gioi han an toan cua mot manh UDP de tranh tran bo dem mang (Kich thuoc 16KB)
    public static final int UDP_CHUNK_SIZE = 16384; 
    
    // Ty le co tinh danh rot goi tin de mo phong mang kem (Vi du: 0.15 = rot 15%)
    // Luu y: Bien nay KHONG co chu 'final' de Giao dien UI cua Server co the dieu chinh truc tiep (Live)
    public static double SIMULATE_DROP_RATE = 0.0;
    
    // =====================================================================
    // DANH SACH MA LENH XU LY ANH (COMMAND CODES)
    // =====================================================================
    // Dung de Client gui yeu cau va Server nhan dien thuat toan tuong ung
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