package com.homely.rental.catalog.service;

import com.homely.rental.auth.entity.User;
import com.homely.rental.auth.security.UserResolver;
import com.homely.rental.catalog.dto.request.RoomCreateRequest;
import com.homely.rental.catalog.dto.request.RoomPatchRequest;
import com.homely.rental.catalog.dto.response.RoomDTO;
import com.homely.rental.catalog.entity.*;
import com.homely.rental.catalog.repository.AmenityRepository;
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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Room service (HOST01–HOST04, BR-01).
 * Handles room CRUD operations with business rule enforcement.
 */
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final AmenityRepository amenityRepository;
    private final ListingRepository listingRepository;
    private final UserResolver userResolver;

    // HOST01: Create room
    @Transactional
    public RoomDTO createRoom(RoomCreateRequest dto) throws IdInvalidException {
        User host = getCurrentHost();

        // Check duplicate unit_code for this host
        if (roomRepository.existsByHostIdAndUnitCode(host.getId(), dto.getUnitCode())) {
            throw new ConflictException("DUPLICATE_UNIT_CODE",
                    "A room with unit code '" + dto.getUnitCode() + "' already exists");
        }

        Room room = new Room();
        room.setHost(host);
        room.setUnitCode(dto.getUnitCode());
        room.setRoomType(RoomType.valueOf(dto.getRoomType()));
        room.setAreaM2(dto.getAreaM2());
        room.setMaxOccupants(dto.getMaxOccupants());
        room.setAvailability(RoomAvailability.AVAILABLE);

        if (dto.getAddress() != null) {
            Address address = new Address();
            address.setLine(dto.getAddress().getLine());
            address.setProvinceCode(dto.getAddress().getProvinceCode());
            address.setProvinceName(dto.getAddress().getProvinceName());
            address.setWardCode(dto.getAddress().getWardCode());
            address.setWardName(dto.getAddress().getWardName());
            room.setAddress(address);
        }

        if (dto.getLocation() != null) {
            room.setLocation(new GeoPoint(dto.getLocation().getLatitude(), dto.getLocation().getLongitude()));
        }

        if (dto.getAmenityIds() != null && !dto.getAmenityIds().isEmpty()) {
            room.setAmenities(resolveAmenities(dto.getAmenityIds()));
        }

        return toDTO(roomRepository.save(room));
    }

    // HOST02: Update room
    @Transactional
    public RoomDTO updateRoom(Long roomId, RoomPatchRequest dto) throws IdInvalidException {
        User host = getCurrentHost();
        Room room = roomRepository.findByIdAndHostId(roomId, host.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));

        // BR-01.5: Room with APPROVED/CONFIRMED booking cannot be modified
        if (room.getAvailability() == RoomAvailability.HELD) {
            throw new ConflictException("ROOM_HELD",
                    "Cannot modify room while it has an active booking hold");
        }

        // Optimistic locking check
        if (dto.getExpectedVersion() != null && room.getVersion() != dto.getExpectedVersion()) {
            throw new ConflictException("VERSION_MISMATCH",
                    "Room has been modified. Expected version: " + dto.getExpectedVersion());
        }

        // Resolve references before mutating this managed entity: an invalid patch must be atomic.
        Set<Amenity> amenities = dto.getAmenityIds() == null ? null : resolveAmenities(dto.getAmenityIds());

        boolean termsChanged = false;

        if (dto.getRoomType() != null) {
            room.setRoomType(RoomType.valueOf(dto.getRoomType()));
            termsChanged = true;
        }
        if (dto.getAreaM2() != null) {
            room.setAreaM2(dto.getAreaM2());
            termsChanged = true;
        }
        if (dto.getMaxOccupants() != null) {
            room.setMaxOccupants(dto.getMaxOccupants());
            termsChanged = true;
        }
        if (dto.getAddress() != null) {
            Address address = room.getAddress() != null ? room.getAddress() : new Address();
            address.setLine(dto.getAddress().getLine());
            address.setProvinceCode(dto.getAddress().getProvinceCode());
            address.setProvinceName(dto.getAddress().getProvinceName());
            address.setWardCode(dto.getAddress().getWardCode());
            address.setWardName(dto.getAddress().getWardName());
            room.setAddress(address);
            termsChanged = true;
        }
        if (dto.getLocation() != null) {
            room.setLocation(new GeoPoint(dto.getLocation().getLatitude(), dto.getLocation().getLongitude()));
        }
        if (dto.getAmenityIds() != null) {
            room.setAmenities(amenities);
            termsChanged = true;
        }

        // BR-01.5: If terms changed, bump version and revert listing to DRAFT
        if (termsChanged) {
            room.setTermsVersion(room.getTermsVersion() + 1);
            Optional<Listing> listingOpt = listingRepository.findByRoomId(roomId);
            listingOpt.ifPresent(listing -> {
                if (listing.getStatus() == ListingStatus.PUBLISHED
                        || listing.getStatus() == ListingStatus.PENDING_REVIEW) {
                    listing.setStatus(ListingStatus.DRAFT);
                    listingRepository.save(listing);
                }
            });
        }

        return toDTO(roomRepository.save(room));
    }

    // HOST03: Get host's rooms
    public PageResponse<RoomDTO> getHostRooms(Pageable pageable) throws IdInvalidException {
        User host = getCurrentHost();
        Page<Room> page = roomRepository.findByHostId(host.getId(), pageable);
        return PageResponse.of(page, page.getContent().stream().map(this::toDTO).toList());
    }

    // HOST04: Get room detail
    public RoomDTO getRoomDetail(Long roomId) throws IdInvalidException {
        User host = getCurrentHost();
        Room room = roomRepository.findByIdAndHostId(roomId, host.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
        return toDTO(room);
    }

    // Get room by ID (for internal use)
    public Room getRoomEntity(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
    }

    // ===== DTO Conversion =====

    public RoomDTO toDTO(Room room) {
        RoomDTO.AddressDTO addressDTO = null;
        if (room.getAddress() != null) {
            addressDTO = RoomDTO.AddressDTO.builder()
                    .line(room.getAddress().getLine())
                    .provinceCode(room.getAddress().getProvinceCode())
                    .provinceName(room.getAddress().getProvinceName())
                    .wardCode(room.getAddress().getWardCode())
                    .wardName(room.getAddress().getWardName())
                    .build();
        }

        RoomDTO.GeoPointDTO geoDTO = null;
        if (room.getLocation() != null) {
            geoDTO = RoomDTO.GeoPointDTO.builder()
                    .latitude(room.getLocation().getLatitude())
                    .longitude(room.getLocation().getLongitude())
                    .build();
        }

        List<RoomDTO.AmenityDTO> amenityDTOs = room.getAmenities().stream()
                .map(a -> RoomDTO.AmenityDTO.builder()
                        .id(a.getId())
                        .name(a.getName())
                        .icon(a.getIcon())
                        .category(a.getCategory())
                        .build())
                .toList();

        return RoomDTO.builder()
                .id(room.getId())
                .hostId(room.getHost().getId())
                .unitCode(room.getUnitCode())
                .roomType(room.getRoomType().name())
                .areaM2(room.getAreaM2())
                .maxOccupants(room.getMaxOccupants())
                .address(addressDTO)
                .location(geoDTO)
                .availability(room.getAvailability().name())
                .termsVersion(room.getTermsVersion())
                .amenities(amenityDTOs)
                .version(room.getVersion())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    private User getCurrentHost() throws IdInvalidException {
        User user = userResolver.requireCurrent();
        if (!user.isEmailVerified()) {
            throw new IdInvalidException("Email must be verified before performing host actions");
        }
        if (!user.isHost()) {
            throw new IdInvalidException("User does not have HOST role. Enable host mode first.");
        }
        return user;
    }

    private Set<Amenity> resolveAmenities(Set<Long> amenityIds) throws IdInvalidException {
        if (amenityIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Amenity> amenities = amenityRepository.findByIdIn(amenityIds);
        Set<Long> foundIds = amenities.stream().map(Amenity::getId).collect(Collectors.toSet());
        if (!foundIds.equals(amenityIds)) {
            throw new IdInvalidException("One or more amenity_ids do not exist");
        }
        return new HashSet<>(amenities);
    }
}
