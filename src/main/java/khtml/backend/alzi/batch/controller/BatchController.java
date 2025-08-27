package khtml.backend.alzi.batch.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import khtml.backend.alzi.batch.service.BatchExecutionService;
import khtml.backend.alzi.utils.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@RequestMapping("/api/batch")
@Tag(name = "Batch API", description = "배치 작업 관리 API")
@Slf4j
public class BatchController {

	private final BatchExecutionService batchExecutionService;

	@PostMapping("/price-data/run")
	@Operation(summary = "시장별 가격 정보 수집 배치 수동 실행",
		description = "Item 테이블의 모든 아이템에 대해 서울시 API를 호출하여 시장별 가격 정보를 수집하고 ItemPrice 테이블에 저장합니다.")
	public ResponseEntity<ApiResponse<String>> runPriceDataCollection() {
		try {
			log.info("시장별 가격 정보 수집 배치 수동 실행 요청");

			batchExecutionService.runPriceDataCollectionManually();

			return ResponseEntity.ok(ApiResponse.success("배치 실행이 완료되었습니다.", "success"));

		} catch (Exception e) {
			log.error("시장별 가격 정보 수집 배치 실행 중 오류", e);
			return ResponseEntity.badRequest()
				.body(ApiResponse.failure("배치 실행 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	@PostMapping("/price-data/schedule-info")
	@Operation(summary = "배치 스케줄 정보 조회", description = "배치 실행 스케줄 정보를 반환합니다.")
	public ResponseEntity<ApiResponse<String>> getBatchScheduleInfo() {
		String scheduleInfo = "매월 1일 오전 2시 (cron: 0 0 2 1 * ?)에 자동 실행됩니다.\n" +
			"수동 실행을 원하시면 /api/batch/price-data/run API를 호출하세요.";

		return ResponseEntity.ok(ApiResponse.success(scheduleInfo, "스케줄 정보 조회 완료"));
	}

	@PostMapping("/price-data/status")
	@Operation(summary = "배치 실행 상태 조회", description = "마지막 배치 실행 상태와 현재 실행 여부를 조회합니다.")
	public ResponseEntity<ApiResponse<BatchStatusResponse>> getBatchStatus() {
		try {
			String lastExecutionStatus = batchExecutionService.getLastJobExecutionStatus();
			boolean isRunning = batchExecutionService.isCurrentlyRunning();

			BatchStatusResponse response = BatchStatusResponse.builder()
				.isCurrentlyRunning(isRunning)
				.lastExecutionStatus(lastExecutionStatus)
				.build();

			return ResponseEntity.ok(ApiResponse.success("배치 상태 조회 완료", response));

		} catch (Exception e) {
			log.error("배치 상태 조회 중 오류", e);
			return ResponseEntity.badRequest()
				.body(ApiResponse.failure("배치 상태 조회 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	// 응답 DTO 클래스
	@lombok.Data
	@lombok.Builder
	public static class BatchStatusResponse {
		private boolean isCurrentlyRunning;
		private String lastExecutionStatus;
	}
}
