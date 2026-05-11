package com.szh.contractReviewSystem.service.review;

import com.szh.contractReviewSystem.mapper.review.ReviewTaskMapper;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReviewTaskRecoveryOnStartup {

    private final JdbcTemplate jdbcTemplate;
    private final ReviewTaskMapper reviewTaskMapper;

    public ReviewTaskRecoveryOnStartup(JdbcTemplate jdbcTemplate, ReviewTaskMapper reviewTaskMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.reviewTaskMapper = reviewTaskMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverRunningTasks() {
        ensureColumn("applied_operation_count",
                "ALTER TABLE review_task ADD COLUMN applied_operation_count INT DEFAULT NULL COMMENT 'applied patch operation count'");
        ensureColumn("skipped_operation_count",
                "ALTER TABLE review_task ADD COLUMN skipped_operation_count INT DEFAULT NULL COMMENT 'skipped patch operation count'");
        ensureColumn("skipped_operation_messages",
                "ALTER TABLE review_task ADD COLUMN skipped_operation_messages LONGTEXT DEFAULT NULL COMMENT 'skipped patch operation messages json'");
        ensureColumn("retryable",
                "ALTER TABLE review_task ADD COLUMN retryable TINYINT NOT NULL DEFAULT 0 COMMENT 'whether task can be retried manually'");
        ensureColumn("retry_count",
                "ALTER TABLE review_task ADD COLUMN retry_count TINYINT NOT NULL DEFAULT 0 COMMENT 'manual retry count'");
        ensureColumn("max_retry",
                "ALTER TABLE review_task ADD COLUMN max_retry TINYINT NOT NULL DEFAULT 3 COMMENT 'max manual retry count'");
        ensureColumn("last_error_code",
                "ALTER TABLE review_task ADD COLUMN last_error_code VARCHAR(64) DEFAULT NULL COMMENT 'last error code'");
        reviewTaskMapper.markInterruptedRunningTasksAsFailed();
    }

    private void ensureColumn(String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'review_task'
                  AND column_name = ?
                """,
                Integer.class,
                columnName
        );
        if (count != null && count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }
}
