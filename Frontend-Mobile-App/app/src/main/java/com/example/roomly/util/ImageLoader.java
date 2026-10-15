package com.example.roomly.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.LruCache;
import android.widget.ImageView;

import com.example.roomly.data.api.ApiClient;

public final class ImageLoader {
    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<>(8192) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return bitmap.getByteCount() / 1024;
                }
            };

    private ImageLoader() {}

    public static void load(ImageView view, String source, int placeholder) {
        view.setImageResource(placeholder);
        String path = source == null ? "" : source.trim();
        String key = ApiClient.cachedUser().optString("id") + ":" + path;
        view.setTag(key);
        if (path.isEmpty()) return;
        if (!path.startsWith("http") && !path.startsWith("/api/")) {
            try {
                view.setImageURI(Uri.parse(path));
            } catch (RuntimeException ignored) {
                view.setImageResource(placeholder);
            }
            return;
        }
        Bitmap cached = CACHE.get(key);
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }
        ApiClient.execute(
                () -> {
                    byte[] bytes = ApiClient.imageBytes(path);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                    options.inSampleSize = 1;
                    while (options.outWidth / options.inSampleSize > 1200
                            || options.outHeight / options.inSampleSize > 1200)
                        options.inSampleSize *= 2;
                    options.inJustDecodeBounds = false;
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                    if (bitmap == null) throw new IllegalArgumentException("Ảnh không hợp lệ.");
                    CACHE.put(key, bitmap);
                    return bitmap;
                },
                bitmap -> {
                    if (key.equals(view.getTag())) view.setImageBitmap(bitmap);
                },
                text -> {});
    }
}
