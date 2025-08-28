package khtml.backend.alzi.market;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import khtml.backend.alzi.exception.CustomException;
import khtml.backend.alzi.exception.ErrorCode;
import khtml.backend.alzi.market.dto.response.MarketItemPriceResponse;
import khtml.backend.alzi.market.dto.response.MarketUpdateResult;
import khtml.backend.alzi.priceData.PriceData;
import khtml.backend.alzi.priceData.PriceDataRepository;
import khtml.backend.alzi.shopping.ItemPrice;
import khtml.backend.alzi.shopping.ItemPriceRepository;
import khtml.backend.alzi.utils.PricePredictionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketService {
	private final MarketRepository marketRepository;
	private final PriceDataRepository priceDataRepository;
	private final ItemPriceRepository itemPriceRepository;
	private final PricePredictionUtil pricePredictionUtil;

	@Transactional
	public MarketUpdateResult updateMarketFromCsv(MultipartFile file) {
		validateCsvFile(file);

		List<String> errorMessages = new ArrayList<>();
		int totalCount = 0;
		int successCount = 0;
		int failCount = 0;

		// 여러 인코딩을 시도하여 한글 깨짐 방지
		String[] encodings = {"UTF-8", "EUC-KR", "MS949", "CP949"};

		for (String encoding : encodings) {
			try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(file.getInputStream(), encoding))) {

				log.info("CSV 파일 읽기 시도 - 인코딩: {}", encoding);

				CSVReader csvReader = new CSVReaderBuilder(reader)
					.withCSVParser(new CSVParserBuilder()
						.withSeparator(',')
						.withIgnoreQuotations(false)
						.build())
					.withSkipLines(1) // 첫 번째 행(헤더) 건너뛰기
					.build();

				List<String[]> records = csvReader.readAll();

				// 첫 번째 레코드로 인코딩 검증 (한글이 포함된 경우)
				if (!records.isEmpty() && isValidEncoding(records.get(0))) {
					log.info("올바른 인코딩 감지: {}", encoding);

					for (int i = 0; i < records.size(); i++) {
						String[] row = records.get(i);
						int rowNumber = i + 2; // CSV의 실제 행 번호 (헤더 제외)
						totalCount++;

						try {
							Market market = parseRowToMarket(row, rowNumber);
							if (market != null) {
								marketRepository.save(market);
								successCount++;
								log.debug("시장 정보 저장 성공: {} ({}행)", market.getName(), rowNumber);
							} else {
								failCount++;
								errorMessages.add(String.format("%d행: 빈 행이거나 필수 데이터가 누락됨", rowNumber));
							}
						} catch (Exception e) {
							failCount++;
							String errorMsg = String.format("%d행: %s", rowNumber, e.getMessage());
							errorMessages.add(errorMsg);
							log.warn("시장 정보 처리 실패 - {}", errorMsg);
						}
					}

					return MarketUpdateResult.of(totalCount, successCount, failCount, errorMessages);
				}

			} catch (IOException | CsvException e) {
				log.warn("인코딩 {}로 파일 읽기 실패: {}", encoding, e.getMessage());
				// 다음 인코딩 시도
			}
		}

		// 모든 인코딩 시도 실패
		log.error("모든 인코딩으로 CSV 파일 읽기 실패");
		throw new CustomException(ErrorCode.FILE_PROCESSING_FAILED,
			"CSV 파일을 읽을 수 없습니다. UTF-8, EUC-KR, MS949 인코딩을 확인해주세요.");
	}

	public List<Market> getMarket() {
		return marketRepository.findAllByDistrict("동대문구");
	}

	/**
	 * 특정 시장의 모든 아이템에 대한 과거/현재 가격 정보를 조회
	 */
	@Transactional(readOnly = true)
	public List<MarketItemPriceResponse> getMarketItemPrices(String marketName) {
		log.info("시장 '{}' 아이템 가격 정보 조회 시작", marketName);

		// 1. PriceData에서 해당 시장의 모든 데이터 조회
		List<PriceData> priceDataList = priceDataRepository.findByMarketNameOrderByDateDesc(marketName);

		// 2. ItemPrice에서 해당 시장의 모든 데이터 조회 (Market 엔터티를 통해)
		List<ItemPrice> itemPriceList = itemPriceRepository.findByMarketNameOrderBySurveyDateDesc(marketName);

		// 3. 아이템별로 그룹화 (null 체크 추가)
		Map<String, List<PriceData>> priceDataByItem = priceDataList.stream()
			.filter(pd -> pd.getItemName() != null && !pd.getItemName().trim().isEmpty())
			.filter(pd -> !pd.getPrice().equals("0"))
			.collect(Collectors.groupingBy(PriceData::getItemName));

		Map<String, List<ItemPrice>> itemPriceByItem = itemPriceList.stream()
			.filter(ip -> ip.getItem() != null && ip.getItem().getName() != null && !ip.getItem()
				.getName()
				.trim()
				.isEmpty())
			.filter(ip -> !Objects.equals(ip.getPrice(), BigDecimal.ZERO))
			.collect(Collectors.groupingBy(item -> item.getItem().getName()));

		// 4. 모든 고유한 아이템명 수집 (null 체크 추가)
		List<String> allItemNames = new ArrayList<>();
		allItemNames.addAll(priceDataByItem.keySet());
		itemPriceByItem.keySet().stream()
			.filter(itemName -> itemName != null && !itemName.trim().isEmpty())
			.filter(itemName -> !allItemNames.contains(itemName))
			.forEach(allItemNames::add);

		// 5. 아이템별로 응답 데이터 생성
		List<MarketItemPriceResponse> responseList = allItemNames.stream()
			.map(itemName -> {
				List<PriceData> itemPriceDataList = priceDataByItem.getOrDefault(itemName, new ArrayList<>());
				List<ItemPrice> itemCurrentPriceList = itemPriceByItem.getOrDefault(itemName, new ArrayList<>());

				// 카테고리 정보 추출 (ItemPrice가 있으면 그것에서, 없으면 null)
				String category = itemCurrentPriceList.stream()
					.findFirst()
					.map(ip -> ip.getItem().getCategory())
					.orElse(null);

				return MarketItemPriceResponse.builder()
					.marketName(marketName)
					.itemName(itemName)
					.category(category)
					.priceDataList(itemPriceDataList.stream()
						.map(MarketItemPriceResponse.PriceDataInfo::from)
						.collect(Collectors.toList()))
					.itemPriceList(itemCurrentPriceList.stream()
						.map(MarketItemPriceResponse.ItemPriceInfo::from)
						.collect(Collectors.toList()))
					.build();
			})
			.collect(Collectors.toList());

		log.info("시장 '{}' 아이템 가격 정보 조회 완료 - 아이템 수: {}, PriceData: {}건, ItemPrice: {}건",
			marketName, responseList.size(), priceDataList.size(), itemPriceList.size());

		return responseList;
	}

	/**
	 * 특정 시장의 모든 아이템에 대한 과거/현재 가격 정보를 조회
	 */
	@Transactional(readOnly = true)
	public List<PricePredictionUtil.PriceAnalysis> getPredictMarketItemPrices(String marketName) {
		log.info("시장 '{}' 아이템 가격 정보 조회 시작", marketName);

		// 1. PriceData에서 해당 시장의 모든 데이터 조회
		List<PriceData> priceDataList = priceDataRepository.findByMarketNameOrderByDateDesc(marketName);

		// 2. ItemPrice에서 해당 시장의 모든 데이터 조회 (Market 엔터티를 통해)
		List<ItemPrice> itemPriceList = itemPriceRepository.findByMarketNameOrderBySurveyDateDesc(marketName);

		// 3. 아이템별로 그룹화 (null 체크 추가)
		Map<String, List<PriceData>> priceDataByItem = priceDataList.stream()
			.filter(pd -> pd.getItemName() != null && !pd.getItemName().trim().isEmpty())
			.filter(pd -> !pd.getPrice().equals("0"))
			.collect(Collectors.groupingBy(PriceData::getItemName));

		Map<String, List<ItemPrice>> itemPriceByItem = itemPriceList.stream()
			.filter(ip -> ip.getItem() != null && ip.getItem().getName() != null && !ip.getItem()
				.getName()
				.trim()
				.isEmpty())
			.filter(ip -> !Objects.equals(ip.getPrice(), BigDecimal.ZERO))
			.collect(Collectors.groupingBy(item -> item.getItem().getName()));

		// 4. 모든 고유한 아이템명 수집 (null 체크 추가)
		List<String> allItemNames = new ArrayList<>();
		allItemNames.addAll(priceDataByItem.keySet());
		itemPriceByItem.keySet().stream()
			.filter(itemName -> itemName != null && !itemName.trim().isEmpty())
			.filter(itemName -> !allItemNames.contains(itemName))
			.forEach(allItemNames::add);

		// 5. 아이템별로 응답 데이터 생성
		List<MarketItemPriceResponse> responseList = allItemNames.stream()
			.map(itemName -> {
				List<PriceData> itemPriceDataList = priceDataByItem.getOrDefault(itemName, new ArrayList<>());
				List<ItemPrice> itemCurrentPriceList = itemPriceByItem.getOrDefault(itemName, new ArrayList<>());

				// 카테고리 정보 추출 (ItemPrice가 있으면 그것에서, 없으면 null)
				String category = itemCurrentPriceList.stream()
					.findFirst()
					.map(ip -> ip.getItem().getCategory())
					.orElse(null);

				return MarketItemPriceResponse.builder()
					.marketName(marketName)
					.itemName(itemName)
					.category(category)
					.priceDataList(itemPriceDataList.stream()
						.map(MarketItemPriceResponse.PriceDataInfo::from)
						.collect(Collectors.toList()))
					.itemPriceList(itemCurrentPriceList.stream()
						.map(MarketItemPriceResponse.ItemPriceInfo::from)
						.collect(Collectors.toList()))
					.build();
			})
			.collect(Collectors.toList());

		log.info("시장 '{}' 아이템 가격 정보 조회 완료 - 아이템 수: {}, PriceData: {}건, ItemPrice: {}건",
			marketName, responseList.size(), priceDataList.size(), itemPriceList.size());
		return responseList.stream()
			.map(response -> pricePredictionUtil.analyzePriceHistory(response.getItemName(),
				response.getPriceDataList()))
			.toList();
	}

	/**
	 * 인코딩이 올바른지 검증 (한글 깨짐 체크)
	 */
	private boolean isValidEncoding(String[] firstRow) {
		for (String field : firstRow) {
			if (field != null && !field.trim().isEmpty()) {
				// 깨진 문자(물음표나 특수문자)가 포함되어 있으면 잘못된 인코딩
				if (field.contains("?") || field.contains("�")) {
					return false;
				}
				// 한글이 포함된 경우 정상적인 한글인지 확인
				if (containsKorean(field) && !isValidKorean(field)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 한글이 포함되어 있는지 확인
	 */
	private boolean containsKorean(String text) {
		return text.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣].*");
	}

	/**
	 * 올바른 한글인지 확인 (깨진 한글 체크)
	 */
	private boolean isValidKorean(String text) {
		// 한글 유니코드 범위 체크
		for (char c : text.toCharArray()) {
			if (c >= 0xAC00 && c <= 0xD7AF) { // 완성형 한글
				continue;
			}
			if (c >= 0x1100 && c <= 0x11FF) { // 초성
				continue;
			}
			if (c >= 0x3130 && c <= 0x318F) { // 호환용 한글
				continue;
			}
			if (c >= 0xA960 && c <= 0xA97F) { // 확장 한글
				continue;
			}
			if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c) &&
				!".,()/-_".contains(String.valueOf(c))) {
				return false; // 이상한 문자 발견
			}
		}
		return true;
	}

	private void validateCsvFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "파일이 비어있습니다.");
		}

		String originalFilename = file.getOriginalFilename();
		if (originalFilename == null) {
			throw new CustomException(ErrorCode.INVALID_FILE_FORMAT, "파일명이 올바르지 않습니다.");
		}

		String extension = getFileExtension(originalFilename);
		if (!extension.equals(".csv")) {
			throw new CustomException(ErrorCode.INVALID_FILE_FORMAT,
				"CSV 파일만 업로드 가능합니다. (.csv)");
		}

		// 파일 크기 제한 (5MB - CSV는 텍스트 파일이므로 더 작은 제한)
		if (file.getSize() > 5 * 1024 * 1024) {
			throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED,
				"파일 크기가 너무 큽니다. 최대 5MB까지 업로드 가능합니다.");
		}
	}

	private Market parseRowToMarket(String[] row, int rowNumber) {
		try {
			// 모든 필드가 비어있는지 확인
			boolean hasData = false;
			for (String field : row) {
				if (field != null && !field.trim().isEmpty()) {
					hasData = true;
					break;
				}
			}

			if (!hasData) {
				return null; // 빈 행
			}

			// CSV는 최소 6개 컬럼이 있어야 함
			if (row.length < 6) {
				throw new RuntimeException("컬럼 수가 부족합니다. 6개 컬럼이 필요합니다.");
			}

			// CSV 컬럼 순서: code, name, address, roadNameAddress, city, district
			String code = getFieldValue(row, 0);
			String name = getFieldValue(row, 1);
			String address = getFieldValue(row, 3);
			String roadNameAddress = getFieldValue(row, 4);
			String city = getFieldValue(row, 5);
			String district = getFieldValue(row, 6);

			// 필수 필드 검증 (code, name)
			if (code == null || code.trim().isEmpty()) {
				throw new RuntimeException("시장 코드가 비어있습니다.");
			}
			if (name == null || name.trim().isEmpty()) {
				throw new RuntimeException("시장명이 비어있습니다.");
			}

			return Market.builder()
				.code(code.trim())
				.name(name.trim())
				.address(address != null ? address.trim() : "")
				.roadNameAddress(roadNameAddress != null ? roadNameAddress.trim() : "")
				.city(city != null ? city.trim() : "")
				.district(district != null ? district.trim() : "")
				.build();

		} catch (Exception e) {
			throw new RuntimeException("데이터 파싱 오류: " + e.getMessage());
		}
	}

	private String getFieldValue(String[] row, int index) {
		if (index < row.length) {
			String value = row[index];
			return (value != null && !value.trim().isEmpty()) ? value : null;
		}
		return null;
	}

	private String getFileExtension(String fileName) {
		if (fileName == null || !fileName.contains(".")) {
			return "";
		}
		return fileName.substring(fileName.lastIndexOf("."));
	}

}
