package com.example.roomly.util;

import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public final class KeyboardUtils {

    private KeyboardUtils() {
    }

    public static void hide(View root) {
        if (root == null) {
            return;
        }

        View focused = root.findFocus();

        WindowInsetsControllerCompat controller =
                ViewCompat.getWindowInsetsController(root);

        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.ime());
        }

        InputMethodManager manager = (InputMethodManager)
                root.getContext().getSystemService(
                        Context.INPUT_METHOD_SERVICE
                );

        View tokenView = focused != null ? focused : root;

        if (manager != null && tokenView.getWindowToken() != null) {
            manager.hideSoftInputFromWindow(
                    tokenView.getWindowToken(),
                    0
            );
        }

        if (focused instanceof EditText) {
            focused.clearFocus();

            root.setFocusableInTouchMode(true);
            root.requestFocus();
        }
    }

    public static void setupTapOutside(View root) {
        if (root == null) {
            return;
        }

        attachTouchListeners(root, root);
    }

    private static void attachTouchListeners(View view, View root) {
        // Giữ nguyên thao tác nhập và chọn chữ trong ô nhập.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener((touchedView, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    View focused = root.findFocus();

                    if (focused instanceof EditText) {
                        Rect bounds = new Rect();
                        boolean visible =
                                focused.getGlobalVisibleRect(bounds);

                        boolean inside = visible && bounds.contains(
                                (int) event.getRawX(),
                                (int) event.getRawY()
                        );

                        if (!inside) {
                            hide(root);
                        }
                    }
                }

                // Cho phép nút, danh sách và vùng cuộn hoạt động tiếp.
                return false;
            });
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int index = 0; index < group.getChildCount(); index++) {
                attachTouchListeners(group.getChildAt(index), root);
            }
        }
    }

    public static void clearTapOutside(View root) {
        if (root == null) {
            return;
        }

        if (!(root instanceof EditText)) {
            root.setOnTouchListener(null);
        }

        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;

            for (int index = 0; index < group.getChildCount(); index++) {
                clearTapOutside(group.getChildAt(index));
            }
        }
    }
}