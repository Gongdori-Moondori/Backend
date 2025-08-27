package khtml.backend.alzi.batch.config;

import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import khtml.backend.alzi.batch.processor.PriceDataProcessor;
import khtml.backend.alzi.batch.reader.ItemNameReader;
import khtml.backend.alzi.batch.writer.PriceDataWriter;
import khtml.backend.alzi.shopping.Item;
import khtml.backend.alzi.shopping.ItemPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
@Slf4j
public class PriceDataBatchConfig {
    
    private final ItemNameReader itemNameReader;
    private final PriceDataProcessor priceDataProcessor;
    private final PriceDataWriter priceDataWriter;
    
    @Bean
    public Job priceDataCollectionJob(JobRepository jobRepository, 
                                     PlatformTransactionManager transactionManager) {
        return new JobBuilder("priceDataCollectionJob", jobRepository)
            .start(priceDataCollectionStep(jobRepository, transactionManager))
            .build();
    }
    
    @Bean
    public Step priceDataCollectionStep(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager) {
        return new StepBuilder("priceDataCollectionStep", jobRepository)
            .<Item, List<ItemPrice>>chunk(5, transactionManager) // 한 번에 5개 아이템씩 처리
            .reader(itemNameReader)
            .processor(priceDataProcessor)
            .writer(priceDataWriter)
            .allowStartIfComplete(true) // 완료된 Job도 다시 실행 가능
            .build();
    }
}
