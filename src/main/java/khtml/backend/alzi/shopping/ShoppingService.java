package khtml.backend.alzi.shopping;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import khtml.backend.alzi.auth.user.User;
import khtml.backend.alzi.priceData.PriceDataRepository;
import khtml.backend.alzi.shopping.dto.CreateShoppingListRequest;
import khtml.backend.alzi.shopping.dto.ShoppingListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShoppingService {
	private final ItemRepository itemRepository;
	private final ShoppingListRepository shoppingListRepository;
	private final ShoppingRecordRepository shoppingRecordRepository;
	private final PriceDataRepository priceDataRepository;

	@Transactional
	public ShoppingListResponse createShoppingList(CreateShoppingListRequest request, User user) {
		log.info("장바구니 생성 시작 - 사용자: {}, 제목: {}", user.getUserId());
		
		// 장보기 리스트 생성
		ShoppingList shoppingList = ShoppingList.builder()
			.user(user)
			.build();
		
		ShoppingList savedShoppingList = shoppingListRepository.save(shoppingList);
		log.info("장보기 리스트 저장 완료 - ID: {}", savedShoppingList.getId());
		
		// 아이템들 처리 및 쇼핑 기록 생성
		if (request.getItems() != null && !request.getItems().isEmpty()) {
			for (CreateShoppingListRequest.ShoppingItemRequest itemRequest : request.getItems()) {
				// 기존 아이템 찾기 또는 새로 생성
				Item item = findOrCreateItem(itemRequest.getItemName(), itemRequest.getCategory());
				
				// 쇼핑 기록 생성
				ShoppingRecord record = ShoppingRecord.builder()
					.shoppingList(savedShoppingList)
					.item(item)
					.quantity(itemRequest.getQuantity())
					.memo(itemRequest.getMemo())
					.build();
				
				shoppingRecordRepository.save(record);
				log.info("쇼핑 기록 생성 - 아이템: {}, 수량: {}", item.getName(), itemRequest.getQuantity());
			}
		}
		
		// 응답 데이터를 위해 다시 조회 (연관관계 포함)
		ShoppingList resultShoppingList = shoppingListRepository.findById(savedShoppingList.getId())
			.orElseThrow(() -> new IllegalStateException("저장된 쇼핑 리스트를 찾을 수 없습니다."));
		
		return ShoppingListResponse.from(resultShoppingList);
	}
	
	/**
	 * 아이템명으로 기존 아이템을 찾거나 새로 생성
	 */
	@Transactional
	public Item findOrCreateItem(String itemName, String category) {
		Optional<Item> existingItem = itemRepository.findByName(itemName);
		
		if (existingItem.isPresent()) {
			log.info("기존 아이템 사용 - 이름: {}", itemName);
			Item item = existingItem.get();
			
			// 카테고리 업데이트 (새로운 카테고리 정보가 있고, 기존 카테고리가 없거나 다른 경우)
			if (category != null && !category.trim().isEmpty() && 
				(item.getCategory() == null || !category.equals(item.getCategory()))) {
				item.updateInfo(category);
				itemRepository.save(item);
				log.info("아이템 카테고리 업데이트 - 이름: {}, 카테고리: {}", itemName, category);
			}
			
			return item;
		} else {
			// 새 아이템 생성
			Item newItem = Item.builder()
				.name(itemName)
				.category(category)
				.build();
			
			Item savedItem = itemRepository.save(newItem);
			log.info("새 아이템 생성 - 이름: {}, 카테고리: {}", itemName, category);
			
			return savedItem;
		}
	}
	
	/**
	 * 사용자의 모든 장보기 리스트 조회
	 */
	@Transactional(readOnly = true)
	public List<ShoppingListResponse> getUserShoppingLists(User user) {
		List<ShoppingList> shoppingLists = shoppingListRepository.findByUserOrderByCreatedAtDesc(user);
		return shoppingLists.stream()
			.map(ShoppingListResponse::from)
			.toList();
	}
	
	/**
	 * 특정 장보기 리스트 조회
	 */
	@Transactional(readOnly = true)
	public ShoppingListResponse getShoppingList(Long shoppingListId, User user) {
		ShoppingList shoppingList = shoppingListRepository.findByIdAndUser(shoppingListId, user)
			.orElseThrow(() -> new IllegalArgumentException("해당 장보기 리스트를 찾을 수 없습니다."));
		
		return ShoppingListResponse.from(shoppingList);
	}

	public void test() {
		List<String> distinctItemNames = priceDataRepository.findDistinctItemNames();
		distinctItemNames.forEach(distinctItemName -> {
			String[] split = distinctItemName.split(" ");
			if (!itemRepository.existsItemByName(split[0])) {
				Item build;
				build = Item.builder()
					.name(split[0]).build();
				itemRepository.save(build);
			}
		});
	}
}
