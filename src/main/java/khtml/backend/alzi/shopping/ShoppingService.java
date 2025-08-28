package khtml.backend.alzi.shopping;

import java.math.BigDecimal;
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
	private final ItemPriceRepository itemPriceRepository;
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
				
				// 아이템 가격 조회
				BigDecimal unitPrice = getItemPrice(item);
				
				// 쇼핑 기록 생성 (가격 정보 포함)
				ShoppingRecord record = ShoppingRecord.builder()
					.shoppingList(savedShoppingList)
					.item(item)
					.quantity(itemRequest.getQuantity())
					.unitPrice(unitPrice)
					.build();
				
				shoppingRecordRepository.save(record);
				log.info("쇼핑 기록 생성 - 아이템: {}, 수량: {}, 단가: {}원", 
					item.getName(), itemRequest.getQuantity(), unitPrice);
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

	/**
	 * 아이템의 현재 평균 가격 조회
	 * ItemPrice에서 해당 아이템의 가격 정보를 가져와서 평균값 계산
	 */
	private BigDecimal getItemPrice(Item item) {
		try {
			List<ItemPrice> itemPrices = itemPriceRepository.findAllByItemName(item.getName());
			
			if (itemPrices.isEmpty()) {
				log.debug("아이템 '{}'의 가격 정보가 없습니다", item.getName());
				return getDefaultItemPrice(item.getName(), item.getCategory());
			}

			// 유효한 가격들만 필터링
			List<BigDecimal> validPrices = itemPrices.stream()
				.map(ItemPrice::getPrice)
				.filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
				.toList();

			if (validPrices.isEmpty()) {
				log.debug("아이템 '{}'의 유효한 가격 정보가 없습니다", item.getName());
				return getDefaultItemPrice(item.getName(), item.getCategory());
			}

			// 평균 가격 계산
			BigDecimal sum = validPrices.stream()
				.reduce(BigDecimal.ZERO, BigDecimal::add);
			
			BigDecimal averagePrice = sum.divide(
				BigDecimal.valueOf(validPrices.size()), 
				0, // 소수점 없이 정수로
				BigDecimal.ROUND_HALF_UP
			);

			log.info("아이템 '{}' 평균 가격 계산 완료 - {}원 ({}개 시장 평균)", 
				item.getName(), averagePrice, validPrices.size());

			return averagePrice;

		} catch (Exception e) {
			log.error("아이템 '{}' 가격 조회 실패: {}", item.getName(), e.getMessage());
			return getDefaultItemPrice(item.getName(), item.getCategory());
		}
	}

	/**
	 * 가격 정보가 없는 경우 기본 가격 설정
	 */
	private BigDecimal getDefaultItemPrice(String itemName, String category) {
		// 카테고리별 기본 가격 설정
		BigDecimal basePrice = switch (category != null ? category.toLowerCase() : "기타") {
			case "채소류" -> switch (itemName.toLowerCase()) {
				case "배추", "무" -> BigDecimal.valueOf(3000);
				case "상추", "시금치", "냉이", "미나리" -> BigDecimal.valueOf(2000);
				case "토마토", "오이", "가지", "호박" -> BigDecimal.valueOf(3500);
				case "대파", "브로콜리" -> BigDecimal.valueOf(2500);
				default -> BigDecimal.valueOf(3000);
			};
			case "과일류" -> switch (itemName.toLowerCase()) {
				case "수박" -> BigDecimal.valueOf(12000);
				case "복숭아", "포도", "사과" -> BigDecimal.valueOf(5000);
				case "딸기", "참외", "자두" -> BigDecimal.valueOf(4000);
				case "배", "감", "귤" -> BigDecimal.valueOf(4500);
				default -> BigDecimal.valueOf(5000);
			};
			case "곡물류" -> switch (itemName.toLowerCase()) {
				case "쌀" -> BigDecimal.valueOf(20000);
				case "옥수수", "고구마" -> BigDecimal.valueOf(3000);
				default -> BigDecimal.valueOf(5000);
			};
			case "수산물" -> BigDecimal.valueOf(15000);
			case "견과류" -> BigDecimal.valueOf(8000);
			default -> BigDecimal.valueOf(5000); // 기본값
		};

		log.info("아이템 '{}' 기본 가격 설정 - {}원 (카테고리: {})", itemName, basePrice, category);
		return basePrice;
	}

	/**
	 * 장바구니 아이템들을 구매 완료 상태로 변경
	 */
	@Transactional
	public ShoppingListResponse completeShoppingItems(Long shoppingListId, List<Long> itemIds, User user) {
		log.info("장바구니 {} 아이템 구매 완료 처리 시작 - 아이템 ID: {}", shoppingListId, itemIds);

		// 장바구니 조회 및 권한 확인
		ShoppingList shoppingList = shoppingListRepository.findByIdAndUser(shoppingListId, user)
			.orElseThrow(() -> new IllegalArgumentException("해당 장바구니를 찾을 수 없습니다."));

		// 아이템 ID들 검증 및 상태 변경
		int updatedCount = 0;
		for (Long itemId : itemIds) {
			Optional<ShoppingRecord> recordOpt = shoppingRecordRepository
				.findByIdAndShoppingListAndUser(itemId, shoppingList, user);
			
			if (recordOpt.isPresent()) {
				ShoppingRecord record = recordOpt.get();
				
				// 이미 완료된 아이템은 건너뛰기
				if (record.getStatus() == ShoppingRecord.PurchaseStatus.PURCHASED) {
					log.info("이미 구매 완료된 아이템 건너뛰기 - 아이템 ID: {}, 아이템명: {}", itemId, record.getItem().getName());
					continue;
				}
				
				record.markAsPurchased();
				shoppingRecordRepository.save(record);
				updatedCount++;
				
				log.info("아이템 구매 완료 처리 - 아이템 ID: {}, 아이템명: {}", itemId, record.getItem().getName());
			} else {
				log.warn("아이템 ID {}를 찾을 수 없거나 권한이 없습니다", itemId);
			}
		}

		log.info("장바구니 {} 아이템 구매 완료 처리 완료 - 총 {}개 처리", shoppingListId, updatedCount);

		// 업데이트된 장바구니 정보 반환
		ShoppingList updatedShoppingList = shoppingListRepository.findByIdAndUser(shoppingListId, user)
			.orElseThrow(() -> new IllegalStateException("장바구니 조회 실패"));
		
		return ShoppingListResponse.from(updatedShoppingList);
	}

	/**
	 * 장바구니 아이템들을 취소 상태로 변경
	 */
	@Transactional
	public ShoppingListResponse cancelShoppingItems(Long shoppingListId, List<Long> itemIds, User user) {
		log.info("장바구니 {} 아이템 취소 처리 시작 - 아이템 ID: {}", shoppingListId, itemIds);

		// 장바구니 조회 및 권한 확인
		ShoppingList shoppingList = shoppingListRepository.findByIdAndUser(shoppingListId, user)
			.orElseThrow(() -> new IllegalArgumentException("해당 장바구니를 찾을 수 없습니다."));

		// 아이템 ID들 검증 및 상태 변경
		int updatedCount = 0;
		for (Long itemId : itemIds) {
			Optional<ShoppingRecord> recordOpt = shoppingRecordRepository
				.findByIdAndShoppingListAndUser(itemId, shoppingList, user);
			
			if (recordOpt.isPresent()) {
				ShoppingRecord record = recordOpt.get();
				
				// 이미 취소된 아이템은 건너뛰기
				if (record.getStatus() == ShoppingRecord.PurchaseStatus.CANCELLED) {
					log.info("이미 취소된 아이템 건너뛰기 - 아이템 ID: {}, 아이템명: {}", itemId, record.getItem().getName());
					continue;
				}
				
				record.cancel();
				shoppingRecordRepository.save(record);
				updatedCount++;
				
				log.info("아이템 취소 처리 - 아이템 ID: {}, 아이템명: {}", itemId, record.getItem().getName());
			} else {
				log.warn("아이템 ID {}를 찾을 수 없거나 권한이 없습니다", itemId);
			}
		}

		log.info("장바구니 {} 아이템 취소 처리 완료 - 총 {}개 처리", shoppingListId, updatedCount);

		// 업데이트된 장바구니 정보 반환
		ShoppingList updatedShoppingList = shoppingListRepository.findByIdAndUser(shoppingListId, user)
			.orElseThrow(() -> new IllegalStateException("장바구니 조회 실패"));
		
		return ShoppingListResponse.from(updatedShoppingList);
	}

	/**
	 * 장바구니의 완료/미완료 아이템 통계 조회
	 */
	@Transactional(readOnly = true)
	public ShoppingListStatistics getShoppingListStatistics(Long shoppingListId, User user) {
		ShoppingList shoppingList = shoppingListRepository.findByIdAndUser(shoppingListId, user)
			.orElseThrow(() -> new IllegalArgumentException("해당 장바구니를 찾을 수 없습니다."));

		List<ShoppingRecord> records = shoppingList.getShoppingRecords();
		
		int totalItems = records.size();
		int completedItems = (int) records.stream()
			.filter(record -> record.getStatus() == ShoppingRecord.PurchaseStatus.PURCHASED)
			.count();
		int cancelledItems = (int) records.stream()
			.filter(record -> record.getStatus() == ShoppingRecord.PurchaseStatus.CANCELLED)
			.count();
		int plannedItems = totalItems - completedItems - cancelledItems;

		return ShoppingListStatistics.builder()
			.shoppingListId(shoppingListId)
			.totalItems(totalItems)
			.completedItems(completedItems)
			.plannedItems(plannedItems)
			.cancelledItems(cancelledItems)
			.completionRate(totalItems > 0 ? (double) completedItems / totalItems * 100 : 0.0)
			.build();
	}

	@lombok.Data
	@lombok.Builder
	public static class ShoppingListStatistics {
		private Long shoppingListId;
		private int totalItems;
		private int completedItems;
		private int plannedItems;
		private int cancelledItems;
		private double completionRate; // 완료율 (%)
	}
}
