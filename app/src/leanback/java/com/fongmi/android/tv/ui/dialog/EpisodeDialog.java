package com.fongmi.android.tv.ui.dialog;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.databinding.DialogEpisodeBinding;
import com.fongmi.android.tv.ui.adapter.EpisodeAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class EpisodeDialog extends BaseAlertDialog implements EpisodeAdapter.OnClickListener {

    private DialogEpisodeBinding binding;
    private EpisodeAdapter adapter;
    private List<Episode> items = new ArrayList<>();
    private int spanCount;

    public static EpisodeDialog create() {
        return new EpisodeDialog();
    }

    public EpisodeDialog episodes(List<Episode> items) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
        return this;
    }

    public void show(FragmentActivity activity) {
        show(activity.getSupportFragmentManager(), null);
    }

    private int getSpanCount() {
        int width = (int) (ResUtil.getScreenWidth() * 0.7f) - ResUtil.dp2px(48);
        return Math.clamp(width / Math.max(1, ResUtil.dp2px(110)), 3, 8);
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogEpisodeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setTitle(R.string.detail_episode).setView(getBinding().getRoot());
    }

    @Override
    protected void initView() {
        spanCount = getSpanCount();
        adapter = new EpisodeAdapter(this).grid(spanCount);
        adapter.addAll(items);
        binding.recycler.setItemAnimator(null);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(spanCount, 8));
        binding.recycler.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));
        binding.recycler.setAdapter(adapter);
        binding.recycler.post(() -> binding.recycler.requestFocus());
        binding.recycler.postDelayed(() -> binding.recycler.scrollToPosition(adapter.getPosition()), 100);
    }

    @Override
    public void onItemClick(Episode item) {
        dismiss();
        ((EpisodeAdapter.OnClickListener) requireActivity()).onItemClick(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (items.isEmpty()) dismiss();
        else setWidth(0.7f);
    }
}
