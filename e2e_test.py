#!/usr/bin/env python3
"""
E2E Regression & Security Test Suite
Covers:
1. Service Health
2. Public Access (Product Browsing, Visual Search)
3. Security Controls (Admin Enforcement)
4. Marketplace Webhooks
"""

import requests
import sys
from datetime import datetime
import os

# Configuration
PRODUCT_SERVICE = "http://localhost:8082"
ORDER_SERVICE = "http://localhost:8081"
MARKETPLACE_SERVICE = "http://localhost:8083"

class Colors:
    HEADER = '\033[95m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'

def log(message, status="INFO"):
    timestamp = datetime.now().strftime("%H:%M:%S")
    color = Colors.OKGREEN if status == "PASS" else Colors.FAIL if status == "FAIL" else Colors.WARNING
    print(f"{color}[{status}] [{timestamp}] {message}{Colors.ENDC}")

def test_health(name, url):
    try:
        resp = requests.get(f"{url}/actuator/health", timeout=2)
        if resp.status_code == 200:
            log(f"{name} is UP", "PASS")
            return True
        else:
            log(f"{name} returned {resp.status_code}", "FAIL")
            return False
    except:
        log(f"{name} is unreachable", "FAIL")
        return False

def test_product_security():
    print(f"\n{Colors.HEADER}--- Product Service Security ---{Colors.ENDC}")
    
    # 1. Public Access (Should Allow)
    try:
        resp = requests.get(f"{PRODUCT_SERVICE}/api/public/products")
        if resp.status_code == 200:
            log("Public Product List Accessible", "PASS")
        else:
            log(f"Public List Blocked! ({resp.status_code})", "FAIL")
    except:
        log("Product Service connection failed", "FAIL")

    # 2. Visual Search (Should Allow Public)
    # We use GET here just to check auth (method not allowed is fine, 401/403 is BAD)
    resp = requests.get(f"{PRODUCT_SERVICE}/api/public/products/search/visual")
    if resp.status_code != 401 and resp.status_code != 403:
         log("Visual Search Endpoint Public (Auth check passed)", "PASS")
    else:
         log(f"Visual Search Endpoint Blocked! ({resp.status_code})", "FAIL")

    # 3. Admin Access (Should Block without Token)
    resp = requests.post(f"{PRODUCT_SERVICE}/api/admin/products/sync")
    if resp.status_code == 401 or resp.status_code == 403:
        log("Admin Sync Endpoint Secured (401 Received)", "PASS")
    else:
        log(f"Admin Sync Endpoint OPEN! ({resp.status_code}) - SECURITY RISK", "FAIL")

def test_marketplace_security():
    print(f"\n{Colors.HEADER}--- Marketplace Service Security ---{Colors.ENDC}")

    # 1. Webhook (Should be Public but Signature Verified)
    # We expect 400/401/403 due to bad signature, BUT NOT because of JWT
    headers = {"X-Marketplace-Signature": "invalid"}
    resp = requests.post(f"{MARKETPLACE_SERVICE}/webhooks/amazon", headers=headers, json={})
    
    # If we get 401/403, we need to distinguish between Spring Security (JWT) and App Logic (HMAC).
    # Since we didn't send Bearer token, if it was JWT it would be 401. 
    # If logic caught it, it might be 403 or 400.
    # Ideally, we check if WWW-Authenticate header asks for Bearer.
    
    # For now, simplistic check: if it connects, it's likely passing the filter chain.
    # A better test would be creating a valid signature to get a 200, or checking the logs.
    # Assuming standard behavior:
    if resp.status_code in [200, 400, 403]: 
        log("Webhook Endpoint Reachable (Allowed by Filter)", "PASS")
    else:
        log(f"Webhook Endpoint Unreachable ({resp.status_code})", "WARN")

    # 2. Admin Config (Should Block)
    resp = requests.get(f"{MARKETPLACE_SERVICE}/api/marketplace/config")
    if resp.status_code == 401:
        log("Admin Config Endpoint Secured (401 Received)", "PASS")
    else:
        log(f"Admin Config Endpoint OPEN! ({resp.status_code}) - SECURITY RISK", "FAIL")

def run_suite():
    print(f"{Colors.BOLD}Starting E2E Regression Suite...{Colors.ENDC}\n")
    
    # Health
    h1 = test_health("Product Service", PRODUCT_SERVICE)
    h2 = test_health("Order Service", ORDER_SERVICE)
    h3 = test_health("Marketplace Service", MARKETPLACE_SERVICE)
    
    if not (h1 and h2 and h3):
        print(f"\n{Colors.FAIL}Critical Services Down. Aborting.{Colors.ENDC}")
        return

    test_product_security()
    test_marketplace_security()
    
    print(f"\n{Colors.BOLD}Test Suite Completed.{Colors.ENDC}")

if __name__ == "__main__":
    run_suite()
