package com.homely.rental.chat.controller;

import com.homely.rental.chat.dto.*;
import com.homely.rental.chat.service.ChatService;
import com.homely.rental.common.dto.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("${apiPrefix}/conversations")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ConversationDTO> create(@Valid @RequestBody ConversationCreateRequest request,
                                                   Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.createConversation(request.getRoomId(), principal.getName()));
    }

    @GetMapping
    public PageResponse<ConversationDTO> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                               @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
                                               Principal principal) {
        return chatService.listConversations(principal.getName(), page, size);
    }

    @GetMapping("/{id}")
    public ConversationDTO detail(@PathVariable @Positive Long id, Principal principal) {
        return chatService.getConversation(id, principal.getName());
    }

    @GetMapping("/{id}/messages")
    public MessagePageDTO messages(@PathVariable @Positive Long id,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
            @RequestParam(name = "before_sequence", required = false) @Positive Long before,
            @RequestParam(name = "after_sequence", required = false) @Min(0) Long after,
            Principal principal) {
        return chatService.getMessages(id, principal.getName(), limit, before, after);
    }

    @PostMapping("/{id}/read-marker")
    public ReadMarkerDTO markRead(@PathVariable @Positive Long id,
                                   @Valid @RequestBody ReadMarkerRequest request, Principal principal) {
        return chatService.markRead(id, principal.getName(), request.getLastReadSequence());
    }
}
