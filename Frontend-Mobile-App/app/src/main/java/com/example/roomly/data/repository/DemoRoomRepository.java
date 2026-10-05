package com.example.roomly.data.repository;

import com.example.roomly.R;
import com.example.roomly.data.model.RoomCard;

import java.util.ArrayList;
import java.util.List;

public class DemoRoomRepository {

    // Một repository dùng chung trong suốt phiên chạy app.
    private static final DemoRoomRepository INSTANCE =
            new DemoRoomRepository();

    // Các màn hình sử dụng chung những đối tượng RoomCard này.
    private final List<RoomCard> rooms = new ArrayList<>();

    /**
     * Khởi tạo danh sách phòng mẫu một lần.
     * Constructor private ngăn các màn hình tự tạo repository riêng.
     */
    private DemoRoomRepository() {
        rooms.add(new RoomCard(
                "Phòng studio sáng thoáng, có ban công",
                "3.500.000 ₫/tháng",
                "Mộ Lao, Hà Đông, Hà Nội",
                "28 m²  •  Có nội thất  •  Wi-Fi",
                R.drawable.room_demo_01,
                false,
                RoomCard.RoomType.STUDIO
        ));

        rooms.add(new RoomCard(
                "Phòng trọ đầy đủ nội thất, gần trường",
                "2.800.000 ₫/tháng",
                "Trần Phú, Hà Đông, Hà Nội",
                "22 m²  •  Điều hòa  •  Chỗ để xe",
                R.drawable.room_demo_01,
                false,
                RoomCard.RoomType.ROOM
        ));

        rooms.add(new RoomCard(
                "Căn hộ mini rộng rãi, có bếp riêng",
                "4.200.000 ₫/tháng",
                "Thanh Xuân, Hà Nội",
                "35 m²  •  Bếp riêng  •  Có nội thất",
                R.drawable.room_demo_01,
                false,
                RoomCard.RoomType.APARTMENT
        ));

        // Phòng studio: 3.500.000 đồng/tháng, diện tích 28 m².
        rooms.get(0).setMonthlyRent(3_500_000L);
        rooms.get(0).setAreaSquareMeters(28);

// Phòng trọ: 2.800.000 đồng/tháng, diện tích 22 m².
        rooms.get(1).setMonthlyRent(2_800_000L);
        rooms.get(1).setAreaSquareMeters(22);

// Căn hộ mini: 4.200.000 đồng/tháng, diện tích 35 m².
        rooms.get(2).setMonthlyRent(4_200_000L);
        rooms.get(2).setAreaSquareMeters(35);
    }

    /**
     * Trả về repository dùng chung cho Khám phá và Đã lưu.
     */
    public static DemoRoomRepository getInstance() {
        return INSTANCE;
    }

    /**
     * Trả về bản sao danh sách chứa toàn bộ phòng.
     * Các đối tượng phòng bên trong vẫn được dùng chung,
     * nên trạng thái lưu được giữ khi chuyển màn hình.
     */
    public List<RoomCard> getRooms() {
        return new ArrayList<>(rooms);
    }

    /**
     * Lọc và trả về những phòng đang có trạng thái đã lưu.
     */
    public List<RoomCard> getSavedRooms() {
        List<RoomCard> savedRooms = new ArrayList<>();

        for (RoomCard room : rooms) {
            if (room.isSaved()) {
                savedRooms.add(room);
            }
        }

        return savedRooms;
    }
}