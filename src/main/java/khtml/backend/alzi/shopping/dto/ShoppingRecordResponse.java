package khtml.backend.alzi.shopping.dto;

import java.math.BigDecimal;

import khtml.backend.alzi.shopping.ShoppingRecord;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShoppingRecordResponse {
    private Long id;
    private String itemName;
    private String category;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String status;

    public static ShoppingRecordResponse from(ShoppingRecord record) {
        return ShoppingRecordResponse.builder()
            .id(record.getId())
            .itemName(record.getItem().getName())
            .category(record.getItem().getCategory())
            .quantity(record.getQuantity())
            .unitPrice(record.getUnitPrice())
            .totalPrice(record.getPrice())
            .status(record.getStatus().name())
            .build();
    }
}
