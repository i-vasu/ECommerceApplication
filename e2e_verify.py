import requests
import time
import json

# Configuration
JAVA_BASE_URL = "http://localhost:8080/api" # Gateway
ERPNEXT_BASE_URL = "http://localhost:8000/api/resource"
ERPNEXT_API_KEY = "5f84158d3c2eb52"
ERPNEXT_API_SECRET = "78c046a0b4a7627"

headers_erp = {
    "Authorization": f"token {ERPNEXT_API_KEY}:{ERPNEXT_API_SECRET}",
    "Content-Type": "application/json"
}

def check_java_health():
    try:
        # Check discovery server
        resp = requests.get("http://localhost:8761", timeout=5)
        if resp.status_code == 200:
            print("[OK] Discovery Server is UP")
        else:
            print(f"[ERR] Discovery Server returned {resp.status_code}")
            return False
        return True
    except:
        print("[ERR] Discovery Server is unreachable")
        return False

def verify_sync():
    print("\nStarting E2E Verification...")
    
    # 1. Create Category in Java
    category_payload = {
        "categoryName": "E2E Test Category"
    }
    # Note: Using public endpoint if available, otherwise need auth
    # Assuming standard admin/user for POC
    print("Creating category in Java...")
    # This might need authentication token. For POC, let's see if we can trigger a sync directly.
    
    # Let's try to trigger a sync from ERPNext to Java first
    print("Triggering sync from ERPNext to Java...")
    try:
        resp = requests.post(f"http://localhost:8083/api/admin/products/sync", timeout=30)
        print(f"Sync Trigger Response: {resp.status_code} - {resp.text}")
    except Exception as e:
        print(f"Failed to trigger sync: {e}")

    # Verify Item exists in ERPNext (One we seeded earlier)
    item_code = "SAR-KAN-BLUE-001"
    print(f"Verifying item {item_code} in ERPNext...")
    resp = requests.get(f"{ERPNEXT_BASE_URL}/Item/{item_code}", headers=headers_erp)
    if resp.status_code == 200:
        print(f"[OK] Item {item_code} found in ERPNext")
        data = resp.json().get("data", {})
        print(f"      Name: {data.get('item_name')}")
        print(f"      Price: {data.get('standard_rate')}")
    else:
        print(f"[ERR] Item {item_code} NOT found in ERPNext (Status {resp.status_code})")

    # Verify Sales Order
    print("Checking Sales Orders in ERPNext...")
    resp = requests.get(f"{ERPNEXT_BASE_URL}/Sales Order", headers=headers_erp)
    if resp.status_code == 200:
        orders = resp.json().get("data", [])
        print(f"[OK] Found {len(orders)} Sales Orders in ERPNext")
        for o in orders:
            print(f"      Order ID: {o.get('name')}")
    else:
        print(f"[ERR] Could not fetch Sales Orders from ERPNext")

if __name__ == "__main__":
    if check_java_health():
        verify_sync()
    else:
        print("Java services are not ready yet. Please ensure they are built and running.")
