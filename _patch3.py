import io

p = "src/main/java/com/storechain/group4/service/impl/StorefrontServiceImpl.java"
s = io.open(p, encoding="utf-8").read()

OLD = '''    /** Đẩy mốc giao tự động: Đã xác nhận quá 10s -> Đang giao, quá 10s nữa -> Đã giao */
    private void autoAdvanceStaleOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> toSave = new ArrayList<>();
        orderRepository.findByStatus(Order.STATUS_CONFIRMED).stream()
                .filter(o -> o.getUpdatedAt() != null && o.getUpdatedAt().plusSeconds(10).isBefore(now))
                .peek(o -> o.setStatus(Order.STATUS_SHIPPING))
                .forEach(toSave::add);
        orderRepository.findByStatus(Order.STATUS_SHIPPING).stream()
                .filter(o -> o.getUpdatedAt() != null && o.getUpdatedAt().plusSeconds(10).isBefore(now))
                .peek(o -> o.setStatus(Order.STATUS_DELIVERED))
                .forEach(toSave::add);
        if (!toSave.isEmpty()) {
            orderRepository.saveAll(toSave);
        }
    }'''
NEW = '''    /**
     * Đơn phải được quản lý/nhân viên xác nhận trước rồi mới tự động trượt xuống:
     * Đã xác nhận qua {app.order.shipping-delay-minutes} phút -> Đang giao,
     * Đang giao qua {app.order.deliver-delay-minutes} phút -> Đã giao.
     * Đơn mới (NEW) không bao giờ tự động chuyển.
     */
    private void autoAdvanceStaleOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> toSave = new ArrayList<>();
        orderRepository.findByStatus(Order.STATUS_CONFIRMED).stream()
                .filter(o -> o.getUpdatedAt() != null
                        && o.getUpdatedAt().plusMinutes(shippingDelayMinutes).isBefore(now))
                .peek(o -> o.setStatus(Order.STATUS_SHIPPING))
                .forEach(toSave::add);
        orderRepository.findByStatus(Order.STATUS_SHIPPING).stream()
                .filter(o -> o.getUpdatedAt() != null
                        && o.getUpdatedAt().plusMinutes(deliverDelayMinutes).isBefore(now))
                .peek(o -> o.setStatus(Order.STATUS_DELIVERED))
                .forEach(toSave::add);
        if (!toSave.isEmpty()) {
            orderRepository.saveAll(toSave);
        }
    }'''
assert OLD in s, "auto advance not found"
s = s.replace(OLD, NEW, 1)

anchor = "public class StorefrontServiceImpl {"
assert anchor in s
s = s.replace(anchor, anchor + '''

    /** Số phút đơn đã xác nhận chuyển sang Đang giao (mặc định 2 phút) */
    @org.springframework.beans.factory.annotation.Value("${app.order.shipping-delay-minutes:2}")
    private long shippingDelayMinutes;

    /** Số phút đơn Đang giao chuyển sang Đã giao (mặc định 5 phút) */
    @org.springframework.beans.factory.annotation.Value("${app.order.deliver-delay-minutes:5}")
    private long deliverDelayMinutes;''', 1)

io.open(p, "w", encoding="utf-8", newline="\n").write(s)
print("patched StorefrontServiceImpl")

RETURNED_OLD = '["RETURNED", "Trả hàng - hoàn tiền"],'
RETURNED_NEW = '["RETURNED", "Đã trả hàng - hoàn tiền"],'
REQ_OLD = '["RETURN_REQUESTED", "Yêu cầu trả hàng"]'
REQ_NEW = '["RETURN_REQUESTED", "Khách yêu cầu trả hàng"]'

for path in ["src/main/resources/static/js/employee.js", "src/main/resources/static/js/profile.js"]:
    s = io.open(path, encoding="utf-8").read()
    assert RETURNED_OLD in s, path + " returned"
    assert REQ_OLD in s, path + " requested"
    s = s.replace(RETURNED_OLD, RETURNED_NEW, 1).replace(REQ_OLD, REQ_NEW, 1)
    io.open(path, "w", encoding="utf-8", newline="\n").write(s)
    print("patched", path)
