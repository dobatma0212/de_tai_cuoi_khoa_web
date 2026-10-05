package com.storechain.group4;

import com.storechain.group4.dto.EmployeeDto;
import com.storechain.group4.model.Role;
import com.storechain.group4.model.RoleType;
import com.storechain.group4.model.Shop;
import com.storechain.group4.model.User;
import com.storechain.group4.repository.RoleRepository;
import com.storechain.group4.repository.UserRepository;
import com.storechain.group4.service.EmployeeService;
import com.storechain.group4.service.impl.ManagerShopServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Sa thải nhân viên KHÔNG xóa tài khoản: trả về ROLE_USER, giữ shop_id
 * để quản lý vẫn xem được thông tin + thống kê sau khi sa thải.
 */
@ExtendWith(MockitoExtension.class)
public class ManagerEmployeeFireTest {

    @Mock
    private com.storechain.group4.repository.ShopRepository shopRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private ManagerShopServiceImpl service;

    private Shop shop;
    private User manager;
    private User staff;
    private Role userRole;
    private Role employeeRole;

    @BeforeEach
    public void setupInject() {
        // employeeService trong impl là field @Autowired (không qua constructor)
        org.springframework.test.util.ReflectionTestUtils.setField(service, "employeeService", employeeService);
    }

    @BeforeEach
    public void setUp() {
        shop = Shop.builder().id(1L).name("Chi nhánh A").build();
        manager = User.builder().id(10L).username("manager").shop(shop).build();
        employeeRole = new Role(RoleType.ROLE_EMPLOYEE, "Nhân viên chi nhánh");
        userRole = new Role(RoleType.ROLE_USER, "Khách hàng");
        staff = User.builder().id(20L).username("nv1").fullName("Nguyễn Văn A")
                .email("nv1@test.com").shop(shop).role(employeeRole).build();
    }

    @Test
    public void fireEmployeeKeepsAccountAndShopId() {
        when(shopRepository.findByManagerId(10L)).thenReturn(java.util.List.of(shop));
        when(userRepository.findById(20L)).thenReturn(Optional.of(staff));
        when(roleRepository.findByName(RoleType.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.fireEmployee(10L, 20L);

        assertEquals(RoleType.ROLE_USER, staff.getRole().getName());
        // giữ shop_id làm dấu vết thuộc chi nhánh — không xóa tài khoản
        assertNotNull(staff.getShop());
        assertEquals(1L, staff.getShop().getId());
        verify(userRepository).save(staff);
    }

    @Test
    public void fireEmployeeRejectsStaffOfOtherShop() {
        Shop other = Shop.builder().id(2L).name("Chi nhánh B").build();
        staff.setShop(other);
        when(shopRepository.findByManagerId(10L)).thenReturn(java.util.List.of(shop));
        when(userRepository.findById(20L)).thenReturn(Optional.of(staff));

        assertThrows(IllegalArgumentException.class, () -> service.fireEmployee(10L, 20L));
        verify(userRepository, never()).save(any());
    }

    @Test
    public void getMyEmployeesMarksFiredStaffAsNotEmployed() {
        User fired = User.builder().id(21L).username("nv2").fullName("Trần Thị B")
                .email("nv2@test.com").shop(shop).role(userRole).build();
        when(shopRepository.findByManagerId(10L)).thenReturn(java.util.List.of(shop));
        when(userRepository.findByShopIdAndRoleNameIn(1L,
                List.of(RoleType.ROLE_EMPLOYEE, RoleType.ROLE_USER)))
                .thenReturn(List.of(staff, fired));

        List<EmployeeDto> result = service.getMyEmployees(10L);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(u -> u.getId() == 20L && u.isEmployed()));
        assertTrue(result.stream().anyMatch(u -> u.getId() == 21L && !u.isEmployed()));
    }

    @Test
    public void getStatsWorksForFiredStaff() {
        staff.setRole(userRole); // đã sa thải nhưng giữ shop_id
        when(shopRepository.findByManagerId(10L)).thenReturn(java.util.List.of(shop));
        when(userRepository.findById(20L)).thenReturn(Optional.of(staff));
        when(employeeService.getMyStats(20L, "2026-09"))
                .thenReturn(com.storechain.group4.dto.EmployeeStatsDto.builder().build());

        assertDoesNotThrow(() -> service.getEmployeeStats(10L, 20L, "2026-09"));
        verify(employeeService).getMyStats(20L, "2026-09");
    }
}
