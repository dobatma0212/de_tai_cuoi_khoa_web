package com.storechain.group4;

import com.storechain.group4.dto.CreateUserRequest;
import com.storechain.group4.dto.UserResponseDto;
import com.storechain.group4.model.Role;
import com.storechain.group4.model.RoleType;
import com.storechain.group4.model.User;
import com.storechain.group4.repository.RoleRepository;
import com.storechain.group4.repository.UserRepository;
import com.storechain.group4.security.services.CustomUserDetails;
import com.storechain.group4.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private jakarta.persistence.EntityManager entityManager;

    @InjectMocks
    private UserServiceImpl userService;

    private Role adminRole;
    private Role managerRole;
    private Role userRole;
    private CustomUserDetails adminUserDetails;
    private CustomUserDetails managerUserDetails;

    @BeforeEach
    void setUp() {
        adminRole = new Role(RoleType.ROLE_ADMIN, "Admin");
        adminRole.setId(1L);

        managerRole = new Role(RoleType.ROLE_MANAGER, "Manager");
        managerRole.setId(2L);

        userRole = new Role(RoleType.ROLE_USER, "User");
        userRole.setId(3L);

        adminUserDetails = new CustomUserDetails(
                1L, "admin", "admin@storechain.com", "Admin System",
                "0123456789", null, "active", "encodedPass", true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        managerUserDetails = new CustomUserDetails(
                2L, "manager", "manager@storechain.com", "Manager System",
                "0987654321", null, "active", "encodedPass", true,
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))
        );
    }

    @Test
    void testCreateUserSuccess() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .email("newuser@storechain.com")
                .password("password123")
                .fullName("New User")
                .role("ROLE_USER")
                .status("active")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@storechain.com")).thenReturn(false);
        when(roleRepository.findByName(RoleType.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        UserResponseDto created = userService.createUser(request, adminUserDetails);

        assertNotNull(created);
        assertEquals("newuser", created.getUsername());
        assertEquals("newuser@storechain.com", created.getEmail());
        assertEquals(RoleType.ROLE_USER, created.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testManagerCannotCreateAdmin() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("fakeadmin")
                .email("fakeadmin@storechain.com")
                .password("password123")
                .fullName("Fake Admin")
                .role("ROLE_ADMIN")
                .build();

        when(userRepository.existsByUsername("fakeadmin")).thenReturn(false);
        when(userRepository.existsByEmail("fakeadmin@storechain.com")).thenReturn(false);
        when(roleRepository.findByName(RoleType.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));

        SecurityException ex = assertThrows(SecurityException.class, () -> {
            userService.createUser(request, managerUserDetails);
        });

        assertTrue(ex.getMessage().contains("ADMIN"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testCannotDeleteSelf() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            User adminUser = User.builder().id(1L).username("admin").role(adminRole).build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
            userService.deleteUser(1L, adminUserDetails);
        });

        assertTrue(ex.getMessage().contains("chính mình"));
    }

    @Test
    void testManagerCannotDeleteAdmin() {
        User adminTarget = User.builder().id(1L).username("admin").role(adminRole).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminTarget));

        SecurityException ex = assertThrows(SecurityException.class, () -> {
            userService.deleteUser(1L, managerUserDetails);
        });

        assertTrue(ex.getMessage().contains("ADMIN"));
        verify(userRepository, never()).delete(any());
    }

    @Test
    void testGetUsersSuccess() {
        User user = User.builder().id(1L).username("user1").email("user1@test.com").role(userRole).status("active").build();
        org.springframework.data.domain.Page<User> page = new org.springframework.data.domain.PageImpl<>(List.of(user));
        when(userRepository.searchUsers(any(), any(), any(), any())).thenReturn(page);

        org.springframework.data.domain.Page<UserResponseDto> result = userService.getUsers(null, null, null, 0, 10, "id", "desc");
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("user1", result.getContent().get(0).getUsername());
    }

    @Test
    void testGetUserStatsSuccess() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByStatus("active")).thenReturn(8L);
        when(userRepository.countByStatus("locked")).thenReturn(1L);
        when(userRepository.countByStatus("banned")).thenReturn(1L);
        when(userRepository.countByRoleType(RoleType.ROLE_ADMIN)).thenReturn(1L);
        when(userRepository.countByRoleType(RoleType.ROLE_MANAGER)).thenReturn(2L);
        when(userRepository.countByRoleType(RoleType.ROLE_USER)).thenReturn(7L);

        var stats = userService.getUserStats();
        assertNotNull(stats);
        assertEquals(10L, stats.getTotalUsers());
        assertEquals(8L, stats.getActiveUsers());
        assertEquals(2L, stats.getLockedUsers());
        assertEquals(1L, stats.getAdminUsers());
        assertEquals(2L, stats.getManagerUsers());
        assertEquals(7L, stats.getRegularUsers());
    }

    @Test
    void testDeleteUserSuccess() {
        User target = User.builder().id(5L).username("targetUser").email("target@test.com").role(userRole).build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));
        jakarta.persistence.Query mockQuery = mock(jakarta.persistence.Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.executeUpdate()).thenReturn(1);

        userService.deleteUser(5L, adminUserDetails);
        verify(userRepository).delete(target);
    }

    @Test
    void testDeleteUserInternalSuccess() {
        User target = User.builder().id(6L).username("employeeTarget").email("emp@test.com").role(userRole).build();
        when(userRepository.findById(6L)).thenReturn(Optional.of(target));
        jakarta.persistence.Query mockQuery = mock(jakarta.persistence.Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.executeUpdate()).thenReturn(1);

        userService.deleteUserInternal(6L);
        verify(userRepository).delete(target);
    }
}

