package khtml.backend.alzi.market.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MarketUpdateResult {
    private int totalCount;       // 총 처리된 행 수
    private int successCount;     // 성공한 행 수
    private int failCount;        // 실패한 행 수
    private LocalDateTime processedAt; // 처리 시간
    private List<String> errorMessages; // 오류 메시지 목록
    
    public static MarketUpdateResult of(int totalCount, int successCount, int failCount, List<String> errorMessages) {
        return MarketUpdateResult.builder()
            .totalCount(totalCount)
            .successCount(successCount)
            .failCount(failCount)
            .processedAt(LocalDateTime.now())
            .errorMessages(errorMessages)
            .build();
    }
}
