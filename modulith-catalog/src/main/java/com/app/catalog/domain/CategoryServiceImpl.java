package com.app.catalog.domain;

import com.app.catalog.ProductService;
import com.app.catalog.entities.Category;
import com.app.catalog.entities.Product;
import com.app.catalog.mappers.CategoryMapper;
import com.app.catalog.payloads.CategoryDTO;
import com.app.catalog.payloads.CategoryResponse;
import com.app.catalog.repositories.CategoryRepo;
import com.app.core.APIException;
import com.app.core.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
public class CategoryServiceImpl implements CategoryService {

	@Autowired
	private CategoryRepo categoryRepo;

	@Autowired
	private ProductService productService;

	@Autowired
	private CategoryMapper categoryMapper;

	@Override
	public CategoryDTO createCategory(CategoryDTO categoryDTO) {
		Category category = categoryMapper.categoryDTOToCategory(categoryDTO);
		Category savedCategory = categoryRepo.findByCategoryName(category.getCategoryName());

		if (savedCategory != null) {
			throw new APIException("Category with the name '" + category.getCategoryName() + "' already exists !!!");
		}

		savedCategory = categoryRepo.save(category);

		return categoryMapper.categoryToCategoryDTO(savedCategory);
	}

	@Override
	public CategoryResponse getCategories(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
		Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending()
				: Sort.by(sortBy).descending();

		Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

		Page<Category> pageCategories = categoryRepo.findAll(pageDetails);

		List<Category> categories = pageCategories.getContent();

		if (categories.size() == 0) {
			throw new APIException("No category is created till now");
		}

		List<CategoryDTO> categoryDTOs = categories.stream()
				.map(categoryMapper::categoryToCategoryDTO).collect(Collectors.toList());

		CategoryResponse categoryResponse = new CategoryResponse(
				categoryDTOs,
				pageCategories.getNumber(),
				pageCategories.getSize(),
				pageCategories.getTotalElements(),
				pageCategories.getTotalPages(),
				pageCategories.isLast());

		return categoryResponse;
	}

	@Override
	public CategoryDTO updateCategory(CategoryDTO categoryDTO, Long categoryId) {
		Category savedCategory = categoryRepo.findById(categoryId)
				.orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

		Category category = categoryMapper.categoryDTOToCategory(categoryDTO);
		category.setCategoryId(categoryId);

		savedCategory = categoryRepo.save(category);

		return categoryMapper.categoryToCategoryDTO(savedCategory);
	}

	@Override
	public String deleteCategory(Long categoryId) {
		Category category = categoryRepo.findById(categoryId)
				.orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

		List<Product> products = category.getProducts();

		products.forEach(product -> {
			productService.deleteProduct(product.getProductId());
		});

		categoryRepo.delete(category);

		return "Category with categoryId: " + categoryId + " deleted successfully !!!";
	}

}
