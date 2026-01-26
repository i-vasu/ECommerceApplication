#!/bin/bash

BASE_URL="http://localhost:8080/api/v1"
FRONTEND_URL="http://localhost:3001"
TIMESTAMP=$(date +%s)
EMAIL="test_${TIMESTAMP}@example.com"
PASSWORD="Password123"

echo "=== 1. Checking Frontend Pages ==="
curl -s -o /dev/null -w "Home: %{http_code}\n" $FRONTEND_URL || echo "Home: FAILED"
curl -s -o /dev/null -w "Login: %{http_code}\n" $FRONTEND_URL/login || echo "Login: FAILED"
curl -s -o /dev/null -w "Register: %{http_code}\n" $FRONTEND_URL/register || echo "Register: FAILED"
curl -s -o /dev/null -w "Checkout: %{http_code}\n" $FRONTEND_URL/checkout || echo "Checkout: FAILED"

echo -e "\n=== 2. Backend API Verification ==="

echo "--- Registering User: $EMAIL ---"
REGISTER_RES=$(curl -s -X POST "$BASE_URL/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"firstName\": \"Tester\",
    \"lastName\": \"UserOne\",
    \"email\": \"$EMAIL\",
    \"password\": \"$PASSWORD\",
    \"mobileNumber\": \"1234567890\",
    \"role\": { \"roleId\": 2, \"roleName\": \"USER\" }
  }")
echo "Response: $REGISTER_RES"

echo -e "\n--- Logging In ---"
LOGIN_RES=$(curl -s -X POST "$BASE_URL/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$EMAIL\",
    \"password\": \"$PASSWORD\"
  }")
echo "Response: $LOGIN_RES"

TOKEN=$(echo $LOGIN_RES | grep -o '"jwt-token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "❌ Login Failed - No Token"
#  exit 1 
else
  echo "✅ Got Token: ${TOKEN:0:15}..."
fi

echo -e "\n--- Creating Cart (Simulated via Add Item) ---"
# Backend creates cart on Add Item usually if logic is implemented that way, 
# or we can try creating one if there is an endpoint.
# Storefront adapter assumes /cart POST creates it.
CART_RES=$(curl -s -X POST "$BASE_URL/cart" -H "Authorization: Bearer $TOKEN")
echo "Create Cart Response: $CART_RES"

# Assuming cartId comes back
CART_ID=$(echo $CART_RES | grep -o '"cartId":[^,]*' | cut -d':' -f2 | tr -d ' ')
echo "Cart ID: $CART_ID"

if [ -z "$CART_ID" ]; then
    echo "⚠️ Could not extract Cart ID, trying with arbitrary ID 1 for test..."
    CART_ID=1
fi

echo -e "\n--- Add Item to Cart ---"
# Assuming Product ID 1 exists (from initial data)
ADD_RES=$(curl -s -X POST "$BASE_URL/cart/$CART_ID/items" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "productId": 302,
    "quantity": 1
  }')
echo "Add Item Response: $ADD_RES"

echo -e "\n--- Submit Checkout ---"
CHECKOUT_RES=$(curl -s -X POST "$BASE_URL/checkout" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"cartId\": $CART_ID,
    \"email\": \"$EMAIL\",
    \"shippingAddress\": {
        \"addressLine1\": \"123 Test St\",
        \"city\": \"Test City\",
        \"state\": \"TS\",
        \"country\": \"US\",
        \"zipCode\": \"12345\"
    },
    \"paymentMethod\": \"CREDIT_CARD\"
  }")
echo "Checkout Response: $CHECKOUT_RES"
