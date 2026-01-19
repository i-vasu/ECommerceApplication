#!/usr/bin/env python3
"""
ERPNext Test Data Setup Script
Creates sample products, customers, and orders for E2E testing
"""

import requests
import json

# Configuration
ERPNEXT_URL = "http://localhost:8000"
API_KEY = "5f84158d3c2eb52"
API_SECRET = "78c046a0b4a7627"

headers = {
    "Authorization": f"token {API_KEY}:{API_SECRET}",
    "Content-Type": "application/json"
}

# Test Products (Items)
products = [
    {
        "item_code": "SAR-KAN-BLUE-001",
        "item_name": "Kanjivaram Blue Silk Saree",
        "item_group": "Products",
        "stock_uom": "Nos",
        "is_stock_item": 1,
        "opening_stock": 50,
        "valuation_rate": 5000,
        "standard_rate": 7500
    },
    {
        "item_code": "SAR-MYS-RED-002",
        "item_name": "Mysore Red Silk Saree",
        "item_group": "Products",
        "stock_uom": "Nos",
        "is_stock_item": 1,
        "opening_stock": 30,
        "valuation_rate": 4500,
        "standard_rate": 6500
    },
    {
        "item_code": "SAR-BAN-GOLD-003",
        "item_name": "Banarasi Gold Saree",
        "item_group": "Products",
        "stock_uom": "Nos",
        "is_stock_item": 1,
        "opening_stock": 20,
        "valuation_rate": 8000,
        "standard_rate": 12000
    },
    {
        "item_code": "KUR-COT-FLOR-004",
        "item_name": "Cotton Kurti - Floral",
        "item_group": "Products",
        "stock_uom": "Nos",
        "is_stock_item": 1,
        "opening_stock": 100,
        "valuation_rate": 500,
        "standard_rate": 1200
    },
    {
        "item_code": "LEH-DES-001",
        "item_name": "Designer Lehenga",
        "item_group": "Products",
        "stock_uom": "Nos",
        "is_stock_item": 1,
        "opening_stock": 15,
        "valuation_rate": 15000,
        "standard_rate": 25000
    }
]

# Test Customers
customers = [
    {
        "customer_name": "Priya Sharma",
        "customer_type": "Individual",
        "customer_group": "Individual",
        "territory": "India"
    },
    {
        "customer_name": "Rahul Mehta",
        "customer_type": "Individual",
        "customer_group": "Individual",
        "territory": "India"
    },
    {
        "customer_name": "Fashion Boutique Ltd",
        "customer_type": "Company",
        "customer_group": "Commercial",
        "territory": "India"
    }
]

def create_prerequisites():
    """Create basic groups and units required for validation"""
    print("Creating Prerequisites...")
    
    # Item Group
    r = requests.post(f"{ERPNEXT_URL}/api/resource/Item Group", headers=headers, json={"item_group_name": "Products", "is_group": 0, "parent_item_group": "All Item Groups"})
    print(f"Item Group: {r.status_code} - {r.text}")
    
    # UOM
    r = requests.post(f"{ERPNEXT_URL}/api/resource/UOM", headers=headers, json={"uom_name": "Nos", "must_be_whole_number": 1})
    print(f"UOM: {r.status_code} - {r.text}")
    
    # Customer Group
    r = requests.post(f"{ERPNEXT_URL}/api/resource/Customer Group", headers=headers, json={"customer_group_name": "Individual", "is_group": 0, "parent_customer_group": "All Customer Groups"})
    print(f"Customer Group (Ind): {r.status_code} - {r.text}")
    r = requests.post(f"{ERPNEXT_URL}/api/resource/Customer Group", headers=headers, json={"customer_group_name": "Commercial", "is_group": 0, "parent_customer_group": "All Customer Groups"})
    print(f"Customer Group (Com): {r.status_code} - {r.text}")
    
    # Territory
    r = requests.post(f"{ERPNEXT_URL}/api/resource/Territory", headers=headers, json={"territory_name": "India", "is_group": 0, "parent_territory": "All Territories"})
    print(f"Territory: {r.status_code} - {r.text}")
    print("[OK] Prerequisites finished.")

def create_items():
    """Create test items in ERPNext"""
    print("\nCreating Items...")
    for product in products:
        try:
            response = requests.post(
                f"{ERPNEXT_URL}/api/resource/Item",
                headers=headers,
                json=product
            )
            if response.status_code == 200:
                print(f"[OK] Created: {product['item_name']}")
            elif "already exists" in response.text:
                print(f"[SKIP] {product['item_name']} already exists")
            else:
                print(f"[ERROR] Failed: {product['item_name']} - {response.text}")
        except Exception as e:
            print(f"[ERROR] Error creating {product['item_name']}: {str(e)}")

def create_customers():
    """Create test customers in ERPNext"""
    print("\nCreating Customers...")
    for customer in customers:
        try:
            response = requests.post(
                f"{ERPNEXT_URL}/api/resource/Customer",
                headers=headers,
                json=customer
            )
            if response.status_code == 200:
                print(f"[OK] Created: {customer['customer_name']}")
            elif "already exists" in response.text:
                print(f"[SKIP] {customer['customer_name']} already exists")
            else:
                print(f"[ERROR] Failed: {customer['customer_name']} - {response.text}")
        except Exception as e:
            print(f"[ERROR] Error creating {customer['customer_name']}: {str(e)}")

def create_sales_order():
    """Create a sample sales order"""
    print("\nCreating Sales Order...")
    order = {
        "customer": "Priya Sharma",
        "company": "Fashion Store",
        "selling_price_list": "Standard Selling",
        "currency": "INR",
        "delivery_date": "2026-01-20",
        "docstatus": 0,
        "items": [
            {
                "item_code": "SAR-KAN-BLUE-001",
                "qty": 2,
                "rate": 7500,
                "delivery_date": "2026-01-20",
                "warehouse": "Stores - FS"
            }
        ]
    }
    
    try:
        response = requests.post(
            f"{ERPNEXT_URL}/api/resource/Sales Order",
            headers=headers,
            json=order
        )
        if response.status_code == 200:
            print(f"[OK] Created Sales Order")
            return response.json()
        else:
            print(f"[ERROR] Failed to create order: {response.text}")
            return None
    except Exception as e:
        print(f"[ERROR] Error: {str(e)}")
        return None

if __name__ == "__main__":
    if not API_KEY or not API_SECRET:
        print("ERROR: Please set API_KEY and API_SECRET in the script")
        exit(1)
    
    create_prerequisites()
    create_items()
    create_customers()
    create_sales_order()
    
    print("\nTest data setup complete!")
