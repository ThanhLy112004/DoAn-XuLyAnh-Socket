# Đồ án: Xử lý Hình ảnh qua Mạng (Java Socket)

Dự án ứng dụng mô hình Client - Server sử dụng Java Socket thuần (TCP/UDP) để truyền tải và xử lý hình ảnh trực tuyến.

## 👥 Phân công công việc

Dự án được chia làm 3 module độc lập cho 3 thành viên:

* **Người 1: Lập trình Client** 
    * **Class chính:** `ImageClient`, `ClientUI`.
    * **Nhiệm vụ:** Xây dựng giao diện ứng dụng Client bằng Java Swing hoặc JavaF. Giao diện bao gồm nút chọn ảnh, menu chọn chức năng và các khu vực hiển thị ảnh gốc, ảnh kết quả. Chịu trách nhiệm đóng gói ảnh thành mảng byte và gửi qua mạng.

* **Người 2: Lập trình Server** 
    * **Class chính:** `ImageServer`, `ClientHandler`.
    * **Nhiệm vụ:** Mở Socket server tại cổng mặc định `5000`. Quản lý đa luồng (Multi-threading) để phục vụ nhiều Client cùng lúc. Đọc gói tin từ Client, tách header và payload, sau đó chuyển payload cho Người 3 xử lý.

* **Người 3: Lập trình Xử lý ảnh & Visualization** 
    * **Class chính:** `ImageProcessor`, `LogWindow`.
    * **Nhiệm vụ:** Xây dựng cửa sổ giám sát log hệ thống để theo dõi luồng đi của dữ liệu trên Server. Viết thuật toán cho 7 chức năng xử lý ảnh với đầu vào/đầu ra là `BufferedImage`.Cung cấp hàm giao tiếp chuẩn: `public static byte[] processRequest(int command, byte[] inputImageData)`.

## 📦 Giao thức truyền tin (Protocol)

Hệ thống sử dụng cấu trúc gói tin thống nhất bao gồm **Header + Payload**. Cụ thể như sau:

* **Byte 0 (Command Code):** 1 byte lưu mã chức năng cần thực hiện (ví dụ: 1 = Nén, 4 = Xoay).
* **Byte 1-4 (Data Length):** 4 byte (số nguyên int) lưu tổng dung lượng của mảng byte dữ liệu ảnh.
* **Byte 5 trở đi (Image Data):** Toàn bộ mảng dữ liệu nhị phân của ảnh (Payload).

## 🛠 Các chức năng xử lý ảnh hỗ trợ

Hệ thống hỗ trợ tối thiểu 7 tính năng biến đổi ảnh:

1.  **Nén ảnh (Compress):** Giảm dung lượng file bằng cách thay đổi chất lượng ảnh JPEG.
2.  **Phóng to (Scale Up):** Tăng kích thước ảnh theo hệ số.
3.  **Thu nhỏ (Scale Down):** Giảm kích thước ảnh có dùng bộ lọc nội suy (Bilinear) để tránh răng cưa.
4.  **Xoay ảnh (Rotate):** Xoay hình ảnh theo các góc 90, 180, 270 độ.
5.  **Đen trắng (Grayscale):** Chuyển đổi ảnh màu sang tông xám.
6.  **Đảo màu (Invert):** Tạo hiệu ứng màu âm bản bằng cách đảo ngược các giá trị kênh R, G, B.
7.  **Làm mờ (Blur):** Áp dụng ma trận lọc (Convolution kernel) để làm nhòe các chi tiết.

## 🚀 Hướng dẫn chạy dự án

1. Clone repository này về máy.
2. Mở dự án bằng IDE (VS Code, IntelliJ hoặc Eclipse).
3. Chạy file `src/server/ImageServer.java` để khởi động máy chủ (sẽ mở cửa sổ giám sát Log).
4. Chạy file `src/client/ImageClient.java` để mở ứng dụng người dùng cuối.
5. Trên Client, chọn một bức ảnh từ thư mục `test_images/input`, chọn chức năng và bấm **GỬI**.
