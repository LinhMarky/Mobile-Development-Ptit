package com.example.roomly.ui.host;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.roomly.R;
import com.example.roomly.data.model.HostListing;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoHostListingRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentHostListingsBinding;
import com.example.roomly.ui.auth.LoginFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Hiển thị các bản nháp bài đăng mẫu thuộc chủ trọ hiện tại.
 */
public class HostListingsFragment extends Fragment {

    private FragmentHostListingsBinding binding;
    private HostListingAdapter listingAdapter;

    /**
     * Tạo giao diện danh sách bài đăng bằng ViewBinding.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentHostListingsBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    /**
     * Thiết lập danh sách, nút đăng nhập và quan sát phiên.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        binding.rvHostListings.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        listingAdapter = new HostListingAdapter();
        listingAdapter.setOnListingClickListener(this::openListingDetail);
        binding.rvHostListings.setAdapter(listingAdapter);

        binding.btnHostListingsLogin.setOnClickListener(
                clickedView -> openLoginScreen()
        );

        SessionRepository.getInstance()
                .getSessionState()
                .observe(
                        getViewLifecycleOwner(),
                        this::renderSession
                );
    }

    /**
     * Cập nhật bản nháp và thông tin phòng khi trở lại màn hình.
     */
    @Override
    public void onResume() {
        super.onResume();

        renderSession(
                SessionRepository.getInstance().getCurrentSession()
        );
    }

    /**
     * Kiểm tra đăng nhập và quyền HOST trước khi đọc dữ liệu.
     * Xóa danh sách đang hiển thị khi tài khoản không còn quyền.
     */
    private void renderSession(@Nullable SessionState session) {
        if (binding == null || listingAdapter == null) {
            return;
        }

        SessionState currentSession = session == null
                ? SessionState.guest()
                : session;

        binding.btnHostListingsLogin.setVisibility(View.GONE);

        if (!currentSession.isLoggedIn()) {
            listingAdapter.updateListings(new ArrayList<>());

            showStatus(
                    "Đăng nhập để xem bài đăng",
                    "Bạn cần đăng nhập bằng tài khoản có quyền chủ trọ."
            );

            binding.btnHostListingsLogin.setVisibility(View.VISIBLE);
            return;
        }

        if (!currentSession.hasRole(UserRole.HOST)) {
            listingAdapter.updateListings(new ArrayList<>());

            showStatus(
                    "Tài khoản chưa có quyền chủ trọ",
                    "Danh sách này dành cho tài khoản có quyền chủ trọ."
            );
            return;
        }

        displayListings();
    }

    /**
     * Hiển thị các bản nháp thuộc tài khoản hiện tại.
     * Khi danh sách trống, hướng dẫn tạo bài đăng từ một phòng.
     */
    private void displayListings() {
        List<HostListing> listings =
                DemoHostListingRepository.getInstance()
                        .getMyListings();

        listingAdapter.updateListings(listings);

        if (listings.isEmpty()) {
            showStatus(
                    "Bạn chưa có bài đăng",
                    "Mở một phòng trong Phòng của tôi và bấm "
                            + "“Tạo bài đăng” để lưu bản nháp."
            );
            return;
        }

        binding.layoutHostListingsStatus.setVisibility(View.GONE);
        binding.rvHostListings.setVisibility(View.VISIBLE);
    }

    /**
     * Hiển thị thông báo trạng thái và ẩn danh sách.
     */
    private void showStatus(String title, String description) {
        binding.rvHostListings.setVisibility(View.GONE);
        binding.layoutHostListingsStatus.setVisibility(View.VISIBLE);

        binding.tvHostListingsStatusTitle.setText(title);
        binding.tvHostListingsStatusDescription.setText(description);
    }

    /**
     * Mở đăng nhập nếu tài khoản hiện tại vẫn là khách.
     */
    private void openLoginScreen() {
        SessionState session = SessionRepository.getInstance()
                .getCurrentSession();

        if (session.isLoggedIn()) {
            renderSession(session);
            return;
        }

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        new LoginFragment()
                )
                .addToBackStack(null)
                .commit();
    }

    /**
     * Kiểm tra bản nháp còn thuộc tài khoản hiện tại trước khi mở chi tiết.
     * Chỉ truyền ID để màn hình chi tiết đọc lại dữ liệu và quyền.
     */
    private void openListingDetail(HostListing listing) {
        HostListing currentListing =
                DemoHostListingRepository.getInstance()
                        .getMyListingById(listing.getId());

        if (currentListing == null) {
            renderSession(
                    SessionRepository.getInstance().getCurrentSession()
            );
            return;
        }

        getParentFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        HostListingDetailFragment.newInstance(
                                currentListing.getId()
                        )
                )
                .addToBackStack(null)
                .commit();
    }

    /**
     * Gỡ listener, adapter và binding khi giao diện bị hủy.
     */
    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.btnHostListingsLogin.setOnClickListener(null);
            binding.rvHostListings.setAdapter(null);
        }

        if (listingAdapter != null) {
            listingAdapter.setOnListingClickListener(null);
        }

        listingAdapter = null;
        binding = null;

        super.onDestroyView();
    }
}