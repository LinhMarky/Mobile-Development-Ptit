package com.example.roomly.ui.host;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.roomly.R;
import com.example.roomly.data.api.ApiMapper;
import com.example.roomly.data.model.ListingFee;
import com.example.roomly.databinding.DialogListingFeeBinding;
import com.example.roomly.databinding.ViewListingFeesBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/** Chỉ giữ giao diện của biểu mẫu hiện tại; Fragment gọi close khi hủy View. */
final class ListingFeeEditor {
    private ViewListingFeesBinding binding;
    private final List<ListingFee> fees = new ArrayList<>();
    private boolean loaded;
    private boolean enabled = true;
    private AlertDialog dialog;

    ListingFeeEditor(View view) {
        binding = ViewListingFeesBinding.bind(view);
        binding.btnAddListingFee.setOnClickListener(ignored -> edit(-1));
        render();
    }

    void load(JSONArray rows) {
        try {
            List<ListingFee> parsed = ListingFee.fromJson(rows);
            fees.clear();
            fees.addAll(parsed);
            loaded = true;
        } catch (JSONException | IllegalArgumentException | ArithmeticException error) {
            loaded = false;
        }
        render();
    }

    boolean isLoaded() { return loaded; }

    JSONArray values() {
        if (!loaded) throw new IllegalStateException("Chưa tải được phụ phí. Hãy thử lại.");
        try {
            return ListingFee.toJson(fees);
        } catch (JSONException error) {
            throw new IllegalStateException("Không thể đọc phụ phí.", error);
        }
    }

    void setEnabled(boolean value) {
        enabled = value;
        render();
    }

    private void render() {
        if (binding == null) return;
        binding.tvListingFeesState.setText(!loaded ? R.string.listing_fees_loading : R.string.listing_fees_empty);
        binding.tvListingFeesState.setVisibility(!loaded || fees.isEmpty() ? View.VISIBLE : View.GONE);
        binding.btnAddListingFee.setEnabled(loaded && enabled && fees.size() < ListingFee.CODES.size());
        binding.containerListingFees.removeAllViews();
        for (int index = 0; index < fees.size(); index++) {
            ListingFee fee = fees.get(index);
            final int position = index;
            LinearLayout row = new LinearLayout(binding.getRoot().getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            TextView label = new TextView(row.getContext());
            label.setText(ApiMapper.feeName(fee.getCode()) + ": " + ApiMapper.vnd(fee.getAmount())
                    + (fee.getUnit().isEmpty() ? "" : "/" + fee.getUnit())
                    + ("PER_UNIT".equals(fee.getMode()) ? " (theo sử dụng)" : " (cố định)"));
            row.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            MaterialButton edit = new MaterialButton(row.getContext());
            edit.setText(R.string.listing_fee_edit);
            edit.setEnabled(enabled);
            edit.setOnClickListener(ignored -> edit(position));
            row.addView(edit);
            binding.containerListingFees.addView(row);
        }
    }

    private void edit(int position) {
        if (binding == null || !loaded || !enabled) return;
        DialogListingFeeBinding form = DialogListingFeeBinding.inflate(LayoutInflater.from(binding.getRoot().getContext()));
        form.spinnerFeeCode.setAdapter(ArrayAdapter.createFromResource(form.getRoot().getContext(),
                R.array.listing_fee_names, android.R.layout.simple_spinner_dropdown_item));
        form.spinnerFeeMode.setAdapter(ArrayAdapter.createFromResource(form.getRoot().getContext(),
                R.array.listing_fee_modes, android.R.layout.simple_spinner_dropdown_item));
        if (position >= 0) {
            ListingFee fee = fees.get(position);
            form.spinnerFeeCode.setSelection(ListingFee.CODES.indexOf(fee.getCode()));
            form.spinnerFeeMode.setSelection("FIXED".equals(fee.getMode()) ? 0 : 1);
            form.edtFeeAmount.setText(String.valueOf(fee.getAmount()));
            form.edtFeeUnit.setText(fee.getUnit());
        }
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(form.getRoot().getContext())
                .setTitle(position < 0 ? R.string.listing_fee_add : R.string.listing_fee_edit)
                .setView(form.getRoot()).setNegativeButton(R.string.listing_fee_cancel, null)
                .setPositiveButton(R.string.listing_fee_save, null);
        if (position >= 0) builder.setNeutralButton(R.string.listing_fee_remove, (ignored, which) -> {
            fees.remove(position);
            render();
        });
        dialog = builder.create();
        dialog.setOnDismissListener(ignored -> dialog = null);
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(clicked -> {
            try {
                String code = ListingFee.CODES.get(form.spinnerFeeCode.getSelectedItemPosition());
                for (int index = 0; index < fees.size(); index++) {
                    if (index != position && code.equals(fees.get(index).getCode())) {
                        throw new IllegalArgumentException(form.getRoot().getContext().getString(R.string.listing_fee_duplicate));
                    }
                }
                ListingFee fee = new ListingFee(code, form.spinnerFeeMode.getSelectedItemPosition() == 0 ? "FIXED" : "PER_UNIT",
                        form.edtFeeUnit.getText().toString(), ListingFee.parseAmount(form.edtFeeAmount.getText().toString().trim()));
                if (position < 0) fees.add(fee); else fees.set(position, fee);
                dialog.dismiss();
                render();
            } catch (IllegalArgumentException error) {
                form.tvFeeError.setText(error.getMessage());
            }
        }));
        dialog.show();
    }

    void close() {
        if (dialog != null) dialog.dismiss();
        if (binding != null) binding.btnAddListingFee.setOnClickListener(null);
        binding = null;
    }
}
