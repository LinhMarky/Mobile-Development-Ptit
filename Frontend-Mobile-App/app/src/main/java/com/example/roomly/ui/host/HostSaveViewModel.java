package com.example.roomly.ui.host;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

/** Giữ kết quả lưu qua xoay màn hình; không giữ Fragment, View hoặc Binding. */
public class HostSaveViewModel extends ViewModel {
    public static final String IDLE = "IDLE";
    public static final String SAVING = "SAVING";
    public static final String SAVED = "SAVED";
    public static final String ERROR = "ERROR";
    public static final String UNCERTAIN = "UNCERTAIN";

    private final SavedStateHandle state;

    public HostSaveViewModel(SavedStateHandle state) {
        this.state = state;
        if (SAVING.equals(state.get("save_phase"))) {
            // Process cũ đã mất. Không biết server đã commit hay chưa nên không gửi lại.
            state.set("save_message", "Yêu cầu trước bị gián đoạn. Quay lại danh sách để kiểm tra kết quả trước khi lưu lại.");
            state.set("save_phase", UNCERTAIN);
        }
    }

    public LiveData<String> phase() {
        return state.getLiveData("save_phase", IDLE);
    }

    public String currentPhase() {
        String phase = state.get("save_phase");
        return phase == null ? IDLE : phase;
    }

    public boolean blocksNewSave() {
        return SAVING.equals(currentPhase()) || SAVED.equals(currentPhase()) || UNCERTAIN.equals(currentPhase());
    }

    public boolean begin(String userId) {
        if (blocksNewSave() || userId == null || userId.isEmpty()) return false;
        state.set("save_owner", userId);
        state.set("save_message", "");
        state.set("save_phase", SAVING);
        return true;
    }

    public void saved(String message) {
        if (!SAVING.equals(currentPhase())) return;
        state.set("save_message", message);
        state.set("save_phase", SAVED);
    }

    public void failed(String message) {
        if (!SAVING.equals(currentPhase())) return;
        state.set("save_message", message);
        state.set("save_phase", ERROR);
    }

    public String message() {
        String message = state.get("save_message");
        return message == null ? "" : message;
    }

    public void listingSaved() {
        saved("Đã lưu bài đăng. Mở danh sách để xem trạng thái mới.");
    }

    public String consumeSuccess(String userId) {
        if (!SAVED.equals(currentPhase()) || userId == null || !userId.equals(state.get("save_owner"))) return null;
        String result = message();
        state.set("save_phase", IDLE);
        return result;
    }
}
