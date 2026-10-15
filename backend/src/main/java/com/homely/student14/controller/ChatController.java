package com.homely.student14.controller;

import com.homely.student14.dto.ApiResponse;
import com.homely.student14.dto.request.chat.CreateConversationDTO;
import com.homely.student14.dto.request.chat.SendMessageDTO;
import com.homely.student14.repository.DatabaseRepository;
import com.homely.student14.service.ChatService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @GetMapping
    public ApiResponse<?> list(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(DatabaseRepository.page(chatService.findConversations(jwt)));
    }

    @PostMapping
    public ApiResponse<?> create(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateConversationDTO request) {
        return ApiResponse.ok(chatService.createConversation(jwt, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> get(
            @PathVariable("id") long conversationId, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(chatService.findConversation(conversationId, jwt));
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<?> messages(
            @PathVariable("id") long conversationId, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(
                DatabaseRepository.page(chatService.findMessages(conversationId, jwt)));
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<?> send(
            @PathVariable("id") long conversationId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SendMessageDTO request) {
        return ApiResponse.ok(chatService.sendMessage(conversationId, jwt, request));
    }
}
