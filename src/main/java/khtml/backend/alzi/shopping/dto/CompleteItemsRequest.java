package khtml.backend.alzi.shopping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "아이템 상태 변경 요청")
public class CompleteItemsRequest {
    
    @Schema(description = "상태를 변경할 아이템 ID 리스트", example = "[1, 2, 3]")
    private List<Long> itemIds;
    
    @Schema(description = "변경 사유 (선택사항)", example = "구매 완료")
    private String reason;
    
    @Schema(description = "아이템별 구매 시장 정보 (아이템 ID -> 시장명)", 
            example = "{\"1\": \"경동시장\", \"2\": \"남대문시장\"}")
    private Map<Long, String> itemMarkets;
}
