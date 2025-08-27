package khtml.backend.alzi.batch.reader;

import java.util.Iterator;
import java.util.List;

import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import khtml.backend.alzi.shopping.Item;
import khtml.backend.alzi.shopping.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ItemNameReader implements ItemReader<Item> {
    
    private final ItemRepository itemRepository;
    private Iterator<Item> itemIterator;
    private boolean initialized = false;
    
    @Override
    public Item read() throws Exception {
        if (!initialized) {
            initialize();
        }
        
        if (itemIterator != null && itemIterator.hasNext()) {
            Item item = itemIterator.next();
            log.debug("읽어온 아이템: {}", item.getName());
            return item;
        }
        
        return null; // 더 이상 읽을 데이터가 없음
    }
    
    private void initialize() {
        List<Item> items = itemRepository.findAll();
        log.info("배치 처리할 아이템 수: {}", items.size());
        
        this.itemIterator = items.iterator();
        this.initialized = true;
    }
}
