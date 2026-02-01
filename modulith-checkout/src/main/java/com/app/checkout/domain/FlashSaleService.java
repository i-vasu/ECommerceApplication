package com.app.checkout.domain;

import com.app.core.ResourceNotFoundException;
import com.app.finance.promo.entities.FlashSale;
import com.app.finance.promo.entities.FlashSaleProduct;
import com.app.finance.promo.repositories.FlashSaleRepo;
import com.app.governance.states.FlashSaleEvent;
import com.app.governance.states.OperationalStateMachineService;
import com.app.logistics.inventory.services.FlashSaleInventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Log4j2
@RequiredArgsConstructor
public class FlashSaleService {

    private final FlashSaleRepo flashSaleRepo;
    private final FlashSaleInventoryService inventoryService;
    private final OperationalStateMachineService stateMachineService;

    @Transactional
    public FlashSale createFlashSale(FlashSale flashSale) {
        flashSale.setCreatedAt(LocalDateTime.now());
        FlashSale saved = flashSaleRepo.save(flashSale);

        // If it's already active or starting soon, warm up the cache
        if (saved.isActive()) {
            warmUpCache(saved);
        }

        return saved;
    }

    @Transactional
    public void activateFlashSale(Long id) {
        FlashSale sale = flashSaleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FlashSale", "id", id));
        sale.setActive(true);
        flashSaleRepo.save(sale);

        // Trigger Standard Event
        stateMachineService.triggerFlashSaleEvent(sale.getFlashSaleId(),
                FlashSaleEvent.START);

        warmUpCache(sale);
    }

    private void warmUpCache(FlashSale sale) {
        log.info("Warming up cache for Flash Sale: {}", sale.getSaleName());
        for (FlashSaleProduct fsp : sale.getProducts()) {
            inventoryService.initializeFlashStock(fsp.getProductId(), fsp.getFlashQuantity());
        }
    }

    public Optional<FlashSaleProduct> getActiveFlashProduct(Long productId) {
        LocalDateTime now = LocalDateTime.now();
        return flashSaleRepo.findActiveSaleForProduct(productId, now)
                .flatMap(fs -> fs.getProducts().stream()
                        .filter(p -> p.getProductId().equals(productId))
                        .findFirst());
    }

    @Transactional
    public void syncStockFromRedis(Long saleId) {
        FlashSale sale = flashSaleRepo.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("FlashSale", "id", saleId));

        for (FlashSaleProduct fsp : sale.getProducts()) {
            int remaining = inventoryService.getRemainingStock(fsp.getProductId());
            int sold = fsp.getFlashQuantity() - remaining;
            fsp.setSoldQuantity(sold);
        }
        flashSaleRepo.save(sale);
    }
}
