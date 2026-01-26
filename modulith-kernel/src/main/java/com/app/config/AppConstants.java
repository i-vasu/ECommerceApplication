package com.app.config;

public class AppConstants {

	public static final String PAGE_NUMBER = "0";
	public static final String PAGE_SIZE = "2";
	public static final String SORT_CATEGORIES_BY = "categoryId";
	public static final String SORT_PRODUCTS_BY = "productId";
	public static final String SORT_USERS_BY = "userId";
	public static final String SORT_ORDERS_BY = "totalAmount";
	public static final String SORT_DIR = "asc";
	public static final Long ADMIN_ID = 101L;
	public static final Long USER_ID = 102L;
	public static final long JWT_TOKEN_VALIDITY = 5 * 60 * 60;
	public static final String[] PUBLIC_URLS = { "/v3/api-docs/**", "/swagger-ui/**", "/api/register/**", "/api/login",
			"/api/public/**" };
	public static final String[] USER_URLS = { "/api/user/**" };
	public static final String[] ADMIN_URLS = { "/api/admin/**" };

	// Safety & Performance Constraints
	public static final Integer MAX_PAGE_SIZE = 50;
	public static final int MIN_KEYWORD_LENGTH = 3;
	public static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
}
