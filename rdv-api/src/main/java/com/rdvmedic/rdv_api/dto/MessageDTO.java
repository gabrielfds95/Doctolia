package com.rdvmedic.rdv_api.dto;

import com.rdvmedic.rdv_api.model.Message;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageDTO {

    private String id;
    private Long slotId;
    private Long senderId;
    private String content;
    private LocalDateTime sentAt;

    public static MessageDTO fromEntity(Message message) {
        if (message == null) return null;
        return MessageDTO.builder()
                .id(message.getId())
                .slotId(message.getSlotId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .build();
    }
}
