package khtml.backend.alzi.shopping.dto;

import java.time.LocalDateTime;
import java.util.List;

import khtml.backend.alzi.shopping.ShoppingList;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShoppingListResponse {
    private Long id;
    private String status;
    private LocalDateTime createdAt;
    private List<ShoppingRecordResponse> items;
    
    public static ShoppingListResponse from(ShoppingList shoppingList) {
        List<ShoppingRecordResponse> items = shoppingList.getShoppingRecords() != null 
            ? shoppingList.getShoppingRecords().stream()
                .map(ShoppingRecordResponse::from)
                .toList()
            : List.of();
            
        return ShoppingListResponse.builder()
            .id(shoppingList.getId())
            .status(shoppingList.getStatus().name())
            .createdAt(shoppingList.getCreatedAt())
            .items(items)
            .build();
    }
}
