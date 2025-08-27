package khtml.backend.alzi.batch.writer;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import khtml.backend.alzi.shopping.ItemPrice;
import khtml.backend.alzi.shopping.ItemPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceDataWriter implements ItemWriter<List<ItemPrice>> {
    
    private final ItemPriceRepository itemPriceRepository;
    
    @Override
    public void write(Chunk<? extends List<ItemPrice>> chunk) throws Exception {
        // Chunk<List<ItemPrice>>를 List<ItemPrice>로 평면화
        List<ItemPrice> allItemPrices = chunk.getItems().stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());
        
        if (allItemPrices.isEmpty()) {
            return;
        }
        
        log.info("시장별 가격 정보 저장 시작 - 건수: {}", allItemPrices.size());
        
        try {
            // 배치로 저장
            List<ItemPrice> savedPrices = itemPriceRepository.saveAll(allItemPrices);
            log.info("시장별 가격 정보 저장 완료 - 저장된 건수: {}", savedPrices.size());
        } catch (Exception e) {
            log.error("시장별 가격 정보 저장 중 오류 발생", e);
            throw e;
        }
    }
}
