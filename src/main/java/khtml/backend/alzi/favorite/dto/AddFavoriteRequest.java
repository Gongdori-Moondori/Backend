package khtml.backend.alzi.favorite.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
@Schema(description = "즐겨찾기 추가 요청")
public class AddFavoriteRequest {
    
    @NotBlank(message = "아이템명은 필수입니다")
    @Schema(description = "아이템명", example = "감자")
    private String itemName;
    
    @NotBlank(message = "시장명은 필수입니다")
    @Schema(description = "시장명", example = "경동시장")
    private String marketName;
    
    @NotNull(message = "가격은 필수입니다")
    @Positive(message = "가격은 0보다 커야 합니다")
    @Schema(description = "가격", example = "3000")
    private BigDecimal price;
    
    @Schema(description = "가격 단위", example = "1kg")
    private String priceUnit;
    
    @Schema(description = "메모 (선택사항)", example = "크기가 큰 것으로")
    private String memo;
}
