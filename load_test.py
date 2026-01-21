import concurrent.futures
import requests
import time
import statistics
import uuid
import random

BASE_URL = "http://localhost:8080/api/v1"
CONCURRENT_USERS = 50
TOTAL_ORDERS = 1000

# Stub Data
PRODUCT_ID = 302 # Default fallback

def get_product_id():
    try:
        resp = requests.get(f"{BASE_URL}/products?pageNumber=0&pageSize=1")
        if resp.status_code == 200:
            data = resp.json()
            if 'content' in data and len(data['content']) > 0:
                return data['content'][0]['productId']
    except:
        pass
    return PRODUCT_ID

def simulate_user_journey(user_id):
    start = time.time()
    session = requests.Session()
    email = f"loadtest_{user_id}_{uuid.uuid4().hex[:8]}@example.com"
    password = "Password123"
    
    try:
        # 1. Register
        reg_payload = {
            "firstName": "Loader",
            "lastName": "Tester",
            "email": email,
            "password": password,
            "mobileNumber": f"9{random.randint(100000000, 999999999)}",
            "role": { "roleId": 2, "roleName": "USER" }
        }
        reg_resp = session.post(f"{BASE_URL}/register", json=reg_payload, timeout=30)
        if reg_resp.status_code != 200 and reg_resp.status_code != 201:
            return "REGISTER_FAIL", (time.time() - start) * 1000

        # 2. Login
        login_resp = session.post(f"{BASE_URL}/login", json={"email": email, "password": password}, timeout=30)
        if login_resp.status_code != 200:
             return "LOGIN_FAIL", (time.time() - start) * 1000
        
        token = login_resp.json().get("jwt-token")
        if not token:
            return "TOKEN_FAIL", (time.time() - start) * 1000
            
        headers = {"Authorization": f"Bearer {token}"}

        # 3. Create Cart (Implicit or Explicit?) 
        # API adapter suggests POST /cart creates generic cart or we can use the user's cart if auto-created.
        # Let's try adding item directly to cart ID 0 or similar if backend handles it, 
        # OR fetch the user to get their cart ID.
        
        # Fetch User to get Cart ID
        # Wait, login response usually has user info? No, just keys.
        # Let's try creating a cart or assuming user has one.
        # Storefront uses: cartService.getCart(userId) logic.
        # Let's try POST /cart
        cart_resp = session.post(f"{BASE_URL}/cart", headers=headers, timeout=30)
        if cart_resp.status_code == 200 or cart_resp.status_code == 201:
            cart_id = cart_resp.json().get('cartId')
        else:
             return "CART_FAIL", (time.time() - start) * 1000

        # 4. Add Item
        add_payload = {"productId": PRODUCT_ID, "quantity": 1}
        add_resp = session.post(f"{BASE_URL}/cart/{cart_id}/items", json=add_payload, headers=headers, timeout=30)
        if add_resp.status_code != 200 and add_resp.status_code != 201:
            return "ADD_FAIL", (time.time() - start) * 1000

        # 5. Checkout
        checkout_payload = {
            "cartId": cart_id,
            "email": email,
            "shippingAddress": {
                "addressLine1": "123 Load St",
                "city": "Test City",
                "state": "TS",
                "country": "US",
                "zipCode": "12345"
            },
            "paymentMethod": "CREDIT_CARD"
        }
        ord_resp = session.post(f"{BASE_URL}/checkout", json=checkout_payload, headers=headers, timeout=30) # Longer timeout for order
        if ord_resp.status_code != 200 and ord_resp.status_code != 201:
             return f"CHECKOUT_FAIL_{ord_resp.status_code}", (time.time() - start) * 1000
        
        total_time = (time.time() - start) * 1000
        return "SUCCESS", total_time

    except Exception as e:
        return f"EXCEPTION: {str(e)}", 0

def load_test():
    global PRODUCT_ID
    PRODUCT_ID = get_product_id()
    print(f"Target Product ID: {PRODUCT_ID}")
    print(f"Starting Load Test: {TOTAL_ORDERS} orders, {CONCURRENT_USERS} concurrent users")
    
    latencies = []
    results = {}
    
    start_time = time.time()
    
    with concurrent.futures.ThreadPoolExecutor(max_workers=CONCURRENT_USERS) as executor:
        futures = [executor.submit(simulate_user_journey, i) for i in range(TOTAL_ORDERS)]
        
        completed = 0
        for future in concurrent.futures.as_completed(futures):
            status, latency = future.result()
            completed += 1
            if status == "SUCCESS":
                latencies.append(latency)
            
            results[status] = results.get(status, 0) + 1
            if completed % 100 == 0:
                print(f"Completed {completed}/{TOTAL_ORDERS}")

    total_time = time.time() - start_time
    tps = len(latencies) / total_time # Successful orders per second
    
    with open("load_test_results.txt", "w") as f:
        f.write("=== Load Test Results ===\n")
        f.write(f"Time Taken: {total_time:.2f}s\n")
        f.write(f"TPS (Successful Orders): {tps:.2f}\n")
        if latencies:
            f.write(f"Avg Latency: {statistics.mean(latencies):.2f}ms\n")
            f.write(f"P95 Latency: {statistics.quantiles(latencies, n=20)[18]:.2f}ms\n")
        f.write(f"Breakdown: {results}\n")
    
    print("\nResults saved to load_test_results.txt")
    print("\n=== Load Test Results ===")
    print(f"Time Taken: {total_time:.2f}s")
    print(f"TPS (Successful Orders): {tps:.2f}")

if __name__ == "__main__":
    load_test()
