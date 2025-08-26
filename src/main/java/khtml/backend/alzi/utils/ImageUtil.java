package khtml.backend.alzi.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ImageUtil {

	public String encodeImageToBase64(String imagePath) {
		try {
			// 경로 유효성 검사
			if (imagePath == null || imagePath.trim().isEmpty()) {
				log.warn("이미지 경로가 비어있습니다.");
				return null;
			}
			
			Path path = Paths.get(imagePath);
			
			// 파일 존재 여부 확인
			if (!Files.exists(path)) {
				log.warn("이미지 파일이 존재하지 않습니다: {}", imagePath);
				return null;
			}
			
			// 파일 읽기 가능 여부 확인
			if (!Files.isReadable(path)) {
				log.warn("이미지 파일을 읽을 수 없습니다: {}", imagePath);
				return null;
			}
			
			// 파일 크기 확인 (10MB 제한)
			long fileSize = Files.size(path);
			if (fileSize > 10 * 1024 * 1024) {
				log.warn("이미지 파일이 너무 큽니다 ({}MB): {}", fileSize / (1024 * 1024), imagePath);
				return null;
			}
			
			byte[] imageBytes = Files.readAllBytes(path);
			String base64 = Base64.getEncoder().encodeToString(imageBytes);
			
			log.debug("이미지 인코딩 성공: {} ({}bytes -> {}chars)", imagePath, imageBytes.length, base64.length());
			return base64;
			
		} catch (IOException e) {
			log.error("이미지 인코딩 중 IO 오류 발생: {}", imagePath, e);
			return null;
		} catch (Exception e) {
			log.error("이미지 인코딩 중 예상치 못한 오류 발생: {}", imagePath, e);
			return null;
		}
	}

	public String getImageMimeType(String fileName) {
		if (fileName == null || fileName.trim().isEmpty()) {
			return "image/jpeg";
		}
		
		int lastDotIndex = fileName.lastIndexOf(".");
		if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
			return "image/jpeg";
		}
		
		String extension = fileName.substring(lastDotIndex + 1).toLowerCase();
		switch (extension) {
			case "jpg":
			case "jpeg":
				return "image/jpeg";
			case "png":
				return "image/png";
			case "gif":
				return "image/gif";
			case "webp":
				return "image/webp";
			case "bmp":
				return "image/bmp";
			case "svg":
				return "image/svg+xml";
			default:
				log.debug("알 수 없는 이미지 확장자: {}, 기본값 사용", extension);
				return "image/jpeg";
		}
	}
	
	/**
	 * 이미지 파일 존재 여부 확인
	 */
	public boolean imageExists(String imagePath) {
		if (imagePath == null || imagePath.trim().isEmpty()) {
			return false;
		}
		
		try {
			Path path = Paths.get(imagePath);
			return Files.exists(path) && Files.isReadable(path);
		} catch (Exception e) {
			log.warn("이미지 파일 존재 여부 확인 실패: {}", imagePath, e);
			return false;
		}
	}
	
	/**
	 * 기본 이미지나 플레이스홀더 반환
	 */
	public String getDefaultImageBase64() {
		// 1x1 투명 PNG 이미지 (89bytes)
		return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
	}
	
	/**
	 * 이미지 URL 생성
	 */
	public String generateImageUrl(String savedFileName, String uploadType) {
		if (savedFileName == null || savedFileName.trim().isEmpty()) {
			return null;
		}
		
		// uploadType: "freeboard", "discussboard", "diary"
		return String.format("/uploads/%s/%s", uploadType, savedFileName);
	}
	
	/**
	 * 기본 이미지 URL 반환
	 */
	public String getDefaultImageUrl() {
		return "/uploads/default/placeholder.png";
	}
}
