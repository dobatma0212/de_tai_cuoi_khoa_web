# StoreChain (Male Fashion) — Hệ Thống Thương Mại Điện Tử & Quản Lý Chuỗi Cửa Hàng Thời Trang

Đồ án môn học thiết kế và phát triển ứng dụng web thương mại điện tử kết hợp quản lý chuỗi chi nhánh thời trang (**StoreChain - Group 4**), xây dựng trên nền tảng **Spring Boot**, **Spring Security (JWT)**, **Spring Data JPA (MySQL)**, **JSP + SiteMesh 3**, và **WebSocket (STOMP)**.

---

## Tính năng chính

### 1. Khách hàng & Cửa hàng trực tuyến (`USER` / `GUEST`)
- ** Khám phá sản phẩm:** Tìm kiếm, lọc đa tiêu chí (danh mục, chi nhánh, khoảng giá, kích cỡ, màu sắc), sắp xếp, lưu lịch sử tìm kiếm và sản phẩm đã xem gần đây.
- **Chi tiết sản phẩm & Đánh giá:** Chọn biến thể (Size, Màu sắc), kiểm tra tồn kho theo từng chi nhánh, xem đánh giá kèm hình ảnh/video (lưu trữ trên **Cloudinary**), thêm vào danh sách yêu thích.
- **Giỏ hàng & Đặt hàng:** Quản lý giỏ hàng theo biến thể, tự động tính giá **Flash Sale**, áp dụng mã **Voucher**, chọn đơn vị vận chuyển và địa chỉ giao hàng (hỗ trợ định vị khoảng cách địa lý).
- **Thanh toán đa phương thức:**
  - Thanh toán khi nhận hàng (**COD**)
  - Thanh toán trực tuyến qua **VNPAY Sandbox**
  - Thanh toán qua **MoMo** (hỗ trợ chế độ giả lập nội bộ và Sandbox)
  - Tự động hủy đơn chưa thanh toán quá hạn và hoàn trả tồn kho.
- **Theo dõi đơn hàng thời gian thực:** Nhận thông báo cập nhật trạng thái đơn hàng tức thời qua **WebSocket (STOMP)** và tự động chuyển trạng thái vận chuyển theo lịch trình.
- **Tài khoản cá nhân:** Đăng ký xác thực **OTP qua Email**, quên mật khẩu, cập nhật hồ sơ, đổi mật khẩu và sổ địa chỉ nhận hàng.

### 2. Nhân viên chi nhánh (`ROLE_EMPLOYEE`)
- **Bán hàng tại quầy (POS / Offline Order):** Tạo đơn hàng trực tiếp tại chi nhánh cho khách mua tại cửa hàng.
- **Quản lý đơn hàng chi nhánh:** Xác nhận và xử lý đơn hàng thuộc chi nhánh được phân công.
- **Chấm công & Hoa hồng:** Điểm danh ngày làm việc, theo dõi doanh số bán hàng và tính hoa hồng tự động (mặc định `2%` trên doanh thu đơn hàng hoàn tất).

### 3. Quản lý cửa hàng (`ROLE_MANAGER`)
- **Quản lý chi nhánh & Danh mục:** Cập nhật thông tin cửa hàng, danh mục sản phẩm riêng của chi nhánh (`ShopCategory`).
- **Quản lý kho & Nhập hàng (`StockReceipt`):** Tạo phiếu nhập kho theo từng biến thể sản phẩm, phân bổ tồn kho chi nhánh (`BranchStock`), tra cứu lịch sử biến động tồn kho (`StockMovement`).
- **Quản lý nhân sự chi nhánh:** Tuyển dụng, phân công và quản lý nhân viên tại chi nhánh, theo dõi bảng công và doanh số nhân viên.
- **Thống kê doanh thu:** Báo cáo doanh thu, số lượng đơn hàng theo thời gian và hiệu suất kinh doanh của chi nhánh.

### 4. Quản trị viên hệ thống (`ROLE_ADMIN`)
- **Quản lý người dùng (`/admin-users.html`):** Quản lý toàn bộ tài khoản, phân quyền (`ADMIN`, `MANAGER`, `EMPLOYEE`, `USER`), khóa/mở khóa tài khoản.
- **Quản lý sản phẩm toàn hệ thống (`/admin-products.html`):** Thêm/sửa/xóa sản phẩm, biến thể, tải ảnh lên Cloudinary hoặc thư mục cục bộ.
- **Quản lý danh mục gốc (`/admin-categories.html`):** Quản lý cây danh mục đa cấp toàn hệ thống.
- **Quản lý chuỗi chi nhánh (`/admin-shops.html`):** Thêm mới chi nhánh, gán quản lý (`Manager`), theo dõi hoạt động các cửa hàng.
- **Quản lý khuyến mãi (`/admin-promotions.html`):** Tạo chiến dịch **Flash Sale** (giảm giá trực tiếp trên sản phẩm) và **Voucher** (mã giảm theo `%` hoặc số tiền cố định, giới hạn lượt dùng).
- **Quản lý đơn vị vận chuyển (`/admin-transporters.html`):** Cấu hình đối tác giao hàng (GHN, GHTK, Viettel Post, J&T...) và cước phí cơ bản.

---

## Công nghệ sử dụng

