package com.app.finance.services;

import com.app.finance.entities.FinanceVendor;
import com.app.finance.payloads.VendorDTO;
import com.app.finance.repositories.VendorRepository;
import com.app.core.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepo;

    @Override
    @Transactional
    public VendorDTO createVendor(VendorDTO dto) {
        FinanceVendor vendor = mapToEntity(dto);
        FinanceVendor saved = vendorRepo.save(vendor);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public VendorDTO updateVendor(Long id, VendorDTO dto) {
        FinanceVendor existing = vendorRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", id));

        existing.setName(dto.name());
        existing.setEmail(dto.email());
        existing.setPhoneNumber(dto.phoneNumber());
        existing.setGstin(dto.gstin());
        existing.setPan(dto.pan());
        existing.setCategory(FinanceVendor.VendorCategory.valueOf(dto.category()));
        existing.setCommissionRate(dto.commissionRate());
        existing.setBankName(dto.bankName());
        existing.setBankAccountNumber(dto.bankAccountNumber());
        existing.setIfscCode(dto.ifscCode());
        existing.setPaymentTerms(dto.paymentTerms());

        if (dto.address() != null) {
            FinanceVendor.Address addr = new FinanceVendor.Address();
            addr.setStreet(dto.address().street());
            addr.setCity(dto.address().city());
            addr.setState(dto.address().state());
            addr.setPincode(dto.address().pincode());
            addr.setCountry(dto.address().country());
            existing.setAddress(addr);
        }

        FinanceVendor updated = vendorRepo.save(existing);
        return mapToDTO(updated);
    }

    @Override
    public VendorDTO getVendorById(Long id) {
        return vendorRepo.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", id));
    }

    @Override
    public List<VendorDTO> getAllVendors() {
        return vendorRepo.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VendorDTO> getVendorsByCategory(String category) {
        return vendorRepo.findByCategory(FinanceVendor.VendorCategory.valueOf(category)).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteVendor(Long id) {
        vendorRepo.deleteById(id);
    }

    @Override
    @Transactional
    public void toggleVendorStatus(Long id) {
        FinanceVendor vendor = vendorRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", id));
        vendor.setActive(!vendor.isActive());
        vendorRepo.save(vendor);
    }

    private FinanceVendor mapToEntity(VendorDTO dto) {
        FinanceVendor vendor = new FinanceVendor();
        vendor.setName(dto.name());
        vendor.setEmail(dto.email());
        vendor.setPhoneNumber(dto.phoneNumber());
        vendor.setGstin(dto.gstin());
        vendor.setPan(dto.pan());
        if (dto.category() != null) {
            vendor.setCategory(FinanceVendor.VendorCategory.valueOf(dto.category()));
        }
        vendor.setCommissionRate(dto.commissionRate());
        vendor.setBankName(dto.bankName());
        vendor.setBankAccountNumber(dto.bankAccountNumber());
        vendor.setIfscCode(dto.ifscCode());
        vendor.setPaymentTerms(dto.paymentTerms());
        vendor.setActive(dto.active());

        if (dto.address() != null) {
            FinanceVendor.Address addr = new FinanceVendor.Address();
            addr.setStreet(dto.address().street());
            addr.setCity(dto.address().city());
            addr.setState(dto.address().state());
            addr.setPincode(dto.address().pincode());
            addr.setCountry(dto.address().country());
            vendor.setAddress(addr);
        }
        return vendor;
    }

    private VendorDTO mapToDTO(FinanceVendor entity) {
        VendorDTO.AddressDTO addrDto = null;
        if (entity.getAddress() != null) {
            addrDto = new VendorDTO.AddressDTO(
                entity.getAddress().getStreet(),
                entity.getAddress().getCity(),
                entity.getAddress().getState(),
                entity.getAddress().getPincode(),
                entity.getAddress().getCountry()
            );
        }

        return new VendorDTO(
            entity.getId(),
            entity.getName(),
            entity.getEmail(),
            entity.getPhoneNumber(),
            entity.getGstin(),
            entity.getPan(),
            entity.getCategory() != null ? entity.getCategory().name() : null,
            addrDto,
            entity.getCommissionRate(),
            entity.getBankName(),
            entity.getBankAccountNumber(),
            entity.getIfscCode(),
            entity.getPaymentTerms(),
            entity.isActive()
        );
    }
}
