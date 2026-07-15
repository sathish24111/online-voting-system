package com.college.voting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync
public class VotingApplication {

    private static final Logger logger = LoggerFactory.getLogger(VotingApplication.class);

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    @jakarta.annotation.PostConstruct
    public void init() {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
        logger.info("Spring Boot default timezone set to Asia/Kolkata (IST)");
    }

    public static void main(String[] args) {
        SpringApplication.run(VotingApplication.class, args);
    }

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("OtpEmail-");
        executor.initialize();
        return executor;
    }

    @Bean
    public CommandLineRunner initDirectories() {
        return args -> {
            try {
                // Initialize local directories for candidate and student photo storage
                Path candidatesPath = Paths.get(uploadDir, "candidates");
                if (!Files.exists(candidatesPath)) {
                    Files.createDirectories(candidatesPath);
                    logger.info("Created local uploads directory at {}", candidatesPath.toAbsolutePath());
                } else {
                    logger.info("Local uploads directory exists at {}", candidatesPath.toAbsolutePath());
                }
            } catch (Exception e) {
                logger.error("Failed to initialize server upload folders: {}", e.getMessage());
            }
        };
    }

    @Bean
    public CommandLineRunner repairNullVotes(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                java.util.List<Long> nullVoteIds = jdbcTemplate.queryForList(
                    "SELECT id FROM votes WHERE student_id IS NULL", Long.class);
                
                if (!nullVoteIds.isEmpty()) {
                    logger.info("Found {} legacy votes with NULL student_id. Starting repair...", nullVoteIds.size());
                    
                    for (Long voteId : nullVoteIds) {
                        Long electionId = jdbcTemplate.queryForObject(
                            "SELECT election_id FROM votes WHERE id = ?", Long.class, voteId);
                            
                        java.util.List<Long> participantStudentIds = jdbcTemplate.queryForList(
                            "SELECT student_id FROM election_participation WHERE election_id = ?", Long.class, electionId);
                            
                        if (!participantStudentIds.isEmpty()) {
                            Long targetStudentId = null;
                            for (Long studentId : participantStudentIds) {
                                int voteCount = jdbcTemplate.queryForObject(
                                    "SELECT COUNT(*) FROM votes WHERE election_id = ? AND student_id = ?", Integer.class, electionId, studentId);
                                if (voteCount == 0) {
                                    targetStudentId = studentId;
                                    break;
                                }
                            }
                            
                            if (targetStudentId == null) {
                                targetStudentId = participantStudentIds.get(0);
                            }
                            
                            jdbcTemplate.update("UPDATE votes SET student_id = ? WHERE id = ?", targetStudentId, voteId);
                            logger.info("Repaired legacy vote ID {} by linking it to student ID {}", voteId, targetStudentId);
                        }
                    }
                    logger.info("Legacy votes repair completed!");
                } else {
                    logger.info("No legacy votes with NULL student_id found. Database is consistent.");
                }
            } catch (Exception e) {
                logger.error("Failed to repair legacy votes: {}", e.getMessage());
            }
        };
    }
}
