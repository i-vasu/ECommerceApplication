import frappe

def initialize():
    frappe.connect()
    site = frappe.local.site
    print(f"Initializing categories for {site}...")
    
    # Root Item Group
    if not frappe.db.exists("Item Group", "All Item Groups"):
        frappe.get_doc({
            "doctype": "Item Group",
            "item_group_name": "All Item Groups",
            "is_group": 1,
            "parent_item_group": ""
        }).insert(ignore_permissions=True)
        print("Created All Item Groups")

    # Products Group
    if not frappe.db.exists("Item Group", "Products"):
        frappe.get_doc({
            "doctype": "Item Group",
            "item_group_name": "Products",
            "is_group": 0,
            "parent_item_group": "All Item Groups"
        }).insert(ignore_permissions=True)
        print("Created Products Group")

    # UOM
    if not frappe.db.exists("UOM", "Nos"):
        frappe.get_doc({
            "doctype": "UOM",
            "uom_name": "Nos",
            "must_be_whole_number": 1
        }).insert(ignore_permissions=True)
        print("Created UOM Nos")

    # Root Customer Group
    if not frappe.db.exists("Customer Group", "All Customer Groups"):
        frappe.get_doc({
            "doctype": "Customer Group",
            "customer_group_name": "All Customer Groups",
            "is_group": 1,
            "parent_customer_group": ""
        }).insert(ignore_permissions=True)
        print("Created All Customer Groups")

    # Indivudual Customer Group
    if not frappe.db.exists("Customer Group", "Individual"):
        frappe.get_doc({
            "doctype": "Customer Group",
            "customer_group_name": "Individual",
            "is_group": 0,
            "parent_customer_group": "All Customer Groups"
        }).insert(ignore_permissions=True)
        print("Created Individual Group")

    # Root Territory
    if not frappe.db.exists("Territory", "All Territories"):
        frappe.get_doc({
            "doctype": "Territory",
            "territory_name": "All Territories",
            "is_group": 1,
            "parent_territory": ""
        }).insert(ignore_permissions=True)
        print("Created All Territories")

    # India Territory
    if not frappe.db.exists("Territory", "India"):
        frappe.get_doc({
            "doctype": "Territory",
            "territory_name": "India",
            "is_group": 0,
            "parent_territory": "All Territories"
        }).insert(ignore_permissions=True)
        print("Created India Territory")

    # Price List
    if not frappe.db.exists("Price List", "Standard Selling"):
        frappe.get_doc({
            "doctype": "Price List",
            "price_list_name": "Standard Selling",
            "enabled": 1,
            "selling": 1
        }).insert(ignore_permissions=True)
        print("Created Standard Selling Price List")

    # Company (Required for Sales Order)
    if not frappe.get_all("Company"):
        frappe.get_doc({
            "doctype": "Company",
            "company_name": "Fashion Store",
            "default_currency": "INR",
            "country": "India"
        }).insert(ignore_permissions=True)
        print("Created Company: Fashion Store")

    frappe.db.commit()
    print("Initialization complete!")

if __name__ == "__main__":
    initialize()
