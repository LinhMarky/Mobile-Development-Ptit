package com.homely.student14.controller;

import static com.homely.student14.dto.ApiResponse.ok;
import static com.homely.student14.repository.DatabaseRepository.page;

import com.homely.student14.dto.ApiResponse;
import com.homely.student14.dto.request.catalog.ListingActionDTO;
import com.homely.student14.dto.request.catalog.SaveListingDTO;
import com.homely.student14.dto.request.catalog.SaveRoomDTO;
import com.homely.student14.service.CatalogService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogController {
    private final CatalogService catalogService;

    @GetMapping("/amenities")
    public ApiResponse<?> amenities() {
        return ok(this.catalogService.amenities());
    }

    @GetMapping("/listings")
    public ApiResponse<?> listings() {
        return ok(page(this.catalogService.listings()));
    }

    @GetMapping("/listings/my")
    public ApiResponse<?> myListings(@AuthenticationPrincipal Jwt jwt) {
        return ok(page(this.catalogService.myListings(jwt)));
    }

    @GetMapping("/listings/{id}")
    public ApiResponse<?> listing(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        return ok(this.catalogService.listing(id, jwt));
    }

    @GetMapping("/rooms")
    public ApiResponse<?> rooms(@AuthenticationPrincipal Jwt jwt) {
        return ok(page(this.catalogService.rooms(jwt)));
    }

    @GetMapping("/rooms/{id}")
    public ApiResponse<?> room(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        return ok(this.catalogService.ownedRoom(id, jwt, false));
    }

    @PostMapping("/rooms")
    public ApiResponse<?> createRoom(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SaveRoomDTO request) {
        return ok(this.catalogService.createRoom(jwt, request));
    }

    @PutMapping("/rooms/{id}")
    public ApiResponse<?> updateRoom(
            @PathVariable long id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SaveRoomDTO request) {
        return ok(this.catalogService.updateRoom(id, jwt, request));
    }

    @PostMapping("/listings/room/{id}")
    public ApiResponse<?> saveListing(
            @PathVariable long id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SaveListingDTO request) {
        return ok(this.catalogService.saveListing(id, jwt, request));
    }

    @PostMapping("/listings/{id}/{action:submit|hide}")
    public ApiResponse<?> listingAction(
            @PathVariable long id,
            @PathVariable String action,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ListingActionDTO request) {
        return ok(this.catalogService.listingAction(id, jwt, action, request));
    }

    @GetMapping("/wishlist")
    public ApiResponse<?> wishlist(@AuthenticationPrincipal Jwt jwt) {
        return ok(page(this.catalogService.wishlist(jwt)));
    }

    @PostMapping("/wishlist/{id}")
    public ApiResponse<?> save(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        this.catalogService.saveWishlist(id, jwt, true);
        return ok(null);
    }

    @DeleteMapping("/wishlist/{id}")
    public ApiResponse<?> unsave(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        this.catalogService.saveWishlist(id, jwt, false);
        return ok(null);
    }

    @GetMapping("/rooms/{id}/reviews")
    public ApiResponse<?> reviews(@PathVariable long id) {
        return ok(page(this.catalogService.reviews(id)));
    }
}
