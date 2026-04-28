package com.szh.contractReviewSystem.utils;

import com.szh.contractReviewSystem.exception.BusinessExceptionEnum;
import com.szh.contractReviewSystem.exception.CustomException;

public final class UserContextHolder {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static Long requireUserId() {
        Long userId = getUserId();
        if (userId == null || userId <= 0) {
            throw new CustomException(BusinessExceptionEnum.UNAUTHORIZED);
        }
        return userId;
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
