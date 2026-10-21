# Tích hợp module giỏ hàng StoreCart vào StoreChain

## Đã tích hợp
- `src/main/resources/static/js/cart-api.js`: adapter tập trung cho các API `/api/user/cart`.
- `shopping-cart.html` nạp adapter trước `cart.js`; trang giỏ hàng vẫn giữ các luồng vận chuyển, voucher, checkout và đơn hàng.
- `cart.js` dùng adapter cho lấy giỏ, tăng/giảm số lượng, xóa từng sản phẩm và xóa toàn bộ; các API đơn hàng/vận chuyển vẫn giữ nguyên.
- `shop.html` và `product-detail.html` nạp adapter; thao tác thêm sản phẩm và badge số lượng giỏ hàng gọi qua adapter.
- Mã nguồn demo độc lập được giữ trong `docs/storecart-module/standalone/`; demo sử dụng localStorage và chỉ phục vụ trình diễn.

## Chạy và kiểm thử
1. Chạy ứng dụng Spring Boot như bình thường và đăng nhập.
2. Mở `/shop.html`, thêm một sản phẩm, mở `/shopping-cart.html`.
3. Thử tăng/giảm số lượng, xóa một sản phẩm, xóa tất cả.
4. Kiểm tra voucher, chọn đơn vị vận chuyển và đặt hàng; xem đơn ở phần “Đơn hàng của tôi”.
5. Kiểm tra `/product-detail.html?productId=<ID_THAT>` và thêm sản phẩm với size/màu nếu có.

## Lưu ý
- Giỏ hàng tích hợp lưu ở database qua backend, không dùng localStorage demo.
- Backend phải xác thực giá/tồn kho/voucher và tổng tiền; frontend không phải nguồn dữ liệu đáng tin cậy.
- Adapter dùng token từ `ScAuth.getSession()`-compatible storage (`sc_auth.token`) hoặc `jwt_token`.
- Cần chạy build/test và kiểm thử thủ công trong môi trường có database, vì không thể xác nhận tích hợp runtime chỉ bằng kiểm tra tĩnh.
