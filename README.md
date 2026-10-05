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


### V8.3.2 - Widget hiện đại
- Thanh tiêu đề chuyển sang gradient xanh, có tên ứng dụng và thống kê 3 trạng thái.
- Ba hồ sơ gần đây hiển thị dạng thẻ bo góc, có dải màu nhóm, trạng thái dạng nhãn và mức độ ở bên phải.
- Tên nhóm và địa điểm dùng tên đầy đủ, tự rút gọn khi dài.
- Giữ RemoteViews thuần để tương thích HiOS Launcher.
