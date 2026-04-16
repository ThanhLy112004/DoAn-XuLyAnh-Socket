package share;

public class Constants {
    // Cấu hình mạng
    public static final String SERVER_IP = "127.0.0.1";
    public static final int SERVER_PORT = 8888; // Hoặc cổng nào nhóm bạn đã chốt

    // Mã lệnh xử lý ảnh (Khớp với 7 chức năng trên UI)
    public static final int CMD_COMPRESS = 1;   // Nén ảnh
    public static final int CMD_ZOOM_IN = 2;    // Phóng to
    public static final int CMD_ZOOM_OUT = 3;   // Thu nhỏ
    public static final int CMD_ROTATE = 4;     // Xoay ảnh
    public static final int CMD_GRAYSCALE = 5;  // Đen trắng
    public static final int CMD_INVERT = 6;     // Đảo màu
    public static final int CMD_BLUR = 7;       // Làm mờ
}