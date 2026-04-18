package core;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogWindow extends JFrame {
	private static LogWindow instance;
	private final JTextArea textArea;
	private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

	private LogWindow() {
		super("Server Log");
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		setSize(700, 420);
		setLayout(new BorderLayout());

		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

		JScrollPane sp = new JScrollPane(textArea);
		add(sp, BorderLayout.CENTER);

		JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton btnClear = new JButton("Clear");
		JButton btnSave = new JButton("Save...");
		JButton btnClose = new JButton("Close");

		btnClear.addActionListener(e -> clear());
		btnSave.addActionListener(e -> saveToFile());
		btnClose.addActionListener(e -> setVisible(false));

		bottom.add(btnClear);
		bottom.add(btnSave);
		bottom.add(btnClose);
		add(bottom, BorderLayout.SOUTH);

		setLocationRelativeTo(null);
	}

	private static synchronized void createIfNeeded() {
        if (instance == null) {
            // SỬA LỖI Ở ĐÂY: Khởi tạo và gán biến ngay lập tức để chốt quyền
            instance = new LogWindow(); 
            
            // Chỉ dùng invokeLater cho thao tác hiển thị giao diện lên màn hình
            SwingUtilities.invokeLater(() -> {
                instance.setVisible(true);
            });
        }
    }

	private void appendLine(String line) {
		String time = LocalDateTime.now().format(timeFmt);
		textArea.append("[" + time + "] " + line + System.lineSeparator());
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}

	public static void log(String message) {
		createIfNeeded();
		SwingUtilities.invokeLater(() -> instance.appendLine(message));
	}

	public static void log(String fmt, Object... args) {
		log(String.format(fmt, args));
	}

	public static void clear() {
		if (instance == null) return;
		SwingUtilities.invokeLater(() -> instance.textArea.setText(""));
	}

	private void saveToFile() {
		JFileChooser chooser = new JFileChooser();
		int res = chooser.showSaveDialog(this);
		if (res != JFileChooser.APPROVE_OPTION) return;
		File f = chooser.getSelectedFile();
		try (FileWriter fw = new FileWriter(f)) {
			fw.write(textArea.getText());
			JOptionPane.showMessageDialog(this, "Saved to " + f.getAbsolutePath());
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	// Quick test runner
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			createIfNeeded();
			log("Log window started.");
			log("This is a test entry.");
			log("Ready to receive logs from server components.");
		});
	}
}
