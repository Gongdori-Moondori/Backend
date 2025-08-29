package khtml.backend.alzi.shopping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import khtml.backend.alzi.auth.user.User;
import khtml.backend.alzi.priceData.PriceDataRepository;
import khtml.backend.alzi.shopping.dto.CreateShoppingListRequest;
import khtml.backend.alzi.shopping.dto.ShoppingListResponse;
import khtml.backend.alzi.utils.ItemCategoryUtil;
import khtml.backend.alzi.utils.SeasonalRecommendationUtil;
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
	private final SeasonalRecommendationUtil seasonalRecommendationUtil;
	private final SavingsService savingsService; // 절약 금액 계산 서비스 추가
	private final khtml.backend.alzi.market.MarketRepository marketRepository; // 시장 Repository 추가

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
		return completeShoppingItems(shoppingListId, itemIds, user, null);
	}

	/**
	 * 장바구니 아이템들을 구매 완료 상태로 변경 (절약 금액 계산 포함)
	 */
	@Transactional
	public ShoppingListResponse completeShoppingItems(Long shoppingListId, List<Long> itemIds, User user, Map<Long, String> itemMarkets) {
		log.info("장바구니 {} 아이템 구매 완료 처리 시작 - 아이템 ID: {}", shoppingListId, itemIds);

		// 장바구니 조회 및 권한 확인
		ShoppingList shoppingList = shoppingListRepository.findByIdAndUser(shoppingListId, user)
			.orElseThrow(() -> new IllegalArgumentException("해당 장바구니를 찾을 수 없습니다."));

		// 아이템 ID들 검증 및 상태 변경
		int updatedCount = 0;
		int savingsCalculatedCount = 0;
		BigDecimal totalSavings = BigDecimal.ZERO;

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
				
				// 시장 정보 업데이트 (있는 경우)
				if (itemMarkets != null && itemMarkets.containsKey(itemId)) {
					String marketName = itemMarkets.get(itemId);
					Optional<khtml.backend.alzi.market.Market> marketOpt = marketRepository.findByName(marketName);
					if (marketOpt.isPresent()) {
						record.updateMarket(marketOpt.get());
						log.info("아이템 ID {} 구매 시장 설정 - {}", itemId, marketName);
					} else {
						log.warn("시장 '{}' 정보를 찾을 수 없습니다", marketName);
					}
				}
				
				// 구매 완료 처리
				record.markAsPurchased();
				shoppingRecordRepository.save(record);
				updatedCount++;
				
				// 절약 금액 계산 및 저장
				try {
					SavingsService.SavingsCalculationResult savingsResult = 
						savingsService.calculateAndSaveSavings(record, user);
					
					if (savingsResult.isSuccess()) {
						savingsCalculatedCount++;
						BigDecimal savingsAmount = savingsResult.getSavingsRecord().getSavingsAmount();
						totalSavings = totalSavings.add(savingsAmount);
						
						log.info("아이템 '{}'의 절약 금액 계산 완료 - {}원 ({}와 비교)", 
							record.getItem().getName(), 
							savingsAmount,
							savingsResult.getComparisonResult().getComparisonType());
					} else {
						log.info("아이템 '{}'의 절약 금액 계산 실패 - {}", 
							record.getItem().getName(), savingsResult.getMessage());
					}
				} catch (Exception e) {
					log.error("아이템 '{}'의 절약 금액 계산 중 오류 발생: {}", 
						record.getItem().getName(), e.getMessage(), e);
				}
				
				log.info("아이템 구매 완료 처리 - 아이템 ID: {}, 아이템명: {}", itemId, record.getItem().getName());
			} else {
				log.warn("아이템 ID {}를 찾을 수 없거나 권한이 없습니다", itemId);
			}
		}

		log.info("장바구니 {} 아이템 구매 완료 처리 완료 - 총 {}개 처리, 절약금액 계산 {}개, 총 절약: {}원", 
			shoppingListId, updatedCount, savingsCalculatedCount, totalSavings);

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

	/**
	 * 자주 구매한 상품 조회 (상위 5개)
	 */
	@Transactional(readOnly = true)
	public List<FrequentItemResponse> getFrequentItems(User user) {
		log.info("사용자 {} 자주 구매한 상품 분석 시작", user.getUserId());

		try {
			// 사용자의 모든 구매 완료 기록 조회
			List<ShoppingRecord> purchasedRecords = shoppingRecordRepository
				.findPurchasedRecordsByUser(user.getUserId());

			if (purchasedRecords.isEmpty()) {
				log.info("사용자 {} 구매 기록이 없습니다", user.getUserId());
				return List.of();
			}

			// 아이템별로 구매 통계 계산
			Map<String, FrequentItemStats> itemStatsMap = purchasedRecords.stream()
				.collect(Collectors.groupingBy(
					record -> record.getItem().getName(),
					Collectors.collectingAndThen(
						Collectors.toList(),
						records -> calculateItemStatistics(records)
					)
				));

			// 구매 횟수 기준으로 정렬하여 상위 5개 선택
			List<FrequentItemResponse> frequentItems = itemStatsMap.entrySet().stream()
				.sorted((entry1, entry2) -> {
					FrequentItemStats stats1 = entry1.getValue();
					FrequentItemStats stats2 = entry2.getValue();
					
					// 1차: 구매 횟수 내림차순
					int compareCount = Integer.compare(stats2.getPurchaseCount(), stats1.getPurchaseCount());
					if (compareCount != 0) return compareCount;
					
					// 2차: 총 구매량 내림차순
					int compareQuantity = Integer.compare(stats2.getTotalQuantity(), stats1.getTotalQuantity());
					if (compareQuantity != 0) return compareQuantity;
					
					// 3차: 마지막 구매일 내림차순 (최근 구매 우선)
					return stats2.getLastPurchaseDate().compareTo(stats1.getLastPurchaseDate());
				})
				.limit(5)
				.map(entry -> {
					String itemName = entry.getKey();
					FrequentItemStats stats = entry.getValue();
					
					return FrequentItemResponse.builder()
						.itemName(itemName)
						.category(stats.getCategory())
						.purchaseCount(stats.getPurchaseCount())
						.totalQuantity(stats.getTotalQuantity())
						.averageQuantityPerPurchase(stats.getAverageQuantityPerPurchase())
						.averagePrice(stats.getAveragePrice())
						.totalSpent(stats.getTotalSpent())
						.lastPurchaseDate(stats.getLastPurchaseDate())
						.daysSinceLastPurchase(stats.getDaysSinceLastPurchase())
						.isSeasonalItem(seasonalRecommendationUtil.isCurrentlyInSeason(itemName))
						.purchaseFrequency(calculatePurchaseFrequency(stats))
						.recommendation(generateItemRecommendation(stats, itemName))
						.build();
				})
				.collect(Collectors.toList());

			log.info("사용자 {} 자주 구매한 상품 분석 완료 - {}개 상품", user.getUserId(), frequentItems.size());
			return frequentItems;

		} catch (Exception e) {
			log.error("자주 구매한 상품 조회 실패: {}", e.getMessage(), e);
			return List.of();
		}
	}

	/**
	 * 아이템별 구매 통계 계산
	 */
	private FrequentItemStats calculateItemStatistics(List<ShoppingRecord> records) {
		if (records.isEmpty()) {
			return FrequentItemStats.builder()
				.category("기타")
				.purchaseCount(0)
				.totalQuantity(0)
				.averageQuantityPerPurchase(0.0)
				.averagePrice(BigDecimal.ZERO)
				.totalSpent(BigDecimal.ZERO)
				.lastPurchaseDate(LocalDateTime.now())
				.daysSinceLastPurchase(999)
				.build();
		}

		ShoppingRecord latestRecord = records.stream()
			.max(Comparator.comparing(ShoppingRecord::getPurchasedAt))
			.orElse(records.get(0));

		int purchaseCount = records.size();
		int totalQuantity = records.stream().mapToInt(ShoppingRecord::getQuantity).sum();
		double averageQuantity = (double) totalQuantity / purchaseCount;

		// 유효한 가격이 있는 기록들만 사용
		List<BigDecimal> validPrices = records.stream()
			.map(ShoppingRecord::getUnitPrice)
			.filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
			.collect(Collectors.toList());

		BigDecimal averagePrice = BigDecimal.ZERO;
		BigDecimal totalSpent = BigDecimal.ZERO;

		if (!validPrices.isEmpty()) {
			BigDecimal priceSum = validPrices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
			averagePrice = priceSum.divide(BigDecimal.valueOf(validPrices.size()), 0, RoundingMode.HALF_UP);
			
			totalSpent = records.stream()
				.map(ShoppingRecord::getPrice)
				.filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		}

		LocalDateTime lastPurchase = latestRecord.getPurchasedAt();
		long daysSinceLastPurchase = lastPurchase != null ? 
			java.time.temporal.ChronoUnit.DAYS.between(lastPurchase.toLocalDate(), LocalDate.now()) : 0;

		return FrequentItemStats.builder()
			.category(latestRecord.getItem().getCategory())
			.purchaseCount(purchaseCount)
			.totalQuantity(totalQuantity)
			.averageQuantityPerPurchase(averageQuantity)
			.averagePrice(averagePrice)
			.totalSpent(totalSpent)
			.lastPurchaseDate(lastPurchase)
			.daysSinceLastPurchase((int) daysSinceLastPurchase)
			.build();
	}

	/**
	 * 구매 빈도 계산
	 */
	private String calculatePurchaseFrequency(FrequentItemStats stats) {
		if (stats.getDaysSinceLastPurchase() <= 7) {
			return "매우 높음";
		} else if (stats.getDaysSinceLastPurchase() <= 30) {
			return "높음";
		} else if (stats.getDaysSinceLastPurchase() <= 90) {
			return "보통";
		} else {
			return "낮음";
		}
	}

	/**
	 * 아이템 추천 메시지 생성
	 */
	private String generateItemRecommendation(FrequentItemStats stats, String itemName) {
		StringBuilder recommendation = new StringBuilder();

		if (stats.getDaysSinceLastPurchase() <= 7) {
			recommendation.append("최근에 구매한 단골 아이템입니다");
		} else if (stats.getDaysSinceLastPurchase() <= 30) {
			recommendation.append("자주 구매하는 아이템입니다");
		} else if (stats.getDaysSinceLastPurchase() <= 90) {
			recommendation.append("다시 구매할 시기가 되었습니다");
		} else {
			recommendation.append("오랫동안 구매하지 않은 아이템입니다");
		}

		// 제철 아이템인 경우 추가 정보
		if (seasonalRecommendationUtil.isCurrentlyInSeason(itemName)) {
			recommendation.append(" (현재 제철!)");
		}

		return recommendation.toString();
	}

	/**
	 * 사용자 절약 통계 조회
	 */
	@Transactional(readOnly = true)
	public SavingsService.UserSavingsStats getSavingsStatistics(User user) {
		log.info("사용자 {} 절약 통계 조회", user.getUserId());
		return savingsService.getUserSavingsStats(user);
	}

	/**
	 * 현재 열려있는 장바구니 찾기 (PLANNED 또는 IN_PROGRESS 상태)
	 */
	@Transactional(readOnly = true)
	public Optional<ShoppingList> getCurrentOpenShoppingList(User user) {
		List<ShoppingList> openLists = shoppingListRepository.findByUserAndStatusInOrderByCreatedAtDesc(
			user, 
			List.of(ShoppingList.ShoppingListStatus.PLANNED, ShoppingList.ShoppingListStatus.IN_PROGRESS)
		);
		
		return openLists.isEmpty() ? Optional.empty() : Optional.of(openLists.get(0));
	}

	/**
	 * 현재 열려있는 장바구니에 아이템 추가 (없으면 새로 생성)
	 */
	@Transactional
	public AddItemToCartResponse addItemToCurrentCart(User user, String itemName, Integer quantity, 
	                                                   String category, String memo) {
		log.info("현재 장바구니에 아이템 추가 - 사용자: {}, 아이템: {}, 수량: {}", 
			user.getUserId(), itemName, quantity);

		try {
			// 1. 현재 열려있는 장바구니 찾기
			Optional<ShoppingList> currentCartOpt = getCurrentOpenShoppingList(user);
			ShoppingList shoppingList;
			boolean newCartCreated = false;

			if (currentCartOpt.isPresent()) {
				shoppingList = currentCartOpt.get();
				log.info("기존 열린 장바구니 사용 - ID: {}", shoppingList.getId());
			} else {
				// 열린 장바구니가 없으면 새로 생성
				shoppingList = ShoppingList.builder()
					.user(user)
					.build();
				shoppingList = shoppingListRepository.save(shoppingList);
				newCartCreated = true;
				log.info("새 장바구니 생성 - ID: {}", shoppingList.getId());
			}

			// 2. 아이템 찾기 또는 생성
			Item item = findOrCreateItem(itemName, category);

			// 3. 기존에 같은 아이템이 장바구니에 있는지 확인
			Optional<ShoppingRecord> existingRecord = shoppingRecordRepository
				.findByShoppingListAndItem(shoppingList, item)
				.stream()
				.filter(record -> record.getStatus() == ShoppingRecord.PurchaseStatus.PLANNED)
				.findFirst();

			ShoppingRecord shoppingRecord;
			boolean itemUpdated = false;

			if (existingRecord.isPresent()) {
				// 기존 아이템 수량 업데이트
				shoppingRecord = existingRecord.get();
				int newQuantity = shoppingRecord.getQuantity() + quantity;
				shoppingRecord.setQuantity(newQuantity);
				
				// 가격 재계산
				BigDecimal unitPrice = getItemPrice(item);
				shoppingRecord.updatePrice(unitPrice);
				
				shoppingRecordRepository.save(shoppingRecord);
				itemUpdated = true;
				
				log.info("기존 아이템 수량 업데이트 - 아이템: {}, 기존: {}개 → 새로운: {}개", 
					itemName, shoppingRecord.getQuantity() - quantity, newQuantity);
			} else {
				// 새 아이템 추가
				BigDecimal unitPrice = getItemPrice(item);
				
				shoppingRecord = ShoppingRecord.builder()
					.shoppingList(shoppingList)
					.item(item)
					.quantity(quantity)
					.unitPrice(unitPrice)
					.build();
				
				shoppingRecordRepository.save(shoppingRecord);
				log.info("새 아이템 추가 - 아이템: {}, 수량: {}개, 단가: {}원", 
					itemName, quantity, unitPrice);
			}

			// 4. 장바구니 상태 업데이트 (아이템이 있으면 IN_PROGRESS)
			if (shoppingList.getStatus() == ShoppingList.ShoppingListStatus.PLANNED) {
				shoppingList.updateStatus(ShoppingList.ShoppingListStatus.IN_PROGRESS);
				shoppingListRepository.save(shoppingList);
			}

			return AddItemToCartResponse.builder()
				.success(true)
				.shoppingListId(shoppingList.getId())
				.itemName(itemName)
				.quantity(shoppingRecord.getQuantity())
				.unitPrice(shoppingRecord.getUnitPrice())
				.totalPrice(shoppingRecord.getPrice())
				.newCartCreated(newCartCreated)
				.itemUpdated(itemUpdated)
				.message(buildAddItemMessage(itemName, quantity, itemUpdated, newCartCreated))
				.build();

		} catch (Exception e) {
			log.error("장바구니에 아이템 추가 실패: {}", e.getMessage(), e);
			return AddItemToCartResponse.builder()
				.success(false)
				.message("아이템 추가 중 오류가 발생했습니다: " + e.getMessage())
				.build();
		}
	}

	/**
	 * 아이템 추가 결과 메시지 생성
	 */
	private String buildAddItemMessage(String itemName, Integer addedQuantity, 
	                                   boolean itemUpdated, boolean newCartCreated) {
		StringBuilder message = new StringBuilder();
		
		if (newCartCreated) {
			message.append("새 장바구니를 생성하고 ");
		}
		
		if (itemUpdated) {
			message.append(String.format("'%s' %d개를 기존 수량에 추가했습니다", itemName, addedQuantity));
		} else {
			message.append(String.format("'%s' %d개를 장바구니에 추가했습니다", itemName, addedQuantity));
		}
		
		return message.toString();
	}

	// === 새로운 DTO 클래스 ===
	
	@lombok.Data
	@lombok.Builder
	public static class AddItemToCartResponse {
		private boolean success;
		private Long shoppingListId;
		private String itemName;
		private Integer quantity;        // 현재 총 수량
		private BigDecimal unitPrice;
		private BigDecimal totalPrice;   // 해당 아이템의 총 가격
		private boolean newCartCreated;  // 새 장바구니 생성 여부
		private boolean itemUpdated;     // 기존 아이템 수량 업데이트 여부
		private String message;
	}

	// DTO 클래스들
	@lombok.Data
	@lombok.Builder
	public static class FrequentItemResponse {
		private String itemName;                    // 아이템명
		private String category;                    // 카테고리
		private int purchaseCount;                  // 구매 횟수
		private int totalQuantity;                  // 총 구매량
		private double averageQuantityPerPurchase;  // 회당 평균 구매량
		private BigDecimal averagePrice;            // 평균 구매 가격
		private BigDecimal totalSpent;              // 총 지출 금액
		private LocalDateTime lastPurchaseDate;     // 마지막 구매일
		private int daysSinceLastPurchase;          // 마지막 구매 후 경과 일수
		private boolean isSeasonalItem;             // 현재 제철 여부
		private String purchaseFrequency;           // 구매 빈도 ("매우 높음", "높음", "보통", "낮음")
		private String recommendation;              // 추천 메시지
	}

	@lombok.Data
	@lombok.Builder
	@lombok.NoArgsConstructor
	@lombok.AllArgsConstructor
	private static class FrequentItemStats {
		private String category;
		private int purchaseCount;
		private int totalQuantity;
		private double averageQuantityPerPurchase;
		private BigDecimal averagePrice;
		private BigDecimal totalSpent;
		private LocalDateTime lastPurchaseDate;
		private int daysSinceLastPurchase;
	}
}
