package com.example.roomly.data.local;

import android.content.Context;
import android.content.res.Resources;

import com.example.roomly.data.model.RoomCard;
import com.example.roomly.data.model.RoomImage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Chuyển RoomCard thành JSON để lưu SQLite.
 * Không sử dụng JSON cache để xác nhận quyền hoặc đặt thuê.
 */
public final class RoomCardJsonCodec {

    private static final int FORMAT_VERSION = 1;

    private RoomCardJsonCodec() {
    }

    public static String encode(Context context, RoomCard room) {
        if (room == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu phòng.");
        }

        try {
            JSONObject json = new JSONObject();

            json.put("version", FORMAT_VERSION);
            json.put("listingId", room.getListingId());
            json.put("roomId", room.getRoomId());
            json.put("ownerId", room.getOwnerId());
            json.put("unitCode", room.getUnitCode());

            json.put("title", room.getTitle());
            json.put("price", room.getPrice());
            json.put("address", room.getAddress());
            json.put("amenities", room.getAmenities());
            json.put("roomType", room.getRoomType().name());

            // Lưu tiền dưới dạng chuỗi để giữ chính xác số nguyên.
            json.put(
                    "monthlyRent",
                    String.valueOf(room.getMonthlyRent())
            );

            json.put(
                    "depositVnd",
                    room.getDepositVnd() == null
                            ? JSONObject.NULL
                            : String.valueOf(room.getDepositVnd())
            );

            json.put(
                    "area",
                    room.getArea() == null
                            ? JSONObject.NULL
                            : room.getArea().toPlainString()
            );

            json.put("description", room.getDescription());
            json.put("hostName", room.getHostName());
            json.put("availability", room.getAvailability().name());
            json.put("imageUri", room.getImageUri());

            // Lưu tên resource, tránh phụ thuộc ID số qua các bản build.
            json.put(
                    "imageResource",
                    getResourceName(context, room.getImageResId())
            );

            JSONArray fees = new JSONArray();

            for (RoomCard.Fee fee : room.getFees()) {
                JSONObject item = new JSONObject();
                item.put("name", fee.name);
                item.put("amountVnd", String.valueOf(fee.amountVnd));
                item.put("unit", fee.unit);
                fees.put(item);
            }

            json.put("fees", fees);

            JSONArray images = new JSONArray();

            for (RoomImage image : room.getImages()) {
                JSONObject item = new JSONObject();
                item.put("description", image.getDescription());

                if (image.isDrawable()) {
                    item.put("type", "drawable");
                    item.put(
                            "resource",
                            getResourceName(
                                    context,
                                    image.getDrawableResId()
                            )
                    );
                } else {
                    item.put("type", "uri");
                    item.put("uri", image.getUri());
                }

                images.put(item);
            }

            json.put("images", images);

            // Không cache trạng thái yêu thích tại đây.
            // Danh sách yêu thích sẽ do nguồn dữ liệu riêng quản lý.
            return json.toString();

        } catch (JSONException exception) {
            throw new IllegalArgumentException(
                    "Không thể chuyển dữ liệu phòng sang JSON.",
                    exception
            );
        }
    }

    public static RoomCard decode(Context context, String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            throw new IllegalArgumentException("Dữ liệu cache trống.");
        }

