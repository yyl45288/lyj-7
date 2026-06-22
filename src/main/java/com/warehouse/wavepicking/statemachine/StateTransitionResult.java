package com.warehouse.wavepicking.statemachine;

public class StateTransitionResult {

    private final boolean allowed;
    private final String errorCode;
    private final String errorMessage;

    private StateTransitionResult(boolean allowed, String errorCode, String errorMessage) {
        this.allowed = allowed;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public static StateTransitionResult allowed() {
        return new StateTransitionResult(true, null, null);
    }

    public static StateTransitionResult denied(String errorCode, String errorMessage) {
        return new StateTransitionResult(false, errorCode, errorMessage);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
