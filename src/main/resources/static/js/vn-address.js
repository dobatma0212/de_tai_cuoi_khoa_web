/**
 * Dropdown Tỉnh/Thành phố - Quận/Huyện chuẩn Việt Nam cho form địa chỉ.
 * Lấy dữ liệu từ API công khai provinces.open-api.vn (miễn phí, không cần key),
 * cache vào localStorage 1 ngày để không gọi lại mỗi lần mở form.
 * Nếu API lỗi mạng thì tự fallback về ô nhập tay để không chặn khách.
 *
 * Cách dùng (đã tự khởi chạy khi load trang):
 *   window.VnAddress.populate("addrProvince", "addrDistrict");
 *   window.VnAddress.setValues("addrProvince", "addrDistrict", "Bình Dương", "Thu Dầu Mot");
 */
(function () {
    "use strict";

    var API_URL = "https://provinces.open-api.vn/api/?depth=2";
    var CACHE_KEY = "sc_vn_provinces";
    var CACHE_TTL = 24 * 60 * 60 * 1000; // 1 ngày

    var provinces = null; // [{name, districts:[{name}]}]

    function readCache() {
        try {
            var raw = localStorage.getItem(CACHE_KEY);
            if (!raw) return null;
            var obj = JSON.parse(raw);
            if (!obj || !obj.data || !obj.data.length || Date.now() - obj.at > CACHE_TTL) return null;
            return obj.data;
        } catch (e) { return null; }
    }

    function writeCache(data) {
        try { localStorage.setItem(CACHE_KEY, JSON.stringify({ at: Date.now(), data: data })); } catch (e) { /* bỏ qua */ }
    }

    function fetchProvinces() {
        if (provinces) return Promise.resolve(provinces);
        var cached = readCache();
        if (cached) { provinces = cached; return Promise.resolve(cached); }
        return fetch(API_URL).then(function (res) {
            if (!res.ok) throw new Error("HTTP " + res.status);
            return res.json();
        }).then(function (list) {
            provinces = (list || []).map(function (p) {
                return {
                    name: p.name,
                    districts: (p.districts || []).map(function (d) { return d.name; })
                };
            });
            writeCache(provinces);
            return provinces;
        });
    }

    /** Chuẩn hóa để so khớp giá trị cũ đã lưu với tên API ("Thủ Dầu Mot" ~ "Thủ Dầu Một")
     * — bỏ dấu, bỏ tiền tố Tỉnh/Thành phố/Quận/Huyện, sotoContain. */
    function norm(s) {
        if (!s) return "";
        s = s.trim().toLowerCase();
        try {
            s = s.normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d");
        } catch (e) { /* trình duyệt cũ */ }
        return s.replace(/^(tinh|thanh pho|quan|huyen)\s+/, "").replace(/\s+/g, " ");
    }

    function findProvince(name) {
        if (!name) return null;
        var n = norm(name);
        return provinces.find(function (p) { return norm(p.name) === n; }) || null;
    }

    function fillSelect(sel, items, placeholder) {
        sel.innerHTML = '<option value="">' + placeholder + "</option>" +
            items.map(function (i) {
                return '<option value="' + escapeAttr(i) + '">' + escapeHtml2(i) + "</option>";
            }).join("");
    }

    function escapeHtml2(s) {
        return String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
    }
    function escapeAttr(s) { return escapeHtml2(s).replace(/"/g, "&quot;"); }

    /** Đồng bộ giao diện nice-select (nếu plugin đã bọc select) với option mới */
    function refreshNiceSelect(sel) {
        if (window.jQuery && window.jQuery.fn && window.jQuery.fn.niceSelect) {
            var $el = window.jQuery(sel);
            if ($el.next(".nice-select").length) $el.niceSelect("update");
        }
    }

    /** Đổ dữ liệu vào cặp select tỉnh / quận. API lỗi thì chuyển 2 ô về input text. */
    function populate(provinceSelId, districtSelId) {
        var pSel = document.getElementById(provinceSelId);
        var dSel = document.getElementById(districtSelId);
        if (!pSel || !dSel) return;
        fetchProvinces().then(function (list) {
            fillSelect(pSel, list.map(function (p) { return p.name; }), "— Chọn tỉnh/thành phố —");
            fillSelect(dSel, [], "— Chọn quận/huyện —");
            refreshNiceSelect(pSel);
            refreshNiceSelect(dSel);
            pSel.addEventListener("change", function () {
                var p = findProvince(pSel.value);
                fillSelect(dSel, p ? p.districts : [], "— Chọn quận/huyện —");
                refreshNiceSelect(dSel);
            });
            pSel.setAttribute("data-vn-ready", "1");
            pSel.dispatchEvent(new CustomEvent("vn-address:ready", { bubbles: true }));
        }).catch(function () {
            // Fallback: giữ nguyên ô nhập tay nếu JSP đã render input, hoặc tạo input mới
            fallbackToInput(pSel, provinceSelId, "Ví dụ: Bình Dương");
            fallbackToInput(dSel, districtSelId, "Ví dụ: Thu Dầu Một");
        });
    }

    function fallbackToInput(sel, id, placeholder) {
        if (!sel || sel.tagName === "INPUT") return;
        var input = document.createElement("input");
        input.type = "text";
        input.id = id;
        input.className = sel.className;
        input.placeholder = placeholder;
        input.required = sel.required;
        sel.parentNode.replaceChild(input, sel);
    }

    /** Gán giá trị đã lưu cũ (khi sửa địa chỉ): khớp mềm theo tên đã chuẩn hóa */
    function setValues(provinceSelId, districtSelId, province, district) {
        var pSel = document.getElementById(provinceSelId);
        var dSel = document.getElementById(districtSelId);
        if (!pSel || !dSel) return;
        fetchProvinces().then(function () {
            var p = findProvince(province);
            if (p) {
                pSel.value = p.name;
                fillSelect(dSel, p.districts, "— Chọn quận/huyện —");
                var n = norm(district);
                var match = p.districts.find(function (d) { return norm(d) === n; });
                if (match) dSel.value = match;
            } else {
                pSel.value = "";
            }
            refreshNiceSelect(pSel);
            refreshNiceSelect(dSel);
        }).catch(function () { /* fallback input tự giữ giá trị qua form.reset/gán trực tiếp */ });
    }

    // Tự khởi chạy với id mặc định của trang Hồ sơ
    if (document.getElementById("addrProvince") && document.getElementById("addrDistrict")) {
        populate("addrProvince", "addrDistrict");
    }

    window.VnAddress = { populate: populate, setValues: setValues };
})();
