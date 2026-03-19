package com.yarnandthread.app.adapter;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import com.yarnandthread.app.R;
import com.yarnandthread.app.model.Project;
import java.util.List;

public class ProjectAdapter extends ListAdapter<Project, ProjectAdapter.VH> {

    public interface OnProjectClick   { void onClick(Project p); }
    public interface OnProjectDelete  { void onDelete(Project p); }

    private final OnProjectClick onClick;
    private final OnProjectDelete onDelete;

    public ProjectAdapter(OnProjectClick onClick, OnProjectDelete onDelete) {
        super(new DiffUtil.ItemCallback<Project>() {
            @Override public boolean areItemsTheSame(@NonNull Project a, @NonNull Project b) { return a.id.equals(b.id); }
            @Override public boolean areContentsTheSame(@NonNull Project a, @NonNull Project b) { return a.name.equals(b.name) && a.annotations.size() == b.annotations.size() && a.counters.size() == b.counters.size(); }
        });
        this.onClick = onClick;
        this.onDelete = onDelete;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Project p = getItem(position);
        holder.tvName.setText(p.name);
        holder.tvMeta.setText(
                p.annotations.size() + " annotation" + (p.annotations.size() != 1 ? "s" : "") +
                " · " + p.counters.size() + " counter" + (p.counters.size() != 1 ? "s" : "")
        );
        holder.itemView.setOnClickListener(v -> onClick.onClick(p));
        holder.btnDelete.setOnClickListener(v -> onDelete.onDelete(p));
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvMeta;
        ImageButton btnDelete;
        VH(View v) {
            super(v);
            tvName    = v.findViewById(R.id.tvProjectName);
            tvMeta    = v.findViewById(R.id.tvProjectMeta);
            btnDelete = v.findViewById(R.id.btnDeleteProject);
        }
    }
}
