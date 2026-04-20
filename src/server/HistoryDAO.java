package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class HistoryDAO {
    // Duong dan tao file Database ngay trong thu muc goc cua project
    private static final String DATABASE_URL = "jdbc:sqlite:image_history.db";

    // Khoi tao bang neu chua co (Chay 1 lan luc bat Server)
    public static void initializeDatabase() {
        String createTableSql = "CREATE TABLE IF NOT EXISTS image_history ("
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
            // Ep Java nap Driver SQLite vao bo nho
            Class.forName("org.sqlite.JDBC");
            
            // Su dung try-with-resources de tu dong dong ket noi (Connection) va lenh (Statement)
            try (Connection databaseConnection = DriverManager.getConnection(DATABASE_URL);
                 Statement sqlStatement = databaseConnection.createStatement()) {
                 
                sqlStatement.execute(createTableSql);
                ServerUI.log("Database: Khoi tao va ket noi bang lich su thanh cong.");
            }
        } catch (Exception exception) {
            ServerUI.log("Loi Database: " + exception.getMessage());
        }
    }

    // Ham goi de luu lich su moi khi xu ly xong 1 anh
    public static boolean saveRecord(String userId, int commandCode, String protocol, long processTimeMs, String originalPath, String resultPath) {
        String insertSql = "INSERT INTO image_history(user_id, command_code, protocol, process_time_ms, original_path, result_path, created_at) " +
                           "VALUES(?, ?, ?, ?, ?, ?, datetime('now', 'localtime'))";
        
        try (Connection databaseConnection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement preparedStatement = databaseConnection.prepareStatement(insertSql)) {
             
            preparedStatement.setString(1, userId);
            preparedStatement.setInt(2, commandCode);
            preparedStatement.setString(3, protocol);
            preparedStatement.setLong(4, processTimeMs);
            preparedStatement.setString(5, originalPath);
            preparedStatement.setString(6, resultPath);
            
            preparedStatement.executeUpdate();
            return true;
            
        } catch (Exception exception) {
            // Sửa lỗi: Dong bo su dung ServerUI.log thay vi System.out.println
            ServerUI.log("Loi luu vao Database: " + exception.getMessage());
            return false;
        }
    }

    // Ham truy xuat va in toan bo lich su ra man hinh Log
    public static void printAllHistory() {
        String selectSql = "SELECT * FROM image_history";
        
        try (Connection databaseConnection = DriverManager.getConnection(DATABASE_URL);
             Statement sqlStatement = databaseConnection.createStatement();
             ResultSet resultSet = sqlStatement.executeQuery(selectSql)) {
            
            ServerUI.log("--- BAT DAU IN LICH SU TU SQLITE ---");
            
            // Duyet qua tung dong du lieu ket qua tra ve tu Database
            while (resultSet.next()) {
                int recordId = resultSet.getInt("id");
                String userId = resultSet.getString("user_id");
                int commandCode = resultSet.getInt("command_code");
                String protocolName = resultSet.getString("protocol");
                long processingTime = resultSet.getLong("process_time_ms");
                String originalImagePath = resultSet.getString("original_path");
                String resultImagePath = resultSet.getString("result_path");
                String createdAt = resultSet.getString("created_at"); 
                
                String logMessage = String.format("ID: %d | Thoi gian: %s | User: %s | Cmd: %d | Giao thuc: %s | Toc do: %d ms", 
                                                  recordId, createdAt, userId, commandCode, protocolName, processingTime);
                ServerUI.log(logMessage);
                ServerUI.log("  -> Goc: " + originalImagePath);
                ServerUI.log("  -> Ket qua: " + resultImagePath);
            }
            
            ServerUI.log("--- KET THUC IN LICH SU ---");
            
        } catch (Exception exception) {
            ServerUI.log("Loi khi truy xuat Database: " + exception.getMessage());
        }
    }
}