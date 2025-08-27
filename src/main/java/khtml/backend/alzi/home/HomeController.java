package khtml.backend.alzi.home;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import khtml.backend.alzi.utils.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class HomeController {
	@GetMapping("/health")
	@Operation(summary = "헬스체크", description = "서버 상태를 확인합니다.")
	public ApiResponse<String> health() {
		return ApiResponse.success("OK", "서버가 정상적으로 작동 중입니다.");
	}
}
