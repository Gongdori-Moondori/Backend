package khtml.backend.alzi.shopping.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class CreateShoppingListRequest {
    private List<ShoppingItemRequest> items;
    
    @Data
    public static class ShoppingItemRequest {
        private String itemName;
        private String category;
        private Integer quantity;
        private String memo;
    }
}
