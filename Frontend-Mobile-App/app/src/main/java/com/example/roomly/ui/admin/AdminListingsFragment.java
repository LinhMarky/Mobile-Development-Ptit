package com.example.roomly.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.roomly.R;
import com.example.roomly.data.model.AdminListing;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoAdminListingRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentAdminListingsBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Hiển thị và lọc bài đăng cho tài khoản quản trị.
 */
public class AdminListingsFragment extends Fragment {

    private static final String STATE_FILTER = "admin_listing_filter";

    private FragmentAdminListingsBinding binding;
    private AdminListingAdapter listingAdapter;

    private int selectedFilterId = R.id.chip_admin_listings_all;

    /**
     * Khôi phục bộ lọc khi Android tạo lại Fragment.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            selectedFilterId = savedInstanceState.getInt(
                    STATE_FILTER,
                    R.id.chip_admin_listings_all
            );
        }
    }

    /**
     * Tạo giao diện danh sách kiểm duyệt.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentAdminListingsBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Thiết lập danh sách, bộ lọc, nút quay lại và theo dõi phiên.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.rvAdminListings.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        listingAdapter = new AdminListingAdapter(new ArrayList<>());
        listingAdapter.setOnListingClickListener(this::openListingDetail);

        binding.rvAdminListings.setAdapter(listingAdapter);

        binding.btnAdminListingsBack.setOnClickListener(
                clickedView -> getParentFragmentManager().popBackStack()
        );

        binding.chipGroupAdminListings.check(selectedFilterId);

        binding.chipGroupAdminListings.setOnCheckedStateChangeListener(
                (group, checkedIds) -> {
                    if (checkedIds.isEmpty()) {
                        return;
                    }

                    selectedFilterId = checkedIds.get(0);
                    displayListings();
                }
        );

        SessionRepository.getInstance()
                .getSessionState()
                .observe(
                        getViewLifecycleOwner(),
                        session -> displayListings()
                );
    }

    /**
     * Áp dụng lại bộ lọc sau khi Android khôi phục trạng thái giao diện.
     */
    @Override
    public void onViewStateRestored(
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewStateRestored(savedInstanceState);

        binding.chipGroupAdminListings.check(selectedFilterId);
        displayListings();
    }

    /**
     * Cập nhật dữ liệu khi trở về từ màn hình khác.
     */
    @Override
    public void onResume() {
        super.onResume();
        displayListings();
    }

    /**
     * Kiểm tra quyền ADMIN trước khi đọc và hiển thị danh sách.
     * Xóa nội dung khi tài khoản không còn quyền truy cập.
     */
    private void displayListings() {
        if (binding == null || listingAdapter == null) {
            return;
        }

        SessionState session =
                SessionRepository.getInstance().getCurrentSession();

        boolean allowed = session.isLoggedIn()
                && session.hasRole(UserRole.ADMIN);

        binding.layoutAdminListingsContent.setVisibility(
                allowed ? View.VISIBLE : View.GONE
        );

        binding.tvAdminListingsAccessError.setVisibility(
                allowed ? View.GONE : View.VISIBLE
        );

        if (!allowed) {
            listingAdapter.updateListings(new ArrayList<>());

            binding.tvAdminListingsAccessError.setText(
                    session.isLoggedIn()
                            ? "Tài khoản hiện tại không có quyền quản trị."
                            : "Bạn cần đăng nhập tài khoản quản trị."
            );
            return;
        }

        List<AdminListing> listings =
                DemoAdminListingRepository.getInstance()
                        .getListings(getSelectedStatus());

        listingAdapter.updateListings(listings);

        boolean empty = listings.isEmpty();

        binding.rvAdminListings.setVisibility(
                empty ? View.GONE : View.VISIBLE
        );

        binding.layoutAdminListingsEmpty.setVisibility(
                empty ? View.VISIBLE : View.GONE
        );

        binding.tvAdminListingsEmptyTitle.setText(
                "Không có bài đăng phù hợp"
        );

        binding.tvAdminListingsEmptyDescription.setText(
                "Chưa có bài đăng thuộc bộ lọc đang chọn."
        );
    }

    /**
     * Chuyển bộ lọc được chọn thành trạng thái trong dữ liệu mẫu.
     * Null tương ứng với Tất cả.
     */
    @Nullable
    private AdminListing.Status getSelectedStatus() {
        if (selectedFilterId == R.id.chip_admin_listings_pending) {
            return AdminListing.Status.PENDING;
        }

        if (selectedFilterId == R.id.chip_admin_listings_published) {
            return AdminListing.Status.PUBLISHED;
        }

        if (selectedFilterId == R.id.chip_admin_listings_hidden) {
            return AdminListing.Status.HIDDEN;
        }

        return null;
    }

    /**
     * Kiểm tra quyền truy cập bài đăng và mở chi tiết kiểm duyệt.
     * Giữ danh sách và bộ lọc trong back stack.
     */
    private void openListingDetail(AdminListing listing) {
        AdminListing currentListing =
                DemoAdminListingRepository.getInstance()
                        .getListingById(listing.getId());

        if (currentListing == null) {
            displayListings();
            return;
        }

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        AdminListingDetailFragment.newInstance(
                                currentListing.getId()
                        )
                )
                .addToBackStack(null)
                .commit();
    }
    /**
     * Lưu bộ lọc để khôi phục khi Fragment được tạo lại.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(STATE_FILTER, selectedFilterId);
        super.onSaveInstanceState(outState);
    }

    /**
     * Gỡ sự kiện, adapter và giải phóng binding.
     */
    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.btnAdminListingsBack.setOnClickListener(null);

            binding.chipGroupAdminListings
                    .setOnCheckedStateChangeListener(null);

            binding.rvAdminListings.setAdapter(null);
        }

        if (listingAdapter != null) {
            listingAdapter.setOnListingClickListener(null);
        }

        listingAdapter = null;
        binding = null;

        super.onDestroyView();
    }
}