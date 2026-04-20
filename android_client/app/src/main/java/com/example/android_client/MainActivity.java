package com.example.android_client;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Locale;

import share.Constants;

public class MainActivity extends AppCompatActivity implements NetworkManager.NetworkCallback {

    private EditText edtUserId, edtIpServer;
    private RadioGroup rgProtocol;
    private Spinner spinnerCommand;
    private Button btnCapture, btnGallery, btnSend;
    
    // Đã thêm imgOriginal vào phần khai báo
    private ImageView imgOriginal, imgResult;
    private ProgressBar progressBar;
    private TextView tvStatus, tvTime, tvSentStats, tvReceivedStats;

    private Bitmap originalBitmap;
    private NetworkManager networkManager;

    private final String[] commandOptions = {
            "1. Nén ảnh (Compress)", "2. Phóng to (Zoom In)", "3. Thu nhỏ (Zoom Out)", 
            "4. Xoay ảnh (Rotate)", "5. Đen trắng (Grayscale)", "6. Đảo màu (Invert)", 
            "7. Làm mờ (Blur)", "8. Color Splash", "9. Phim cũ (Sepia)", "10. Bút chì (Sketch)"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        networkManager = new NetworkManager(this);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, commandOptions);
        spinnerCommand.setAdapter(spinnerAdapter);

        // 1. Bộ lắng nghe chọn ảnh từ Thư viện (Gallery)
        ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            originalBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                            
                            // CẬP NHẬT: Hiển thị ảnh Preview ngay lập tức
                            imgOriginal.setImageBitmap(originalBitmap);
                            
                            btnSend.setEnabled(true);
                            tvStatus.setText("Trạng thái: Đã chọn ảnh");
                        } catch (IOException exception) { 
                            exception.printStackTrace(); 
                        }
                    }
                }
        );

        // 2. Bộ lắng nghe chụp ảnh từ Camera
        ActivityResultLauncher<Intent> captureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null) {
                            originalBitmap = (Bitmap) extras.get("data");
                            
                            // CẬP NHẬT: Hiển thị ảnh Preview ngay lập tức
                            imgOriginal.setImageBitmap(originalBitmap);
                            
                            btnSend.setEnabled(true);
                            tvStatus.setText("Trạng thái: Đã chụp ảnh");
                        }
                    }
                }
        );

        btnGallery.setOnClickListener(v -> galleryLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)));
        btnCapture.setOnClickListener(v -> captureLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)));
        btnSend.setOnClickListener(v -> executeImageProcessing());
    }

    private void initializeViews() {
        edtUserId = findViewById(R.id.edtUserId);
        edtIpServer = findViewById(R.id.edtIpServer);
        rgProtocol = findViewById(R.id.rgProtocol);
        spinnerCommand = findViewById(R.id.spinnerCommand);
        btnCapture = findViewById(R.id.btnCapture);
        btnGallery = findViewById(R.id.btnGallery);
        btnSend = findViewById(R.id.btnSend);
        
        // CẬP NHẬT: Ánh xạ biến imgOriginal với ID trong XML
        imgOriginal = findViewById(R.id.imgOriginal);
        imgResult = findViewById(R.id.imgResult);
        
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        tvTime = findViewById(R.id.tvTime);
        tvSentStats = findViewById(R.id.tvSentStats);
        tvReceivedStats = findViewById(R.id.tvReceivedStats);
    }

    @Override
    public void onProgressUpdate(String sentStats, String receivedStats) {
        runOnUiThread(() -> {
            if (sentStats != null) tvSentStats.setText(sentStats);
            if (receivedStats != null) tvReceivedStats.setText(receivedStats);
        });
    }

    private void executeImageProcessing() {
        String userId = edtUserId.getText().toString().trim();
        String serverIp = edtIpServer.getText().toString().trim();
        
        if (userId.isEmpty() || serverIp.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập UserID và IP Server", Toast.LENGTH_SHORT).show();
            return;
        }

        int commandCode = spinnerCommand.getSelectedItemPosition() + 1;
        boolean isTcpProtocol = rgProtocol.getCheckedRadioButtonId() == R.id.rbTcp;

        // Cập nhật Giao diện trước khi vào luồng mạng
        progressBar.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);
        tvStatus.setText("Hệ thống đang xử lý...");
        tvSentStats.setText("Đang chuẩn bị dữ liệu...");
        tvReceivedStats.setText("...");

        // Mở luồng (Thread) mới để không làm đứng UI của Android
        new Thread(() -> {
            long startTimeMs = System.currentTimeMillis();
            byte[] imagePayload = networkManager.bitmapToByteArray(originalBitmap);
            Bitmap processedBitmap;

            if (isTcpProtocol) {
                processedBitmap = networkManager.sendViaTCP(serverIp, userId, commandCode, imagePayload);
            } else {
                int totalChunks = (int) Math.ceil((double) imagePayload.length / Constants.UDP_CHUNK_SIZE);
                processedBitmap = networkManager.sendViaUDP(serverIp, userId, commandCode, imagePayload, totalChunks);
            }

            // Khấu trừ 300ms đợi Timeout của UDP để tính thời gian thật
            long totalProcessingTimeMs = System.currentTimeMillis() - startTimeMs;
            if (!isTcpProtocol) {
                totalProcessingTimeMs = Math.max(0, totalProcessingTimeMs - 300);
            }

            final long finalTimeTaken = totalProcessingTimeMs;
            
            // Quay lại luồng chính để cập nhật hình ảnh kết quả lên màn hình
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                btnSend.setEnabled(true);
                tvTime.setText(String.format(Locale.US, "Tổng thời gian: %d ms", finalTimeTaken));
                
                if (processedBitmap != null) {
                    // Đổ ảnh nhận được từ Server vào khung Result bên dưới
                    imgResult.setImageBitmap(processedBitmap);
                    tvStatus.setText("Trạng thái: Hoàn tất!");
                } else {
                    tvStatus.setText("Trạng thái: Thất bại!");
                    Toast.makeText(MainActivity.this, "Lỗi kết nối hoặc dữ liệu ảnh bị hỏng nặng!", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }
}
