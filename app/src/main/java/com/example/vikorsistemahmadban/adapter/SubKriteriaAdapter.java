package com.example.vikorsistemahmadban.adapter;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vikorsistemahmadban.R;
import com.example.vikorsistemahmadban.model.SubKriteriaModel;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class SubKriteriaAdapter extends RecyclerView.Adapter<SubKriteriaAdapter.SubKriteriaViewHolder> {
    private List<SubKriteriaModel> subKriteriaList;
    private Context context;
    private OnSubKriteriaActionListener listener;

    public interface OnSubKriteriaActionListener {
        void onEditSubKriteria(SubKriteriaModel subKriteria, int position);
        void onDeleteSubKriteria(SubKriteriaModel subKriteria, int position);
    }

    public SubKriteriaAdapter(Context context) {
        this.context = context;
        this.subKriteriaList = new ArrayList<>();
    }

    public void setOnSubKriteriaActionListener(OnSubKriteriaActionListener listener) {
        this.listener = listener;
    }

    public void setSubKriteriaList(List<SubKriteriaModel> subKriterias) {
        this.subKriteriaList.clear();
        if (subKriterias != null) {
            this.subKriteriaList.addAll(subKriterias);
        }
        notifyDataSetChanged();
    }

    public void addSubKriteria(SubKriteriaModel subKriteria) {
        subKriteriaList.add(subKriteria);
        notifyItemInserted(subKriteriaList.size() - 1);
    }

    public void updateSubKriteria(SubKriteriaModel subKriteria, int position) {
        if (position >= 0 && position < subKriteriaList.size()) {
            subKriteriaList.set(position, subKriteria);
            notifyItemChanged(position);
        }
    }

    public void removeSubKriteria(int position) {
        if (position >= 0 && position < subKriteriaList.size()) {
            subKriteriaList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, subKriteriaList.size());
        }
    }

    public List<SubKriteriaModel> getSubKriteriaList() {
        return new ArrayList<>(subKriteriaList);
    }

    @NonNull
    @Override
    public SubKriteriaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_subkriteria, parent, false);
        return new SubKriteriaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubKriteriaViewHolder holder, int position) {
        SubKriteriaModel subKriteria = subKriteriaList.get(position);
        holder.bind(subKriteria, position);
    }

    @Override
    public int getItemCount() {
        return subKriteriaList.size();
    }

    public class SubKriteriaViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardView;
        private TextView tvKlasifikasi, tvBobot;
        private Chip chipEdit, chipDelete;

        public SubKriteriaViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardSubKriteria);
            tvKlasifikasi = itemView.findViewById(R.id.tvKlasifikasi);
            tvBobot = itemView.findViewById(R.id.tvBobot);
            chipEdit = itemView.findViewById(R.id.chipEdit);
            chipDelete = itemView.findViewById(R.id.chipDelete);
        }

        public void bind(SubKriteriaModel subKriteria, int position) {
            tvKlasifikasi.setText(subKriteria.getKlasifikasi());
            tvBobot.setText("Bobot: " + subKriteria.getBobot_sub_kriteria());

            // Set background color based on classification
            setBackgroundByKlasifikasi(subKriteria.getBobot_sub_kriteria());

            chipEdit.setOnClickListener(v -> {
                if (listener != null) {
                    animateChipClick(chipEdit);
                    listener.onEditSubKriteria(subKriteria, position);
                }
            });

            chipDelete.setOnClickListener(v -> {
                if (listener != null) {
                    animateChipClick(chipDelete);
                    listener.onDeleteSubKriteria(subKriteria, position);
                }
            });

            animateItemEntry();
        }

        private void setBackgroundByKlasifikasi(int bobot) {
            int colorRes;

            // Konversi string bobot menjadi nilai integer
            int nilaiBobot;
            try {
                nilaiBobot = Integer.parseInt(String.valueOf(bobot));
            } catch (NumberFormatException e) {
                nilaiBobot = -1; // Nilai default jika parsing gagal
            }

            // Tentukan warna berdasarkan nilai bobot
            if (nilaiBobot >= 100) {
                colorRes = R.color.success_green; // Sangat Baik
            } else if (nilaiBobot >= 80) {
                colorRes = R.color.primary_blue; // Baik
            } else if (nilaiBobot >= 60) {
                colorRes = R.color.purple_200; // Cukup
            } else if (nilaiBobot >= 40) {
                colorRes = R.color.warning_orange; // Buruk
            } else {
                colorRes = R.color.error_red; // Sangat Buruk atau tidak valid
            }

            cardView.setCardBackgroundColor(ContextCompat.getColor(context, colorRes));
        }

        private void animateItemEntry() {
            cardView.setAlpha(0f);
            cardView.setTranslationX(100f);

            ObjectAnimator.ofFloat(cardView, "alpha", 0f, 1f)
                    .setDuration(300)
                    .start();

            ObjectAnimator.ofFloat(cardView, "translationX", 100f, 0f)
                    .setDuration(300)
                    .start();
        }

        private void animateChipClick(View view) {
            ObjectAnimator pulse = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f);
            ObjectAnimator pulseY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f);
            pulse.setDuration(150);
            pulseY.setDuration(150);
            pulse.start();
            pulseY.start();
        }
    }
}
