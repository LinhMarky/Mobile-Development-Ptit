package com.homely.rental.catalog.service;

import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.security.UserResolver;
import com.homely.rental.catalog.dto.request.FeeInput;
import com.homely.rental.catalog.dto.request.ListingWriteRequest;
import com.homely.rental.catalog.dto.response.ListingDTO;
import com.homely.rental.catalog.dto.response.RoomDTO;
import com.homely.rental.catalog.entity.*;
import com.homely.rental.catalog.repository.ListingFeeRepository;
import com.homely.rental.catalog.repository.ListingRepository;
import com.homely.rental.catalog.repository.RoomRepository;
import com.homely.rental.common.dto.PageResponse;
import com.homely.rental.common.exception.ConflictException;
import com.homely.rental.common.exception.IdInvalidException;
import com.homely.rental.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Listing service (CAT01–CAT10, BR-02, BR-03, BR-04).
 * Handles listing lifecycle: create → update → submit → search.
 */
@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final RoomRepository roomRepository;
    private final ListingFeeRepository listingFeeRepository;
    private final UserResolver userResolver;
    private final RoomService roomService;
    private final jakarta.persistence.EntityManager entityManager;

    private static final long LISTING_EXPIRY_DAYS = 30;

    // CAT01: Create listing (DRAFT)
    @Transactional
    public ListingDTO createListing(Long roomId, ListingWriteRequest dto) throws IdInvalidException {
        User host = getCurrentHost();
        Room room = roomRepository.lockById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
        if (!room.getHost().getId().equals(host.getId())) throw new ResourceNotFoundException("Room", roomId);

        // Check if room already has a listing
        if (listingRepository.findByRoomId(roomId).isPresent()) {
            throw new ConflictException("LISTING_EXISTS",
                    "Room already has a listing. Update the existing one instead.");
        }

        Listing listing = new Listing();
        listing.setRoom(room);
        listing.setTitle(dto.getTitle() != null ? dto.getTitle() : "");
        listing.setDescription(dto.getDescription() != null ? dto.getDescription() : "");
        listing.setRentVnd(dto.getRentVnd());
        listing.setDepositVnd(dto.getDepositVnd());
        listing.setStatus(ListingStatus.DRAFT);
        listing.setTermsVersion(room.getTermsVersion());

        return toDTO(listingRepository.save(listing));
    }

    // CAT02: Update listing
    @Transactional
    public ListingDTO updateListing(Long listingId, ListingWriteRequest dto) throws IdInvalidException {
        User host = getCurrentHost();
        Listing listing = ownedLocked(listingId, host.getId());

        // BR-02: Only DRAFT, HIDDEN, REJECTED, EXPIRED can be edited
        if (!ListingStatus.EDITABLE.contains(listing.getStatus())) {
            throw new ConflictException("LISTING_NOT_EDITABLE",
                    "Listing can only be edited in DRAFT, HIDDEN, REJECTED, or EXPIRED status. Current: " + listing.getStatus());
        }

        // Optimistic locking
        if (dto.getExpectedVersion() != null && listing.getVersion() != dto.getExpectedVersion()) {
            throw new ConflictException("VERSION_MISMATCH",
                    "Listing has been modified. Expected version: " + dto.getExpectedVersion());
        }

        if (dto.getTitle() != null) listing.setTitle(dto.getTitle());
        if (dto.getDescription() != null) listing.setDescription(dto.getDescription());
        if (dto.getRentVnd() != null) listing.setRentVnd(dto.getRentVnd());
        if (dto.getDepositVnd() != null) listing.setDepositVnd(dto.getDepositVnd());

        // Sync terms version from room
        listing.setTermsVersion(listing.getRoom().getTermsVersion());

        return toDTO(listingRepository.save(listing));
    }

    // CAT03: Submit listing for review
    @Transactional
    public ListingDTO submitForReview(Long listingId) throws IdInvalidException {
        User host = getCurrentHost();
        Listing listing = ownedLocked(listingId, host.getId());

        // BR-02: Can submit from DRAFT, HIDDEN, REJECTED, EXPIRED
        if (!ListingStatus.SUBMITTABLE.contains(listing.getStatus())) {
            throw new ConflictException("INVALID_STATUS_TRANSITION",
                    "Listing can only be submitted from DRAFT, HIDDEN, REJECTED, or EXPIRED status. Current: " + listing.getStatus());
        }

        // Validate required fields before submission
        if (listing.getRentVnd() == null) {
            throw new IdInvalidException("Rent amount is required before submission");
        }
        if (listing.getTitle() == null || listing.getTitle().isBlank()) {
            throw new IdInvalidException("Title is required before submission");
        }

        listing.setStatus(ListingStatus.PENDING_REVIEW);
        return toDTO(listingRepository.save(listing));
    }

    // CAT04: Host hides a published listing
    @Transactional
    public ListingDTO hideListing(Long listingId) throws IdInvalidException {
        User host = getCurrentHost();
        Listing listing = ownedLocked(listingId, host.getId());

        if (listing.getStatus() != ListingStatus.PUBLISHED) {
            throw new ConflictException("INVALID_STATUS_TRANSITION",
                    "Only PUBLISHED listings can be hidden");
        }

        listing.setStatus(ListingStatus.HIDDEN);
        return toDTO(listingRepository.save(listing));
    }

    // CAT05: Search published listings (public, no auth needed)
    @Transactional(readOnly = true)
    public PageResponse<ListingDTO> searchListings(com.homely.rental.catalog.dto.request.ListingSearchQuery filter) {
        var page = listingRepository.findAll(ListingSearch.matching(filter),
                org.springframework.data.domain.PageRequest.of(filter.getPage(), filter.getSize()));
        return PageResponse.of(page, page.getContent().stream().map(this::toDTO).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingDTO> searchListings(String query, Pageable pageable) {
        Instant now = Instant.now();
        Page<Listing> page;
        if (query != null && !query.isBlank()) {
            page = listingRepository.searchPublishedListings(query.trim(), now, pageable);
        } else {
            page = listingRepository.findPublishedAvailableListings(now, pageable);
        }
        return PageResponse.of(page, page.getContent().stream().map(this::toDTO).toList());
    }

    // CAT07: Get listing detail (public)
    @Transactional(readOnly = true)
    public ListingDTO getListingDetail(Long listingId) {
        Listing listing = getPublicListing(listingId);
        ListingDTO dto = toDTO(listing);
        // Add fees
        List<ListingFee> fees = listingFeeRepository.findByListingIdOrderBySortOrder(listingId);
        dto.setFees(fees.stream().map(this::toFeeDTO).toList());
        return dto;
    }

    // Get host's listings
    public PageResponse<ListingDTO> getHostListings(Pageable pageable) throws IdInvalidException {
        User host = getCurrentHost();
        Page<Listing> page = listingRepository.findByRoomHostId(host.getId(), pageable);
        return PageResponse.of(page, page.getContent().stream().map(this::toDTO).toList());
    }

    // ===== Fee management (CAT08–10) =====

    @Transactional
    public List<ListingDTO.FeeDTO> setFees(Long listingId, List<FeeInput> feeInputs) throws IdInvalidException {
        User host = getCurrentHost();
        Listing listing = ownedLocked(listingId, host.getId());
        Room room = roomRepository.lockById(listing.getRoom().getId()).orElseThrow();
        if (room.getAvailability() == RoomAvailability.HELD || !ListingStatus.EDITABLE.contains(listing.getStatus()))
            throw new ConflictException("TERMS_LOCKED", "Hide the listing before changing fees; held rooms cannot be changed");
        room.setTermsVersion(room.getTermsVersion() + 1);
        listing.setTermsVersion(room.getTermsVersion());

        // Delete existing fees
        listingFeeRepository.deleteByListingId(listingId);

        // Create new fees
        List<ListingFee> newFees = feeInputs.stream().map(input -> {
            ListingFee fee = new ListingFee();
            fee.setListing(listing);
            fee.setFeeCode(input.getFeeCode());
            fee.setFeeMode(input.getFeeMode());
            fee.setAmountVnd(input.getAmountVnd());
            fee.setUnitName(input.getUnitName());
            fee.setNote(input.getNote());
            fee.setSortOrder(input.getSortOrder());
            return fee;
        }).toList();

        List<ListingFee> savedFees = listingFeeRepository.saveAll(newFees);
        return savedFees.stream().map(this::toFeeDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ListingDTO.FeeDTO> getFees(Long listingId) {
        getPublicListing(listingId);
        List<ListingFee> fees = listingFeeRepository.findByListingIdOrderBySortOrder(listingId);
        return fees.stream().map(this::toFeeDTO).toList();
    }

    private Listing getPublicListing(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));
        // Check expiration on reads even before the scheduled expiration job runs.
        // Null expiry is retained for legacy published rows, matching the search query.
        if (listing.getStatus() != ListingStatus.PUBLISHED
                || (listing.getExpiresAt() != null && !listing.getExpiresAt().isAfter(Instant.now()))) {
            throw new ResourceNotFoundException("Listing", listingId);
        }
        // Room availability controls search; existing public links still expose HELD/RENTED status.
        return listing;
    }

    // ===== Admin actions =====

    @Transactional
    public ListingDTO approveListing(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        if (listing.getStatus() != ListingStatus.PENDING_REVIEW) {
            throw new ConflictException("INVALID_STATUS_TRANSITION",
                    "Only PENDING_REVIEW listings can be approved");
        }

        listing.setStatus(ListingStatus.PUBLISHED);
        listing.setPublishedAt(Instant.now());
        listing.setExpiresAt(Instant.now().plus(LISTING_EXPIRY_DAYS, ChronoUnit.DAYS));

        return toDTO(listingRepository.save(listing));
    }

    @Transactional
    public ListingDTO rejectListing(Long listingId, String reason) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        if (listing.getStatus() != ListingStatus.PENDING_REVIEW) {
            throw new ConflictException("INVALID_STATUS_TRANSITION",
                    "Only PENDING_REVIEW listings can be rejected");
        }

        listing.setStatus(ListingStatus.REJECTED);
        // We might want to store the reason somewhere in a history table, but for now we just change the state.
        
        return toDTO(listingRepository.save(listing));
    }

    // ===== DTO Conversion =====

    private Listing ownedLocked(Long id, Long hostId) {
        Long roomId = listingRepository.findRoomId(id).orElseThrow(() -> new ResourceNotFoundException("Listing", id));
        Room room = roomRepository.lockById(roomId).orElseThrow();
        entityManager.refresh(room);
        Listing listing = listingRepository.findById(id).orElseThrow();
        entityManager.refresh(listing);
        if (!room.getHost().getId().equals(hostId)) throw new ResourceNotFoundException("Listing", id);
        if (room.getAvailability() == RoomAvailability.HELD)
            throw new ConflictException("TERMS_LOCKED", "Cannot change a listing while the room is held");
        return listing;
    }

    public ListingDTO toDTO(Listing listing) {
        Room room = listing.getRoom();
        ListingDTO.RoomSummary roomSummary = ListingDTO.RoomSummary.builder()
                .id(room.getId())
                .hostId(room.getHost().getId())
                .unitCode(room.getUnitCode())
                .roomType(room.getRoomType().name())
                .areaM2(room.getAreaM2())
                .maxOccupants(room.getMaxOccupants())
                .availability(room.getAvailability().name())
                .build();

        if (room.getAddress() != null) {
            roomSummary.setAddress(RoomDTO.AddressDTO.builder()
                    .line(room.getAddress().getLine())
                    .provinceCode(room.getAddress().getProvinceCode())
                    .provinceName(room.getAddress().getProvinceName())
                    .wardCode(room.getAddress().getWardCode())
                    .wardName(room.getAddress().getWardName())
                    .build());
        }
        if (room.getLocation() != null) {
            roomSummary.setLocation(RoomDTO.GeoPointDTO.builder()
                    .latitude(room.getLocation().getLatitude())
                    .longitude(room.getLocation().getLongitude())
                    .build());
        }

        roomSummary.setAmenities(room.getAmenities().stream()
                .map(a -> RoomDTO.AmenityDTO.builder()
                        .id(a.getId()).name(a.getName())
                        .icon(a.getIcon()).category(a.getCategory())
                        .build())
                .toList());

        return ListingDTO.builder()
                .id(listing.getId())
                .roomId(room.getId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .rentVnd(listing.getRentVnd())
                .depositVnd(listing.getDepositVnd())
                .status(listing.getStatus().name())
                .termsVersion(listing.getTermsVersion())
                .publishedAt(listing.getPublishedAt())
                .expiresAt(listing.getExpiresAt())
                .version(listing.getVersion())
                .room(roomSummary)
                .createdAt(listing.getCreatedAt())
                .build();
    }

    private ListingDTO.FeeDTO toFeeDTO(ListingFee fee) {
        return ListingDTO.FeeDTO.builder()
                .id(fee.getId())
                .feeCode(fee.getFeeCode())
                .feeMode(fee.getFeeMode())
                .amountVnd(fee.getAmountVnd())
                .unitName(fee.getUnitName())
                .note(fee.getNote())
                .sortOrder(fee.getSortOrder())
                .build();
    }

    private User getCurrentHost() throws IdInvalidException {
        User user = userResolver.requireCurrent();
        if (!user.isEmailVerified()) {
            throw new IdInvalidException("Email must be verified before performing host actions");
        }
        if (!user.isHost()) {
            throw new IdInvalidException("User does not have HOST role");
        }
        return user;
    }
}
