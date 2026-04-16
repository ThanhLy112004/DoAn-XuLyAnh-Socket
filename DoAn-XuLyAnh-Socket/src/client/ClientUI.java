package client;
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
        
        // Danh sách chức năng (Mã 1 đến 7)
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
        
        // Khung ảnh gốc
        lblOriginal = new JLabel("Ảnh gốc sẽ hiện ở đây", SwingConstants.CENTER);
        lblOriginal.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        // Khung ảnh kết quả
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
                // Hiển thị ảnh lên khung bên trái (Scale cho vừa khung)
                ImageIcon icon = new ImageIcon(new ImageIcon(selectedFile.getAbsolutePath()).getImage().getScaledInstance(400, 400, Image.SCALE_SMOOTH));
                lblOriginal.setIcon(icon);
                lblOriginal.setText("");
                btnSend.setEnabled(true); // Mở khóa nút Gửi
                lblResult.setIcon(null);
                lblResult.setText("Sẵn sàng gửi...");
            }
        });

        // 2. Nút Gửi lên Server
        btnSend.addActionListener(e -> {
            if (selectedFile == null) return;

            // Lấy mã lệnh từ ComboBox (index bắt đầu từ 0 nên cộng 1)
            int commandCode = cbFunctions.getSelectedIndex() + 1;
            lblResult.setText("Đang gửi và chờ Server xử lý...");
            btnSend.setEnabled(false); // Khóa nút trong lúc chờ

            // Dùng Thread (Luồng phụ) để giao diện không bị đơ khi chờ mạng
            new Thread(() -> {
                try {
                    // Gọi hàm bên class ImageClient
                    BufferedImage resultImage = ImageClient.sendAndReceiveImage(selectedFile, commandCode);
                    
                    // Hiển thị kết quả (Cập nhật UI phải chạy trên luồng chính)
                    SwingUtilities.invokeLater(() -> {
                        ImageIcon resultIcon = new ImageIcon(resultImage.getScaledInstance(400, 400, Image.SCALE_SMOOTH));
                        lblResult.setIcon(resultIcon);
                        lblResult.setText("");
                        btnSend.setEnabled(true);
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        lblResult.setText("Lỗi kết nối Server!");
                        lblResult.setIcon(null);
                        btnSend.setEnabled(true);
                        ex.printStackTrace();
                    });
                }
            }).start();
        });
    }

    // Hàm Main để chạy chương trình
    public static void main(String[] args) {
        // Chạy giao diện an toàn
        SwingUtilities.invokeLater(() -> {
            ClientUI ui = new ClientUI();
            ui.setVisible(true);
        });
    }
}