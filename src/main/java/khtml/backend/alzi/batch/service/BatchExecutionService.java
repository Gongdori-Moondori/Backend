package khtml.backend.alzi.batch.service;

import java.time.LocalDateTime;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchExecutionService {
    
    private final JobLauncher jobLauncher;
    private final Job priceDataCollectionJob;
    
    private volatile JobExecution lastJobExecution;
    private volatile boolean isRunning = false;
    
    /**
     * 매달 1일 오전 2시에 가격 정보 수집 배치 실행
     */
    @Scheduled(cron = "0 0 2 1 * ?") // 초 분 시 일 월 요일
    public void runMonthlyPriceDataCollection() {
        try {
            log.info("월간 시장별 가격 정보 수집 배치 시작");
            
            JobParameters jobParameters = new JobParametersBuilder()
                .addLocalDateTime("runTime", LocalDateTime.now())
                .addString("type", "scheduled")
                .toJobParameters();
            
            executeJob(jobParameters);
            
            log.info("월간 시장별 가격 정보 수집 배치 완료");
            
        } catch (Exception e) {
            log.error("월간 시장별 가격 정보 수집 배치 실행 중 오류 발생", e);
        }
    }
    
    /**
     * 수동으로 배치 실행
     */
    public void runPriceDataCollectionManually() {
        try {
            if (isRunning) {
                throw new IllegalStateException("이미 배치가 실행 중입니다. 잠시 후 다시 시도해주세요.");
            }
            
            log.info("수동 시장별 가격 정보 수집 배치 시작");
            
            JobParameters jobParameters = new JobParametersBuilder()
                .addLocalDateTime("runTime", LocalDateTime.now())
                .addString("type", "manual")
                .toJobParameters();
            
            executeJob(jobParameters);
            
            log.info("수동 시장별 가격 정보 수집 배치 완료");
            
        } catch (Exception e) {
            log.error("수동 시장별 가격 정보 수집 배치 실행 중 오류 발생", e);
            throw new RuntimeException("배치 실행 실패: " + e.getMessage(), e);
        }
    }
    
    private void executeJob(JobParameters jobParameters) throws Exception {
        isRunning = true;
        try {
            lastJobExecution = jobLauncher.run(priceDataCollectionJob, jobParameters);
        } finally {
            isRunning = false;
        }
    }
    
    /**
     * 마지막 배치 실행 상태 조회
     */
    public String getLastJobExecutionStatus() {
        if (lastJobExecution == null) {
            return "아직 실행된 배치가 없습니다.";
        }
        
        BatchStatus status = lastJobExecution.getStatus();
        LocalDateTime startTime = lastJobExecution.getStartTime() != null ?
			LocalDateTime.from(lastJobExecution.getStartTime().toLocalTime()) : null;
        LocalDateTime endTime = lastJobExecution.getEndTime() != null ?
			LocalDateTime.from(lastJobExecution.getEndTime().toLocalTime()) : null;
        
        StringBuilder sb = new StringBuilder();
        sb.append("상태: ").append(status.toString()).append("\n");
        sb.append("시작 시간: ").append(startTime != null ? startTime.toString() : "알 수 없음").append("\n");
        sb.append("종료 시간: ").append(endTime != null ? endTime.toString() : "진행 중").append("\n");
        
        if (lastJobExecution.getJobParameters() != null) {
            String type = lastJobExecution.getJobParameters().getString("type");
            sb.append("실행 타입: ").append(type != null ? type : "알 수 없음").append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 현재 배치 실행 여부 확인
     */
    public boolean isCurrentlyRunning() {
        return isRunning;
    }
}
