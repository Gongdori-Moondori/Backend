package khtml.backend.alzi.shopping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "아이템 상태 변경 요청")
public class CompleteItemsRequest {
    
    @Schema(description = "상태를 변경할 아이템 ID 리스트", example = "[1, 2, 3]")
    private List<Long> itemIds;
    
    @Schema(description = "변경 사유 (선택사항)", example = "구매 완료")
    private String reason;
}
