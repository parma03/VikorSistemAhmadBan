package com.example.vikorsistemahmadban.adapter;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.vikorsistemahmadban.R;
import com.example.vikorsistemahmadban.model.BanModel;
import com.example.vikorsistemahmadban.model.ProsesModel;
import com.google.android.material.card.MaterialCardView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProsesAdapter extends RecyclerView.Adapter<ProsesAdapter.ProsesViewHolder> {
    private List<ProsesModel> prosesModelList;
    private List<ProsesModel> prosesModelListFull;
    private Context context;
    private OnItemClickListener listener;
    private OnSwipeActionListener swipeListener;

    // Optional: Jika Anda perlu data ban untuk ditampilkan
    private List<BanModel> banList = new ArrayList<>();

    public interface OnItemClickListener {
        void onItemClick(ProsesModel proses);
        void onItemLongClick(ProsesModel proses);
    }

    public interface OnSwipeActionListener {
        void onUpdateClick(ProsesModel proses);
        void onDeleteClick(ProsesModel proses);
    }

    public ProsesAdapter(Context context) {
        this.context = context;
        this.prosesModelList = new ArrayList<>();
        this.prosesModelListFull = new ArrayList<>();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnSwipeActionListener(OnSwipeActionListener listener) {
        this.swipeListener = listener;
    }

    public void setProsesModelList(List<ProsesModel> proseses) {
        this.prosesModelList = new ArrayList<>(proseses);
        this.prosesModelListFull = new ArrayList<>(proseses);
        notifyDataSetChanged();
    }

    // Method untuk set data ban jika diperlukan untuk ditampilkan
    public void setBanList(List<BanModel> banList) {
        this.banList = new ArrayList<>(banList);
        notifyDataSetChanged();
    }

    public void addProses(ProsesModel proses) {
        prosesModelList.add(0, proses);
        prosesModelListFull.add(0, proses);
        notifyItemInserted(0);
    }

    public void updateProses(ProsesModel updateProses) {
        for (int i = 0; i < prosesModelList.size(); i++) {
            if (prosesModelList.get(i).getId_proses().equals(updateProses.getId_proses())) {
                prosesModelList.set(i, updateProses);
                notifyItemChanged(i);
                break;
            }
        }
        // Update full list too
        for (int i = 0; i < prosesModelListFull.size(); i++) {
            if (prosesModelListFull.get(i).getId_proses().equals(updateProses.getId_proses())) {
                prosesModelListFull.set(i, updateProses);
                break;
            }
        }
    }

    public void removeProses(String prosesId) {
        for (int i = 0; i < prosesModelList.size(); i++) {
            if (prosesModelList.get(i).getId_proses().equals(prosesId)) {
                prosesModelList.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
        // Remove from full list too
        for (int i = 0; i < prosesModelListFull.size(); i++) {
            if (prosesModelListFull.get(i).getId_proses().equals(prosesId)) {
                prosesModelListFull.remove(i);
                break;
            }
        }
    }

    public void filter(String query) {
        prosesModelList.clear();
        if (query.isEmpty()) {
            prosesModelList.addAll(prosesModelListFull);
        } else {
            String filterPattern = query.toLowerCase().trim();
            for (ProsesModel proses : prosesModelListFull) {
                // Filter berdasarkan data yang ada di ProsesModel
                if (proses.getId_proses().toLowerCase().contains(filterPattern) ||
                        proses.getId_ban().toLowerCase().contains(filterPattern) ||
                        proses.getCreated_at().toLowerCase().contains(filterPattern)) {
                    prosesModelList.add(proses);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProsesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_vikor_proses, parent, false);
        return new ProsesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProsesViewHolder holder, int position) {
        ProsesModel proses = prosesModelList.get(position);
        holder.bind(proses);
    }

    @Override
    public int getItemCount() {
        return prosesModelList.size();
    }

    public class ProsesViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView foregroundCard;
        private ImageView ivFotoBan;
        private TextView tvNamaBan, tvIdProses, tvCreatedAt;
        private View updateAction, deleteAction;

        public ProsesViewHolder(@NonNull View itemView) {
            super(itemView);

            foregroundCard = itemView.findViewById(R.id.foregroundCard);
            ivFotoBan = itemView.findViewById(R.id.ivFotoBan);
            tvNamaBan = itemView.findViewById(R.id.tvNamaBan);
            tvIdProses = itemView.findViewById(R.id.tvIdProses);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
            updateAction = itemView.findViewById(R.id.updateAction);
            deleteAction = itemView.findViewById(R.id.deleteAction);

            setupClickListeners();
        }

        private void setupClickListeners() {
            // Card click listener
            foregroundCard.setOnClickListener(v -> {
                if (listener != null) {
                    animateCardClick(foregroundCard);
                    listener.onItemClick(prosesModelList.get(getAdapterPosition()));
                }
            });

            // Long click listener
            foregroundCard.setOnLongClickListener(v -> {
                if (listener != null) {
                    animateCardLongClick(foregroundCard);
                    listener.onItemLongClick(prosesModelList.get(getAdapterPosition()));
                    return true;
                }
                return false;
            });

            // Swipe action listeners
            updateAction.setOnClickListener(v -> {
                if (swipeListener != null) {
                    animateActionClick(updateAction);
                    swipeListener.onUpdateClick(prosesModelList.get(getAdapterPosition()));
                }
            });

            deleteAction.setOnClickListener(v -> {
                if (swipeListener != null) {
                    animateActionClick(deleteAction);
                    swipeListener.onDeleteClick(prosesModelList.get(getAdapterPosition()));
                }
            });
        }

        public void bind(ProsesModel proses) {
            tvIdProses.setText("ID: " + proses.getId_proses());
            tvCreatedAt.setText(proses.getCreated_at());

            // Cari data ban berdasarkan id_ban dari proses
            BanModel banData = findBanById(proses.getId_ban());
            if (banData != null) {
                tvNamaBan.setText(banData.getNama_ban());
                loadProfileImage(banData.getFoto_ban());
            } else {
                // Jika data ban tidak ditemukan, tampilkan ID ban
                tvNamaBan.setText("Ban ID: " + proses.getId_ban());
                ivFotoBan.setImageResource(R.mipmap.ic_tire_foreground);
            }

            animateItemEntry();
        }

        private BanModel findBanById(String banId) {
            for (BanModel ban : banList) {
                if (ban.getId_ban().equals(banId)) {
                    return ban;
                }
            }
            return null;
        }

        private void loadProfileImage(String profileUrl) {
            if (profileUrl != null && !profileUrl.isEmpty()) {
                if (profileUrl.startsWith("data:image") || profileUrl.length() > 100) {
                    // Base64 encoded image
                    try {
                        byte[] decodedBytes = Base64.decode(profileUrl, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                        // Create circular bitmap
                        Glide.with(context)
                                .load(bitmap)
                                .circleCrop()
                                .into(ivFotoBan);
                    } catch (Exception e) {
                        ivFotoBan.setImageResource(R.mipmap.ic_tire_foreground);
                    }
                } else {
                    // URL image
                    Glide.with(context)
                            .load(profileUrl)
                            .apply(new RequestOptions()
                                    .placeholder(R.mipmap.ic_tire_foreground)
                                    .error(R.mipmap.ic_tire_foreground)
                                    .circleCrop())
                            .into(ivFotoBan);
                }
            } else {
                // Set default avatar
                ivFotoBan.setImageResource(R.mipmap.ic_tire_foreground);
            }
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