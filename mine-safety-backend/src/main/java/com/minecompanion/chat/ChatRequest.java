package com.minecompanion.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Inbound DTO for POST /api/chat.
 *
 * sessionId  — client-generated UUID; ties conversation history to this session.
 * message    — the user's raw text input.
 * role       — controls language complexity and response depth in PromptBuilder.
 */
@Data
public class ChatRequest {

    @NotBlank(message = "sessionId must not be blank")
    private String sessionId;

    @NotBlank(message = "message must not be blank")
    @Size(max = 2000, message = "message must not exceed 2000 characters")
    private String message;

    /**
     * The role of the user sending this message.
     * Defaults to WORKER if not supplied by the client.
     */
    private UserRole role = UserRole.WORKER;
}
