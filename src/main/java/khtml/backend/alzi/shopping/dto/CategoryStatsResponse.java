package khtml.backend.alzi.shopping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "카테고리별 통계 응답")
public class CategoryStatsResponse {
    
    @Schema(description = "카테고리별 아이템 개수")
    private List<CategoryInfo> categories;
    
    @Schema(description = "전체 카테고리 수")
    private int totalCategories;
    
    @Schema(description = "전체 아이템 수")
    private long totalItems;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryInfo {
        @Schema(description = "카테고리명", example = "채소류")
        private String categoryName;
        
        @Schema(description = "해당 카테고리의 아이템 개수", example = "25")
        private long itemCount;
        
        @Schema(description = "전체 대비 비율 (%)", example = "18.5")
        private double percentage;
        
        @Schema(description = "해당 카테고리의 아이템 목록 (상위 5개)", example = "[\"감자\", \"양파\", \"당근\", \"배추\", \"무\"]")
        private List<String> topItems;
    }
}
