package khtml.backend.alzi.shopping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveItemFromCartRequest {
	private Long shoppingListId;  // 장바구니 ID (optional, null이면 현재 열린 장바구니에서 제거)
	private Long itemId;          // 제거할 쇼핑 레코드 ID
	private Integer quantityToRemove;  // 제거할 수량 (optional, null이면 전체 제거)
}
