package com.storechain.group4.config;

import com.storechain.group4.security.jwt.JwtUtils;
import com.storechain.group4.security.services.CustomUserDetails;
import com.storechain.group4.security.services.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Set;

/**
 * WebSocket (STOMP) cho thông báo và cập nhật đơn hàng theo thời gian thực.
 * Endpoint: /ws  — client gửi JWT trong header "Authorization" của frame CONNECT.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String USER_ORDERS = "/topic/orders/user/";
    private static final String SHOP_ORDERS = "/topic/orders/shop/";
    private static final Set<String> STAFF_ROLES = Set.of("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE");

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null || accessor.getCommand() == null) {
                    return message;
                }

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
                } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    authorizeSubscription(accessor);
                }
                return message;
            }
        });
    }

    private Authentication authenticate(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new MessagingException("Thiếu token đăng nhập");
        }
        String jwt = header.substring(7);
        if (!jwtUtils.validateJwtToken(jwt)) {
            throw new MessagingException("Token không hợp lệ");
        }
        var userDetails = userDetailsService.loadUserByUsername(jwtUtils.getUserNameFromJwtToken(jwt));
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (!(accessor.getUser() instanceof Authentication auth)
                || !(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new MessagingException("Chưa đăng nhập");
        }
        if (destination == null) {
            throw new MessagingException("Thiếu destination");
        }
        if (destination.startsWith(USER_ORDERS)) {
            // Khách chỉ được nghe đơn của chính mình
            if (!destination.equals(USER_ORDERS + user.getId())) {
                throw new MessagingException("Không có quyền theo dõi đơn của người khác");
            }
        } else if (destination.startsWith(SHOP_ORDERS)) {
            boolean staff = auth.getAuthorities().stream()
                    .anyMatch(a -> STAFF_ROLES.contains(a.getAuthority()));
            if (!staff) {
                throw new MessagingException("Không có quyền theo dõi đơn của chi nhánh");
            }
        } else {
            throw new MessagingException("Destination không được hỗ trợ");
        }
    }
}
