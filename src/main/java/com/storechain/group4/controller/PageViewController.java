package com.storechain.group4.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Set;

/**
 * Phục vụ các trang giao diện JSP giữ nguyên URL .html cũ (ví dụ /index.html),
 * forward tới /WEB-INF/views/<page>.jsp để SiteMesh bọc decorator.
 * Toàn bộ link trong JS/markup không cần thay đổi.
 */
@Controller
public class PageViewController {

    private static final Set<String> PAGES = Set.of(
            // trang public
            "index", "shop", "shop-details", "product-detail", "shopping-cart", "checkout",
            "login", "register", "forgot-password", "profile",
            "blog", "blog-details", "about", "contact",
            // trang quản trị
            "admin-users", "admin-products", "admin-categories", "admin-shops",
            "admin-transporters", "admin-promotions");

    @GetMapping("/")
    public String home() {
        return "forward:/WEB-INF/views/index.jsp";
    }

    @GetMapping("/{page}.html")
    public String page(@PathVariable String page) {
        if (!PAGES.contains(page)) {
            return "forward:/WEB-INF/views/index.jsp";
        }
        return "forward:/WEB-INF/views/" + page + ".jsp";
    }
}
