package com.app.order.batch;

import com.app.product.payloads.ProductDTO;
import com.app.order.services.ERPNextService;
import lombok.extern.slf4j.Slf4j;
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.Step;
//import org.springframework.batch.core.job.builder.JobBuilder;
//import org.springframework.batch.core.repository.JobRepository;
//import org.springframework.batch.core.step.builder.StepBuilder;
//import org.springframework.batch.item.ItemProcessor;
//import org.springframework.batch.item.ItemReader;
//import org.springframework.batch.item.ItemWriter;
//import org.springframework.batch.item.support.ListItemReader;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.transaction.PlatformTransactionManager;
//
//import java.util.List;

/**
 * Spring Batch Configuration for ERPNext Product Sync
 * Processes products in chunks for memory efficiency
 * 
 * TODO: Re-enable after verifying Spring Batch 5 compatibility with Spring Boot
 * 4
 * This class has been temporarily disabled for the Spring Boot 4 migration.
 * Spring Batch 5 API has changed significantly and requires updates to this
 * configuration.
 */
// @Configuration // Temporarily disabled for Spring Boot 4 migration
public class ProductSyncBatchConfig {

    // All functionality temporarily commented out for Spring Boot 4.0 migration
    // TODO: Update to Spring Batch 5 API when re-enabling this feature

    // @Autowired
    // private ERPNextService erpNextService;
    //
    // @Bean
    // public Job productSyncJob(JobRepository jobRepository, Step productSyncStep)
    // {
    // return new JobBuilder("productSyncJob", jobRepository)
    // .start(productSyncStep)
    // .build();
    // }
    //
    // @Bean
    // public Step productSyncStep(JobRepository jobRepository,
    // PlatformTransactionManager transactionManager,
    // ItemReader<ProductDTO> productReader,
    // ItemProcessor<ProductDTO, ProductDTO> productProcessor,
    // ItemWriter<ProductDTO> productWriter) {
    // return new StepBuilder("productSyncStep", jobRepository)
    // .<ProductDTO, ProductDTO>chunk(100, transactionManager) // Process 100 at a
    // time
    // .reader(productReader)
    // .processor(productProcessor)
    // .writer(productWriter)
    // .build();
    // }
    //
    // @Bean
    // public ItemReader<ProductDTO> productReader() {
    // return new ListItemReader<>(List.of()); // TODO: Implement ERPNext reader
    // }
    //
    // @Bean
    // public ItemProcessor<ProductDTO, ProductDTO> productProcessor() {
    // return product -> {
    // log.debug("Processing product: {}", product.getProductName());
    // // Add any transformation logic here
    // return product;
    // };
    // }
    //
    // @Bean
    // public ItemWriter<ProductDTO> productWriter() {
    // return items -> {
    // log.info("Writing {} products to database", items.size());
    // // TODO: Implement database writer
    // };
    // }
}
