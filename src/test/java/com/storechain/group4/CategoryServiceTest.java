package com.storechain.group4;

import com.storechain.group4.dto.CategoryRequestDto;
import com.storechain.group4.dto.CategoryResponseDto;
import com.storechain.group4.dto.CategoryStatsDto;
import com.storechain.group4.model.Category;
import com.storechain.group4.repository.CategoryRepository;
import com.storechain.group4.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category parentCategory;
    private Category childCategory;

    @BeforeEach
    void setUp() {
        parentCategory = Category.builder()
                .id(1L)
                .name("Thời trang nam")
                .slug("thoi-trang-nam")
                .sortOrder(1)
                .status("active")
                .createdAt(LocalDateTime.now())
                .build();

        childCategory = Category.builder()
                .id(2L)
                .name("Áo thun nam")
                .slug("ao-thun-nam")
                .parent(parentCategory)
                .sortOrder(1)
                .status("active")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateCategory_Success_WithAutoSlug() {
        CategoryRequestDto request = CategoryRequestDto.builder()
                .name("Áo Sơ Mi Nam")
                .build();

        when(categoryRepository.existsBySlug("ao-so-mi-nam")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(3L);
            c.setCreatedAt(LocalDateTime.now());
            return c;
        });

        CategoryResponseDto response = categoryService.createCategory(request);

        assertNotNull(response);
        assertEquals("Áo Sơ Mi Nam", response.getName());
        assertEquals("ao-so-mi-nam", response.getSlug());
        assertEquals("active", response.getStatus());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void testCreateCategory_DuplicateSlug_ThrowsException() {
        CategoryRequestDto request = CategoryRequestDto.builder()
                .name("Thời trang nam")
                .slug("thoi-trang-nam")
                .build();

        when(categoryRepository.existsBySlug("thoi-trang-nam")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void testUpdateCategory_Success() {
        CategoryRequestDto request = CategoryRequestDto.builder()
                .name("Thời trang nam cao cấp")
                .slug("thoi-trang-nam-cao-cap")
                .sortOrder(5)
                .status("active")
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.existsBySlugAndIdNot("thoi-trang-nam-cao-cap", 1L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        CategoryResponseDto response = categoryService.updateCategory(1L, request);

        assertEquals("Thời trang nam cao cấp", response.getName());
        assertEquals("thoi-trang-nam-cao-cap", response.getSlug());
        assertEquals(5, response.getSortOrder());
    }

    @Test
    void testUpdateCategory_CircularReference_ThrowsException() {
        CategoryRequestDto request = CategoryRequestDto.builder()
                .name("Thời trang nam")
                .parentId(1L) // Choosing self as parent
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategory));

        assertThrows(IllegalArgumentException.class, () -> categoryService.updateCategory(1L, request));
    }

    @Test
    void testDeleteCategory_HasChildren_ThrowsException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.existsByParentId(1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> categoryService.deleteCategory(1L));
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void testDeleteCategory_NoChildren_Success() {
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(childCategory));
        when(categoryRepository.existsByParentId(2L)).thenReturn(false);

        categoryService.deleteCategory(2L);
        verify(categoryRepository).delete(childCategory);
    }

    @Test
    void testUpdateStatus_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        CategoryResponseDto response = categoryService.updateStatus(1L, "inactive");
        assertEquals("inactive", response.getStatus());
    }

    @Test
    void testGetStats() {
        when(categoryRepository.count()).thenReturn(10L);
        when(categoryRepository.countByStatus("active")).thenReturn(8L);
        when(categoryRepository.countByStatus("inactive")).thenReturn(2L);
        when(categoryRepository.countByParentIsNull()).thenReturn(3L);

        CategoryStatsDto stats = categoryService.getStats();

        assertEquals(10L, stats.getTotalCategories());
        assertEquals(8L, stats.getActiveCategories());
        assertEquals(2L, stats.getInactiveCategories());
        assertEquals(3L, stats.getRootCategories());
    }
}
