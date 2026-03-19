package com.yarnandthread.app.adapter;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import com.yarnandthread.app.R;
import com.yarnandthread.app.model.Counter;
import java.util.List;

public class CounterAdapter extends ListAdapter<Counter, CounterAdapter.VH> {

    public interface OnCounterAction { void onAction(int idx); }

    private final OnCounterAction onIncrement;
    private final OnCounterAction onDecrement;
    private final OnCounterAction onRename;
    private final OnCounterAction onSetMax;
    private final OnCounterAction onLink;
    private final OnCounterAction onRemove;

    public CounterAdapter(OnCounterAction onIncrement,
                          OnCounterAction onDecrement,
                          OnCounterAction onRename,
                          OnCounterAction onSetMax,
                          OnCounterAction onLink,
                          OnCounterAction onRemove) {
        super(new DiffUtil.ItemCallback<Counter>() {
            @Override public boolean areItemsTheSame(@NonNull Counter a, @NonNull Counter b) {
                return a.name.equals(b.name);
            }
            @Override public boolean areContentsTheSame(@NonNull Counter a, @NonNull Counter b) {
                return a.value == b.value && a.max == b.max && a.linkedTo == b.linkedTo;
            }
        });
        this.onIncrement = onIncrement;
        this.onDecrement = onDecrement;
        this.onRename    = onRename;
        this.onSetMax    = onSetMax;
        this.onLink      = onLink;
        this.onRemove    = onRemove;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_counter, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Counter c = getItem(position);
        int pos = position; // effectively final

        h.tvName.setText(c.name);
        h.tvValue.setText(String.valueOf(c.value));

        // Show max label if set
        if (c.hasMax()) {
            h.tvMax.setVisibility(View.VISIBLE);
            h.tvMax.setText("/ " + c.max);
        } else {
            h.tvMax.setVisibility(View.GONE);
        }

        // Show link indicator
        if (c.hasLink()) {
            h.btnLink.setSelected(true);
            h.btnLink.setContentDescription("Linked");
        } else {
            h.btnLink.setSelected(false);
            h.btnLink.setContentDescription("Link");
        }

        h.btnInc.setOnClickListener(v -> onIncrement.onAction(h.getAdapterPosition()));
        h.btnDec.setOnClickListener(v -> onDecrement.onAction(h.getAdapterPosition()));
        h.tvName.setOnClickListener(v -> onRename.onAction(h.getAdapterPosition()));
        h.tvMax.setOnClickListener(v -> onSetMax.onAction(h.getAdapterPosition()));
        h.btnLink.setOnClickListener(v -> onLink.onAction(h.getAdapterPosition()));
        h.btnRemove.setOnClickListener(v -> onRemove.onAction(h.getAdapterPosition()));
        h.itemView.setOnLongClickListener(v -> {
            onSetMax.onAction(h.getAdapterPosition());
            return true;
        });
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvValue, tvMax;
        ImageButton btnInc, btnDec, btnLink, btnRemove;

        VH(View v) {
            super(v);
            tvName    = v.findViewById(R.id.tvCounterName);
            tvValue   = v.findViewById(R.id.tvCounterValue);
            tvMax     = v.findViewById(R.id.tvCounterMax);
            btnInc    = v.findViewById(R.id.btnCounterInc);
            btnDec    = v.findViewById(R.id.btnCounterDec);
            btnLink   = v.findViewById(R.id.btnCounterLink);
            btnRemove = v.findViewById(R.id.btnCounterRemove);
        }
    }
}
