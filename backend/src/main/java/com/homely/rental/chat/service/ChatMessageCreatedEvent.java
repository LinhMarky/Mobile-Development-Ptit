package com.homely.rental.chat.service;

import com.homely.rental.chat.dto.MessageDTO;

public record ChatMessageCreatedEvent(MessageDTO message, String tenantEmail, String hostEmail) {
}
