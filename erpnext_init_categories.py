import frappe

def initialize():
    frappe.connect()
    print(f"Updating Company and creating Warehouse for {frappe.local.site}...")
    
    # 1. Update Company Abbreviation
    if frappe.db.exists("Company", "Fashion Store"):
        frappe.db.set_value("Company", "Fashion Store", "abbr", "FS")
        frappe.db.commit()
        print("Updated Company 'Fashion Store' with abbr 'FS'")

    # 1.5 Create Price List
    if not frappe.db.exists("Price List", "Standard Selling"):
        doc = frappe.get_doc({
            "doctype": "Price List",
            "price_list_name": "Standard Selling",
            "enabled": 1,
            "selling": 1,
            "currency": "INR"
        })
        doc.insert(ignore_permissions=True)
        frappe.db.commit()
        print("Created Price List: Standard Selling")

    # 2. Create Warehouse Type 'Transit' if missing
    if not frappe.db.exists("Warehouse Type", "Transit"):
        frappe.db.sql("INSERT INTO `tabWarehouse Type` (name) VALUES ('Transit')")
        frappe.db.commit()

    # 3. Create Warehouse 'Stores'
    warehouse_name = "Stores - FS"
    if not frappe.db.exists("Warehouse", warehouse_name):
        doc = frappe.get_doc({
            "doctype": "Warehouse",
            "warehouse_name": "Stores",
            "company": "Fashion Store",
            "warehouse_type": "Transit"
        })
        doc.insert(ignore_permissions=True)
        frappe.db.commit()
        print(f"Created Warehouse: {warehouse_name}")
    else:
        print(f"Warehouse {warehouse_name} already exists")

    print("Initialization complete!")

if __name__ == "__main__":
    initialize()
