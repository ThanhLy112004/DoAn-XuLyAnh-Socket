package client;

import share.Constants; // Import file cấu hình chung
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class ClientUI extends JFrame {
    private JLabel lblOriginal, lblResult;
    private JButton btnChoose, btnSend;
    private JComboBox<String> cbFunctions;
    private File selectedFile;

    public ClientUI() {
        setTitle("Đồ Án Lập Trình Mạng - Client Xử Lý Ảnh");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // --- PHẦN TRÊN: CÁC NÚT ĐIỀU KHIỂN ---
        JPanel panelTop = new JPanel();
        btnChoose = new JButton("Chọn Ảnh");
        
        // Danh sách chức năng hiển thị cho người dùng
        String[] functions = {"1. Nén ảnh", "2. Phóng to", "3. Thu nhỏ", "4. Xoay ảnh", "5. Đen trắng", "6. Đảo màu", "7. Làm mờ"};
        cbFunctions = new JComboBox<>(functions);
        
        btnSend = new JButton("Gửi qua Server");
        btnSend.setEnabled(false); // Ẩn nút gửi khi chưa chọn ảnh

        panelTop.add(btnChoose);
        panelTop.add(new JLabel("  Chức năng:"));
        panelTop.add(cbFunctions);
        panelTop.add(btnSend);
        add(panelTop, BorderLayout.NORTH);

        // --- PHẦN GIỮA: HIỂN THỊ ẢNH ---
        JPanel panelImages = new JPanel(new GridLayout(1, 2, 10, 0));
        
        lblOriginal = new JLabel("Ảnh gốc sẽ hiện ở đây", SwingConstants.CENTER);
        lblOriginal.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        lblResult = new JLabel("Ảnh kết quả sẽ hiện ở đây", SwingConstants.CENTER);
        lblResult.setBorder(BorderFactory.createLineBorder(Color.RED));

        panelImages.add(lblOriginal);
        panelImages.add(lblResult);
        add(panelImages, BorderLayout.CENTER);

        // --- GÁN SỰ KIỆN CHO NÚT BẤM ---
        setupListeners();
    }

    private void setupListeners() {
        // 1. Nút Chọn Ảnh
        btnChoose.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                // Hiển thị ảnh (Scale cho vừa khung 400x400)
                ImageIcon icon = new ImageIcon(new ImageIcon(selectedFile.getAbsolutePath()).getImage().getScaledInstance(400, 400, Image.SCALE_SMOOTH));
                lblOriginal.setIcon(icon);
                lblOriginal.setText("");
                btnSend.setEnabled(true); 
                lblResult.setIcon(null);
                lblResult.setText("Sẵn sàng gửi...");
            }
        });

        // 2. Nút Gửi lên Server
        btnSend.addActionListener(e -> {
            if (selectedFile == null) return;

            // Ánh xạ vị trí người dùng chọn trên ComboBox với Mã lệnh chuẩn trong Constants
            int[] commandMap = {
                Constants.CMD_COMPRESS,  // 0
                Constants.CMD_ZOOM_IN,   // 1
                Constants.CMD_ZOOM_OUT,  // 2
                Constants.CMD_ROTATE,    // 3
                Constants.CMD_GRAYSCALE, // 4
                Constants.CMD_INVERT,    // 5
                Constants.CMD_BLUR       // 6
            };
            
            // Lấy ra mã lệnh tương ứng
            int commandCode = commandMap[cbFunctions.getSelectedIndex()];
            
            lblResult.setText("Đang gửi và chờ Server xử lý...");
            btnSend.setEnabled(false); // Khóa nút trong lúc chờ mạng

            // Dùng Thread để giao diện không bị đơ
            new Thread(() -> {
                try {
                    // Gọi hàm gửi ảnh qua mạng
                    BufferedImage resultImage = ImageClient.sendAndReceiveImage(selectedFile, commandCode);
                    
                    // Cập nhật giao diện khi có ảnh trả về
                    SwingUtilities.invokeLater(() -> {
                        ImageIcon resultIcon = new ImageIcon(resultImage.getScaledInstance(400, 400, Image.SCALE_SMOOTH));
                        lblResult.setIcon(resultIcon);
                        lblResult.setText("");
                        btnSend.setEnabled(true);
                    });
                } catch (Exception ex) {
                    // Xử lý khi có lỗi (ví dụ Server chưa bật)
                    SwingUtilities.invokeLater(() -> {
                        lblResult.setText("Lỗi kết nối Server! (Server đã bật chưa?)");
                        lblResult.setIcon(null);
                        btnSend.setEnabled(true);
                        System.out.println("Lỗi mạng: " + ex.getMessage());
                    });
                }
            }).start();
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientUI ui = new ClientUI();
            ui.setVisible(true);
        });
    }
}