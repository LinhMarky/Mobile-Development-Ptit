package com.example.roomly.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.roomly.R;
import com.example.roomly.data.model.BookingCase;
import com.example.roomly.data.model.SessionState;
import com.example.roomly.data.model.UserRole;
import com.example.roomly.data.repository.DemoBookingCaseRepository;
import com.example.roomly.data.repository.SessionRepository;
import com.example.roomly.databinding.FragmentAdminBookingCasesBinding;

import java.util.Collections;
import java.util.List;

public final class AdminBookingCasesFragment extends Fragment {

    private static final String STATE_FILTER = "case_filter";

    private static final String FILTER_OPEN = "open";
    private static final String FILTER_CLOSED = "closed";
    private static final String FILTER_ALL = "all";

    private FragmentAdminBookingCasesBinding binding;
    private AdminBookingCaseAdapter adapter;

    private String selectedFilter = FILTER_OPEN;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentAdminBookingCasesBinding.inflate(
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

        if (savedInstanceState != null) {
            selectedFilter = savedInstanceState.getString(
                    STATE_FILTER,
                    FILTER_OPEN
            );
        }

        adapter = new AdminBookingCaseAdapter(this::openCase);

        binding.rvAdminCases.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );
        binding.rvAdminCases.setAdapter(adapter);

        binding.btnAdminCasesBack.setOnClickListener(
                clicked -> getParentFragmentManager().popBackStack()
        );

        binding.btnAdminCasesRetry.setOnClickListener(
                clicked -> loadCases()
        );

        int checkedId = R.id.chip_admin_cases_open;

        if (FILTER_CLOSED.equals(selectedFilter)) {
            checkedId = R.id.chip_admin_cases_closed;
        } else if (FILTER_ALL.equals(selectedFilter)) {
            checkedId = R.id.chip_admin_cases_all;
        }

        binding.chipGroupAdminCases.check(checkedId);

        binding.chipGroupAdminCases.setOnCheckedStateChangeListener(
                (group, checkedIds) -> {
                    updateSelectedFilter();
                    loadCases();
                }
        );

        getChildFragmentManager().setFragmentResultListener(
                AdminBookingCaseDialogFragment.RESULT_UPDATED,
                getViewLifecycleOwner(),
                (requestKey, result) -> loadCases()
        );
    }

    @Override
    public void onViewStateRestored(
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewStateRestored(savedInstanceState);

        if (binding != null) {
            updateSelectedFilter();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCases();
    }

    private void updateSelectedFilter() {
        int checkedId = binding.chipGroupAdminCases.getCheckedChipId();

        if (checkedId == R.id.chip_admin_cases_closed) {
            selectedFilter = FILTER_CLOSED;
        } else if (checkedId == R.id.chip_admin_cases_all) {
            selectedFilter = FILTER_ALL;
        } else {
            selectedFilter = FILTER_OPEN;
        }
    }

    private boolean hasAdminAccess() {
        SessionState session =
                SessionRepository.getInstance().getCurrentSession();

        return session.isLoggedIn()
                && session.hasRole(UserRole.ADMIN);
    }

    private void loadCases() {
        if (binding == null || adapter == null) {
            return;
        }

        binding.rvAdminCases.setVisibility(View.GONE);
        binding.tvAdminCasesEmpty.setVisibility(View.GONE);
        binding.layoutAdminCasesError.setVisibility(View.GONE);

        adapter.submitCases(Collections.emptyList());

        if (!hasAdminAccess()) {
            showError("Màn hình này dành cho tài khoản Admin.");
            return;
        }

        try {
            Boolean closed = null;

            if (FILTER_OPEN.equals(selectedFilter)) {
                closed = false;
            } else if (FILTER_CLOSED.equals(selectedFilter)) {
                closed = true;
            }

            List<BookingCase> cases =
                    DemoBookingCaseRepository.getInstance()
                            .getAdminCases(closed);

            adapter.submitCases(cases);

            if (cases.isEmpty()) {
                binding.tvAdminCasesEmpty.setVisibility(View.VISIBLE);
            } else {
                binding.rvAdminCases.setVisibility(View.VISIBLE);
            }
        } catch (RuntimeException exception) {
            showError(getErrorMessage(exception));
        }
    }

    private void openCase(BookingCase bookingCase) {
        if (binding == null || !isAdded()) {
            return;
        }

        if (!hasAdminAccess()) {
            loadCases();
            return;
        }

        FragmentManager fragmentManager = getChildFragmentManager();

        if (fragmentManager.isStateSaved()
                || fragmentManager.findFragmentByTag(
                AdminBookingCaseDialogFragment.TAG
        ) != null) {
            return;
        }

        AdminBookingCaseDialogFragment.newInstance(bookingCase.getId())
                .showNow(
                        fragmentManager,
                        AdminBookingCaseDialogFragment.TAG
                );
    }

    private void showError(String message) {
        if (binding == null) {
            return;
        }

        binding.rvAdminCases.setVisibility(View.GONE);
        binding.tvAdminCasesEmpty.setVisibility(View.GONE);
        binding.layoutAdminCasesError.setVisibility(View.VISIBLE);
        binding.tvAdminCasesError.setText(message);
    }

    private String getErrorMessage(RuntimeException exception) {
        String message = exception.getMessage();

        return message == null || message.trim().isEmpty()
                ? "Không thể tải yêu cầu hỗ trợ. Vui lòng thử lại."
                : message;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(STATE_FILTER, selectedFilter);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.btnAdminCasesBack.setOnClickListener(null);
            binding.btnAdminCasesRetry.setOnClickListener(null);

            binding.chipGroupAdminCases
                    .setOnCheckedStateChangeListener(null);

            binding.rvAdminCases.setAdapter(null);
        }

        adapter = null;
        binding = null;

        super.onDestroyView();
    }
}