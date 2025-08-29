package khtml.backend.alzi.shopping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Data
@Schema(description = "장바구니에 아이템 추가 요청")
public class AddItemToCartRequest {
    
    @NotBlank(message = "아이템명은 필수입니다")
    @Schema(description = "추가할 아이템명", example = "감자")
    private String itemName;
    
    @Positive(message = "수량은 1 이상이어야 합니다")
    @Schema(description = "수량", example = "2")
    private Integer quantity = 1;
    
    @Schema(description = "카테고리 (선택사항, 없으면 자동 분류)", example = "채소류")
    private String category;
    
    @Schema(description = "메모 (선택사항)", example = "큰 것으로")
    private String memo;
}
