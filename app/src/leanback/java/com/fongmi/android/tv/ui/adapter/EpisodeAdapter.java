package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.databinding.AdapterEpisodeBinding;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Episode> mItems;
    private final int maxWidth;
    private int spanCount;
    private int nextFocusDown;
    private int nextFocusUp;

    public EpisodeAdapter(OnClickListener listener) {
        mListener = listener;
        mItems = new ArrayList<>();
        maxWidth = ResUtil.getScreenWidth() - ResUtil.dp2px(48);
    }

    public EpisodeAdapter grid(int spanCount) {
        this.spanCount = spanCount;
        return this;
    }

    public void addAll(List<Episode> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }

    public List<Episode> getItems() {
        return new ArrayList<>(mItems);
    }

    public int getPosition() {
        for (int i = 0; i < mItems.size(); i++) if (mItems.get(i).isSelected()) return i;
        return 0;
    }

    public Episode getActivated() {
        return mItems.isEmpty() ? new Episode() : mItems.get(getPosition());
    }

    public Episode getNext() {
        int current = getPosition();
        int max = getItemCount() - 1;
        current = ++current > max ? max : current;
        return mItems.get(current);
    }

    public Episode getPrev() {
        int current = getPosition();
        current = --current < 0 ? 0 : current;
        return mItems.get(current);
    }

    public void setNextFocusDown(int nextFocusDown) {
        this.nextFocusDown = nextFocusDown;
        notifyDataSetChanged();
    }

    public void setNextFocusUp(int nextFocusUp) {
        this.nextFocusUp = nextFocusUp;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (spanCount > 0) return new ViewHolder((MaterialTextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_episode_grid, parent, false));
        return new ViewHolder(AdapterEpisodeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Episode item = mItems.get(position);
        holder.text.setMaxWidth(maxWidth);
        holder.text.setNextFocusUpId(spanCount > 0 && position >= spanCount ? View.NO_ID : nextFocusUp);
        holder.text.setNextFocusDownId(spanCount > 0 && position + spanCount < mItems.size() ? View.NO_ID : nextFocusDown);
        holder.text.setSelected(item.isSelected());
        holder.text.setText(item.getDesc().concat(item.getName()));
        holder.text.setOnClickListener(v -> mListener.onItemClick(item));
        holder.text.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) mListener.onItemFocus(item, v);
        });
    }

    public interface OnClickListener {

        void onItemClick(Episode item);

        default void onItemFocus(Episode item, View view) {
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final MaterialTextView text;

        ViewHolder(@NonNull MaterialTextView text) {
            super(text);
            this.text = text;
        }
    }
}
