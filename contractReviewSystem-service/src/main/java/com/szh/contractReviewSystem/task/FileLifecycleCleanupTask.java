package com.szh.contractReviewSystem.task;

import com.szh.contractReviewSystem.config.FileLifecycleProperties;
import com.szh.contractReviewSystem.entity.file.FileStorageEntity;
import com.szh.contractReviewSystem.service.db.FileStorageRecordService;
import com.szh.contractReviewSystem.service.file.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

@Component
public class FileLifecycleCleanupTask {

    private static final Logger logger = LoggerFactory.getLogger(FileLifecycleCleanupTask.class);

    private final FileLifecycleProperties properties;
    private final FileStorageRecordService fileStorageRecordService;
    private final FileStorageService fileStorageService;

    public FileLifecycleCleanupTask(FileLifecycleProperties properties,
                                    FileStorageRecordService fileStorageRecordService,
                                    FileStorageService fileStorageService) {
        this.properties = properties;
        this.fileStorageRecordService = fileStorageRecordService;
        this.fileStorageService = fileStorageService;
    }

    @Scheduled(cron = "${file.lifecycle.cleanup-cron:0 0 * * * *}")
    public void cleanupExpiredTempFiles() {
        if (!properties.isCleanupEnabled()) {
            return;
        }
        cleanupExpiredRegisteredRecords();
        cleanupOrphanDocxAgentWorkDirectories(resolveDocxAgentWorkRoot());
        cleanupLegacyDocxAgentWorkDirectories(fileStorageService.getUploadRoot());
    }

    private void cleanupExpiredRegisteredRecords() {
        List<FileStorageEntity> records = fileStorageRecordService.listExpiredTempRecords(LocalDateTime.now());
        for (FileStorageEntity record : records) {
            try {
                fileStorageService.deleteManagedPath(record.getFilePath());
                fileStorageRecordService.softDeleteRecord(record.getId());
            } catch (Exception e) {
                logger.warn("Failed to clean expired file record, id={}, path={}",
                        record.getId(), record.getFilePath(), e);
            }
        }
    }

    private void cleanupOrphanDocxAgentWorkDirectories(Path root) {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(root, 2)) {
            stream.filter(Files::isDirectory)
                    .filter(path -> path.getFileName() != null
                            && path.getFileName().toString().startsWith("task-"))
                    .filter(this::isExpired)
                    .forEach(this::deleteDirectoryQuietly);
        } catch (IOException e) {
            logger.warn("Failed to scan docx agent work root: {}", root, e);
        }
    }

    private void cleanupLegacyDocxAgentWorkDirectories(Path uploadRoot) {
        if (!Files.isDirectory(uploadRoot)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(uploadRoot, 4)) {
            stream.filter(Files::isDirectory)
                    .filter(path -> path.getFileName() != null
                            && "docx-agent-work".equals(path.getFileName().toString()))
                    .forEach(this::cleanupLegacyWorkRoot);
        } catch (IOException e) {
            logger.warn("Failed to scan legacy docx agent work directories under: {}", uploadRoot, e);
        }
    }

    private void cleanupLegacyWorkRoot(Path workRoot) {
        try (Stream<Path> stream = Files.list(workRoot)) {
            stream.filter(Files::isDirectory)
                    .filter(this::isExpired)
                    .forEach(this::deleteDirectoryQuietly);
        } catch (IOException e) {
            logger.warn("Failed to scan legacy docx agent work root: {}", workRoot, e);
        }
    }

    private boolean isExpired(Path path) {
        try {
            Instant lastModified = Files.getLastModifiedTime(path).toInstant();
            Instant expireBefore = Instant.now().minusSeconds(properties.getTempTtlHours() * 3600);
            return lastModified.isBefore(expireBefore);
        } catch (IOException e) {
            logger.warn("Failed to read last modified time: {}", path, e);
            return false;
        }
    }

    private void deleteDirectoryQuietly(Path path) {
        try {
            fileStorageService.deleteManagedPath(path.toString());
        } catch (Exception e) {
            logger.warn("Failed to delete temp directory: {}", path, e);
        }
    }

    private Path resolveDocxAgentWorkRoot() {
        String configured = properties.getDocxAgentWorkRoot();
        Path path = Path.of(configured == null || configured.trim().isEmpty()
                ? "work/docx-agent"
                : configured.trim());
        if (path.isAbsolute()) {
            return path.toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.dir"), path.toString()).toAbsolutePath().normalize();
    }
}
