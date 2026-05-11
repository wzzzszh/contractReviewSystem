package com.szh.contractReviewSystem.mapper.review;

import com.szh.contractReviewSystem.entity.review.ReviewTaskEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.StringTypeHandler;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReviewTaskMapper {

    @Insert("""
            INSERT INTO review_task (
                user_id, source_file_id, result_file_id, task_type, status,
                progress, perspective, user_focus, risk_report, generated_requirement,
                applied_operation_count, skipped_operation_count, skipped_operation_messages,
                retryable, retry_count, max_retry, last_error_code, error_message
            )
            VALUES (
                #{userId}, #{sourceFileId}, #{resultFileId}, #{taskType}, #{status},
                #{progress}, #{perspective}, #{userFocus}, #{riskReport}, #{generatedRequirement},
                #{appliedOperationCount}, #{skippedOperationCount}, #{skippedOperationMessagesJson},
                #{retryable}, #{retryCount}, #{maxRetry}, #{lastErrorCode}, #{errorMessage}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ReviewTaskEntity task);

    @Select("""
            SELECT id, user_id, source_file_id, result_file_id, task_type, status,
                   progress, perspective, user_focus, risk_report, generated_requirement,
                   applied_operation_count, skipped_operation_count, skipped_operation_messages,
                   retryable, retry_count, max_retry, last_error_code,
                   error_message, llm_provider, start_time, finish_time, duration_ms,
                   create_time, update_time
            FROM review_task
            WHERE id = #{id}
            """)
    @Results(id = "reviewTaskResultMap", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "user_id", property = "userId"),
            @Result(column = "source_file_id", property = "sourceFileId"),
            @Result(column = "result_file_id", property = "resultFileId"),
            @Result(column = "task_type", property = "taskType"),
            @Result(column = "status", property = "status"),
            @Result(column = "progress", property = "progress"),
            @Result(column = "perspective", property = "perspective"),
            @Result(column = "user_focus", property = "userFocus"),
            @Result(column = "risk_report", property = "riskReport"),
            @Result(column = "generated_requirement", property = "generatedRequirement"),
            @Result(column = "applied_operation_count", property = "appliedOperationCount"),
            @Result(column = "skipped_operation_count", property = "skippedOperationCount"),
            @Result(column = "skipped_operation_messages", property = "skippedOperationMessagesJson"),
            @Result(column = "retryable", property = "retryable"),
            @Result(column = "retry_count", property = "retryCount"),
            @Result(column = "max_retry", property = "maxRetry"),
            @Result(column = "last_error_code", property = "lastErrorCode"),
            @Result(column = "error_message", property = "errorMessage"),
            @Result(column = "llm_provider", property = "llmProvider"),
            @Result(column = "start_time", property = "startTime"),
            @Result(column = "finish_time", property = "finishTime"),
            @Result(column = "duration_ms", property = "durationMs"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "update_time", property = "updateTime")
    })
    ReviewTaskEntity selectById(@Param("id") Long id);

    @Select("""
            SELECT id, user_id, source_file_id, result_file_id, task_type, status,
                   progress, perspective, user_focus, risk_report, generated_requirement,
                   applied_operation_count, skipped_operation_count, skipped_operation_messages,
                   retryable, retry_count, max_retry, last_error_code,
                   error_message, llm_provider, start_time, finish_time, duration_ms,
                   create_time, update_time
            FROM review_task
            WHERE id = #{id}
              AND user_id = #{userId}
            """)
    @Results(id = "reviewTaskOwnedResultMap", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "user_id", property = "userId"),
            @Result(column = "source_file_id", property = "sourceFileId"),
            @Result(column = "result_file_id", property = "resultFileId"),
            @Result(column = "task_type", property = "taskType"),
            @Result(column = "status", property = "status"),
            @Result(column = "progress", property = "progress"),
            @Result(column = "perspective", property = "perspective"),
            @Result(column = "user_focus", property = "userFocus"),
            @Result(column = "risk_report", property = "riskReport"),
            @Result(column = "generated_requirement", property = "generatedRequirement"),
            @Result(column = "applied_operation_count", property = "appliedOperationCount"),
            @Result(column = "skipped_operation_count", property = "skippedOperationCount"),
            @Result(column = "skipped_operation_messages", property = "skippedOperationMessagesJson"),
            @Result(column = "retryable", property = "retryable"),
            @Result(column = "retry_count", property = "retryCount"),
            @Result(column = "max_retry", property = "maxRetry"),
            @Result(column = "last_error_code", property = "lastErrorCode"),
            @Result(column = "error_message", property = "errorMessage"),
            @Result(column = "llm_provider", property = "llmProvider"),
            @Result(column = "start_time", property = "startTime"),
            @Result(column = "finish_time", property = "finishTime"),
            @Result(column = "duration_ms", property = "durationMs"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "update_time", property = "updateTime")
    })
    ReviewTaskEntity selectOwnedById(@Param("id") Long id, @Param("userId") Long userId);

    @Select("""
            SELECT id, user_id, source_file_id, result_file_id, task_type, status,
                   progress, perspective, user_focus, risk_report, generated_requirement,
                   applied_operation_count, skipped_operation_count, skipped_operation_messages,
                   retryable, retry_count, max_retry, last_error_code,
                   error_message, llm_provider, start_time, finish_time, duration_ms,
                   create_time, update_time
            FROM review_task
            WHERE user_id = #{userId}
            ORDER BY id DESC
            """)
    @Results(id = "reviewTaskListResultMap", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "user_id", property = "userId"),
            @Result(column = "source_file_id", property = "sourceFileId"),
            @Result(column = "result_file_id", property = "resultFileId"),
            @Result(column = "task_type", property = "taskType"),
            @Result(column = "status", property = "status"),
            @Result(column = "progress", property = "progress"),
            @Result(column = "perspective", property = "perspective"),
            @Result(column = "user_focus", property = "userFocus"),
            @Result(column = "risk_report", property = "riskReport"),
            @Result(column = "generated_requirement", property = "generatedRequirement"),
            @Result(column = "applied_operation_count", property = "appliedOperationCount"),
            @Result(column = "skipped_operation_count", property = "skippedOperationCount"),
            @Result(column = "skipped_operation_messages", property = "skippedOperationMessagesJson"),
            @Result(column = "retryable", property = "retryable"),
            @Result(column = "retry_count", property = "retryCount"),
            @Result(column = "max_retry", property = "maxRetry"),
            @Result(column = "last_error_code", property = "lastErrorCode"),
            @Result(column = "error_message", property = "errorMessage"),
            @Result(column = "llm_provider", property = "llmProvider"),
            @Result(column = "start_time", property = "startTime"),
            @Result(column = "finish_time", property = "finishTime"),
            @Result(column = "duration_ms", property = "durationMs"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "update_time", property = "updateTime")
    })
    List<ReviewTaskEntity> selectByUserId(@Param("userId") Long userId);

    @Update("""
            UPDATE review_task
            SET status = 'running',
                progress = #{progress},
                result_file_id = NULL,
                retryable = 0,
                llm_provider = #{llmProvider},
                start_time = #{startTime},
                finish_time = NULL,
                duration_ms = NULL,
                last_error_code = NULL,
                error_message = NULL
            WHERE id = #{id}
            """)
    int markRunning(@Param("id") Long id,
                    @Param("progress") Integer progress,
                    @Param("llmProvider") String llmProvider,
                    @Param("startTime") LocalDateTime startTime);

    @Update("""
            UPDATE review_task
            SET progress = #{progress}
            WHERE id = #{id}
            """)
    int updateProgress(@Param("id") Long id, @Param("progress") Integer progress);

    @Update("""
            UPDATE review_task
            SET status = 'success',
                progress = 100,
                result_file_id = #{resultFileId},
                risk_report = #{riskReport},
                generated_requirement = #{generatedRequirement},
                applied_operation_count = #{appliedOperationCount},
                skipped_operation_count = #{skippedOperationCount},
                skipped_operation_messages = #{skippedOperationMessagesJson},
                retryable = 0,
                finish_time = #{finishTime},
                duration_ms = #{durationMs},
                last_error_code = NULL,
                error_message = NULL
            WHERE id = #{id}
            """)
    int markSuccess(@Param("id") Long id,
                    @Param("resultFileId") Long resultFileId,
                    @Param("riskReport") String riskReport,
                    @Param("generatedRequirement") String generatedRequirement,
                    @Param("appliedOperationCount") Integer appliedOperationCount,
                    @Param("skippedOperationCount") Integer skippedOperationCount,
                    @Param("skippedOperationMessagesJson") String skippedOperationMessagesJson,
                    @Param("finishTime") LocalDateTime finishTime,
                    @Param("durationMs") Long durationMs);

    @Update("""
            UPDATE review_task
            SET status = 'failed',
                retryable = #{retryable},
                last_error_code = #{lastErrorCode},
                applied_operation_count = #{appliedOperationCount},
                skipped_operation_count = #{skippedOperationCount},
                skipped_operation_messages = #{skippedOperationMessagesJson},
                finish_time = #{finishTime},
                duration_ms = #{durationMs},
                error_message = #{errorMessage}
            WHERE id = #{id}
            """)
    int markFailed(@Param("id") Long id,
                   @Param("errorMessage") String errorMessage,
                   @Param("retryable") Boolean retryable,
                   @Param("lastErrorCode") String lastErrorCode,
                   @Param("appliedOperationCount") Integer appliedOperationCount,
                   @Param("skippedOperationCount") Integer skippedOperationCount,
                   @Param("skippedOperationMessagesJson") String skippedOperationMessagesJson,
                   @Param("finishTime") LocalDateTime finishTime,
                   @Param("durationMs") Long durationMs);

    @Update("""
            UPDATE review_task
            SET status = 'failed',
                retryable = 1,
                last_error_code = 'INTERRUPTED_ON_RESTART',
                error_message = '任务因服务重启中断，请手动重试'
            WHERE status = 'running'
            """)
    int markInterruptedRunningTasksAsFailed();

    @Update("""
            UPDATE review_task
            SET status = 'pending',
                progress = 0,
                result_file_id = NULL,
                retryable = 0,
                retry_count = retry_count + 1,
                llm_provider = NULL,
                start_time = NULL,
                finish_time = NULL,
                duration_ms = NULL,
                last_error_code = NULL,
                error_message = NULL
            WHERE id = #{id}
              AND user_id = #{userId}
              AND status = 'failed'
              AND retryable = 1
              AND retry_count < max_retry
            """)
    int retryTaskByOwner(@Param("id") Long id, @Param("userId") Long userId);
}
