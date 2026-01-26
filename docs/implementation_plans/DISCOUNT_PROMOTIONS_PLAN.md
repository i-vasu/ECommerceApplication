---
name: Discount & Promotions Plan
description: Plan for implementing the Discount and Promotions system for Phase 4 Commerce & Engagement.
---

# Feature: Discount & Promotions System

## Objective
Implement a comprehensive discount and promotions system with coupon validation, automatic discount application, and promotional campaigns. This will increase customer engagement and enable marketing campaigns.

## Status
- [x] Basic interfaces defined
- [x] Coupon Entity & Repository
- [x] Discount Calculation Service
- [x] Coupon Service (CRUD)
- [x] Coupon Controller (API)
- [x] Integration with Cart/Order
- [ ] Promotion Campaign Support
- [ ] Unit Tests (Basic tests created)

## Detailed Steps

### 1. Data Model
Create entities:
- **Coupon**: code, discountType (PERCENTAGE, FIXED_AMOUNT), value, minOrderAmount, maxDiscount, validFrom, validTo, usageLimit, usedCount, applicableCategories
- **CouponUsage**: Track which users used which coupons

### 2. Repository Layer
- `CouponRepo`: Find by code, find active coupons, find by user
- `CouponUsageRepo`: Track usage history

### 3. Service Layer
Implement `CouponService`:
- `validateCoupon(String code, Double orderAmount)`
- `applyCoupon(String code, Long userId, Long orderId)`
- `calculateDiscount(CouponEntity coupon, Double orderAmount)`
- `createCoupon(CouponDTO)` - Admin only
- `getAllActiveCoupons()`

Enhance `PromotionService`:
- Automatic discount detection based on cart value
- Category-specific promotions
- Time-based promotions (flash sales)

### 4. Controller Layer
Create `CouponController`:
- `POST /api/v1/coupons` - Admin: Create coupon
- `GET /api/v1/coupons/validate/{code}` - Validate coupon
- `POST /api/v1/coupons/apply` - Apply coupon to cart
- `GET /api/v1/coupons/active` - Get all active coupons

### 5. Integration
- Update `CartService` to apply discounts
- Update `OrderService` to track applied coupons
- Add coupon field to `OrderDTO`

### 6. Business Rules
- One coupon per order
- Coupon expiry validation
- Usage limit enforcement
- Minimum order amount validation
- Category restrictions
