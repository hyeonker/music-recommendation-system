package com.example.musicrecommendation.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MatchingEvent extends ApplicationEvent {
    private final Long userId1;
    private final Long userId2;
    private final Long roomId;
    private final String status; // "SUCCESS" or "FAILED"
    private final String message;
    private final Object additionalData;

    public MatchingEvent(Object source, Long userId1, Long userId2, Long roomId, 
                        String status, String message, Object additionalData) {
        super(source);
        this.userId1 = userId1;
        this.userId2 = userId2;
        this.roomId = roomId;
        this.status = status;
        this.message = message;
        this.additionalData = additionalData;
    }

    public MatchingEvent(Object source, Long userId, String status, String message) {
        super(source);
        this.userId1 = userId;
        this.userId2 = null;
        this.roomId = null;
        this.status = status;
        this.message = message;
        this.additionalData = null;
    }
}