package khtml.backend.alzi.shopping;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import khtml.backend.alzi.auth.user.User;
import khtml.backend.alzi.priceData.PriceDataRepository;
import khtml.backend.alzi.shopping.dto.CreateShoppingListRequest;
import khtml.backend.alzi.shopping.dto.ShoppingListResponse;
import khtml.backend.alzi.utils.ItemCategoryUtil;
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
	private final ItemCategoryUtil itemCategoryUtil;

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
			
			// 기존 아이템의 카테고리가 null이거나 비어있는 경우 자동 분류
			String finalCategory = category;
			if (item.getCategory() == null || item.getCategory().trim().isEmpty()) {
				finalCategory = (category != null && !category.trim().isEmpty()) 
					? category 
					: itemCategoryUtil.categorizeItem(itemName);
				
				item.updateInfo(finalCategory);
				itemRepository.save(item);
				log.info("아이템 카테고리 자동 설정 - 이름: {}, 카테고리: {}", itemName, finalCategory);
			} else if (category != null && !category.trim().isEmpty() && 
					   !category.equals(item.getCategory())) {
				// 새로운 카테고리 정보가 있고 기존과 다른 경우 업데이트
				item.updateInfo(category);
				itemRepository.save(item);
				log.info("아이템 카테고리 업데이트 - 이름: {}, 기존: {}, 신규: {}", 
					itemName, item.getCategory(), category);
			}
			
			return item;
		} else {
			// 새 아이템 생성 - 카테고리가 없으면 자동 분류
			String finalCategory = (category != null && !category.trim().isEmpty()) 
				? category 
				: itemCategoryUtil.categorizeItem(itemName);
			
			Item newItem = Item.builder()
				.name(itemName)
				.category(finalCategory)
				.build();
			
			Item savedItem = itemRepository.save(newItem);
			log.info("새 아이템 생성 - 이름: {}, 카테고리: {} (자동분류: {})", 
				itemName, finalCategory, category == null || category.trim().isEmpty());
			
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
				// 카테고리 자동 분류하여 아이템 생성
				String autoCategory = itemCategoryUtil.categorizeItem(split[0]);
				Item build = Item.builder()
					.name(split[0])
					.category(autoCategory)
					.build();
				itemRepository.save(build);
				log.info("가격 데이터에서 새 아이템 생성 - 이름: {}, 카테고리: {}", split[0], autoCategory);
			}
		});
	}
	
	/**
	 * 기존 아이템들의 카테고리를 일괄 업데이트
	 * 카테고리가 null이거나 "기타"인 아이템들을 대상으로 자동 분류 실행
	 */
	@Transactional
	public void updateItemCategories() {
		log.info("아이템 카테고리 일괄 업데이트 시작");
		
		List<Item> itemsToUpdate = itemRepository.findAll().stream()
			.filter(item -> item.getCategory() == null || 
						   item.getCategory().trim().isEmpty() ||
						   ItemCategoryUtil.OTHERS.equals(item.getCategory()))
			.toList();
		
		int updatedCount = 0;
		for (Item item : itemsToUpdate) {
			String autoCategory = itemCategoryUtil.categorizeItem(item.getName());
			
			// "기타"가 아닌 카테고리로 분류된 경우에만 업데이트
			if (!ItemCategoryUtil.OTHERS.equals(autoCategory)) {
				item.updateInfo(autoCategory);
				itemRepository.save(item);
				updatedCount++;
				log.info("카테고리 업데이트 - 아이템: {}, 카테고리: {}", item.getName(), autoCategory);
			}
		}
		
		log.info("아이템 카테고리 일괄 업데이트 완료 - 총 {}개 아이템 업데이트", updatedCount);
	}
}
