package com.example.roomly.data.repository;

import com.example.roomly.BuildConfig;
import com.example.roomly.R;
import com.example.roomly.data.model.RoomCard;
import com.example.roomly.data.model.RoomImage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dữ liệu mẫu để kiểm tra giao diện.
 * Chỉ tạo phòng minh họa trong bản debug.
 */
public class DemoRoomRepository {

    private static final DemoRoomRepository INSTANCE =
            new DemoRoomRepository();

    private final List<RoomCard> rooms = new ArrayList<>();

    private final Map<String, RoomCard> publishedCards =
            new HashMap<>();

    private DemoRoomRepository() {
        if (!BuildConfig.DEBUG) {
            return;
        }

        // Phòng 1: còn trống, có 3 ảnh mẫu.
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
        studio.setDepositVnd(3_500_000L);
        studio.setAvailability(RoomCard.Availability.AVAILABLE);

        studio.setFees(Arrays.asList(
                new RoomCard.Fee(
                        "Tiền điện",
                        3_500L,
                        "kWh"
                ),
                new RoomCard.Fee(
                        "Tiền nước",
                        100_000L,
                        "người/tháng"
                ),
                new RoomCard.Fee(
                        "Internet",
                        100_000L,
                        "phòng/tháng"
                ),
                new RoomCard.Fee(
                        "Gửi xe",
                        80_000L,
                        "xe/tháng"
                )
        ));

        // Lặp ảnh có sẵn để kiểm tra thao tác vuốt.
        studio.setImages(Arrays.asList(
                RoomImage.fromDrawable(
                        R.drawable.room_demo_01,
                        "Ảnh minh họa studio số 1"
                ),
                RoomImage.fromDrawable(
                        R.drawable.room_demo_01,
                        "Ảnh minh họa studio số 2"
                ),
                RoomImage.fromDrawable(
                        R.drawable.room_demo_01,
                        "Ảnh minh họa studio số 3"
                )
        ));

        rooms.add(studio);

        // Phòng 2: đang giữ, có 2 ảnh mẫu.
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
        room.setDepositVnd(0L);
        room.setAvailability(RoomCard.Availability.HELD);

        room.setFees(Arrays.asList(
                new RoomCard.Fee(
                        "Internet",
                        0L,
                        "phòng/tháng"
                )
        ));

        room.setImages(Arrays.asList(
                RoomImage.fromDrawable(
                        R.drawable.room_demo_01,
                        "Ảnh minh họa phòng trọ số 1"
                ),
                RoomImage.fromDrawable(
                        R.drawable.room_demo_01,
                        "Ảnh minh họa phòng trọ số 2"
                )
        ));

        rooms.add(room);

        // Phòng 3: đã thuê, dùng ảnh đơn dự phòng.
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
        apartment.setAvailability(RoomCard.Availability.RENTED);

        rooms.add(apartment);
    }

    public static DemoRoomRepository getInstance() {
        return INSTANCE;
    }

    /**
     * Ghép phòng mẫu với bài đăng đang công khai.
     * Giữ trạng thái đã lưu khi cập nhật thẻ phòng.
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