package com.example.roomly.data.repository;

import com.example.roomly.BuildConfig;
import com.example.roomly.R;
import com.example.roomly.data.model.RoomCard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cung cấp phòng minh họa và bài công khai cho Khám phá, Đã lưu.
 * Làm mới thông tin thẻ mà giữ trạng thái trái tim trong bộ nhớ.
 */
public class DemoRoomRepository {

    private static final DemoRoomRepository INSTANCE =
            new DemoRoomRepository();

    private final List<RoomCard> rooms = new ArrayList<>();

    private final Map<String, RoomCard> publishedCards = new HashMap<>();

    /** Tạo phòng minh họa chỉ trong bản debug. */
    private DemoRoomRepository() {
        if (!BuildConfig.DEBUG) {
            return;
        }

        RoomCard studio = new RoomCard(
                "Phòng studio sáng thoáng, có ban công",
                "3.500.000 ₫/tháng",
                "Mộ Lao, Hà Đông, Hà Nội",
                "28 m²  •  Có nội thất  •  Wi-Fi",
                R.drawable.room_demo_01,
                false,
                RoomCard.RoomType.STUDIO
        );

        studio.setMonthlyRent(3_500_000L);
        studio.setAreaSquareMeters(28);
        studio.setDescription(
                "Phòng minh họa để thử giao diện: studio sáng thoáng, "
                        + "có ban công, nội thất và Wi-Fi."
        );
        studio.setHostName("Chủ trọ minh họa");
        rooms.add(studio);

        RoomCard room = new RoomCard(
                "Phòng trọ đầy đủ nội thất, gần trường",
                "2.800.000 ₫/tháng",
                "Trần Phú, Hà Đông, Hà Nội",
                "22 m²  •  Điều hòa  •  Chỗ để xe",
                R.drawable.room_demo_01,
                false,
                RoomCard.RoomType.ROOM
        );

        room.setMonthlyRent(2_800_000L);
        room.setAreaSquareMeters(22);
        room.setDescription(
                "Phòng minh họa để thử giao diện: phòng trọ gần trường, "
                        + "có nội thất, điều hòa và chỗ để xe."
        );
        room.setHostName("Chủ trọ minh họa");
        rooms.add(room);

        RoomCard apartment = new RoomCard(
                "Căn hộ mini rộng rãi, có bếp riêng",
                "4.200.000 ₫/tháng",
                "Thanh Xuân, Hà Nội",
                "35 m²  •  Bếp riêng  •  Có nội thất",
                R.drawable.room_demo_01,
                false,
                RoomCard.RoomType.APARTMENT
        );

        apartment.setMonthlyRent(4_200_000L);
        apartment.setAreaSquareMeters(35);
        apartment.setDescription(
                "Phòng minh họa để thử giao diện: căn hộ mini "
                        + "có bếp riêng và nội thất."
        );
        apartment.setHostName("Chủ trọ minh họa");
        rooms.add(apartment);
    }

    /** Trả về repository dùng chung. */
    public static DemoRoomRepository getInstance() {
        return INSTANCE;
    }

    /**
     * Ghép phòng minh họa với các bài đang công khai.
     * Cập nhật thông tin mới trên cùng đối tượng để giữ trái tim.
     */
    public List<RoomCard> getRooms() {
        List<RoomCard> results = new ArrayList<>(rooms);

        List<RoomCard> published =
                DemoAdminListingRepository.getInstance()
                        .getPublishedRoomCards();

        for (RoomCard incoming : published) {
            String listingId = incoming.getListingId();
            RoomCard existing = publishedCards.get(listingId);

            if (existing == null) {
                existing = incoming;
                publishedCards.put(listingId, existing);
            } else {
                existing.updateDisplayFrom(incoming);
            }

            results.add(existing);
        }

        return results;
    }

    /** Lấy những thẻ đã lưu và hiện vẫn được phép công khai. */
    public List<RoomCard> getSavedRooms() {
        List<RoomCard> results = new ArrayList<>();

        for (RoomCard room : getRooms()) {
            if (room.isSaved()) {
                results.add(room);
            }
        }

        return results;
    }
}