package com.chatbot.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatResponse {
    private String message;
    private String timestamp;
    private boolean success;
    private String sessionId;
    private String error;

    public ChatResponse() {
        // Default constructor for JSON serialization
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.success = true;
    }

    public ChatResponse(String message, String sessionId) {
        this();
        this.message = message;
        this.sessionId = sessionId;
    }

    public ChatResponse(String message, String sessionId, boolean success) {
        this(message, sessionId);
        this.success = success;
    }

    public static ChatResponse error(String errorMessage, String sessionId) {
        ChatResponse response = new ChatResponse();
        response.setError(errorMessage);
        response.setSessionId(sessionId);
        response.setSuccess(false);
        return response;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
