package khtml.backend.alzi.market;

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
import khtml.backend.alzi.market.dto.PriceUpdateRequest;
import khtml.backend.alzi.market.dto.response.MarketUpdateResult;
import khtml.backend.alzi.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@RequestMapping("/api/market")
@Tag(name = "Market API", description = "시장 정보 관리 API")
@Slf4j
public class MarketController {
	private final MarketService marketService;
	private final SeoulOpenApiService seoulOpenApiService;

	@PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "시장 정보 업데이트", description = "CSV 파일을 업로드하여 시장 정보를 업데이트합니다.")
	public ApiResponse<?> updateMarketInfo(
		@Parameter(description = "업로드할 CSV 파일 (.csv)")
		@RequestParam("file") MultipartFile file
	) {
		try {
			log.info("시장 정보 업데이트 요청 - 파일명: {}", file.getOriginalFilename());

			MarketUpdateResult result = marketService.updateMarketFromCsv(file);

			log.info("시장 정보 업데이트 완료 - 총 {}개 처리 (성공: {}, 실패: {})",
				result.getTotalCount(), result.getSuccessCount(), result.getFailCount());

			return ApiResponse.success("시장 정보 업데이트가 완료되었습니다.", result);

		} catch (Exception e) {
			log.error("시장 정보 업데이트 실패: {}", e.getMessage(), e);
			return ApiResponse.failure("MARKET_UPDATE_FAILED",
				"시장 정보 업데이트 중 오류가 발생했습니다: " + e.getMessage());
		}
	}

	@GetMapping("/")
	public ApiResponse<?> getMarket() {
		return ApiResponse.success(marketService.getMarket());
	}
	
	@PostMapping("/update-prices")
	@Operation(summary = "특정 시장 가격 정보 업데이트", description = "서울 열린데이터광장 API를 통해 특정 시장의 품목 가격을 업데이트합니다.")
	public ApiResponse<?> updatePricesFromApi(
			@Parameter(description = "시장명") @RequestParam String marketName,
			@Parameter(description = "품목명") @RequestParam String itemName,
			@Parameter(description = "조사년월 (YYYY-MM)") @RequestParam String yearMonth) {
		
		try {
			log.info("가격 정보 업데이트 요청 - 시장: {}, 품목: {}, 년월: {}", marketName, itemName, yearMonth);
			
			PriceUpdateRequest request = PriceUpdateRequest.builder()
					.marketName(marketName)
					.itemName(itemName)
					.yearMonth(yearMonth)
					.build();
			
			seoulOpenApiService.updatePricesFromSeoulApi(request);
			
			return ApiResponse.success("가격 정보가 성공적으로 업데이트되었습니다.");
			
		} catch (Exception e) {
			log.error("가격 정보 업데이트 실패: {}", e.getMessage(), e);
			return ApiResponse.failure("PRICE_UPDATE_FAILED", 
					"가격 정보 업데이트 중 오류가 발생했습니다: " + e.getMessage());
		}
	}
	
	@PostMapping("/update-all-prices")
	@Operation(summary = "전체 시장 가격 정보 업데이트", description = "모든 등록된 시장에 대해 특정 품목의 가격 정보를 업데이트합니다.")
	public ApiResponse<?> updateAllMarketPrices(
			@Parameter(description = "품목명") @RequestParam String itemName,
			@Parameter(description = "조사년월 (YYYY-MM)") @RequestParam String yearMonth) {
		
		try {
			log.info("전체 시장 가격 정보 업데이트 요청 - 품목: {}, 년월: {}", itemName, yearMonth);
			
			seoulOpenApiService.updateAllMarketPrices(itemName, yearMonth);
			
			return ApiResponse.success("모든 시장의 가격 정보가 성공적으로 업데이트되었습니다.");
			
		} catch (Exception e) {
			log.error("전체 시장 가격 정보 업데이트 실패: {}", e.getMessage(), e);
			return ApiResponse.failure("ALL_PRICE_UPDATE_FAILED", 
					"전체 시장 가격 정보 업데이트 중 오류가 발생했습니다: " + e.getMessage());
		}
	}
}
