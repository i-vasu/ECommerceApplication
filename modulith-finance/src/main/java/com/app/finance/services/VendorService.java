package com.app.finance.services;

import com.app.finance.payloads.VendorDTO;
import java.util.List;

public interface VendorService {
    VendorDTO createVendor(VendorDTO vendorDTO);
    VendorDTO updateVendor(Long id, VendorDTO vendorDTO);
    VendorDTO getVendorById(Long id);
    List<VendorDTO> getAllVendors();
    List<VendorDTO> getVendorsByCategory(String category);
    void deleteVendor(Long id);
    void toggleVendorStatus(Long id);
}
