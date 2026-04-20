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
    private ImageView imgResult;
    private ProgressBar progressBar;
    private TextView tvStatus, tvTime, tvSentStats, tvReceivedStats;

    private Bitmap originalBitmap;
    private NetworkManager networkManager;

    private final String[] commandOptions = {
            "1. Nen anh (Compress)", "2. Phong to (Zoom In)", "3. Thu nho (Zoom Out)", 
            "4. Xoay anh (Rotate)", "5. Den trang (Grayscale)", "6. Dao mau (Invert)", 
            "7. Lam mo (Blur)", "8. Color Splash", "9. Phim cu (Sepia)", "10. But chi (Sketch)"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        networkManager = new NetworkManager(this);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, commandOptions);
        spinnerCommand.setAdapter(spinnerAdapter);

        // Bo lang nghe chon anh tu Thu vien (Gallery)
        ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            originalBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                            imgResult.setImageBitmap(originalBitmap);
                            btnSend.setEnabled(true);
                            tvStatus.setText("Trang thai: Da chon anh");
                        } catch (IOException exception) { 
                            exception.printStackTrace(); 
                        }
                    }
                }
        );

        // Bo lang nghe chup anh tu Camera
        ActivityResultLauncher<Intent> captureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null) {
                            originalBitmap = (Bitmap) extras.get("data");
                            btnSend.setEnabled(true);
                            tvStatus.setText("Trang thai: Da chup anh");
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
            Toast.makeText(this, "Vui long nhap UserID va IP Server", Toast.LENGTH_SHORT).show();
            return;
        }

        int commandCode = spinnerCommand.getSelectedItemPosition() + 1;
        boolean isTcpProtocol = rgProtocol.getCheckedRadioButtonId() == R.id.rbTcp;

        // Cap nhat Giao dien truoc khi vao luong xu ly nang
        progressBar.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);
        tvStatus.setText("He thong dang xu ly...");
        tvSentStats.setText("Dang chuan bi du lieu...");
        tvReceivedStats.setText("...");

        // Mo luong (Thread) moi de khong lam dung UI cua Android
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

            // Tru khau hao 300ms doi Timeout cua UDP de tinh thoi gian that
            long totalProcessingTimeMs = System.currentTimeMillis() - startTimeMs;
            if (!isTcpProtocol) {
                totalProcessingTimeMs = Math.max(0, totalProcessingTimeMs - 300);
            }

            final long finalTimeTaken = totalProcessingTimeMs;
            
            // Quay lai luong chinh de cap nhat hinh anh len man hinh
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                btnSend.setEnabled(true);
                tvTime.setText(String.format(Locale.US, "Tong thoi gian: %d ms", finalTimeTaken));
                
                if (processedBitmap != null) {
                    imgResult.setImageBitmap(processedBitmap);
                    tvStatus.setText("Trang thai: Xu ly hoan tat!");
                } else {
                    tvStatus.setText("Trang thai: Giao dich that bai!");
                    Toast.makeText(MainActivity.this, "Khong the ket noi hoac anh qua loi!", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }
}
