package com.homely.rental.media.controller;

import io.swagger.v3.oas.annotations.Operation;
import com.homely.rental.common.exception.IdInvalidException;
import com.homely.rental.media.dto.AttachMediaRequest;
import com.homely.rental.media.dto.MediaDTO;
import com.homely.rental.media.entity.MediaPurpose;
import com.homely.rental.media.service.MediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Media controller (MEDIA01, BR-09).
 * Handles file upload, attachment to resources, and listing.
 */
@RestController
@RequestMapping(path = "${apiPrefix}/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @GetMapping("/{id}/content")
    @Operation(summary = "Read authorized media content")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Raw file bytes; content type matches the stored file", content = {
            @io.swagger.v3.oas.annotations.media.Content(mediaType = "image/jpeg", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "binary")),
            @io.swagger.v3.oas.annotations.media.Content(mediaType = "image/png", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "binary")),
            @io.swagger.v3.oas.annotations.media.Content(mediaType = "image/webp", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "binary")),
            @io.swagger.v3.oas.annotations.media.Content(mediaType = "video/mp4", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "binary"))})
    public ResponseEntity<org.springframework.core.io.InputStreamResource> content(@PathVariable Long id) {
        var content = mediaService.content(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(content.contentType()))
                .contentLength(content.size()).header("Cache-Control", "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(new org.springframework.core.io.InputStreamResource(content.stream()));
    }

    /**
     * MEDIA01: Upload a file.
     * Returns MediaDTO with READY status. File is not yet attached to any resource.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload media file")
    public ResponseEntity<MediaDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("purpose") MediaPurpose purpose) throws IdInvalidException {
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaService.upload(file, purpose));
    }

    /**
     * Attach uploaded media to a room.
     */
    @PostMapping("/attach/room/{roomId}")
    @Operation(summary = "Attach media to room")
    public ResponseEntity<List<MediaDTO>> attachToRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody AttachMediaRequest request,
            @RequestParam(value = "purpose", defaultValue = "ROOM_PHOTO") MediaPurpose purpose)
            throws IdInvalidException {
        return ResponseEntity.ok(
                mediaService.attachToResource(request.getMediaIds(), "room", roomId, purpose));
    }

    /**
     * Get all media for a room.
     */
    @GetMapping("/room/{roomId}")
    @Operation(summary = "Get room media")
    public ResponseEntity<List<MediaDTO>> getRoomMedia(@PathVariable Long roomId) {
        return ResponseEntity.ok(mediaService.getResourceMedia("room", roomId));
    }

    /**
     * Delete a media file.
     */
    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Delete media")
    public ResponseEntity<Void> deleteMedia(@PathVariable Long mediaId) throws IdInvalidException {
        mediaService.deleteMedia(mediaId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get my unattached (READY) media for a given purpose.
     */
    @GetMapping("/my")
    @Operation(summary = "Get my ready media")
    public ResponseEntity<List<MediaDTO>> getMyReadyMedia(
            @RequestParam("purpose") MediaPurpose purpose) throws IdInvalidException {
        return ResponseEntity.ok(mediaService.getMyReadyMedia(purpose));
    }
}
