package com.app.batch;

import com.app.dto.ProductDTO;
import com.app.services.ERPNextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

/**
 * Spring Batch Configuration for ERPNext Product Sync
 * Processes products in chunks for memory efficiency
 */
@Slf4j
@Configuration
public class ProductSyncBatchConfig {

    @Autowired
    private ERPNextService erpNextService;

    @Bean
    public Job productSyncJob(JobRepository jobRepository, Step productSyncStep) {
        return new JobBuilder("productSyncJob", jobRepository)
                .start(productSyncStep)
                .build();
    }

    @Bean
    public Step productSyncStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<ProductDTO> productReader,
            ItemProcessor<ProductDTO, ProductDTO> productProcessor,
            ItemWriter<ProductDTO> productWriter) {
        return new StepBuilder("productSyncStep", jobRepository)
                .<ProductDTO, ProductDTO>chunk(100, transactionManager) // Process 100 at a time
                .reader(productReader)
                .processor(productProcessor)
                .writer(productWriter)
                .build();
    }

    @Bean
    public ItemReader<ProductDTO> productReader() {
        return new ListItemReader<>(List.of()); // TODO: Implement ERPNext reader
    }

    @Bean
    public ItemProcessor<ProductDTO, ProductDTO> productProcessor() {
        return product -> {
            log.debug("Processing product: {}", product.getName());
            // Add any transformation logic here
            return product;
        };
    }

    @Bean
    public ItemWriter<ProductDTO> productWriter() {
        return items -> {
            log.info("Writing {} products to database", items.size());
            // TODO: Implement database writer
        };
    }
}