| Thành phần | Công nghệ / Thư viện |
| :--- | :--- |
| **Ngôn ngữ & Nền tảng** | Java 26, Spring Boot 4.1.1 (Packaging: `WAR`) |
| **Bảo mật & Xác thực** | Spring Security, JJWT (`0.12.6`), BCrypt, OTP qua Gmail SMTP |
| **Cơ sở dữ liệu & ORM** | MySQL (Aiven Cloud / Local), Spring Data JPA (Hibernate), HikariCP |
| **Giao diện (Frontend)** | JSP, JSTL, SiteMesh 3 Decorator, Bootstrap, SASS/CSS, Vanilla JS / jQuery |
| **Thời gian thực** | Spring WebSocket (STOMP over SockJS) |
| **Lưu trữ đa phương tiện** | Cloudinary SDK (`cloudinary-http5`), Local Multipart Upload |
| **Cổng thanh toán** | VNPAY Sandbox, MoMo Payment (Mock & Sandbox API) |
| **Công cụ Build & Test** | Maven Wrapper (`mvnw`), Lombok, JUnit 5, Spring Boot Test |

---

## Cấu trúc thư mục dự án

```text
de_tai_cuoi_khoa_web/
├── src/
│   ├── main/
│   │   ├── java/com/storechain/group4/
│   │   │   ├── config/          # Cấu hình Cloudinary, WebMvc, WebSocket, UTF-8, DataInitializer
│   │   │   ├── controller/      # REST API Controllers & PageViewController (phục vụ JSP)
│   │   │   ├── dto/             # Data Transfer Objects (Request / Response payloads)
│   │   │   ├── exception/       # GlobalExceptionHandler xử lý lỗi tập trung
│   │   │   ├── model/           # Các JPA Entities & Enum (User, Shop, Product, Order, Promotion...)
│   │   │   ├── realtime/        # Lắng nghe và phát sự kiện đơn hàng qua WebSocket
│   │   │   ├── repository/      # Spring Data JPA Repositories
│   │   │   ├── security/        # Cấu hình Spring Security, bộ lọc JWT, CustomUserDetails
│   │   │   ├── service/         # Tầng nghiệp vụ (Interfaces, Implementations, Scheduler, Geo)
│   │   │   └── util/            # Tiện ích xử lý tồn kho theo size/màu (SizeStockUtil)
│   │   ├── resources/
│   │   │   ├── static/          # Tài nguyên tĩnh (CSS, JS, Fonts, Images, trang MoMo mock)
│   │   │   └── application.properties
│   │   └── webapp/
│   │       ├── WEB-INF/views/   # Các trang giao diện JSP (index, shop, checkout, admin-*, ...)
│   │       ├── decorator.jsp    # Layout chung cho khách hàng (SiteMesh 3)
│   │       └── admin-decorator.jsp # Layout chung cho trang quản trị
│   └── test/                    # Unit Tests & Integration Tests cho các Service
├── uploads/                     # Thư mục lưu tệp tải lên cục bộ
└── pom.xml                      # Khai báo dependencies và cấu hình Maven
```

---

## Hướng dẫn cài đặt và chạy dự án

### 1. Yêu cầu hệ thống
- **JDK:** Phiên bản 26 (hoặc chỉnh `<java.version>` trong `pom.xml` phù hợp với JDK trên máy, ví dụ JDK 21+).
- **Maven:** Đã tích hợp sẵn Maven Wrapper (`mvnw` / `mvnw.cmd`).
- **MySQL:** Phiên bản 8.0+ (Mặc định dự án đã được cấu hình sẵn kết nối MySQL Cloud trên Aiven trong `application.properties`, có thể chạy ngay hoặc đổi sang MySQL локаl).

### 2. Cấu hình môi trường (`src/main/resources/application.properties`)
Kiểm tra hoặc tùy chỉnh các thông số quan trọng nếu cần:
- **Database:** `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`
- **Gửi Email OTP (SMTP):** `spring.mail.username`, `spring.mail.password` (App Password Gmail)
- **Cloudinary:** Có thể đặt qua biến môi trường `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`
- **Thanh toán:**
  - `payment.base-url` (mặc định `http://localhost:8080`)
  - `payment.momo.mock=true` (dùng cổng thanh toán MoMo giả lập nội bộ để test nhanh không cần khóa API)

### 3. Chạy ứng dụng
Mở terminal tại thư mục gốc của dự án và chạy lệnh:

**Trên Windows (PowerShell / CMD):**
```powershell
.\mvnw.cmd spring-boot:run
```

**Trên macOS / Linux:**
```bash
./mvnw spring-boot:run
```

Sau khi ứng dụng khởi động thành công, truy cập tại:
- **Trang chủ cửa hàng:** http://localhost:8080/
- **Trang đăng nhập:** http://localhost:8080/login.html
- **Trang quản trị (Admin):** http://localhost:8080/admin-users.html

---

## Tài khoản & Dữ liệu mẫu mặc định

Khi khởi động lần đầu, `DataInitializer` sẽ tự động khởi tạo các vai trò, danh mục, 3 chi nhánh mẫu (TP.HCM, Hà Nội, Đà Nẵng), đơn vị vận chuyển, sản phẩm, chương trình Flash Sale / Voucher (`WELCOME15`, `SALE50K`) và các tài khoản mặc định sau:

| Vai trò | Tên đăng nhập | Email | Mật khẩu | Ghi chú |
| :--- | :--- | :--- | :--- | :--- |
| **Quản trị viên (`ROLE_ADMIN`)** | `admin` | `admin@storechain.com` | `admin123` | Toàn quyền quản trị hệ thống |
| **Quản lý (`ROLE_MANAGER`)** | `manager` | `manager@storechain.com` | `manager123` | Quản lý Chi nhánh Quận 1 |
| **Nhân viên (`ROLE_EMPLOYEE`)** | `employee` | `employee@storechain.com` | `employee123` | Nhân viên thuộc Chi nhánh Quận 1 |

---

## Chạy kiểm thử (Unit Tests)

Để chạy bộ kiểm thử tự động (Product/FlashSale pricing, Import kho, Quản lý nhân sự, JWT, User, Category, SizeStock...):

```powershell
.\mvnw.cmd test
```
