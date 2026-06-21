package com.warehouse.wavepicking.exception;

import java.time.LocalDateTime;

public class ErrorResponse {

    private String code;
    private String message;
    private LocalDateTime timestamp;
    private String path;

    public ErrorResponse(String code, String message, String path) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.path = path;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
