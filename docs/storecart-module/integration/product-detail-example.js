/** Example integration snippet for a product-detail page. */
async function addCurrentProductToCart(productId, quantity, size, color) {
  try {
    if (!window.StoreChainCartAPI) throw new Error('Chưa nạp integration/cart-api.js');
    await window.StoreChainCartAPI.add(productId, quantity || 1, size || '', color || '');
    alert('Đã thêm sản phẩm vào giỏ hàng!');
    // Update the cart badge if your header has this element.
    const badge = document.querySelector('[data-cart-count]');
    if (badge) badge.textContent = await window.StoreChainCartAPI.count();
  } catch (error) {
    if (error.status === 401 || error.status === 403) {
      alert('Vui lòng đăng nhập trước khi thêm vào giỏ hàng.');
      return;
    }
    alert(error.message || 'Không thể thêm sản phẩm vào giỏ hàng.');
  }
}
// Example button: <button onclick="addCurrentProductToCart(productId, 1, selectedSize, selectedColor)">Thêm vào giỏ</button>
