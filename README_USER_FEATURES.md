# StoreChain – Bản tích hợp đầy đủ tính năng User + VNPay/MoMo

## Đã tích hợp

### Backend
1. **Yêu thích (Favorite)** – entity, API toggle/list/count
2. **Đã xem (ProductView)** – ghi nhận + danh sách
3. **Top 20 sản phẩm** – newest / bestseller / top_rated / most_favorited (phân trang)
4. **Review** – min 50 ký tự + chỉ khi đơn RECEIVED + không review trùng
5. **Đơn hàng theo trạng thái** – `GET /api/user/orders?status=SHIPPING`
6. **OrderCreateRequest** – paymentMethod, shippingAddressId, địa chỉ
7. **VNPay / MoMo skeleton** – PaymentService + PaymentController + config trong application.properties

### Frontend
- `js/storechain-api.js` – helper API dùng chung
- **index.html** – 4 tab top sản phẩm + lazy "Xem thêm"
- **product-detail.html** – ghi nhận đã xem, nút yêu thích, validate review ≥ 50 ký tự
- **checkout.html** – chọn COD / VNPay / MoMo + mã giảm giá + gọi tạo link thanh toán
- **profile.html** – đơn hàng, yêu thích, đã xem + thông báo kết quả thanh toán
- **shopping-cart.html** – gắn API helper

## Cấu hình thanh toán

Trong `application.properties`:

```properties
app.payment.vnpay.tmn-code=...
app.payment.vnpay.hash-secret=...
app.payment.momo.partner-code=...
app.payment.momo.access-key=...
app.payment.momo.secret-key=...
```

Sandbox VNPay: https://sandbox.vnpayment.vn  
MoMo test: https://test-payment.momo.vn  

Khi chưa điền key, API vẫn chạy và trả message hướng dẫn cấu hình.

## API chính mới

| Method | Path | Mô tả |
|--------|------|--------|
| POST | /api/user/favorites/{productId} | Toggle yêu thích |
| GET | /api/user/favorites | Danh sách yêu thích |
| POST | /api/user/views/{productId} | Ghi nhận đã xem |
| GET | /api/user/views | Sản phẩm đã xem |
| GET | /api/public/top/products?type=&page=&size= | Top sản phẩm |
| GET | /api/user/orders?status= | Lọc đơn theo trạng thái |
| POST | /api/payment/create | Tạo link VNPay/MoMo |
| GET | /api/payment/vnpay/return | Callback VNPay |
| GET/POST | /api/payment/momo/return\|ipn | Callback MoMo |

## Chạy

```bash
./mvnw spring-boot:run
```

Hibernate `ddl-auto=update` sẽ tự tạo bảng `favorites`, `product_views`.
