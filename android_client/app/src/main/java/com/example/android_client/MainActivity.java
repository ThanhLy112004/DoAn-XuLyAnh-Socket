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

public class MainActivity extends AppCompatActivity {

    private EditText edtUserId, edtIpServer;
    private RadioGroup rgProtocol;
    private Spinner spinnerCommand;
    private Button btnCapture, btnGallery, btnSend;
    private ImageView imgOriginal, imgResult;
    private ProgressBar progressBar;
    private TextView tvStatus, tvTime;

    private Bitmap originalBitmap;
    private NetworkManager networkManager;

    // Đủ 10 chức năng theo yêu cầu Server
    private String[] commands = {
            "1. Nén ảnh", "2. Phóng to", "3. Thu nhỏ", "4. Xoay ảnh", "5. Đen trắng",
            "6. Đảo màu", "7. Làm mờ", "8. Color Splash", "9. Phim cũ (Sepia)", "10. Bút chì"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        networkManager = new NetworkManager(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, commands);
        spinnerCommand.setAdapter(adapter);

        ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            originalBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                            imgOriginal.setImageBitmap(originalBitmap);
                            btnSend.setEnabled(true);
                            tvStatus.setText("Trạng thái: Đã chọn ảnh");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        ActivityResultLauncher<Intent> captureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null) {
                            originalBitmap = (Bitmap) extras.get("data");
                            imgOriginal.setImageBitmap(originalBitmap);
                            btnSend.setEnabled(true);
                            tvStatus.setText("Trạng thái: Đã chụp ảnh");
                        }
                    }
                }
        );

        btnGallery.setOnClickListener(v -> galleryLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)));
        btnCapture.setOnClickListener(v -> captureLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)));
        btnSend.setOnClickListener(v -> sendImageToServer());
    }

    private void initViews() {
        edtUserId = findViewById(R.id.edtUserId);
        edtIpServer = findViewById(R.id.edtIpServer); // Thêm ô nhập IP
        rgProtocol = findViewById(R.id.rgProtocol);
        spinnerCommand = findViewById(R.id.spinnerCommand);
        btnCapture = findViewById(R.id.btnCapture);
        btnGallery = findViewById(R.id.btnGallery);
        btnSend = findViewById(R.id.btnSend);
        imgOriginal = findViewById(R.id.imgOriginal);
        imgResult = findViewById(R.id.imgResult);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        tvTime = findViewById(R.id.tvTime); // Thêm nhãn thời gian
    }

    private void sendImageToServer() {
        String userId = edtUserId.getText().toString().trim();
        String serverIp = edtIpServer.getText().toString().trim();

        if (userId.isEmpty() || serverIp.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ UserID và IP Server", Toast.LENGTH_SHORT).show();
            return;
        }

        if (originalBitmap == null) {
            Toast.makeText(this, "Vui lòng chọn/chụp ảnh trước", Toast.LENGTH_SHORT).show();
            return;
        }

        int cmd = spinnerCommand.getSelectedItemPosition() + 1;
        boolean isTcp = rgProtocol.getCheckedRadioButtonId() == R.id.rbTcp;

        progressBar.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);
        tvStatus.setText("Trạng thái: Đang gửi lên " + serverIp + "...");
        tvTime.setText("Thời gian: Đang đo...");

        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            byte[] imgData = networkManager.bitmapToByteArray(originalBitmap);
            Bitmap result;

            if (isTcp) {
                result = networkManager.sendViaTCP(serverIp, userId, cmd, imgData);
            } else {
                int totalChunks = (int) Math.ceil(imgData.length / 16384.0); // 16KB/mảnh
                result = networkManager.sendViaUDP(serverIp, userId, cmd, imgData, totalChunks);
            }

            long timeTaken = System.currentTimeMillis() - startTime;

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                btnSend.setEnabled(true);
                tvTime.setText("Thời gian phản hồi: " + timeTaken + " ms");

                if (result != null) {
                    imgResult.setImageBitmap(result);
                    tvStatus.setText("Trạng thái: Nhận ảnh thành công!");
                } else {
                    tvStatus.setText("Trạng thái: Có lỗi xảy ra, xem Logcat.");
                }
            });
        }).start();
    }
}