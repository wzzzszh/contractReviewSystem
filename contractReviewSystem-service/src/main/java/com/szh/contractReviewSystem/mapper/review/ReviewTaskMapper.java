package com.szh.contractReviewSystem.mapper.review;

import com.szh.contractReviewSystem.entity.review.ReviewTaskEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ReviewTaskMapper {

    @Insert("""
            INSERT INTO review_task (
                user_id, source_file_id, result_file_id, task_type, status,
                progress, perspective, user_focus, risk_report,
                generated_requirement, error_message
            )
            VALUES (
                #{userId}, #{sourceFileId}, #{resultFileId}, #{taskType}, #{status},
                #{progress}, #{perspective}, #{userFocus}, #{riskReport},
                #{generatedRequirement}, #{errorMessage}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ReviewTaskEntity task);

    @Select("""
            SELECT id, user_id, source_file_id, result_file_id, task_type, status,
                   progress, perspective, user_focus, risk_report,
                   generated_requirement, error_message, create_time, update_time
            FROM review_task
            WHERE id = #{id}
            """)
    ReviewTaskEntity selectById(@Param("id") Long id);

    @Select("""
            SELECT id, user_id, source_file_id, result_file_id, task_type, status,
                   progress, perspective, user_focus, risk_report,
                   generated_requirement, error_message, create_time, update_time
            FROM review_task
            WHERE id = #{id}
              AND user_id = #{userId}
            """)
    ReviewTaskEntity selectOwnedById(@Param("id") Long id, @Param("userId") Long userId);

    @Select("""
            SELECT id, user_id, source_file_id, result_file_id, task_type, status,
                   progress, perspective, user_focus, risk_report,
                   generated_requirement, error_message, create_time, update_time
            FROM review_task
            WHERE user_id = #{userId}
            ORDER BY id DESC
            """)
    List<ReviewTaskEntity> selectByUserId(@Param("userId") Long userId);

    @Update("""
            UPDATE review_task
            SET status = 'running',
                progress = #{progress},
                error_message = NULL
            WHERE id = #{id}
            """)
    int markRunning(@Param("id") Long id, @Param("progress") Integer progress);

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
                error_message = NULL
            WHERE id = #{id}
            """)
    int markSuccess(@Param("id") Long id,
                    @Param("resultFileId") Long resultFileId,
                    @Param("riskReport") String riskReport,
                    @Param("generatedRequirement") String generatedRequirement);

    @Update("""
            UPDATE review_task
            SET status = 'failed',
                error_message = #{errorMessage}
            WHERE id = #{id}
            """)
    int markFailed(@Param("id") Long id, @Param("errorMessage") String errorMessage);
}
