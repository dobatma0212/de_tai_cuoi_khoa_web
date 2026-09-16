package com.storechain.group4;

import com.storechain.group4.dto.ShopRequestDto;
import com.storechain.group4.dto.ShopResponseDto;
import com.storechain.group4.dto.ShopStatsDto;
import com.storechain.group4.dto.UserResponseDto;
import com.storechain.group4.model.Role;
import com.storechain.group4.model.RoleType;
import com.storechain.group4.model.Shop;
import com.storechain.group4.model.User;
import com.storechain.group4.repository.ShopRepository;
import com.storechain.group4.repository.UserRepository;
import com.storechain.group4.service.impl.ShopServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShopServiceTest {

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ShopServiceImpl shopService;

    private User managerUser;
    private User normalUser;
    private Shop sampleShop;

    @BeforeEach
    void setUp() {
        Role managerRole = new Role(RoleType.ROLE_MANAGER, "Quản lý");
        Role userRole = new Role(RoleType.ROLE_USER, "Khách hàng");

        managerUser = User.builder()
                .id(2L)
                .username("manager1")
                .fullName("Quản Lý Chi Nhánh 1")
                .email("manager1@malefashion.vn")
                .phone("0901234567")
                .role(managerRole)
                .status("active")
                .build();

        normalUser = User.builder()
                .id(3L)
                .username("customer1")
                .fullName("Khách hàng A")
                .email("customer1@gmail.com")
                .role(userRole)
                .status("active")
                .build();

        sampleShop = Shop.builder()
                .id(1L)
                .name("Male Fashion Quận 1")
                .slug("male-fashion-quan-1")
                .address("123 Nguyễn Huệ, Q.1, TP.HCM")
                .phone("028 3822 1234")
                .description("Chi nhánh trung tâm")
                .status("active")
                .manager(managerUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateShop_Success_WithManager() {
        ShopRequestDto request = ShopRequestDto.builder()
                .name("Male Fashion Thủ Đức")
                .address("45 Võ Văn Ngân, Thủ Đức")
                .phone("028 3899 9999")
                .description("Chi nhánh mới mở")
                .status("active")
                .managerId(2L)
                .build();

        when(shopRepository.existsByNameIgnoreCase("Male Fashion Thủ Đức")).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(managerUser));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> {
            Shop s = invocation.getArgument(0);
            s.setId(10L);
            return s;
        });

        ShopResponseDto result = shopService.createShop(request);

        assertNotNull(result);
        assertEquals("Male Fashion Thủ Đức", result.getName());
        assertEquals("male-fashion-thu-duc", result.getSlug());
        assertEquals(2L, result.getManagerId());
        assertEquals("manager1", result.getManagerUsername());
        verify(shopRepository, times(1)).save(any(Shop.class));
    }

    @Test
    void testCreateShop_DuplicateName_ThrowsException() {
        ShopRequestDto request = ShopRequestDto.builder()
                .name("Male Fashion Quận 1")
                .build();

        when(shopRepository.existsByNameIgnoreCase("Male Fashion Quận 1")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            shopService.createShop(request);
        });

        assertTrue(ex.getMessage().contains("đã tồn tại"));
        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    void testCreateShop_UserNotManager_ThrowsException() {
        ShopRequestDto request = ShopRequestDto.builder()
                .name("Chi nhánh Mới")
                .managerId(3L)
                .build();

        when(shopRepository.existsByNameIgnoreCase("Chi nhánh Mới")).thenReturn(false);
        when(userRepository.findById(3L)).thenReturn(Optional.of(normalUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            shopService.createShop(request);
        });

        assertTrue(ex.getMessage().contains("ROLE_MANAGER"));
        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    void testUpdateShop_Success() {
        ShopRequestDto updateRequest = ShopRequestDto.builder()
                .name("Male Fashion Quận 1 Updated")
                .address("999 Nguyễn Huệ, Q.1")
                .phone("028 9999 8888")
                .description("Mô tả mới")
                .status("inactive")
                .managerId(2L)
                .build();

        when(shopRepository.findById(1L)).thenReturn(Optional.of(sampleShop));
        when(shopRepository.existsByNameIgnoreCaseAndIdNot("Male Fashion Quận 1 Updated", 1L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(managerUser));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopResponseDto result = shopService.updateShop(1L, updateRequest);

        assertEquals("Male Fashion Quận 1 Updated", result.getName());
        assertEquals("inactive", result.getStatus());
        assertEquals("999 Nguyễn Huệ, Q.1", result.getAddress());
        verify(shopRepository).save(any(Shop.class));
    }

    @Test
    void testUpdateStatus_Success() {
        when(shopRepository.findById(1L)).thenReturn(Optional.of(sampleShop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopResponseDto result = shopService.updateStatus(1L, "inactive");

        assertEquals("inactive", result.getStatus());
        verify(shopRepository).save(sampleShop);
    }

    @Test
    void testAssignManager_Success() {
        sampleShop.setManager(null);
        when(shopRepository.findById(1L)).thenReturn(Optional.of(sampleShop));
        when(userRepository.findById(2L)).thenReturn(Optional.of(managerUser));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopResponseDto result = shopService.assignManager(1L, 2L);

        assertEquals(2L, result.getManagerId());
        assertEquals("manager1", result.getManagerUsername());
    }

    @Test
    void testAssignManager_Unassign_Success() {
        when(shopRepository.findById(1L)).thenReturn(Optional.of(sampleShop));
        when(shopRepository.save(any(Shop.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopResponseDto result = shopService.assignManager(1L, 0L);

        assertNull(result.getManagerId());
        assertNull(result.getManagerUsername());
    }

    @Test
    void testDeleteShop_CleansProductsAndDeletes() {
        Query mockQuery = mock(Query.class);
        when(mockQuery.setParameter(eq("shopId"), eq(1L))).thenReturn(mockQuery);
        when(mockQuery.executeUpdate()).thenReturn(5);

        when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(shopRepository.findById(1L)).thenReturn(Optional.of(sampleShop));
        doNothing().when(shopRepository).delete(sampleShop);

        assertDoesNotThrow(() -> shopService.deleteShop(1L));

        verify(entityManager).createNativeQuery(contains("DELETE FROM products WHERE shop_id = :shopId"));
        verify(shopRepository).delete(sampleShop);
    }

    @Test
    void testGetStats_Success() {
        when(shopRepository.count()).thenReturn(10L);
        when(shopRepository.countByStatus("active")).thenReturn(8L);
        when(shopRepository.countByStatus("inactive")).thenReturn(2L);
        when(shopRepository.countByManagerIsNotNull()).thenReturn(7L);

        ShopStatsDto stats = shopService.getStats();

        assertEquals(10L, stats.getTotalShops());
        assertEquals(8L, stats.getActiveShops());
        assertEquals(2L, stats.getInactiveShops());
        assertEquals(7L, stats.getAssignedShops());
    }

    @Test
    void testGetAvailableManagers_Success() {
        when(userRepository.findByRoleType(RoleType.ROLE_MANAGER))
                .thenReturn(List.of(managerUser));

        List<UserResponseDto> managers = shopService.getAvailableManagers();

        assertNotNull(managers);
        assertEquals(1, managers.size());
        assertEquals("manager1", managers.get(0).getUsername());
        assertEquals(RoleType.ROLE_MANAGER, managers.get(0).getRole());
    }

    @Test
    void testGetShops_Paginated() {
        Page<Shop> page = new PageImpl<>(List.of(sampleShop));
        when(shopRepository.searchShops(any(), any(), any(Pageable.class))).thenReturn(page);

        Page<ShopResponseDto> result = shopService.getShops("Quận 1", "active", 0, 10, "id", "desc");

        assertEquals(1, result.getTotalElements());
        assertEquals("Male Fashion Quận 1", result.getContent().get(0).getName());
    }
}
