# Đồ án: Xử lý Hình ảnh qua Mạng (Java Socket)

Dự án ứng dụng mô hình Client - Server sử dụng Java Socket thuần (TCP/UDP) để truyền tải và xử lý hình ảnh trực tuyến.

## 👥 Phân công công việc

Dự án được chia làm 3 module độc lập cho 3 thành viên:

* [cite_start]**Người 1: Lập trình Client** [cite: 8]
    * [cite_start]**Class chính:** `ImageClient`, `ClientUI`[cite: 8].
    * [cite_start]**Nhiệm vụ:** Xây dựng giao diện ứng dụng Client bằng Java Swing hoặc JavaFX[cite: 10]. [cite_start]Giao diện bao gồm nút chọn ảnh, menu chọn chức năng và các khu vực hiển thị ảnh gốc, ảnh kết quả[cite: 10, 44, 45]. [cite_start]Chịu trách nhiệm đóng gói ảnh thành mảng byte và gửi qua mạng[cite: 12].

* [cite_start]**Người 2: Lập trình Server** [cite: 18]
    * [cite_start]**Class chính:** `ImageServer`, `ClientHandler`[cite: 18].
    * [cite_start]**Nhiệm vụ:** Mở Socket server tại cổng mặc định `5000`[cite: 40]. [cite_start]Quản lý đa luồng (Multi-threading) để phục vụ nhiều Client cùng lúc[cite: 20]. [cite_start]Đọc gói tin từ Client, tách header và payload, sau đó chuyển payload cho Người 3 xử lý[cite: 22, 23, 24, 25].

* [cite_start]**Người 3: Lập trình Xử lý ảnh & Visualization** [cite: 27]
    * [cite_start]**Class chính:** `ImageProcessor`, `LogWindow`[cite: 27].
    * [cite_start]**Nhiệm vụ:** Xây dựng cửa sổ giám sát log hệ thống để theo dõi luồng đi của dữ liệu trên Server[cite: 31, 32, 48]. [cite_start]Viết thuật toán cho 7 chức năng xử lý ảnh với đầu vào/đầu ra là `BufferedImage`[cite: 29]. [cite_start]Cung cấp hàm giao tiếp chuẩn: `public static byte[] processRequest(int command, byte[] inputImageData)`[cite: 37, 38].

## 📦 Giao thức truyền tin (Protocol)

[cite_start]Hệ thống sử dụng cấu trúc gói tin thống nhất bao gồm **Header + Payload**[cite: 3]. Cụ thể như sau:

* [cite_start]**Byte 0 (Command Code):** 1 byte lưu mã chức năng cần thực hiện (ví dụ: 1 = Nén, 4 = Xoay)[cite: 4].
* [cite_start]**Byte 1-4 (Data Length):** 4 byte (số nguyên int) lưu tổng dung lượng của mảng byte dữ liệu ảnh[cite: 5].
* [cite_start]**Byte 5 trở đi (Image Data):** Toàn bộ mảng dữ liệu nhị phân của ảnh (Payload)[cite: 6].

## 🛠 Các chức năng xử lý ảnh hỗ trợ

Hệ thống hỗ trợ tối thiểu 7 tính năng biến đổi ảnh:

1.  [cite_start]**Nén ảnh (Compress):** Giảm dung lượng file bằng cách thay đổi chất lượng ảnh JPEG[cite: 33].
2.  [cite_start]**Phóng to (Scale Up):** Tăng kích thước ảnh theo hệ số[cite: 33].
3.  [cite_start]**Thu nhỏ (Scale Down):** Giảm kích thước ảnh có dùng bộ lọc nội suy (Bilinear) để tránh răng cưa[cite: 33].
4.  [cite_start]**Xoay ảnh (Rotate):** Xoay hình ảnh theo các góc 90, 180, 270 độ[cite: 33].
5.  [cite_start]**Đen trắng (Grayscale):** Chuyển đổi ảnh màu sang tông xám[cite: 33].
6.  [cite_start]**Đảo màu (Invert):** Tạo hiệu ứng màu âm bản bằng cách đảo ngược các giá trị kênh R, G, B[cite: 33].
7.  [cite_start]**Làm mờ (Blur):** Áp dụng ma trận lọc (Convolution kernel) để làm nhòe các chi tiết[cite: 33].

## 🚀 Hướng dẫn chạy dự án

1. Clone repository này về máy.
2. Mở dự án bằng IDE (VS Code, IntelliJ hoặc Eclipse).
3. Chạy file `src/server/ImageServer.java` để khởi động máy chủ (sẽ mở cửa sổ giám sát Log).
4. Chạy file `src/client/ImageClient.java` để mở ứng dụng người dùng cuối.
5. Trên Client, chọn một bức ảnh từ thư mục `test_images/input`, chọn chức năng và bấm **GỬI**.