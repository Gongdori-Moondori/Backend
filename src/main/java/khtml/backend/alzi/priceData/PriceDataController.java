package khtml.backend.alzi.priceData;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import khtml.backend.alzi.market.dto.response.MarketUpdateResult;
import khtml.backend.alzi.priceData.dto.ItemListResponse;
import khtml.backend.alzi.priceData.dto.PriceDataResponse;
import khtml.backend.alzi.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@RequestMapping("/api/price_data")
@Tag(name = "PriceData API", description = "시장 정보 관리 API")
@Slf4j
public class PriceDataController {
	private final PriceDataService priceDataService;

	// @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	// @Operation(summary = "시장 정보 업데이트", description = "CSV 파일을 업로드하여 시장 정보를 업데이트합니다.")
	// public ApiResponse<?> updateMarketInfo(
	// 	@Parameter(description = "업로드할 CSV 파일 (.csv)")
	// 	@RequestParam("file") MultipartFile file
	// ) {
	// 	try {
	// 		log.info("시장 정보 업데이트 요청 - 파일명: {}", file.getOriginalFilename());
	//
	// 		MarketUpdateResult result = priceDataService.updatePriceDataFromCsv(file);
	//
	// 		log.info("시장 정보 업데이트 완료 - 총 {}개 처리 (성공: {}, 실패: {})",
	// 			result.getTotalCount(), result.getSuccessCount(), result.getFailCount());
	//
	// 		return ApiResponse.success("시장 정보 업데이트가 완료되었습니다.", result);
	//
	// 	} catch (Exception e) {
	// 		log.error("시장 정보 업데이트 실패: {}", e.getMessage(), e);
	// 		return ApiResponse.failure("MARKET_UPDATE_FAILED",
	// 			"시장 정보 업데이트 중 오류가 발생했습니다: " + e.getMessage());
	// 	}
	// }

	@GetMapping("/items")
	@Operation(summary = "아이템 목록 조회", description = "모든 고유한 아이템명과 마켓명을 조회합니다.")
	public ApiResponse<ItemListResponse> getItemLists() {
		try {
			ItemListResponse itemList = priceDataService.getItemAndMarketList();
			return ApiResponse.success("아이템 목록 조회 성공", itemList);
		} catch (Exception e) {
			log.error("아이템 목록 조회 실패: {}", e.getMessage(), e);
			return ApiResponse.failure("ITEM_LIST_FAILED", "아이템 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
		}
	}

	@GetMapping("/data")
	@Operation(summary = "가격 데이터 조회", description = "마켓명과 아이템명으로 가격 데이터를 조회합니다. 둘 중 하나만 입력해도 조회 가능합니다.")
	public ApiResponse<List<PriceDataResponse>> getPriceData(
			@Parameter(description = "마켓명 (선택사항)")
			@RequestParam(required = false) String marketName,
			@Parameter(description = "아이템명 (선택사항)")
			@RequestParam(required = false) String itemName
	) {
		try {
			if (marketName == null && itemName == null) {
				return ApiResponse.failure("INVALID_PARAMETER", "marketName 또는 itemName 중 하나는 필수입니다.");
			}
			
			List<PriceDataResponse> priceData = priceDataService.getPriceData(marketName, itemName);
			
			String message = String.format("가격 데이터 조회 성공 (%d건)", priceData.size());
			if (marketName != null && itemName != null) {
				message += String.format(" - 마켓: %s, 아이템: %s", marketName, itemName);
			} else if (marketName != null) {
				message += String.format(" - 마켓: %s", marketName);
			} else {
				message += String.format(" - 아이템: %s", itemName);
			}
			
			return ApiResponse.success(message, priceData);
		} catch (Exception e) {
			log.error("가격 데이터 조회 실패: {}", e.getMessage(), e);
			return ApiResponse.failure("PRICE_DATA_FAILED", "가격 데이터 조회 중 오류가 발생했습니다: " + e.getMessage());
		}
	}
}
