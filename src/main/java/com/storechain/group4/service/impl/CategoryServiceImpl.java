package com.storechain.group4.service.impl;

import com.storechain.group4.dto.CategoryRequestDto;
import com.storechain.group4.dto.CategoryResponseDto;
import com.storechain.group4.dto.CategoryStatsDto;
import com.storechain.group4.model.Category;
import com.storechain.group4.repository.CategoryRepository;
import com.storechain.group4.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern COMBINING_DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponseDto> getCategories(
            String keyword,
            String status,
            Long parentId,
            Boolean onlyRoots,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "sortOrder";
        } else if ("id".equalsIgnoreCase(sortBy)) {
            sortBy = "id";
        }

        Sort sort = "desc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return categoryRepository.searchCategories(keyword, status, parentId, onlyRoots, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllCategoriesTree() {
        List<Category> rootCategories = categoryRepository.findByParentIsNullOrderBySortOrderAsc();
        return rootCategories.stream()
                .map(this::toTreeDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getRootCategories() {
        return categoryRepository.findByParentIsNullOrderBySortOrderAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getSubCategories(Long parentId) {
        return categoryRepository.findByParentIdOrderBySortOrderAsc(parentId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllActiveCategoriesFlat() {
        return categoryRepository.findByStatusOrderBySortOrderAsc("active").stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục với ID: " + id));
        return toDto(category);
    }

    @Override
    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto request) {
        String name = request.getName() != null ? request.getName().trim() : "";
        if (name.isBlank()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống");
        }

        String slug = request.getSlug() != null && !request.getSlug().isBlank()
                ? toSlug(request.getSlug())
                : generateUniqueSlug(name, null);

        if (categoryRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("Slug danh mục '" + slug + "' đã tồn tại, vui lòng chọn slug khác");
        }

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục cha với ID " + request.getParentId() + " không tồn tại"));
        }

        Category category = Category.builder()
                .name(name)
                .slug(slug)
                .parent(parent)
                .imageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : null)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .status(request.getStatus() != null && !request.getStatus().isBlank() ? request.getStatus() : "active")
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Đã tạo danh mục mới: ID={}, Name={}, Slug={}", saved.getId(), saved.getName(), saved.getSlug());
        return toDto(saved);
    }

    @Override
    @Transactional
    public CategoryResponseDto updateCategory(Long id, CategoryRequestDto request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục với ID: " + id));

        String name = request.getName() != null ? request.getName().trim() : "";
        if (name.isBlank()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống");
        }

        String slug = request.getSlug() != null && !request.getSlug().isBlank()
                ? toSlug(request.getSlug())
                : category.getSlug();

        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new IllegalArgumentException("Slug '" + slug + "' đã được sử dụng bởi danh mục khác");
        }

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new IllegalArgumentException("Danh mục không thể chọn chính nó làm danh mục cha");
            }
            if (isDescendant(id, request.getParentId())) {
                throw new IllegalArgumentException("Không thể chọn danh mục con/cháu làm danh mục cha (tránh vòng lặp)");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục cha không tồn tại"));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        category.setName(name);
        category.setSlug(slug);
        category.setImageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : null);
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            category.setStatus(request.getStatus());
        }

        Category updated = categoryRepository.save(category);
        log.info("Đã cập nhật danh mục: ID={}, Name={}, Slug={}", updated.getId(), updated.getName(), updated.getSlug());
        return toDto(updated);
    }

    @Override
    @Transactional
    public CategoryResponseDto updateStatus(Long id, String status) {
        if (!"active".equalsIgnoreCase(status) && !"inactive".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ. Chỉ chấp nhận 'active' hoặc 'inactive'");
        }
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục với ID: " + id));

        category.setStatus(status.toLowerCase());
        Category saved = categoryRepository.save(category);
        log.info("Đã cập nhật trạng thái danh mục ID {} thành {}", id, status);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục với ID: " + id));

        if (categoryRepository.existsByParentId(id)) {
            throw new IllegalArgumentException("Không thể xóa danh mục này vì đang có danh mục con trực thuộc. Vui lòng chuyển hoặc xóa danh mục con trước.");
        }

        categoryRepository.delete(category);
        log.info("Đã xóa danh mục ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryStatsDto getStats() {
        long total = categoryRepository.count();
        long active = categoryRepository.countByStatus("active");
        long inactive = categoryRepository.countByStatus("inactive");
        long root = categoryRepository.countByParentIsNull();

        return CategoryStatsDto.builder()
                .totalCategories(total)
                .activeCategories(active)
                .inactiveCategories(inactive)
                .rootCategories(root)
                .build();
    }

    // --- Helper Methods ---

    private CategoryResponseDto toDto(Category category) {
        int childrenCount = category.getChildren() != null ? category.getChildren().size() : (int) categoryRepository.countByParentId(category.getId());
        return CategoryResponseDto.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .imageUrl(category.getImageUrl())
                .sortOrder(category.getSortOrder())
                .status(category.getStatus())
                .createdAt(category.getCreatedAt())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .childrenCount(childrenCount)
                .build();
    }

    private CategoryResponseDto toTreeDto(Category category) {
        CategoryResponseDto dto = toDto(category);
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            List<CategoryResponseDto> childDtos = category.getChildren().stream()
                    .filter(Objects::nonNull)
                    .sorted((c1, c2) -> {
                        int o1 = c1.getSortOrder() != null ? c1.getSortOrder() : 0;
                        int o2 = c2.getSortOrder() != null ? c2.getSortOrder() : 0;
                        return Integer.compare(o1, o2);
                    })
                    .map(this::toTreeDto)
                    .collect(Collectors.toList());
            dto.setChildren(childDtos);
            dto.setChildrenCount(childDtos.size());
        } else {
            dto.setChildren(new ArrayList<>());
            dto.setChildrenCount(0);
        }
        return dto;
    }

    private boolean isDescendant(Long ancestorId, Long targetId) {
        Optional<Category> currentOpt = categoryRepository.findById(targetId);
        while (currentOpt.isPresent()) {
            Category current = currentOpt.get();
            if (current.getParent() == null) {
                return false;
            }
            if (current.getParent().getId().equals(ancestorId)) {
                return true;
            }
            currentOpt = categoryRepository.findById(current.getParent().getId());
        }
        return false;
    }

    private String generateUniqueSlug(String name, Long currentId) {
        String baseSlug = toSlug(name);
        String slug = baseSlug;
        int counter = 1;

        while (currentId == null ? categoryRepository.existsBySlug(slug) : categoryRepository.existsBySlugAndIdNot(slug, currentId)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        return slug;
    }

    private String toSlug(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutDiacritics = COMBINING_DIACRITICS.matcher(normalized).replaceAll("");
        withoutDiacritics = withoutDiacritics.replace('đ', 'd').replace('Đ', 'D');

        String nowhitespace = WHITESPACE.matcher(withoutDiacritics).replaceAll("-");
        String normalizedSlug = NON_LATIN.matcher(nowhitespace).replaceAll("");
        return normalizedSlug.toLowerCase(Locale.ENGLISH).replaceAll("-+", "-").replaceAll("^-|-$", "");
    }
}
