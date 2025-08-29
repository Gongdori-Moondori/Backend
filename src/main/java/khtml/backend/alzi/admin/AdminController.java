package khtml.backend.alzi.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import khtml.backend.alzi.utils.ApiResponse;
import khtml.backend.alzi.utils.DataInitializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@Tag(name = "Admin API", description = "관리자 기능 API")
@Slf4j
public class AdminController {

    private final DataInitializer dataInitializer;

    // @PostMapping("/initialize-mart-data")
    // @Operation(
    //     summary = "대형마트 데이터 초기화",
    //     description = "이마트, 롯데마트, 홈플러스 데이터를 생성하고, " +
    //                  "기존 전통시장 가격보다 3,000~15,000원 더 비싼 가격으로 ItemPrice 데이터를 생성합니다."
    // )
    // public ApiResponse<?> initializeMartData() {
    //     log.info("대형마트 데이터 초기화 요청");
    //
    //     try {
    //         // DataInitializer의 initializeData 메서드 호출
    //         dataInitializer.initializeData();
    //
    //         return ApiResponse.success("대형마트 데이터 초기화가 완료되었습니다.");
    //
    //     } catch (Exception e) {
    //         log.error("대형마트 데이터 초기화 실패", e);
    //         return ApiResponse.failure("MART_DATA_INITIALIZATION_FAILED",
    //             "대형마트 데이터 초기화 중 오류가 발생했습니다: " + e.getMessage());
    //     }
    // }
}
