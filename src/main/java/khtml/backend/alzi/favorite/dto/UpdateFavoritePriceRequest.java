package khtml.backend.alzi.favorite.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
@Schema(description = "즐겨찾기 가격 업데이트 요청")
public class UpdateFavoritePriceRequest {
    
    @NotNull(message = "새 가격은 필수입니다")
    @Positive(message = "가격은 0보다 커야 합니다")
    @Schema(description = "새 가격", example = "2800")
    private BigDecimal newPrice;
}
