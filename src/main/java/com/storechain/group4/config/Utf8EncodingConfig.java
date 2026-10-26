package com.storechain.group4.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Ép UTF-8 cho response của các trang JSP.
 *
 * Cần thiết vì: (1) decorator JSP được SiteMesh include — theo spec Servlet, JSP được
 * include không được phép đổi content-type của response, nên nếu không set trước Tomcat
 * dùng ISO-8859-1 mặc định → tiếng Việt trong header/footer bị lỗi font; (2) một số bước
 * trong pipeline render gọi setContentType không kèm charset, khiến charset rơi về mặc
 * định của Tomcat (ISO-8859-1) ngay trước khi SiteMesh ghi nội dung ra response.
 *
 * Wrapper ghi đè setContentType/setCharacterEncoding để mọi response text/html trên các
 * URL *.html và / luôn là UTF-8. API JSON ( Jackson tự ghi byte qua OutputStream) không
 * bị ảnh hưởng vì filter chỉ áp dụng cho *.html và /.
 */
@Configuration
public class Utf8EncodingConfig {

    @Bean
    public FilterRegistrationBean<Filter> utf8ResponseEncodingFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(
                (request, response, chain) -> {
                    HttpServletResponse res = (HttpServletResponse) response;
                    res.setCharacterEncoding("UTF-8");
                    chain.doFilter(request, new HttpServletResponseWrapper(res) {
                        @Override
                        public void setContentType(String type) {
                            if (type != null && type.toLowerCase().contains("iso-8859-1")) {
                                type = type.replaceAll("(?i)iso-8859-1", "UTF-8");
                            } else if (type != null && type.toLowerCase().startsWith("text/html")
                                    && !type.toLowerCase().contains("charset")) {
                                type = type + ";charset=UTF-8";
                            }
                            super.setContentType(type);
                            super.setCharacterEncoding("UTF-8");
                        }

                        @Override
                        public void setCharacterEncoding(String charset) {
                            super.setCharacterEncoding("UTF-8");
                        }
                    });
                });
        registration.addUrlPatterns("*.html", "/");
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("utf8ResponseEncodingFilter");
        return registration;
    }
}
