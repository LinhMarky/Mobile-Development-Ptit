package com.example.roomly.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

/**
 * Quản lý dữ liệu lưu cục bộ của Roomly.
 * Không lưu mật khẩu hoặc token đăng nhập tại đây.
 */
public final class RoomlyDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "roomly_local.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_LISTING_DRAFTS = "listing_drafts";
    public static final String TABLE_MESSAGE_DRAFTS = "message_drafts";
    public static final String TABLE_SEARCH_HISTORY = "search_history";
    public static final String TABLE_VIEWED_ROOMS = "viewed_rooms";

    private static volatile RoomlyDatabaseHelper instance;

    /**
     * Dùng application context để không giữ tham chiếu Activity.
     */
    private RoomlyDatabaseHelper(Context context) {
        super(
                context.getApplicationContext(),
                DATABASE_NAME,
                null,
                DATABASE_VERSION
        );
    }

    public static RoomlyDatabaseHelper getInstance(
            @NonNull Context context
    ) {
        if (instance == null) {
            synchronized (RoomlyDatabaseHelper.class) {
                if (instance == null) {
                    instance = new RoomlyDatabaseHelper(context);
                }
            }
        }

        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createListingDraftsTable(db);
        createMessageDraftsTable(db);
        createSearchHistoryTable(db);
        createViewedRoomsTable(db);
    }

    /**
     * Mỗi tài khoản có một bản đang soạn cho từng phòng.
     *
     * Giá và tiền cọc lưu dạng TEXT vì bản nháp có thể
     * chưa nhập xong hoặc chưa hợp lệ.
     *
     * Đây là bản soạn trên điện thoại, không phải trạng thái
     * DRAFT chính thức của bài đăng trên backend.
     */
    private void createListingDraftsTable(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE_LISTING_DRAFTS + " ("
                        + "user_id TEXT NOT NULL, "
                        + "room_id TEXT NOT NULL, "
                        + "title TEXT NOT NULL DEFAULT '', "
                        + "price_text TEXT NOT NULL DEFAULT '', "
                        + "deposit_text TEXT NOT NULL DEFAULT '', "
                        + "description TEXT NOT NULL DEFAULT '', "
                        + "updated_at INTEGER NOT NULL, "
                        + "PRIMARY KEY (user_id, room_id)"
                        + ")"
        );
    }

    /**
     * Mỗi tài khoản có một bản nháp cho từng hội thoại.
     */
    private void createMessageDraftsTable(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE_MESSAGE_DRAFTS + " ("
                        + "user_id TEXT NOT NULL, "
                        + "conversation_id TEXT NOT NULL, "
                        + "content TEXT NOT NULL DEFAULT '', "
                        + "updated_at INTEGER NOT NULL, "
                        + "PRIMARY KEY (user_id, conversation_id)"
                        + ")"
        );
    }

    /**
     * keyword_key dùng để tránh lưu trùng từ khóa.
     * keyword giữ nguyên nội dung để hiển thị.
     */
    private void createSearchHistoryTable(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE_SEARCH_HISTORY + " ("
                        + "user_id TEXT NOT NULL, "
                        + "keyword_key TEXT NOT NULL, "
                        + "keyword TEXT NOT NULL, "
                        + "searched_at INTEGER NOT NULL, "
                        + "PRIMARY KEY (user_id, keyword_key)"
                        + ")"
        );

        db.execSQL(
                "CREATE INDEX idx_search_history_recent ON "
                        + TABLE_SEARCH_HISTORY
                        + " (user_id, searched_at DESC)"
        );
    }

    /**
     * Lưu bản sao thông tin phòng đã xem.
     *
     * payload_json chứa thông tin phục vụ hiển thị,
     * sẽ được định nghĩa ở bước làm cache phòng.
     *
     * Dùng listing_id để phân biệt bài đăng của phòng.
     * Dữ liệu này không xác nhận phòng còn trống.
     */
    private void createViewedRoomsTable(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE_VIEWED_ROOMS + " ("
                        + "user_id TEXT NOT NULL, "
                        + "listing_id TEXT NOT NULL, "
                        + "room_id TEXT NOT NULL, "
                        + "payload_json TEXT NOT NULL, "
                        + "cached_at INTEGER NOT NULL, "
                        + "viewed_at INTEGER NOT NULL, "
                        + "PRIMARY KEY (user_id, listing_id)"
                        + ")"
        );

        db.execSQL(
                "CREATE INDEX idx_viewed_rooms_recent ON "
                        + TABLE_VIEWED_ROOMS
                        + " (user_id, viewed_at DESC)"
        );
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion
    ) {
        /*
         * Hiện mới có phiên bản 1.
         * Khi thay đổi cấu trúc, sẽ bổ sung migration tại đây
         * trước khi tăng DATABASE_VERSION.
         *
         * Không xóa bảng rồi tạo lại vì sẽ mất bản nháp.
         */
        throw new IllegalStateException(
                "Chưa có migration SQLite từ phiên bản "
                        + oldVersion + " lên " + newVersion
        );
    }
}