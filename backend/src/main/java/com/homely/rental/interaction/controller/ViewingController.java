package com.homely.rental.interaction.controller;

import io.swagger.v3.oas.annotations.Operation;
import com.homely.rental.common.dto.PageResponse;
import com.homely.rental.common.exception.IdInvalidException;
import com.homely.rental.interaction.dto.*;
import com.homely.rental.interaction.service.ViewingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Viewing controller (VIEW01–VIEW08).
 */
@RestController
@RequestMapping(path = "${apiPrefix}")
@RequiredArgsConstructor
public class ViewingController {

    private final ViewingService viewingService;

    @GetMapping("/viewings/{id}")
    public ResponseEntity<ViewingDTO> getViewing(@PathVariable Long id) throws IdInvalidException {
        return ResponseEntity.ok(viewingService.getViewing(id));
    }

    // VIEW01: Get available slots for a room
    @GetMapping("/viewing-slots/{roomId}")
    @Operation(summary = "Get available viewing slots")
    public ResponseEntity<List<ViewingSlotDTO>> getSlots(@PathVariable Long roomId) {
        return ResponseEntity.ok(viewingService.getSlots(roomId));
    }

    // VIEW02: Create slot (Host)
    @PostMapping("/viewing-slots")
    @Operation(summary = "Create viewing slot")
    public ResponseEntity<ViewingSlotDTO> createSlot(
            @Valid @RequestBody ViewingSlotCreateRequest request) throws IdInvalidException {
        return ResponseEntity.status(HttpStatus.CREATED).body(viewingService.createSlot(request));
    }

    // VIEW03: Delete slot (Host)
    @DeleteMapping("/viewing-slots/{id}")
    @Operation(summary = "Delete viewing slot")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id) throws IdInvalidException {
        viewingService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }

    // VIEW04: My viewings (Tenant)
    @GetMapping("/viewings/my")
    @Operation(summary = "Get my viewings")
    public ResponseEntity<PageResponse<ViewingDTO>> getMyViewings(Pageable pageable) throws IdInvalidException {
        return ResponseEntity.ok(viewingService.getMyViewings(pageable));
    }

    // VIEW04 host: Host's viewings
    @GetMapping("/viewings/host")
    @Operation(summary = "Get host viewings")
    public ResponseEntity<PageResponse<ViewingDTO>> getHostViewings(Pageable pageable) throws IdInvalidException {
        return ResponseEntity.ok(viewingService.getHostViewings(pageable));
    }

    // VIEW05: Create viewing (Tenant+Verified)
    @PostMapping("/viewings")
    @Operation(summary = "Book a viewing")
    public ResponseEntity<ViewingDTO> createViewing(
            @Valid @RequestBody ViewingCreateRequest request) throws IdInvalidException {
        return ResponseEntity.status(HttpStatus.CREATED).body(viewingService.createViewing(request));
    }

    // VIEW06: Confirm viewing (Host)
    @PostMapping("/viewings/{id}/confirm")
    @Operation(summary = "Confirm viewing")
    public ResponseEntity<ViewingDTO> confirmViewing(@PathVariable Long id) throws IdInvalidException {
        return ResponseEntity.ok(viewingService.confirmViewing(id));
    }

    // VIEW07: Cancel viewing (Auth)
    @PostMapping("/viewings/{id}/cancel")
    @Operation(summary = "Cancel viewing")
    public ResponseEntity<ViewingDTO> cancelViewing(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) throws IdInvalidException {
        return ResponseEntity.ok(viewingService.cancelViewing(id, reason));
    }

    // VIEW08: Complete viewing (Host)
    @PostMapping("/viewings/{id}/complete")
    @Operation(summary = "Complete viewing")
    public ResponseEntity<ViewingDTO> completeViewing(@PathVariable Long id) throws IdInvalidException {
        return ResponseEntity.ok(viewingService.completeViewing(id));
    }
}
