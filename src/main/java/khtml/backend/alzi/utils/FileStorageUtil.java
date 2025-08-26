package khtml.backend.alzi.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import khtml.backend.alzi.exception.CustomException;
import khtml.backend.alzi.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FileStorageUtil {

    private final String baseUploadDir = "uploads/";
    private final String freeBoardDir = "uploads/freeboard/";
    private final String discussBoardDir = "uploads/discussboard/";
    private final String diaryDir = "uploads/diary/";

    // 허용되는 파일 확장자
    private final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"};
    
    // 최대 파일 크기 (5MB)
    private final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // @PostConstruct 제거 - 서버 배포 시 파일 손실 방지를 위해
    // 디렉토리는 파일 업로드 시점에 필요할 때만 생성

    /**
     * 파일을 지정된 디렉토리에 저장
     * @param file 저장할 파일
     * @param uploadDir 저장할 디렉토리 (예: "uploads/freeboard/")
     * @return 저장된 파일명
     */
    public String saveFile(MultipartFile file, String uploadDir) {
        // 파일 유효성 검사
        validateFile(file);
        
        try {
            // 디렉토리가 존재하지 않으면 생성 (필요시에만)
            Path dirPath = Paths.get(uploadDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                log.info("업로드 디렉토리 생성: {}", uploadDir);
            }
            
            // 파일명 생성 (UUID + 원본 파일명)
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || originalFileName.isEmpty()) {
                throw new CustomException(ErrorCode.INVALID_FILE_FORMAT, "파일명이 올바르지 않습니다.");
            }
            
            String extension = getFileExtension(originalFileName);
            String savedFileName = UUID.randomUUID().toString() + extension;

            // 파일 저장
            Path targetLocation = Paths.get(uploadDir + savedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("파일 저장 완료: originalName={}, savedName={}, size={}", 
                originalFileName, savedFileName, file.getSize());
            
            return savedFileName; // 저장된 파일명 반환
            
        } catch (CustomException e) {
            throw e;
        } catch (IOException e) {
            log.error("파일 저장 실패: fileName={}, error={}", file.getOriginalFilename(), e.getMessage(), e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (Exception e) {
            log.error("파일 저장 중 예상치 못한 오류: fileName={}, error={}", file.getOriginalFilename(), e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "파일 저장 중 오류가 발생했습니다.");
        }
    }

    /**
     * 파일 삭제
     * @param savedFileName 삭제할 파일명
     * @param uploadDir 파일이 저장된 디렉토리
     */
    public void deleteFile(String savedFileName, String uploadDir) {
        if (savedFileName == null || savedFileName.trim().isEmpty()) {
            log.warn("삭제할 파일명이 비어있음");
            return;
        }
        
        try {
            Path filePath = Paths.get(uploadDir + savedFileName);
            boolean deleted = Files.deleteIfExists(filePath);
            
            if (deleted) {
                log.info("파일 삭제 완료: {}", savedFileName);
            } else {
                log.warn("삭제할 파일이 존재하지 않음: {}", savedFileName);
            }
            
        } catch (IOException e) {
            log.error("파일 삭제 실패: fileName={}, error={}", savedFileName, e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "파일 삭제 중 오류가 발생했습니다.");
        }
    }

    /**
     * 파일 유효성 검사
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "파일이 비어있습니다.");
        }
        
        // 파일 크기 검사
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED, 
                String.format("파일 크기가 너무 큽니다. 최대 크기: %dMB", MAX_FILE_SIZE / (1024 * 1024)));
        }
        
        // 파일 확장자 검사
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_FILE_FORMAT, "파일명이 올바르지 않습니다.");
        }
        
        String extension = getFileExtension(originalFileName).toLowerCase();
        boolean isAllowed = false;
        for (String allowedExt : ALLOWED_EXTENSIONS) {
            if (allowedExt.equals(extension)) {
                isAllowed = true;
                break;
            }
        }
        
        if (!isAllowed) {
            throw new CustomException(ErrorCode.INVALID_FILE_FORMAT, 
                "지원하지 않는 파일 형식입니다. 지원 형식: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
        
        // Content Type 검사
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CustomException(ErrorCode.INVALID_FILE_FORMAT, "이미지 파일만 업로드 가능합니다.");
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new CustomException(ErrorCode.INVALID_FILE_FORMAT, "파일 확장자가 없습니다.");
        }
        
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * 파일 존재 여부 확인
     */
    public boolean fileExists(String fileName, String uploadDir) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }
        
        try {
            Path filePath = Paths.get(uploadDir + fileName);
            return Files.exists(filePath);
        } catch (Exception e) {
            log.error("파일 존재 확인 실패: fileName={}, error={}", fileName, e.getMessage());
            return false;
        }
    }
}
