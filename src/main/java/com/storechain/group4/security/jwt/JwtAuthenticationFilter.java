package com.storechain.group4.security.jwt;

import com.storechain.group4.security.services.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            if (isDatabaseFailure(e)) {
                // DB quá tải / mất kết nối: KHÔNG coi là "chưa đăng nhập" (sẽ trả 401 và
                // front-end tự đăng xuất người dùng) mà báo 503 để client thử lại sau.
                log.error("Không truy cập được CSDL khi xác thực người dùng: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"success\":false,\"message\":\"Hệ thống đang bận kết nối cơ sở dữ liệu, vui lòng thử lại sau ít phút.\"}");
                return;
            }
            log.error("Không thể thiết lập xác thực người dùng: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isDatabaseFailure(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof org.springframework.dao.DataAccessException
                    || t instanceof org.springframework.transaction.TransactionException
                    || t instanceof java.sql.SQLException) {
                return true;
            }
            if (t.getCause() == t) break;
        }
        return false;
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}

