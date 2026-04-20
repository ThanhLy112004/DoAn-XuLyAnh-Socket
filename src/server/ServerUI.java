package server;

import share.Constants; 
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

public class ServerUI {
    private static JFrame mainFrame;
    private static JLabel originalImageLabel;
    private static JLabel resultImageLabel;
    private static JTextArea logTextArea;

    public static void startUI() {
        mainFrame = new JFrame("Server Giam Sat Do An (TCP/UDP)");
        mainFrame.setSize(1000, 650); 
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLayout(new BorderLayout(10, 10));

        // ==========================================================
        // 1. KHU VUC DIEU KHIEN MANG (BANG ĐIEU KHIEN LIVE)
        // ==========================================================
        JPanel networkControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        networkControlPanel.setBorder(BorderFactory.createTitledBorder("Bang Dieu Khien Moi Truong Mang (Live)"));

        JLabel sliderInfoLabel = new JLabel("Ty le rot mang (Drop Rate): 0%");
        sliderInfoLabel.setFont(new Font("Arial", Font.BOLD, 14));
        sliderInfoLabel.setForeground(Color.RED);

        // Thanh keo tu 0 den 20 (Dai dien cho 0% den 20%)
        JSlider dropRateSlider = new JSlider(0, 20, 0); 
        dropRateSlider.setMajorTickSpacing(5);
        dropRateSlider.setMinorTickSpacing(1);
        dropRateSlider.setPaintTicks(true);
        dropRateSlider.setPaintLabels(true);
        dropRateSlider.setPreferredSize(new Dimension(300, 50));

        // Bat su kien khi thao tac keo thanh truot
        dropRateSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent event) {
                if (!dropRateSlider.getValueIsAdjusting()) { 
                    int selectedValue = dropRateSlider.getValue();
                    sliderInfoLabel.setText("Ty le rot mang (Drop Rate): " + selectedValue + "%");
                    
                    // Chuyen tu so nguyen (15) sang so thap phan (0.15) gan vao bien toan cuc
                    Constants.SIMULATE_DROP_RATE = selectedValue / 100.0; 
                    
                    log("========== THAY DOI MANG ==========");
                    log("=> Da cap nhat Drop Rate Live thanh: " + selectedValue + "%");
                }
            }
        });

        networkControlPanel.add(sliderInfoLabel);
        networkControlPanel.add(dropRateSlider);

        // ==========================================================
        // 2. KHU VUC HIEN THI ANH GOC VA KET QUA
        // ==========================================================
        JPanel imageDisplayPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        imageDisplayPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        originalImageLabel = new JLabel("Chua co anh goc", SwingConstants.CENTER);
        originalImageLabel.setBorder(BorderFactory.createTitledBorder("Anh Client Gui Len"));
        
        resultImageLabel = new JLabel("Chua co anh ket qua", SwingConstants.CENTER);
        resultImageLabel.setBorder(BorderFactory.createTitledBorder("Anh Server Tra Ve"));

        imageDisplayPanel.add(originalImageLabel);
        imageDisplayPanel.add(resultImageLabel);

        // ==========================================================
        // 3. KHU VUC GHI NHAN LOG HE THONG
        // ==========================================================
        logTextArea = new JTextArea(10, 50);
        logTextArea.setEditable(false);
        logTextArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        JScrollPane logScrollPane = new JScrollPane(logTextArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Bang Dieu Khien Log"));

        // Gan cac thanh phan vao khung chinh
        mainFrame.add(networkControlPanel, BorderLayout.NORTH); 
        mainFrame.add(imageDisplayPanel, BorderLayout.CENTER); 
        mainFrame.add(logScrollPane, BorderLayout.SOUTH);  

        mainFrame.setLocationRelativeTo(null); 
        mainFrame.setVisible(true);
    }

    public static void updateImages(byte[] originalImageData, byte[] resultImageData) {
        // Su dung SwingUtilities.invokeLater de khong lam dung (freeze) giao dien UI
        SwingUtilities.invokeLater(() -> {
            try {
                if (originalImageData != null && originalImageData.length > 0) {
                    BufferedImage bufferedOriginalImage = ImageIO.read(new ByteArrayInputStream(originalImageData));
                    if (bufferedOriginalImage != null) {
                        Image scaledOriginalImage = bufferedOriginalImage.getScaledInstance(450, 350, Image.SCALE_SMOOTH);
                        originalImageLabel.setIcon(new ImageIcon(scaledOriginalImage));
                        originalImageLabel.setText("");
                    } else {
                        originalImageLabel.setIcon(null);
                        originalImageLabel.setText("Anh goc bi loi cau truc JPEG (Client -> Server rot goi)");
                    }
                }
            } catch (Exception exception) {
                // Bo qua loi hien thi de khong lam chet chuong trinh
            }

            try {
                if (resultImageData != null && resultImageData.length > 0) {
                    BufferedImage bufferedResultImage = ImageIO.read(new ByteArrayInputStream(resultImageData));
                    if (bufferedResultImage != null) {
                        Image scaledResultImage = bufferedResultImage.getScaledInstance(450, 350, Image.SCALE_SMOOTH);
                        resultImageLabel.setIcon(new ImageIcon(scaledResultImage));
                        resultImageLabel.setText("");
                    } else {
                        resultImageLabel.setIcon(null);
                        resultImageLabel.setText("Anh ket qua bi loi cau truc.");
                    }
                }
            } catch (Exception exception) {
                // Bo qua loi hien thi
            }
        });
    }

    public static void log(String message) {
        SwingUtilities.invokeLater(() -> {
            if (logTextArea != null) {
                logTextArea.append(message + "\n");
                // Tu dong cuon xuong dong moi nhat
                logTextArea.setCaretPosition(logTextArea.getDocument().getLength()); 
            }
            System.out.println(message); 
        });
    }
}