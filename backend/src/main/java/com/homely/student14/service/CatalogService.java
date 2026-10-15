package com.homely.student14.service;

import static com.homely.student14.repository.DatabaseRepository.fail;
import static com.homely.student14.repository.DatabaseRepository.flag;
import static com.homely.student14.repository.DatabaseRepository.id;

import com.homely.student14.dto.request.catalog.ListingActionDTO;
import com.homely.student14.dto.request.catalog.ListingFeeDTO;
import com.homely.student14.dto.request.catalog.SaveListingDTO;
import com.homely.student14.dto.request.catalog.SaveRoomDTO;
import com.homely.student14.entity.ListingStatus;
import com.homely.student14.repository.CatalogRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final CatalogRepository catalogRepository;
    private final AuthService authService;

    // AdminService dùng chung danh sách cột để trả cùng cấu trúc tin đăng.
    public static final String SELECT = CatalogRepository.LISTING_SELECT;

    public List<Map<String, Object>> listings() {
        return this.catalogRepository.findPublishedListings().stream().map(this::decorate).toList();
    }

    public Map<String, Object> listing(long listingId, Jwt jwt) {
        Map<String, Object> listing = this.catalogRepository.findListingById(listingId);
        Object expiresAt = listing.get("expires_at");
        boolean expired =
                expiresAt != null && !Instant.parse(expiresAt.toString()).isAfter(Instant.now());

        if (!"PUBLISHED".equals(listing.get("status")) || expired) {
            Map<String, Object> currentUser = this.authService.user(jwt);
            boolean isOwner = id(currentUser.get("id")) == id(listing.get("host_id"));
            boolean isAdmin = flag(currentUser.get("is_admin"));

            if (!isOwner && !isAdmin) {
                throw fail(403, "Không được xem tin này.");
            }
        }

        return decorate(listing);
    }

    public Map<String, Object> decorate(Map<String, Object> listing) {
        long roomId = id(listing.get("room_id"));
        listing.put("fees", this.catalogRepository.parseJson(listing.get("fees")));
        listing.put("amenity_ids", this.catalogRepository.parseJson(listing.get("amenity_ids")));
        listing.put("images", this.catalogRepository.findRoomImages(roomId));
        return listing;
    }

    public List<Map<String, Object>> rooms(Jwt jwt) {
        Map<String, Object> currentHost = this.authService.role(jwt, "host");
        long hostId = id(currentHost.get("id"));

        return this.catalogRepository.findRoomsByHostId(hostId).stream()
                .map(this::addRoomImage)
                .toList();
    }

    public Map<String, Object> ownedRoom(long roomId, Jwt jwt, boolean lock) {
        Map<String, Object> currentHost = this.authService.role(jwt, "host");
        Map<String, Object> room;

        if (lock) {
            room = this.catalogRepository.findRoomByIdForUpdate(roomId);
        } else {
            room = this.catalogRepository.findRoomById(roomId);
        }

        if (id(room.get("host_id")) != id(currentHost.get("id"))) {
            throw fail(403, "Phòng này thuộc chủ nhà khác.");
        }

        return addRoomImage(room);
    }

    private Map<String, Object> addRoomImage(Map<String, Object> room) {
        List<Map<String, Object>> images =
                this.catalogRepository.findRoomImages(id(room.get("id")));

        if (images.isEmpty()) {
            room.put("image_url", "");
        } else {
            room.put("image_url", images.get(0).get("url"));
        }

        return room;
    }

    @Transactional
    public Map<String, Object> createRoom(Jwt jwt, SaveRoomDTO request) {
        Map<String, Object> currentHost = this.authService.role(jwt, "host");
        long hostId = id(currentHost.get("id"));
        prepareRoomRequest(request);
        request.setUnitCode(trimText(request.getUnitCode()));

        if (request.getUnitCode().isEmpty()) {
            throw fail(400, "Vui lòng nhập mã phòng.");
        }

        long roomId = this.catalogRepository.createRoom(hostId, request);
        return this.catalogRepository.findRoomById(roomId);
    }

    @Transactional
    public Map<String, Object> updateRoom(long roomId, Jwt jwt, SaveRoomDTO request) {
        Map<String, Object> room = ownedRoom(roomId, jwt, true);
        if (!"AVAILABLE".equals(room.get("availability"))) {
            throw fail(409, "Chỉ sửa phòng đang trống.");
        }

        prepareRoomRequest(request);
        this.catalogRepository.updateRoom(roomId, request);
        return this.catalogRepository.findRoomById(roomId);
    }

    private void prepareRoomRequest(SaveRoomDTO request) {
        request.setName(trimText(request.getName()));
        request.setDescription(trimText(request.getDescription()));
        request.setRoomType(trimText(request.getRoomType()));
        request.setAddressLine(trimText(request.getAddressLine()));
        validateAmenities(request.getAmenityIds());
    }

    private void validateAmenities(List<Long> amenityIds) {
        Set<Long> selectedAmenities = new HashSet<>();

        for (Long amenityId : amenityIds) {
            if (amenityId == null
                    || amenityId < 1
                    || amenityId > 15
                    || !selectedAmenities.add(amenityId)) {
                throw fail(400, "Tiện ích không hợp lệ hoặc trùng.");
            }
        }
    }

    @Transactional
    public Map<String, Object> saveListing(long roomId, Jwt jwt, SaveListingDTO request) {
        Map<String, Object> room = ownedRoom(roomId, jwt, true);
        request.setTitle(trimText(request.getTitle()));
        request.setDescription(trimText(request.getDescription()));
        request.setRentVnd(money(request.getRentVnd()));
        request.setDepositVnd(money(request.getDepositVnd()));
        validateFees(request.getFees());

        List<Map<String, Object>> existingListings =
                this.catalogRepository.findRoomListingsForUpdate(roomId);
        long listingId;

        if (existingListings.isEmpty()) {
            listingId = this.catalogRepository.createListing(roomId, request);
        } else {
            Map<String, Object> currentListing = existingListings.get(0);
            ListingStatus status = ListingStatus.valueOf(currentListing.get("status").toString());
            boolean isRoomAvailable = "AVAILABLE".equals(room.get("availability"));

            if (!status.canHostEdit() || !isRoomAvailable) {
                throw fail(409, "Trạng thái tin hoặc phòng không cho phép chỉnh sửa.");
            }

            listingId = id(currentListing.get("id"));
            this.catalogRepository.updateListing(listingId, request);
        }

        return listing(listingId, jwt);
    }

    private void validateFees(List<ListingFeeDTO> fees) {
        Set<String> selectedFeeCodes = new HashSet<>();

        for (ListingFeeDTO fee : fees) {
            if (!selectedFeeCodes.add(fee.getFeeCode())) {
                throw fail(400, "Mã phụ phí không hợp lệ hoặc trùng.");
            }

            fee.setUnitName(trimText(fee.getUnitName()));
            if ("PER_UNIT".equals(fee.getFeeMode()) && fee.getUnitName().isEmpty()) {
                throw fail(400, "Phụ phí theo đơn vị cần có đơn vị tính.");
            }
            fee.setAmountVnd(money(fee.getAmountVnd()));
        }
    }

    @Transactional
    public Map<String, Object> listingAction(
            long listingId, Jwt jwt, String action, ListingActionDTO request) {
        Map<String, Object> currentListing =
                this.catalogRepository.findListingByIdForUpdate(listingId);
        Map<String, Object> currentUser = this.authService.user(jwt);
        ListingStatus status = ListingStatus.valueOf(currentListing.get("status").toString());
        String nextStatus;

        if (Set.of("approve", "reject", "suspend").contains(action)) {
            if (!flag(currentUser.get("is_admin"))) {
                throw fail(403, "Chỉ admin được duyệt tin.");
            }

            if (!"suspend".equals(action)
                    && !"PENDING_REVIEW".equals(currentListing.get("status"))) {
                throw fail(409, "Tin không ở trạng thái chờ duyệt.");
            }
            if ("suspend".equals(action) && !status.canAdminSuspend()) {
                throw fail(409, "Chỉ tạm ngưng tin đang công khai hoặc được chủ trọ tự ẩn.");
            }

            nextStatus =
                    switch (action) {
                        case "approve" -> "PUBLISHED";
                        case "reject" -> "REJECTED";
                        default -> "SUSPENDED";
                    };
        } else {
            if (id(currentUser.get("id")) != id(currentListing.get("host_id"))) {
                throw fail(403, "Tin thuộc chủ nhà khác.");
            }

            if (!Set.of("submit", "hide").contains(action)) {
                throw fail(400, "Thao tác không hợp lệ.");
            }

            if ("submit".equals(action) && !status.canHostSubmit()) {
                throw fail(409, "Không thể gửi duyệt từ trạng thái này.");
            }
            if ("hide".equals(action) && !status.canHostHide()) {
                throw fail(409, "Chỉ tự ẩn tin đang công khai; không thay đổi tin bị admin tạm ngưng.");
            }

            if ("submit".equals(action)) {
                nextStatus = "PENDING_REVIEW";
            } else {
                nextStatus = "HIDDEN";
            }
        }

        String reason = trimText(request.getReason());
        if (Set.of("reject", "suspend").contains(action) && reason.isEmpty()) {
            throw fail(400, "Vui lòng nhập lý do từ chối hoặc tạm ẩn tin.");
        }

        this.catalogRepository.updateListingStatus(listingId, nextStatus, reason);
        this.catalogRepository.notifyListingStatus(
                id(currentListing.get("host_id")), listingId, nextStatus, reason);
        return listing(listingId, jwt);
    }

    public List<Map<String, Object>> myListings(Jwt jwt) {
        Map<String, Object> currentHost = this.authService.role(jwt, "host");
        return this.catalogRepository.findListingsByHostId(id(currentHost.get("id"))).stream()
                .map(this::decorate)
                .toList();
    }

    public List<Map<String, Object>> wishlist(Jwt jwt) {
        Map<String, Object> currentTenant = this.authService.role(jwt, "tenant");
        return this.catalogRepository.findWishlistByUserId(id(currentTenant.get("id"))).stream()
                .map(this::decorate)
                .toList();
    }

    @Transactional
    public void saveWishlist(long roomId, Jwt jwt, boolean saved) {
        Map<String, Object> currentTenant = this.authService.role(jwt, "tenant");
        long tenantId = id(currentTenant.get("id"));
        this.catalogRepository.findRoomById(roomId);

        if (saved) {
            this.catalogRepository.saveWishlist(tenantId, roomId);
        } else {
            this.catalogRepository.deleteWishlist(tenantId, roomId);
        }
    }

    public List<Map<String, Object>> reviews(long roomId) {
        return this.catalogRepository.findVisibleReviewsByRoomId(roomId);
    }

    public Object amenities() {
        return this.catalogRepository.findAmenities();
    }

    private String trimText(String value) {
        return value == null ? "" : value.trim();
    }

    static BigDecimal money(Object amount) {
        try {
            BigDecimal value = new BigDecimal(Objects.toString(amount, "0"))
                    .setScale(0, RoundingMode.UNNECESSARY);
            if (value.signum() < 0 || value.precision() > 18) {
                throw fail(400, "Tiền phải không âm và tối đa 18 chữ số.");
            }
            return value;
        } catch (ArithmeticException | NumberFormatException exception) {
            throw fail(400, "Tiền phải là số nguyên VND.");
        }
    }
}
