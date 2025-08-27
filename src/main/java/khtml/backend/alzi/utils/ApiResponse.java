package khtml.backend.alzi.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 통일된 API 응답 형식
 * @param <T> 응답 데이터 타입
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    private boolean success;        // 성공 여부
    private String message;         // 응답 메시지
    private T data;                 // 실제 데이터
    private String errorCode;       // 에러 코드 (실패시)
    
    // 성공 응답 생성 메서드
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "요청이 성공적으로 처리되었습니다.", data, null);
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }
    
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, "요청이 성공적으로 처리되었습니다.", null, null);
    }
    
    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null, null);
    }
    
    // 실패 응답 생성 메서드
    public static <T> ApiResponse<T> failure(String message) {
        return new ApiResponse<>(false, message, null, null);
    }
    
    public static <T> ApiResponse<T> failure(String message, String errorCode) {
        return new ApiResponse<>(false, message, null, errorCode);
    }
    
    public static <T> ApiResponse<T> failure(String message, String errorCode, T data) {
        return new ApiResponse<>(false, message, data, errorCode);
    }
}
