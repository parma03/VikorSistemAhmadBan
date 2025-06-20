package com.example.vikorsistemahmadban.adapter;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vikorsistemahmadban.R;
import com.example.vikorsistemahmadban.model.KriteriaModel;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class KriteriaAdapter extends RecyclerView.Adapter<KriteriaAdapter.KriteriaViewHolder> {
    private List<KriteriaModel> kriteriaModelList;
    private List<KriteriaModel> kriteriaModelListFull;
    private Context context;
    private OnItemClickListener listener;
    private OnSwipeActionListener swipeListener;

    public interface OnItemClickListener {
        void onItemClick(KriteriaModel kriteria);
        void onItemLongClick(KriteriaModel kriteria);
    }

    public interface OnSwipeActionListener {
        void onUpdateClick(KriteriaModel kriteria);
        void onDeleteClick(KriteriaModel kriteria);
    }

    public KriteriaAdapter(Context context) {
        this.context = context;
        this.kriteriaModelList = new ArrayList<>();
        this.kriteriaModelListFull = new ArrayList<>();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnSwipeActionListener(OnSwipeActionListener listener) {
        this.swipeListener = listener;
    }

    public void setKriteriaList(List<KriteriaModel> kriterias) {
        this.kriteriaModelList.clear();
        this.kriteriaModelListFull.clear();

        if (kriterias != null) {
            this.kriteriaModelList.addAll(kriterias);
            this.kriteriaModelListFull.addAll(kriterias);
        }

        notifyDataSetChanged();
    }

    public void addKriteria(KriteriaModel kriteria) {
        kriteriaModelList.add(0, kriteria);
        kriteriaModelListFull.add(0, kriteria);
        notifyItemInserted(0);
    }

    public void updateKriteria(KriteriaModel updateKriteria) {
        for (int i = 0; i < kriteriaModelList.size(); i++) {
            if(kriteriaModelList.get(i).getId_kriteria().equals((updateKriteria.getId_kriteria()))) {
                kriteriaModelList.set(i, updateKriteria);
                notifyItemChanged(i);
                break;
            }
        }
        for (int i = 0; i < kriteriaModelListFull.size(); i++) {
            if (kriteriaModelListFull.get(i).getId_kriteria().equals(updateKriteria.getId_kriteria())) {
                kriteriaModelListFull.set(i, updateKriteria);
                break;
            }
        }
    }

    public void removeKriteria(String kriteriaId) {
        for (int i = 0; i < kriteriaModelList.size(); i++) {
            if(kriteriaModelList.get(i).getId_kriteria().equals(kriteriaId)) {
                kriteriaModelList.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
        for (int i = 0; i < kriteriaModelListFull.size(); i++) {
            if(kriteriaModelListFull.get(i).getId_kriteria().equals(kriteriaId)) {
                kriteriaModelListFull.remove(i);
                break;
            }
        }
    }

    public void filter(String query) {
        kriteriaModelList.clear();
        if (query.isEmpty()) {
            kriteriaModelList.addAll(kriteriaModelListFull);
        } else {
            String filterPattern = query.toLowerCase().trim();
            for (KriteriaModel kriteria : kriteriaModelListFull) {
                if (kriteria.getNama_kriteria().toLowerCase().contains(filterPattern) ||
                        kriteria.getId_kriteria().toLowerCase().contains(filterPattern) ||
                        kriteria.getBobot().toLowerCase().contains(filterPattern)) {
                    kriteriaModelList.add(kriteria);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public KriteriaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_kriteria, parent, false);
        return new KriteriaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KriteriaViewHolder holder, int position) {
        KriteriaModel kriteria = kriteriaModelList.get(position);
        holder.bind(kriteria);
    }

    @Override
    public int getItemCount() {
        return kriteriaModelList.size();
    }

    public class KriteriaViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView foregroundCard;
        private TextView tvNamaKriteria, tvIdKriteria, tvBobot;
        private View updateAction, deleteAction;

        public KriteriaViewHolder(@NonNull View itemView) {
            super(itemView);

            foregroundCard = itemView.findViewById(R.id.foregroundCard);
            tvNamaKriteria = itemView.findViewById(R.id.tvNamaKriteria);
            tvIdKriteria = itemView.findViewById(R.id.tvIdKriteria);
            tvBobot = itemView.findViewById(R.id.tvBobot);
            updateAction = itemView.findViewById(R.id.updateAction);
            deleteAction = itemView.findViewById(R.id.deleteAction);

            setupClickListeners();
        }

        private void setupClickListeners() {
            foregroundCard.setOnClickListener(v -> {
                if (listener != null) {
                    animateCardClick(foregroundCard);
                    listener.onItemClick(kriteriaModelList.get(getAdapterPosition()));
                }
            });

            foregroundCard.setOnLongClickListener(v -> {
                if (listener != null) {
                    animateCardLongClick(foregroundCard);
                    listener.onItemLongClick(kriteriaModelList.get(getAdapterPosition()));
                    return true;
                }
                return false;
            });

            // Swipe action listeners
            updateAction.setOnClickListener(v -> {
                if (swipeListener != null) {
                    animateActionClick(updateAction);
                    swipeListener.onUpdateClick(kriteriaModelList.get(getAdapterPosition()));
                }
            });

            deleteAction.setOnClickListener(v -> {
                if (swipeListener != null) {
                    animateActionClick(deleteAction);
                    swipeListener.onDeleteClick(kriteriaModelList.get(getAdapterPosition()));
                }
            });
        }

        public void bind(KriteriaModel kriteria) {
            tvNamaKriteria.setText(kriteria.getNama_kriteria());
            tvIdKriteria.setText("ID: " + kriteria.getId_kriteria());
            tvBobot.setText(kriteria.getBobot());

            animateItemEntry();
        }

        private void animateItemEntry() {
            foregroundCard.setAlpha(0f);
            foregroundCard.setTranslationY(50f);

            ObjectAnimator.ofFloat(foregroundCard, "alpha", 0f, 1f)
                    .setDuration(300)
                    .start();

            ObjectAnimator.ofFloat(foregroundCard, "translationY", 50f, 0f)
                    .setDuration(300)
                    .start();
        }

        private void animateCardClick(View view) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f, 1f);
            scaleX.setDuration(150);
            scaleY.setDuration(150);
            scaleX.start();
            scaleY.start();
        }

        private void animateCardLongClick(View view) {
            ObjectAnimator shake = ObjectAnimator.ofFloat(view, "translationX", 0f, 10f, -10f, 10f, 0f);
            shake.setDuration(300);
            shake.start();
        }

        private void animateActionClick(View view) {
            ObjectAnimator pulse = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f, 1f);
            ObjectAnimator pulseY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f, 1f);
            pulse.setDuration(200);
            pulseY.setDuration(200);
            pulse.start();
            pulseY.start();
        }
    }
}
