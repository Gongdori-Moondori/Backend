package khtml.backend.alzi.priceData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import khtml.backend.alzi.exception.CustomException;
import khtml.backend.alzi.exception.ErrorCode;
import khtml.backend.alzi.market.dto.response.MarketUpdateResult;
import khtml.backend.alzi.priceData.dto.ItemListResponse;
import khtml.backend.alzi.priceData.dto.PriceDataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceDataService {
	private final PriceDataRepository priceDataRepository;

	@Transactional
	public MarketUpdateResult updatePriceDataFromCsv(MultipartFile file) {
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
							PriceData market = parseRowToMarket(row, rowNumber);
							if (market != null) {
								priceDataRepository.save(market);
								successCount++;
								log.debug("시장 정보 저장 성공: {} ({}행)", market.getMarketName(), rowNumber);
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
		if (file.getSize() > 20 * 1024 * 1024) {
			throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED,
				"파일 크기가 너무 큽니다. 최대 5MB까지 업로드 가능합니다.");
		}
	}

	private PriceData parseRowToMarket(String[] row, int rowNumber) {
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
			String serialNumber = getFieldValue(row, 0);
			String marketNumber = getFieldValue(row, 1);
			String marketName = getFieldValue(row, 2);
			String itemNuber = getFieldValue(row, 3);
			String itemName = getFieldValue(row, 4);
			String actualSalesSpecifications = getFieldValue(row, 5);
			String price = getFieldValue(row, 6);
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
			YearMonth yearMonth = YearMonth.parse(getFieldValue(row, 7), formatter);
			LocalDate date = yearMonth.atDay(1); // 해당 월의 1일
			String note = getFieldValue(row, 8);
			String marketTypeNumber = getFieldValue(row, 9);
			String marketType = getFieldValue(row, 10);
			String boroughCode = getFieldValue(row, 11);
			String boroughName = getFieldValue(row, 12);

			// 필수 필드 검증 (code, name)
			if (serialNumber == null || serialNumber.trim().isEmpty()) {
				throw new RuntimeException("시장 코드가 비어있습니다.");
			}
			if (marketName == null || marketName.trim().isEmpty()) {
				throw new RuntimeException("시장명이 비어있습니다.");
			}

			return PriceData.builder()
				.serialNumber(serialNumber)
				.marketNumber(marketNumber)
				.marketName(marketName)
				.itemNuber(itemNuber)
				.itemName(itemName)
				.actualSalesSpecifications(actualSalesSpecifications)
				.price(price)
				.date(date)
				.note(note)
				.marketTypeNumber(marketTypeNumber)
				.marketType(marketType)
				.boroughCode(boroughCode)
				.boroughName(boroughName)
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

	public List<PriceData> getItemList() {
		return priceDataRepository.findDistinctByMarketName("경동시장");
	}
	
	/**
	 * 모든 고유한 아이템명과 마켓명 조회
	 */
	public ItemListResponse getItemAndMarketList() {
		List<String> itemNames = priceDataRepository.findDistinctItemNames();
		List<String> marketNames = priceDataRepository.findDistinctMarketNames();
		return ItemListResponse.of(itemNames, marketNames);
	}
	
	/**
	 * marketName과 itemName으로 가격 데이터 조회
	 */
	public List<PriceDataResponse> getPriceData(String marketName, String itemName) {
		List<PriceData> priceDataList;
		
		if (marketName != null && itemName != null) {
			// 둘 다 있는 경우
			priceDataList = priceDataRepository.findByMarketNameAndItemNameOrderByDateDesc(marketName, itemName);
		} else if (marketName != null) {
			// marketName만 있는 경우
			priceDataList = priceDataRepository.findByMarketNameOrderByDateDesc(marketName);
		} else if (itemName != null) {
			// itemName만 있는 경우
			priceDataList = priceDataRepository.findByItemNameOrderByDateDesc(itemName);
		} else {
			// 둘 다 없는 경우 - 빈 리스트 반환
			return new ArrayList<>();
		}
		
		return PriceDataResponse.fromList(priceDataList);
	}
}
