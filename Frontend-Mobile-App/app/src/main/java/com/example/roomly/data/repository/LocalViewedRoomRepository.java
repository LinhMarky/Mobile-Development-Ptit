package com.example.roomly.data.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.example.roomly.data.local.RoomlyDatabaseHelper;
import com.example.roomly.data.model.ViewedRoomCache;

import java.util.ArrayList;
import java.util.List;

/**
 * Lưu bản sao thông tin phòng đã xem theo từng tài khoản.
 * Gọi các thao tác đọc/ghi trên luồng nền.
 */
public final class LocalViewedRoomRepository {

    private static final int MAX_CACHED_ROOMS = 30;

    private static final String[] COLUMNS = {
            "user_id",
            "listing_id",
            "room_id",
            "payload_json",
            "cached_at",
            "viewed_at"
    };

    private final RoomlyDatabaseHelper databaseHelper;

    public LocalViewedRoomRepository(Context context) {
        databaseHelper = RoomlyDatabaseHelper.getInstance(context);
    }

    /**
     * Lưu thông tin vừa lấy từ nguồn dữ liệu chính.
     * Không dùng hàm này khi chỉ xem lại bản cache.
     *
     * Giữ tối đa 30 phòng đã xem cho mỗi tài khoản.
     */
    @WorkerThread
    public void saveCache(ViewedRoomCache cache) {
        if (cache == null) {
            throw new IllegalArgumentException(
                    "Thiếu dữ liệu cache phòng."
            );
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        db.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put("user_id", cache.getUserId());
            values.put("listing_id", cache.getListingId());
            values.put("room_id", cache.getRoomId());
            values.put("payload_json", cache.getPayloadJson());
            values.put("cached_at", cache.getCachedAtMillis());
            values.put("viewed_at", cache.getViewedAtMillis());

            long result = db.insertWithOnConflict(
                    RoomlyDatabaseHelper.TABLE_VIEWED_ROOMS,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
            );

            if (result == -1) {
                throw new IllegalStateException(
                        "Không thể lưu cache phòng."
                );
            }

            db.execSQL(
                    "DELETE FROM "
                            + RoomlyDatabaseHelper.TABLE_VIEWED_ROOMS
                            + " WHERE user_id = ?"
                            + " AND listing_id NOT IN ("
                            + "SELECT listing_id FROM "
                            + RoomlyDatabaseHelper.TABLE_VIEWED_ROOMS
                            + " WHERE user_id = ?"
                            + " ORDER BY viewed_at DESC, rowid DESC"
                            + " LIMIT ?"
                            + ")",
                    new Object[]{
                            cache.getUserId(),
                            cache.getUserId(),
                            MAX_CACHED_ROOMS
                    }
            );

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Đọc một bài đăng đã cache của đúng tài khoản.
     */
    @Nullable
    @WorkerThread
    public ViewedRoomCache getCache(
            String userId,
            String listingId
    ) {
        String cleanUserId = requireId(userId, "mã tài khoản");
        String cleanListingId = requireId(listingId, "mã bài đăng");

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
                RoomlyDatabaseHelper.TABLE_VIEWED_ROOMS,
                COLUMNS,
                "user_id = ? AND listing_id = ?",
                new String[]{cleanUserId, cleanListingId},
                null,
                null,
                null,
                "1"
        )) {
            return cursor.moveToFirst()
                    ? readCache(cursor)
                    : null;
        }
    }

    /**
     * Đọc danh sách theo lần xem gần nhất.
     * Chưa chuyển JSON thành RoomCard tại lớp repository.
     */
    @WorkerThread
    public List<ViewedRoomCache> getViewedRooms(String userId) {
        String cleanUserId = requireId(userId, "mã tài khoản");

        List<ViewedRoomCache> result = new ArrayList<>();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
                RoomlyDatabaseHelper.TABLE_VIEWED_ROOMS,
                COLUMNS,
                "user_id = ?",
                new String[]{cleanUserId},
                null,
                null,
                "viewed_at DESC, rowid DESC",
                String.valueOf(MAX_CACHED_ROOMS)
        )) {
            while (cursor.moveToNext()) {
                result.add(readCache(cursor));
            }
        }

        return result;
    }

    /**
     * Ghi nhận việc xem lại cache.
     * Không đổi payload hoặc thời điểm cập nhật dữ liệu.
     * Không tự tạo bản ghi nếu cache đã bị xóa.
     */
    @WorkerThread
    public void markViewed(
            String userId,
            String listingId
    ) {
        String cleanUserId = requireId(userId, "mã tài khoản");
        String cleanListingId = requireId(listingId, "mã bài đăng");

        ContentValues values = new ContentValues();
        values.put("viewed_at", System.currentTimeMillis());

        databaseHelper.getWritableDatabase().update(
                RoomlyDatabaseHelper.TABLE_VIEWED_ROOMS,
                values,
                "user_id = ? AND listing_id = ?",
                new String[]{cleanUserId, cleanListingId}
        );
    }

    /**
     * Xóa một phòng đã cache.
     */
    @WorkerThread
    public void deleteCache(
            String userId,
            String listingId
    ) {
        String cleanUserId = requireId(userId, "mã tài khoản");
        String cleanListingId = requireId(listingId, "mã bài đăng");

        databaseHelper.getWritableDatabase().delete(
                RoomlyDatabaseHelper.TABLE_VIEWED_ROOMS,
                "user_id = ? AND listing_id = ?",
                new String[]{cleanUserId, cleanListingId}
        );
    }

    /**
     * Xóa toàn bộ cache phòng của một tài khoản.
     */
    @WorkerThread
    public void clearCache(String userId) {
        String cleanUserId = requireId(userId, "mã tài khoản");

        databaseHelper.getWritableDatabase().delete(
                RoomlyDatabaseHelper.TABLE_VIEWED_ROOMS,
                "user_id = ?",
                new String[]{cleanUserId}
        );
    }

    private static ViewedRoomCache readCache(Cursor cursor) {
        return new ViewedRoomCache(
                cursor.getString(
                        cursor.getColumnIndexOrThrow("user_id")
                ),
                cursor.getString(
                        cursor.getColumnIndexOrThrow("listing_id")
                ),
                cursor.getString(
                        cursor.getColumnIndexOrThrow("room_id")
                ),
                cursor.getString(
                        cursor.getColumnIndexOrThrow("payload_json")
                ),
                cursor.getLong(
                        cursor.getColumnIndexOrThrow("cached_at")
                ),
                cursor.getLong(
                        cursor.getColumnIndexOrThrow("viewed_at")
                )
        );
    }

    private static String requireId(
            String value,
            String fieldName
    ) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Thiếu " + fieldName + "."
            );
        }

        return value.trim();
    }
}