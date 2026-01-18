import requests
import json
import sys

BASE_URL = "http://localhost:8000"
# Common default credentials for development
API_KEY = "admin" 
API_SECRET = "admin"

def check_connection():
    try:
        response = requests.get(f"{BASE_URL}/api/method/ping")
        print(f"Ping Status: {response.status_code}")
        print(f"Ping Response: {response.text}")
        return response.status_code == 200
    except Exception as e:
        print(f"Connection failed: {e}")
        return False

def login(username, password):
    session = requests.Session()
    try:
        response = session.post(f"{BASE_URL}/api/method/login", data={"usr": username, "pwd": password})
        if response.status_code == 200 and response.json().get('message') == 'Logged In':
            print("Login successful")
            return session
        else:
            print(f"Login failed: {response.text}")
            return None
    except Exception as e:
        print(f"Login exception: {e}")
        return None

def check_doctype(session, doctype):
    try:
        response = session.get(f"{BASE_URL}/api/resource/DocType/{doctype}")
        if response.status_code == 200:
            print(f"DocType '{doctype}' exists.")
            return True
        else:
            print(f"DocType '{doctype}' MISSING.")
            return False
    except Exception as e:
        print(f"Error checking {doctype}: {e}")
        return False

if __name__ == "__main__":
    if not check_connection():
        sys.exit(1)
    
    # Try login as Administrator
    session = login("Administrator", "admin")
    if not session:
        # Try finding a generated API Key/Secret if possible, but login is best for admin tasks
        print("Could not login as Administrator. Aborting DocType check.")
        sys.exit(1)
        
    required_doctypes = [
        "Item Attribute", "Brand", "Season", "Size Guide", 
        "Price List", "Loyalty Program", "Campaign", "Sales Return", "Shipping Rule", "Tax Rule"
    ]
    
    for dt in required_doctypes:
        check_doctype(session, dt)
