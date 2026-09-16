package com.storechain.group4.controller;

import com.storechain.group4.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestRoleController {

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<String>> allAccess() {
        return ResponseEntity.ok(ApiResponse.success("Nội dung công khai, không cần đăng nhập!"));
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> userAccess() {
        return ResponseEntity.ok(ApiResponse.success("Dành cho USER hoặc ADMIN."));
    }

    @GetMapping("/guest")
    @PreAuthorize("hasRole('GUEST') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> guestAccess() {
        return ResponseEntity.ok(ApiResponse.success("Dành cho GUEST hoặc ADMIN."));
    }

    @GetMapping("/manager")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> managerAccess() {
        return ResponseEntity.ok(ApiResponse.success("Dành cho MANAGER hoặc ADMIN."));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> adminAccess() {
        return ResponseEntity.ok(ApiResponse.success("Dành RIÊNG cho ADMIN."));
    }
}

