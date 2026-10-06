# Personal Growth V8.3.1 TECNO Spark 30 5G

Bản đầy đủ dành cho Android 14 / HiOS 14.5.

- Không yêu cầu quyền CAMERA vì ứng dụng dùng Camera hệ thống qua ACTION_IMAGE_CAPTURE.
- Tạo tệp ảnh thật bằng File.createTempFile trước khi mở Camera.
- Gắn EXTRA_OUTPUT, ClipData và cấp quyền URI cho mọi ứng dụng Camera có thể xử lý Intent.
- Kiểm tra URI ảnh đọc được trước khi mở màn hình cắt.
- Cắt ảnh hỗ trợ EXIF, kéo, phóng và hàng đợi nhiều ảnh.
- Hủy một ảnh chỉ bỏ qua ảnh đó, không làm mất ảnh đã cắt trước.
- Widget dùng RemoteViews bảo thủ: không emoji, không custom view, không previewLayout, không layout_weight trong các hàng dữ liệu.
- Receiver widget dùng android:exported=false và có giao diện dự phòng nếu dữ liệu lỗi.

## Thay đổi V8.3.1
- Widget bỏ mã nhóm viết tắt và hiển thị tên nhóm đầy đủ ở dòng thứ hai.
- Tên nhóm và địa điểm dài tự rút gọn bằng dấu ba chấm.
- Trạng thái và mức độ vẫn nằm bên phải; chạm hồ sơ mở trực tiếp chi tiết.
- Giữ nguyên camera hệ thống, cắt ảnh EXIF, hàng đợi nhiều ảnh và tương thích TECNO Spark 30 5G / HiOS 14.5.

## Sửa lỗi V8.3.1
- Widget dùng cấu trúc RemoteViews tối giản chỉ gồm LinearLayout, TextView và View.
- Loại bỏ RelativeLayout và mọi liên kết layout_toStartOf để tăng tương thích HiOS Launcher.

### Widget V8.3.1 theo cấu trúc V73 WidgetFactor
- Dùng RemoteViews thuần gồm LinearLayout, TextView và Button; không dùng View trần hoặc layout tùy biến.
- Không có WidgetConfigActivity và không hiện hộp thoại xác nhận riêng của ứng dụng.
- Thả widget trực tiếp ra màn hình chính; provider cập nhật riêng từng appWidgetId.
- Giao diện gồm thanh tổng hợp và ba hồ sơ gần đây theo minh họa.

### V8.3.1 - GitHub build path fix
- Bộ dự án đầy đủ.
- Workflow tự tìm thư mục chứa settings.gradle.
- Kiểm tra từng tệp và hiển thị rõ OK/MISSING.
- Tự chuẩn hóa tên file HTML trong assets thành index.html nếu cần.


### V8.3.3
- Khôi phục JSON dùng trình chọn tệp Android native và nhận thêm application/json, text/plain, application/octet-stream.
- Widget hiện đại hơn, thêm thời gian cập nhật và biểu tượng Vấn đề/Sự kiện.
- Nút + trên widget mở thẳng giao diện tạo hồ sơ mới.


### V8.3.4
- Biểu tượng minh họa theo nhóm được đặt ngay bên trái tên hồ sơ.
- Dòng thứ hai hiển thị nhóm và địa điểm bên trái; thời gian cập nhật cùng mức độ được đặt bên phải trên cùng một dòng.
- Giữ khôi phục JSON native và nút + mở thẳng biểu mẫu tạo hồ sơ.