        try {
            JSONObject json = new JSONObject(payload);

            if (json.getInt("version") != FORMAT_VERSION) {
                throw new IllegalArgumentException(
                        "Phiên bản dữ liệu cache không được hỗ trợ."
                );
            }

            String listingId = requireText(json, "listingId");
            String roomId = requireText(json, "roomId");

            RoomCard.RoomType roomType = RoomCard.RoomType.valueOf(
                    json.getString("roomType")
            );

            RoomCard room = new RoomCard(
                    json.getString("title"),
                    json.getString("price"),
                    json.getString("address"),
                    json.getString("amenities"),
                    resolveResource(
                            context,
                            json.optString("imageResource", "")
                    ),
                    false,
                    roomType,
                    roomId,
                    json.getString("ownerId"),
                    json.getString("unitCode")
            );

            room.setListingId(listingId);
            room.setImageUri(json.optString("imageUri", ""));
            room.setDescription(json.getString("description"));
            room.setHostName(json.getString("hostName"));

            long monthlyRent = Long.parseLong(
                    json.getString("monthlyRent")
            );

            if (monthlyRent < 0) {
                throw new IllegalArgumentException(
                        "Giá thuê trong cache không hợp lệ."
                );
            }

            room.setMonthlyRent(monthlyRent);

            room.setDepositVnd(
                    json.isNull("depositVnd")
                            ? null
                            : Long.parseLong(json.getString("depositVnd"))
            );

            if (!json.isNull("area")) {
                BigDecimal area = new BigDecimal(
                        json.getString("area")
                );

                if (area.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException(
                            "Diện tích trong cache không hợp lệ."
                    );
                }

                room.setArea(area);
            }

            room.setAvailability(
                    readAvailability(
                            json.optString("availability", "UNKNOWN")
                    )
            );

            List<RoomCard.Fee> fees = new ArrayList<>();
            JSONArray feeItems = json.getJSONArray("fees");

            for (int index = 0; index < feeItems.length(); index++) {
                JSONObject item = feeItems.getJSONObject(index);

                fees.add(new RoomCard.Fee(
                        item.getString("name"),
                        Long.parseLong(item.getString("amountVnd")),
                        item.getString("unit")
                ));
            }

            room.setFees(fees);

            List<RoomImage> images = new ArrayList<>();
            JSONArray imageItems = json.getJSONArray("images");

            for (int index = 0; index < imageItems.length(); index++) {
                JSONObject item = imageItems.getJSONObject(index);
                String description = item.optString(
                        "description",
                        "Ảnh phòng"
                );

                String type = item.getString("type");

                if ("drawable".equals(type)) {
                    int resourceId = resolveResource(
                            context,
                            item.optString("resource", "")
                    );

                    // Ảnh đã bị bỏ khỏi project thì bỏ qua.
                    if (resourceId > 0) {
                        images.add(RoomImage.fromDrawable(
                                resourceId,
                                description
                        ));
                    }
                } else if ("uri".equals(type)) {
                    images.add(RoomImage.fromUri(
                            item.getString("uri"),
                            description
                    ));
                } else {
                    throw new IllegalArgumentException(
                            "Loại ảnh trong cache không hợp lệ."
                    );
                }
            }

            room.setImages(images);

            return room;

        } catch (JSONException | IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Không thể đọc dữ liệu phòng đã cache.",
                    exception
            );
        }
    }

    private static String requireText(
            JSONObject json,
            String key
    ) throws JSONException {
        if (json.isNull(key)) {
            throw new IllegalArgumentException("Thiếu " + key + ".");
        }

        String value = json.getString(key).trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException("Thiếu " + key + ".");
        }

        return value;
    }

    private static RoomCard.Availability readAvailability(
            String value
    ) {
        try {
            return RoomCard.Availability.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return RoomCard.Availability.UNKNOWN;
        }
    }

    private static String getResourceName(
            Context context,
            int resourceId
    ) {
        if (resourceId <= 0) {
            return "";
        }

        try {
            return context.getResources().getResourceName(resourceId);
        } catch (Resources.NotFoundException exception) {
            return "";
        }
    }

    private static int resolveResource(
            Context context,
            String resourceName
    ) {
        if (resourceName == null || resourceName.isEmpty()) {
            return 0;
        }

        // Chỉ nhận resource thuộc chính ứng dụng.
        String packagePrefix = context.getPackageName() + ":";

        if (!resourceName.startsWith(packagePrefix)) {
            return 0;
        }

        boolean supportedType =
                resourceName.startsWith(packagePrefix + "drawable/")
                        || resourceName.startsWith(
                        packagePrefix + "mipmap/"
                );

        if (!supportedType) {
            return 0;
        }

        return context.getResources().getIdentifier(
                resourceName,
                null,
                null
        );
    }
}