# Widget load fix V8.6.3

## Nguyên nhân khả nghi đã loại bỏ
- Button trong cây RemoteViews được thay bằng TextView để tránh khác biệt inflate trên launcher tùy biến.
- Widget hiện chỉ dùng LinearLayout và TextView.
- Không thay đổi ID, PendingIntent, nguồn dữ liệu hoặc logic đồng bộ.

## Kiểm tra
- XML parse thành công.
- Tất cả ID XML và Java khớp nhau.
- Ô trên trái và mọi thành phần con cùng mở tạo hồ sơ mới.
- Widget đọc từ full data và tự dựng compact data.
- Metadata rút gọn độc lập; progress luôn giữ đủ sáu dấu.
