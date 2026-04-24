package server;

import core.LogWindow;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class HistoryDAO {
    // Đường dẫn tạo file Database ngay trong thư mục gốc của project
    private static final String DB_URL = "jdbc:sqlite:image_history.db";

    // Khởi tạo bảng nếu chưa có (Chạy 1 lần lúc bật Server)
    public static void initializeDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS history ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id TEXT,"
                + "command_code INTEGER,"
                + "protocol TEXT,"
                + "process_time_ms INTEGER,"
                + "original_path TEXT,"
                + "result_path TEXT,"
                + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ");";

        try {
            // Ép Java nạp Driver SQLite vào bộ nhớ (Khắc phục triệt để lỗi No suitable driver)
            Class.forName("org.sqlite.JDBC");
            
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                LogWindow.log("Database: Khởi tạo/Kết nối bảng lịch sử thành công.");
            }
        } catch (Exception e) {
            LogWindow.log("Lỗi Database: " + e.getMessage());
        }
    }

    // Hàm gọi để lưu lịch sử mỗi khi xử lý xong 1 ảnh
    public static boolean saveRecord(String userId, int cmd, String protocol, 
                                     long timeMs, String origPath, String resPath) {
        String sql = "INSERT INTO history(user_id, command_code, protocol, process_time_ms, original_path, result_path) VALUES(?,?,?,?,?,?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            pstmt.setInt(2, cmd);
            pstmt.setString(3, protocol);
            pstmt.setLong(4, timeMs);
            pstmt.setString(5, origPath);
            pstmt.setString(6, resPath);
            
            pstmt.executeUpdate();
            return true;
        } catch (Exception e) {
            LogWindow.log("Lỗi lưu DB: " + e.getMessage());
            return false;
        }
    }

    // Hàm truy xuất và in toàn bộ lịch sử ra màn hình Log
    public static void printAllHistory() {
        String sql = "SELECT * FROM history";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            LogWindow.log("--- BẮT ĐẦU IN LỊCH SỬ TỪ SQLITE ---");
            while (rs.next()) {
                int id = rs.getInt("id");
                String userId = rs.getString("user_id");
                int cmd = rs.getInt("command_code");
                String protocol = rs.getString("protocol");
                long time = rs.getLong("process_time_ms");
                String origPath = rs.getString("original_path");
                String resPath = rs.getString("result_path");
                
                String logMessage = String.format("ID: %d | User: %s | Cmd: %d | Giao thức: %s | Tốc độ: %d ms", 
                                                  id, userId, cmd, protocol, time);
                LogWindow.log(logMessage);
                LogWindow.log("  -> Gốc: " + origPath);
                LogWindow.log("  -> Kết quả: " + resPath);
            }
            LogWindow.log("--- KẾT THÚC IN LỊCH SỬ ---");
            
        } catch (Exception e) {
            LogWindow.log("Lỗi khi truy xuất DB: " + e.getMessage());
        }
    }
}