package com.szh.contractReviewSystem.controller;

import com.szh.contractReviewSystem.annotation.RequiresPermissions;
import com.szh.contractReviewSystem.common.Result;
import com.szh.contractReviewSystem.controller.notdb.BaseController;
import com.szh.contractReviewSystem.model.request.CreateReviewTaskRequest;
import com.szh.contractReviewSystem.model.response.ReviewTaskResponse;
import com.szh.contractReviewSystem.service.review.ReviewTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/api/review/tasks")
public class ReviewTaskController extends BaseController {

    private final ReviewTaskService reviewTaskService;

    public ReviewTaskController(ReviewTaskService reviewTaskService) {
        this.reviewTaskService = reviewTaskService;
    }

    @RequiresPermissions("review:modify")
    @PostMapping
    public Result<ReviewTaskResponse> createTask(@Valid @RequestBody CreateReviewTaskRequest request) {
        return success("review task submitted", reviewTaskService.createTask(request));
    }

    @RequiresPermissions("review:modify")
    @GetMapping("/{id}")
    public Result<ReviewTaskResponse> getTask(
            @PathVariable @NotNull(message = "task id must not be null") Long id) {
        return success(reviewTaskService.getOwnedTask(id));
    }

    @RequiresPermissions("review:modify")
    @GetMapping
    public Result<List<ReviewTaskResponse>> listTasks() {
        return success(reviewTaskService.listOwnedTasks());
    }
}
