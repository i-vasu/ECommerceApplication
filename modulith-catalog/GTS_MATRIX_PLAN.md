# Implementation Plan: Global Textile Scheme (GTS) Matrix for Heritage Fashion

This plan implements the industry-standard **Global Textile Scheme (GTS)** to manage textile product data, specialized for the complex requirements of South Indian traditional wear.

## 1. GTS Architecture
The GTS standard organizes data into three tiers:
- **Article Master**: Shared style data (e.g., Fabric type, weaving region).
- **Variant Dimensions**: Primary physical axes (Color, Size, Length).
- **Property Catalog**: A coded system of specialized traits (e.g., Zari Grade, Border Height, Service Level).

---

## Phase 1: GTS Property Catalog (Coded System)
**Objective**: Implement a standardized Key-Value system using GTS-compliant property IDs.

### Steps:
1.  **Standardize Property Entities**:
    - `GtsProperty`: Standardized property keys (e.g., ID `202` for Fiber Composition, `305` for Zari Type).
    - `GtsPropertyValue`: Coded values (e.g., ID `50` for "Pure Silk", `101` for "Temple Border").
2.  **Schema Alignment**:
    - Create `catalog_gts_properties` and `catalog_gts_property_values`.
    - Map traditional attributes (e.g., Handloom vs Powerloom) to these standardized codes.

---

## Phase 2: Matrix Dimensions & Article Master
**Objective**: Define the core variant matrix based on GTS dimensions.

### Steps:
1.  **Refactor `Product` (Article Master)**:
    - Enrich with GTS-compliant header data (e.g., Market Segment, Care Instructions).
2.  **Refactor `ProductVariant` (Matrix Dimensions)**:
    - Standardize dimensions: `Dimension_1` (Color), `Dimension_2` (Size), `Dimension_3` (Service/Length).
3.  **Junction Mapping**:
    - Link variants to the Property Catalog via `variant_gts_properties` table.

---

## Phase 3: GTS-Compliant SKU Generation
**Objective**: Automate SKU creation using the GTS deterministic pattern.

### Steps:
1.  **GTS-Language Pattern**:
    - Format: `[Article_ID]-[Color_Code]-[Size_Code]-[Prop_Suffix]`
    - Example: `KNC402-MRN-ST-P01` (Kanchuram Style 402, Maroon, Stitched, Property: Pure Gold Zari).
2.  **SKU Generator Service**:
    - Update the internal generator to follow the concatenation of GTS codes.

---

## Phase 4: Saree-Specific "Service Attributes"
**Objective**: Use the Property Catalog to handle "Service Dimensions" (S dimension in my earlier GTS proposal).

### Steps:
1.  **Service as a Property**:
    - Add Property ID `900` (Value Added Service).
    - Map values like `RAW` (Unfinished), `FP` (Falls/Pico), `CH` (Custom Blouse Stitching).
2.  **Dynamic Pricing**:
    - Link `GtsPropertyValue` to price modifiers.

---

## Phase 5: Digital Product Passport (DPP) Readiness
**Objective**: Ensure the system is ready for global transparency regulations facilitated by GTS.

### Steps:
1.  **Provenance Metadata**:
    - Use the property catalog to track "Weaver Cluster ID" and "Geographical Indication (GI)" status.
2.  **Traceability API**:
    - Expose the GTS matrix via a standardized JSON-LD format for supply chain partners.

