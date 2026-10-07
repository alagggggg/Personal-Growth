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


### V8.3.5
- Sáu dấu chấm bên phải dòng hai thể hiện tiến độ thực tế theo 6 bước.
- Dải màu mép trái chỉ thể hiện mức độ ban đầu: 1 xám, 2 xanh nhạt, 3 vàng, 4 cam, 5 đỏ.
- Không dùng thanh tiến độ dưới cùng.
- Biểu tượng nhóm nằm bên trái tên hồ sơ; giữ khôi phục JSON native và nút + mở biểu mẫu mới.


### V8.3.6
- Phần Hồ sơ gần đây trong giao diện Ghi nhận được thiết kế lại đồng bộ với ngôn ngữ thẻ của widget.
- Biểu tượng nhóm nằm bên trái tiêu đề; dải màu trái thể hiện mức độ ban đầu.
- Dòng thứ hai hiển thị nhóm, địa điểm, thời gian cập nhật và 6 dấu chấm tiến độ.
- Thẻ trong ứng dụng có thêm thanh 6 đoạn ở cạnh dưới theo bố cục minh họa; widget vẫn giữ quy tắc không có thanh dưới.


### V8.3.7
- Bỏ thanh tiến độ sáu đoạn phía dưới thẻ Hồ sơ gần đây trong ứng dụng.
- Giữ sáu dấu chấm tiến độ ở bên phải dòng thông tin.
- Widget vẫn giữ bố cục sáu dấu chấm, không có thanh dưới.


### V8.3.8
- Widget dùng hàng cố định để HiOS luôn hiển thị đủ dòng thứ hai.
- Dòng thứ hai gồm tên nhóm, địa điểm, thời gian dd/MM/yy · HH:mm và sáu dấu chấm tiến độ.
- Biểu tượng nhóm chuyển sang emoji giống giao diện Ghi nhận.
- Dải màu trái tiếp tục thể hiện mức độ ban đầu.


### V8.3.9
- Đồng bộ typography và bố cục giữa Hồ sơ gần đây trong Ghi nhận và widget.
- Tăng cỡ chữ widget: tiêu đề 10sp, nhóm/địa điểm 8sp, thời gian 7sp, sáu dấu chấm 8sp.
- Trang Ghi nhận dùng title 24px, subtitle ngắn, ô tìm kiếm 42px và thẻ hồ sơ gần đây gọn hơn.
- Thêm màn hình tải tối giản và tooltip cho nút chế độ sáng/tối, cài đặt.


### V8.4.0
- Thêm Người liên quan vào dòng thông tin của Hồ sơ gần đây và widget.
- Thứ tự hiển thị: Nhóm · 👤 Người liên quan · Địa điểm.
- Nếu hồ sơ không có Người liên quan, giao diện tự bỏ phần này và giữ bố cục cũ.
- Giữ ngày giờ và sáu dấu chấm tiến độ ở bên phải.


### V8.4.2
- Đồng bộ màu chữ trạng thái và sáu dấu chấm ở Ghi nhận với widget.
- Khôi phục JSON chịu được BOM, ký tự null, JSON lồng và cấu trúc data.cases.
- Chuẩn hóa hồ sơ trước khi lưu và báo lỗi cụ thể.
- Khi thiếu bộ nhớ do ảnh, cho phép khôi phục hồ sơ không gồm ảnh.
- Giữ bố cục widget ngày giờ sát sáu dấu chấm để mở rộng metadata.


### V8.4.3
- Bổ sung nhận diện UTF-8 BOM, UTF-16LE và UTF-16BE khi đọc tệp JSON sao lưu.
- Giữ bộ phân tích JSON chịu lỗi và quy tắc màu đồng bộ của V8.4.2.


### V8.4.6
- Sửa chênh lệch dữ liệu chi tiết khi mở cùng hồ sơ từ widget và từ giao diện Ghi nhận.
- Khi mở từ widget, WebView luôn tải mới thay vì khôi phục trạng thái màn hình cũ.
- Trước khi mở chi tiết, ứng dụng đọc lại hồ sơ đầy đủ từ localStorage; không dùng dữ liệu compact của widget làm dữ liệu chi tiết.
- Sau khi cập nhật từ widget, giao diện và danh sách được render lại, widget được đồng bộ, sau đó mới đóng task.
- Khi mở từ ứng dụng, hành vi bình thường và dữ liệu đầy đủ vẫn được giữ.


### V8.4.7
- Sửa triệt để lỗi cập nhật từ widget không xuất hiện khi mở ứng dụng.
- Mỗi lần lưu ghi đồng thời dữ liệu đầy đủ vào localStorage và Android SharedPreferences bằng commit đồng bộ.
- Khi mở ứng dụng hoặc mở hồ sơ từ widget, hệ thống chọn bản dữ liệu đầy đủ có updatedAt mới hơn.
- Dữ liệu compact của widget chỉ dùng để hiển thị, không còn là nguồn dữ liệu cập nhật hồ sơ.
- Chỉ tự đóng sau khi xác nhận cả hai vùng lưu dữ liệu đều thành công.


### V8.4.8
- Sau kiểm thử mở lại ứng dụng, thay cơ chế chọn toàn bộ nguồn bằng hợp nhất từng hồ sơ theo ID.
- Với mỗi hồ sơ, bản có updatedAt mới hơn được giữ; hồ sơ khác có thời gian mới hơn không thể che mất cập nhật từ widget.
- Sắp xếp lại danh sách theo thời gian cập nhật sau khi hợp nhất.


### V8.4.9
- Sửa lỗi hồ sơ cũ xuất hiện lại sau Xóa toàn bộ dữ liệu và mở lại ứng dụng.
- Thêm mốc xóa toàn bộ (clear tombstone) lưu ở cả localStorage và Android SharedPreferences.
- Khi đọc dữ liệu, mọi hồ sơ có updatedAt cũ hơn hoặc bằng mốc xóa đều bị bỏ qua.
- Xóa đồng thời dữ liệu đầy đủ, dữ liệu widget và cập nhật widget bằng commit đồng bộ.
- Tạo hồ sơ mới hoặc khôi phục dữ liệu mới sẽ xóa mốc chặn và chỉ giữ dữ liệu mới.
- Mỗi lần lưu hồ sơ gắn `_savedAt` nội bộ; dữ liệu mới hoặc dữ liệu khôi phục sau khi xóa được nhận diện mà không cần bỏ mốc xóa.
