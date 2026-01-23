package com.app.order.repositories;

import com.app.order.entities.FlashSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlashSaleRepo extends JpaRepository<FlashSale, Long> {

    @Query("SELECT fs FROM FlashSale fs WHERE fs.active = true AND fs.startTime <= :now AND fs.endTime >= :now")
    List<FlashSale> findActiveSales(LocalDateTime now);

    @Query("SELECT fs FROM FlashSale fs JOIN fs.products fsp WHERE fs.active = true AND fs.startTime <= :now AND fs.endTime >= :now AND fsp.productId = :productId")
    Optional<FlashSale> findActiveSaleForProduct(Long productId, LocalDateTime now);
}
