package com.bankcore360.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiChatRequest {

    @NotBlank(message = "Message query cannot be empty")
    @Size(max = 2000, message = "Message query cannot exceed 2000 characters")
    private String message;

    private String category; // e.g., "ELIGIBILITY", "DOCUMENTATION", "POLICY", "EMI"

    public AiChatRequest() {
    }

    public AiChatRequest(String message, String category) {
        this.message = message;
        this.category = category;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}

