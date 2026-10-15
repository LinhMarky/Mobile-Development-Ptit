package com.example.roomly.ui.explore;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.roomly.R;
import com.example.roomly.data.local.RoomCardJsonCodec;
import com.example.roomly.data.model.RoomCard;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.ViewedRoomCache;
import com.example.roomly.data.repository.LocalViewedRoomRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentViewedRoomsBinding;
import com.example.roomly.ui.detail.RoomDetailFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Hiển thị bản lưu phòng của tài khoản hiện tại.
 * Mọi thao tác SQLite và giải mã JSON chạy trên luồng nền.
 */
public final class ViewedRoomsFragment extends Fragment {

    private static final String TAG = "ROOMLY_VIEWED_ROOMS";

    private static final ExecutorService CACHE_EXECUTOR =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private FragmentViewedRoomsBinding binding;
    private ViewedRoomAdapter adapter;
    private LocalViewedRoomRepository repository;

    private int requestVersion = 0;
    private boolean busy = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        repository = new LocalViewedRoomRepository(
                requireContext().getApplicationContext()
        );
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentViewedRoomsBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ViewedRoomAdapter();

        binding.rvViewedRooms.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );
        binding.rvViewedRooms.setAdapter(adapter);

        adapter.setListener(new ViewedRoomAdapter.Listener() {
            @Override
            public void onOpenRequested(ViewedRoomAdapter.Item item) {
                openCachedRoom(item);
            }

            @Override
            public void onDeleteRequested(ViewedRoomAdapter.Item item) {
                deleteCachedRoom(item);
            }
        });

        binding.btnViewedRoomsBack.setOnClickListener(
                clicked -> getParentFragmentManager().popBackStack()
        );

        binding.btnViewedRoomsRetry.setOnClickListener(
                clicked -> loadRooms()
        );

        binding.btnViewedRoomsClear.setOnClickListener(clicked -> {
            String ownerId = getCurrentUserId();

            if (!busy && !ownerId.isEmpty()) {
                runOperation(
                        ownerId,
                        () -> repository.clearCache(ownerId)
                );
            }
        });

        SessionRepository.getInstance()
                .getSessionState()
                .observe(
                        getViewLifecycleOwner(),
                        session -> loadRooms()
                );
    }

    @Override
    public void onResume() {
        super.onResume();

        if (binding != null && adapter != null) {
            loadRooms();
        }
    }

    private String getCurrentUserId() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (!session.isLoggedIn() || session.getUserId() == null) {
            return "";
        }

        return session.getUserId().trim();
    }

    private void loadRooms() {
        if (binding == null || adapter == null) {
            return;
        }

        String ownerId = getCurrentUserId();

        if (ownerId.isEmpty()) {
            requestVersion++;
            busy = false;

            adapter.submitItems(new ArrayList<>());

            binding.progressViewedRooms.setVisibility(View.GONE);
            binding.layoutViewedRoomsError.setVisibility(View.GONE);
            binding.rvViewedRooms.setVisibility(View.GONE);
            binding.btnViewedRoomsClear.setEnabled(false);

            binding.tvViewedRoomsEmpty.setText(
                    "Bạn cần đăng nhập để xem phòng đã lưu."
            );
            binding.tvViewedRoomsEmpty.setVisibility(View.VISIBLE);
            return;
        }

        runOperation(ownerId, () -> {
            // Chỉ đọc lại danh sách.
        });
    }

    /**
     * Thực hiện thao tác rồi đọc lại danh sách trên cùng hàng đợi.
     */
    private void runOperation(
            String ownerId,
            Runnable operation
    ) {
        if (binding == null
                || adapter == null
                || !ownerId.equals(getCurrentUserId())) {
            return;
        }

        final int version = ++requestVersion;
        final View originalRoot = binding.getRoot();
        final Context appContext =
                requireContext().getApplicationContext();
        final LocalViewedRoomRepository localRepository = repository;

        busy = true;

        // Không giữ dữ liệu tài khoản trước trên giao diện.
        adapter.submitItems(new ArrayList<>());

        binding.progressViewedRooms.setVisibility(View.VISIBLE);
        binding.layoutViewedRoomsError.setVisibility(View.GONE);
        binding.tvViewedRoomsEmpty.setVisibility(View.GONE);
        binding.rvViewedRooms.setVisibility(View.GONE);
        binding.btnViewedRoomsClear.setEnabled(false);

        CACHE_EXECUTOR.execute(() -> {
            try {
                operation.run();

                List<ViewedRoomCache> caches =
                        localRepository.getViewedRooms(ownerId);

                List<ViewedRoomAdapter.Item> items = new ArrayList<>();
                int invalidCount = 0;

                for (ViewedRoomCache cache : caches) {
                    try {
                        RoomCard room = RoomCardJsonCodec.decode(
                                appContext,
                                cache.getPayloadJson()
                        );

                        items.add(new ViewedRoomAdapter.Item(
                                cache,
                                room
                        ));
                    } catch (RuntimeException exception) {
                        invalidCount++;

                        Log.e(
                                TAG,
                                "Bỏ qua bản cache không đọc được: "
                                        + cache.getListingId(),
                                exception
                        );
                    }
                }

                final int skippedCount = invalidCount;
                final boolean hasStoredRows = !caches.isEmpty();

                mainHandler.post(() -> {
                    if (!isCurrentRequest(
                            originalRoot,
                            version,
                            ownerId
                    )) {
                        return;
                    }

                    busy = false;
                    adapter.submitItems(items);

                    binding.progressViewedRooms.setVisibility(View.GONE);
                    binding.btnViewedRoomsClear.setEnabled(hasStoredRows);

                    boolean empty = items.isEmpty();

                    binding.rvViewedRooms.setVisibility(
                            empty ? View.GONE : View.VISIBLE
                    );

                    binding.tvViewedRoomsEmpty.setText(
                            skippedCount > 0
                                    ? "Không có bản lưu đọc được. "
                                      + "Bạn có thể xóa tất cả rồi xem lại "
                                      + "phòng từ Khám phá để tạo cache mới."
                                    : "Chưa có phòng đã lưu trên điện thoại."
                    );

                    binding.tvViewedRoomsEmpty.setVisibility(
                            empty ? View.VISIBLE : View.GONE
                    );

                    if (skippedCount > 0) {
                        binding.tvViewedRoomsError.setText(
                                "Có " + skippedCount
                                        + " bản lưu không đọc được."
                        );
                        binding.layoutViewedRoomsError.setVisibility(
                                View.VISIBLE
                        );
                    }
                });

            } catch (RuntimeException exception) {
                Log.e(TAG, "Không xử lý được danh sách cache.", exception);

                mainHandler.post(() -> {
                    if (!isCurrentRequest(
                            originalRoot,
                            version,
                            ownerId
                    )) {
                        return;
                    }

                    busy = false;

                    binding.progressViewedRooms.setVisibility(View.GONE);
                    binding.tvViewedRoomsError.setText(
                            "Không xử lý được danh sách phòng đã xem. "
                                    + "Bạn hãy thử lại."
                    );
                    binding.layoutViewedRoomsError.setVisibility(
                            View.VISIBLE
                    );
                });
            }
        });
    }

    private boolean isCurrentRequest(
            View originalRoot,
            int version,
            String ownerId
    ) {
        return binding != null
                && binding.getRoot() == originalRoot
                && adapter != null
                && requestVersion == version
                && ownerId.equals(getCurrentUserId());
    }

    private void deleteCachedRoom(ViewedRoomAdapter.Item item) {
        if (binding == null || busy) {
            return;
        }

        String ownerId = getCurrentUserId();

        if (ownerId.isEmpty()
                || !ownerId.equals(item.getCache().getUserId())) {
            loadRooms();
            return;
        }

        String listingId = item.getCache().getListingId();

        runOperation(
                ownerId,
                () -> repository.deleteCache(ownerId, listingId)
        );
    }

    private void openCachedRoom(ViewedRoomAdapter.Item item) {
        if (binding == null
                || busy
                || getParentFragmentManager().isStateSaved()) {
            return;
        }

        String ownerId = getCurrentUserId();

        if (ownerId.isEmpty()
                || !ownerId.equals(item.getCache().getUserId())) {
            loadRooms();
            return;
        }

        final String listingId = item.getCache().getListingId();
        final LocalViewedRoomRepository localRepository = repository;

        // Chỉ cập nhật lần xem, không làm mới thời gian dữ liệu.
        CACHE_EXECUTOR.execute(() -> {
            try {
                localRepository.markViewed(ownerId, listingId);
            } catch (RuntimeException exception) {
                Log.e(TAG, "Không cập nhật được lần xem.", exception);
            }
        });

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        RoomDetailFragment.newCachedInstance(
                                item.getRoom(),
                                ownerId,
                                item.getCache().getCachedAtMillis()
                        )
                )
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        requestVersion++;
        busy = false;

        if (adapter != null) {
            adapter.setListener(null);
        }

        if (binding != null) {
            binding.btnViewedRoomsBack.setOnClickListener(null);
            binding.btnViewedRoomsClear.setOnClickListener(null);
            binding.btnViewedRoomsRetry.setOnClickListener(null);
            binding.rvViewedRooms.setAdapter(null);
        }

        adapter = null;
        binding = null;

        super.onDestroyView();
    }
}