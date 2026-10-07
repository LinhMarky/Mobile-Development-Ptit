package com.homely.rental.catalog.controller;

import com.homely.rental.catalog.dto.request.RoomCreateRequest;
import com.homely.rental.catalog.dto.request.RoomPatchRequest;
import com.homely.rental.catalog.dto.response.RoomDTO;
import com.homely.rental.catalog.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import com.homely.rental.common.dto.PageResponse;
import com.homely.rental.common.exception.IdInvalidException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Room controller (HOST01–HOST04).
 * All endpoints require HOST role.
 */
@RestController
@RequestMapping(path = "${apiPrefix}/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    // HOST01: Create room
    @PostMapping
    @PreAuthorize("hasRole('ROLE_HOST')")
    @Operation(summary = "Create a new room")
    public ResponseEntity<RoomDTO> createRoom(@Valid @RequestBody RoomCreateRequest dto) throws IdInvalidException {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.createRoom(dto));
    }

    // HOST02: Update room
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_HOST')")
    @Operation(summary = "Update room")
    public ResponseEntity<RoomDTO> updateRoom(@PathVariable Long id,
                                               @Valid @RequestBody RoomPatchRequest dto) throws IdInvalidException {
        return ResponseEntity.ok(roomService.updateRoom(id, dto));
    }

    // HOST03: List host's rooms
    @GetMapping
    @PreAuthorize("hasRole('ROLE_HOST')")
    @Operation(summary = "List host rooms")
    public ResponseEntity<PageResponse<RoomDTO>> getHostRooms(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable)
            throws IdInvalidException {
        return ResponseEntity.ok(roomService.getHostRooms(pageable));
    }

    // HOST04: Get room detail
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_HOST')")
    @Operation(summary = "Get room detail")
    public ResponseEntity<RoomDTO> getRoomDetail(@PathVariable Long id) throws IdInvalidException {
        return ResponseEntity.ok(roomService.getRoomDetail(id));
    }
}
