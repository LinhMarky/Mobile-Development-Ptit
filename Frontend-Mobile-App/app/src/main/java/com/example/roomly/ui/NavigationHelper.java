package com.example.roomly.ui;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.example.roomly.data.repository.SessionRepository;

import java.util.Objects;

/** Chờ màn hình hoạt động trở lại trước khi điều hướng từ kết quả API. */
public final class NavigationHelper {
    private NavigationHelper() {}

    public static void whenResumed(Fragment fragment, Runnable action) {
        LifecycleOwner viewOwner = fragment.getViewLifecycleOwnerLiveData().getValue();
        if (viewOwner == null) {
            return;
        }
        String requestingUser = SessionRepository.getInstance().getCurrentSession().getUserId();

        viewOwner
                .getLifecycle()
                .addObserver(
                        new DefaultLifecycleObserver() {
                            private boolean completed;

                            @Override
                            public void onResume(@NonNull LifecycleOwner owner) {
                                View view = fragment.getView();
                                if (view != null) {
                                    // FragmentManager có thể vẫn đang cập nhật vòng đời của các màn
                                    // hình.
                                    view.post(this::navigate);
                                }
                            }

                            private void navigate() {
                                if (completed
                                        || fragment.getViewLifecycleOwnerLiveData().getValue()
                                                != viewOwner) {
                                    return;
                                }
                                if (!Objects.equals(
                                        requestingUser,
                                        SessionRepository.getInstance()
                                                .getCurrentSession()
                                                .getUserId())) {
                                    completed = true;
                                    viewOwner.getLifecycle().removeObserver(this);
                                    return;
                                }
                                if (!viewOwner
                                                .getLifecycle()
                                                .getCurrentState()
                                                .isAtLeast(Lifecycle.State.RESUMED)
                                        || !fragment.isAdded()
                                        || fragment.getParentFragmentManager().isStateSaved()) {
                                    return;
                                }

                                completed = true;
                                viewOwner.getLifecycle().removeObserver(this);
                                action.run();
                            }

                            @Override
                            public void onDestroy(@NonNull LifecycleOwner owner) {
                                completed = true;
                                owner.getLifecycle().removeObserver(this);
                            }
                        });
    }
}
