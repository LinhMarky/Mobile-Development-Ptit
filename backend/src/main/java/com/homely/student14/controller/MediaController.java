package com.homely.student14.controller;

import static com.homely.student14.dto.ApiResponse.ok;

import com.homely.student14.dto.ApiResponse;
import com.homely.student14.dto.request.media.UploadImageDTO;
import com.homely.student14.service.MediaService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MediaController {
    private final MediaService mediaService;

    @PostMapping("/rooms/{id}/image")
    public ApiResponse<?> upload(
            @PathVariable long id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UploadImageDTO request) {
        return ok(this.mediaService.upload(id, jwt, request));
    }

    @DeleteMapping("/rooms/{id}/image")
    public ApiResponse<?> delete(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        this.mediaService.remove(id, jwt);
        return ok(null);
    }

    @GetMapping("/media/{id}/content")
    public ResponseEntity<FileSystemResource> content(
            @PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header("Cache-Control", "private, max-age=300")
                .body(new FileSystemResource(this.mediaService.content(id, jwt)));
    }
}
