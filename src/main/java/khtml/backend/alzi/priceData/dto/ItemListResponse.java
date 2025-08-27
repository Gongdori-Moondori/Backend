package khtml.backend.alzi.priceData.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemListResponse {
    private List<String> itemNames;
    private List<String> marketNames;
    
    public static ItemListResponse of(List<String> itemNames, List<String> marketNames) {
        return new ItemListResponse(itemNames, marketNames);
    }
}
