package client;

import share.Constants;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class ClientUI extends JFrame {
    private JLabel lblOriginal, lblResult, lblStatus;
    private JButton btnChoose, btnSend;
    private JComboBox<String> cbFunctions;
    private JRadioButton radTCP, radUDP;
    private JTextField txtUserId; // Thêm ô nhập UserID
    private File selectedFile;

    public ClientUI() {
        setTitle("Đồ Án Lập Trình Mạng - TCP/UDP Image Processor (Test UI)");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // --- PHẦN TRÊN: ĐIỀU KHIỂN ---
        JPanel panelTop = new JPanel(new FlowLayout());
        btnChoose = new JButton("Chọn Ảnh");
        
        txtUserId = new JTextField("SV001", 5); // Khởi tạo UserID mặc định
        
        String[] functions = {
            "1. Nén ảnh", "2. Phóng to", "3. Thu nhỏ", "4. Xoay ảnh", "5. Đen trắng", 
            "6. Đảo màu", "7. Làm mờ", "8. Color Splash", "9. Phim cũ (Sepia)", "10. Phác họa Bút chì"
        };
        cbFunctions = new JComboBox<>(functions);
        
        radTCP = new JRadioButton("TCP", true);
        radUDP = new JRadioButton("UDP");
        ButtonGroup bg = new ButtonGroup();
        bg.add(radTCP); bg.add(radUDP);
        
        btnSend = new JButton("Gửi qua Server");
        btnSend.setEnabled(false);

        panelTop.add(btnChoose);
        panelTop.add(new JLabel("  UserID:"));
        panelTop.add(txtUserId);
        panelTop.add(new JLabel("  Chức năng:"));
        panelTop.add(cbFunctions);
        panelTop.add(radTCP);
        panelTop.add(radUDP);
        panelTop.add(btnSend);
        add(panelTop, BorderLayout.NORTH);

        // --- PHẦN GIỮA: HIỂN THỊ ẢNH ---
        JPanel panelImages = new JPanel(new GridLayout(1, 2, 10, 0));
        lblOriginal = new JLabel("Ảnh gốc", SwingConstants.CENTER);
        lblResult = new JLabel("Ảnh kết quả", SwingConstants.CENTER);
        
        panelImages.add(new JScrollPane(lblOriginal));
        panelImages.add(new JScrollPane(lblResult));
        add(panelImages, BorderLayout.CENTER);

        // --- PHẦN DƯỚI: TRẠNG THÁI ---
        lblStatus = new JLabel(" Trạng thái: Sẵn sàng");
        lblStatus.setForeground(Color.BLUE);
        add(lblStatus, BorderLayout.SOUTH);

        setupListeners();
    }

    private void setupListeners() {
        btnChoose.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                lblOriginal.setIcon(new ImageIcon(new ImageIcon(selectedFile.getAbsolutePath()).getImage().getScaledInstance(400, 400, Image.SCALE_SMOOTH)));
                btnSend.setEnabled(true);
            }
        });

        btnSend.addActionListener(e -> {
            if (selectedFile == null) return;
            
            String userId = txtUserId.getText().trim();
            if (userId.isEmpty()) userId = "Khach";

            int[] commandMap = {
                Constants.CMD_COMPRESS, Constants.CMD_ZOOM_IN, Constants.CMD_ZOOM_OUT,
                Constants.CMD_ROTATE, Constants.CMD_GRAYSCALE, Constants.CMD_INVERT,
                Constants.CMD_BLUR, Constants.CMD_COLOR_SPLASH, Constants.CMD_SEPIA, Constants.CMD_PENCIL_SKETCH
            };
            int commandCode = commandMap[cbFunctions.getSelectedIndex()];
            boolean isTCP = radTCP.isSelected();
            
            lblStatus.setText(" Trạng thái: Đang gửi lên Server (" + userId + ")...");
            btnSend.setEnabled(false);

            final String finalUserId = userId;
            new Thread(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    BufferedImage resultImage;
                    
                    if (isTCP) {
                        resultImage = ImageClient.sendAndReceiveImage(selectedFile, commandCode, finalUserId);
                    } else {
                        resultImage = UdpImageClient.sendAndReceiveImage(selectedFile, commandCode, finalUserId);
                    }
                    
                    long timeTaken = System.currentTimeMillis() - startTime;

                    SwingUtilities.invokeLater(() -> {
                        lblResult.setIcon(new ImageIcon(resultImage));
                        lblStatus.setText(String.format(" Thành công! Giao thức: %s | Tốc độ: %d ms", isTCP ? "TCP" : "UDP", timeTaken));
                        btnSend.setEnabled(true);
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        lblStatus.setText(" Lỗi mạng: " + ex.getMessage());
                        btnSend.setEnabled(true);
                    });
                }
            }).start();
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientUI().setVisible(true));
    }
}