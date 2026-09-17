package com.verifipay.common.api;

public record ApiResponse<T>(
        ApiStatus status,
        String message,
        T data
) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
                ApiStatus.SUCCESS,
                message,
                data
        );
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(
                "Operación realizada correctamente",
                data
        );
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(
                ApiStatus.ERROR,
                message,
                null
        );
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(
                ApiStatus.ERROR,
                message,
                data
        );
    }
}