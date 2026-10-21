# StoreCart — module giỏ hàng độc lập

Module được tách riêng để phát triển/kiểm thử trước khi tích hợp vào dự án StoreChain (Spring Boot + HTML/CSS/JS).

## 1. Chạy bản demo độc lập

1. Giải nén ZIP.
2. Mở `standalone/index.html` bằng trình duyệt. Nếu trình duyệt hạn chế ảnh ngoài, chạy server tĩnh tại thư mục `standalone`, ví dụ `python -m http.server 8080` rồi mở `http://localhost:8080`.
3. Bản demo có dữ liệu mẫu và lưu giỏ hàng trong `localStorage`. Tính năng gồm thêm sản phẩm, tăng/giảm số lượng, xóa từng sản phẩm, xóa toàn bộ, tạm tính, phí vận chuyển demo, mã `SAVE10` (giảm 10%) và `WELCOME50` (giảm tối đa 50.000đ).

**Lưu ý:** bản demo không tạo đơn hàng thật và không phải nơi lưu giỏ hàng production. `localStorage` chỉ phục vụ trình diễn độc lập.

## 2. Tích hợp vào StoreChain

Dự án StoreChain trong ZIP nguồn đã có API giỏ hàng ở `UserStoreController`, prefix `/api/user`:

- `GET /api/user/cart` — lấy giỏ hàng
- `GET /api/user/cart/count` — đếm số lượng mặt hàng
- `POST /api/user/cart` — thêm sản phẩm; body: `{"productId": 1, "quantity": 1, "size": "M", "color": "Black"}`
- `PATCH /api/user/cart/{itemId}` — cập nhật số lượng; body: `{"quantity": 2}`
- `DELETE /api/user/cart/{itemId}` — xóa một mặt hàng
- `DELETE /api/user/cart` — xóa giỏ hàng

### Các bước tích hợp

1. Sao lưu `src/main/resources/static/shopping-cart.html` và các file JS liên quan trước khi sửa.
2. Sao chép `integration/cart-api.js` vào `src/main/resources/static/js/cart-api.js`.
3. Trên trang cần dùng API, nạp script sau phần cấu hình chung: `<script src="/js/cart-api.js"></script>`.
4. Nút thêm sản phẩm gọi `StoreChainCartAPI.add(productId, quantity, size, color)`. `productId` phải là ID sản phẩm thật lấy từ backend, không dùng ID demo 101–104.
5. Trang giỏ hàng gọi `StoreChainCartAPI.get()` để lấy dữ liệu thật; dùng `item.id`, `item.productId`, `item.productName`, `item.imageUrl`, `item.price`, `item.quantity`, `item.stock`, `item.subtotal` để render. Gọi `update(item.id, quantity)` khi thay đổi số lượng và `remove(item.id)` khi xóa.
6. Đổi nút thanh toán sang `checkout.html` hoặc luồng checkout hiện tại. Không tạo đơn hàng chỉ từ dữ liệu demo.
7. Kiểm thử khi đã đăng nhập. Adapter đọc token từ `sc_auth.token` hoặc `jwt_token`, theo cách lưu phiên đang có trong `auth.js`.

### Ví dụ

```html
<script src="/js/cart-api.js"></script>
<button onclick="addCurrentProductToCart(REAL_PRODUCT_ID, 1, 'M', 'Black')">
  Thêm vào giỏ hàng
</button>
<script src="/js/product-detail-example.js"></script>
```

Hoặc gọi API trực tiếp:

```js
await StoreChainCartAPI.add(product.id, 1, selectedSize, selectedColor);
const items = await StoreChainCartAPI.get();
await StoreChainCartAPI.update(items[0].id, 2);
await StoreChainCartAPI.remove(items[0].id);
```

## 3. Trước khi đưa vào sử dụng thật

- Backend phải là nguồn quyết định giá, tồn kho, giảm giá và tổng tiền; không tin giá/tổng tiền do trình duyệt gửi.
- Kiểm tra xử lý sản phẩm hết hàng và giới hạn số lượng ở backend.
- Cần đăng nhập để gọi API `/api/user/cart`.
- Voucher trong demo chỉ là minh họa; khi tích hợp hãy gọi API kiểm tra voucher phía backend.
- Không đưa mật khẩu, JWT secret, API key hay thông tin kết nối database vào frontend.

## Cấu trúc

- `standalone/`: demo giỏ hàng độc lập, chạy bằng trình duyệt.
- `integration/cart-api.js`: adapter gọi API giỏ hàng hiện có của StoreChain.
- `integration/product-detail-example.js`: ví dụ nối nút thêm sản phẩm vào giỏ.
