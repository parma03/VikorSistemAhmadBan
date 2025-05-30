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
import com.google.android.material.card.MaterialCardView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BanAdapter extends RecyclerView.Adapter<BanAdapter.BanViewHolder> {
    private List<BanModel> banModelList;
    private List<BanModel> banModelListFull;
    private Context context;
    private OnItemClickListener listener;
    private OnSwipeActionListener swipeListener;

    public interface OnItemClickListener {
        void onItemClick(BanModel ban);
        void onItemLongClick(BanModel ban);
    }

    public interface OnSwipeActionListener {
        void onUpdateClick(BanModel ban);
        void onDeleteClick(BanModel ban);
    }

    public BanAdapter(Context context) {
        this.context = context;
        this.banModelList = new ArrayList<>();
        this.banModelListFull = new ArrayList<>();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnSwipeActionListener(OnSwipeActionListener listener) {
        this.swipeListener = listener;
    }

    public void setBanList(List<BanModel> bans) {
        this.banModelList = new ArrayList<>(bans);
        this.banModelListFull = new ArrayList<>(bans);
        notifyDataSetChanged();
    }

    public void addBan(BanModel ban) {
        banModelList.add(0, ban);
        banModelListFull.add(0, ban);
        notifyItemInserted(0);
    }

    public void updateBan(BanModel updatedBan) {
        for (int i = 0; i < banModelList.size(); i++) {
            if (banModelList.get(i).getId_ban().equals(updatedBan.getId_ban())) {
                banModelList.set(i, updatedBan);
                notifyItemChanged(i);
                break;
            }
        }
        // Update full list too
        for (int i = 0; i < banModelListFull.size(); i++) {
            if (banModelListFull.get(i).getId_ban().equals(updatedBan.getId_ban())) {
                banModelListFull.set(i, updatedBan);
                break;
            }
        }
    }

    public void removeBan(String banId) {
        for (int i = 0; i < banModelList.size(); i++) {
            if (banModelList.get(i).getId_ban().equals(banId)) {
                banModelList.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
        // Remove from full list too
        for (int i = 0; i < banModelListFull.size(); i++) {
            if (banModelListFull.get(i).getId_ban().equals(banId)) {
                banModelListFull.remove(i);
                break;
            }
        }
    }

    public void filter(String query) {
        banModelList.clear();
        if (query.isEmpty()) {
            banModelList.addAll(banModelListFull);
        } else {
            String filterPattern = query.toLowerCase().trim();
            for (BanModel ban : banModelListFull) {
                if (ban.getNama_ban().toLowerCase().contains(filterPattern) ||
                        ban.getId_ban().toLowerCase().contains(filterPattern) ||
                        ban.getHarga().toLowerCase().contains(filterPattern) ||
                        ban.getCreated_at().toLowerCase().contains(filterPattern)) {
                    banModelList.add(ban);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_ban, parent, false);
        return new BanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BanViewHolder holder, int position) {
        BanModel ban = banModelList.get(position);
        holder.bind(ban);
    }

    @Override
    public int getItemCount() {
        return banModelList.size();
    }

    public class BanViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView foregroundCard;
        private ImageView ivFotoBan;
        private TextView tvNamaBan, tvIdBan, tvHarga, tvDeskripsi, tvCreatedAt;
        private View updateAction, deleteAction;

        public BanViewHolder(@NonNull View itemView) {
            super(itemView);

            foregroundCard = itemView.findViewById(R.id.foregroundCard);
            ivFotoBan = itemView.findViewById(R.id.ivFotoBan);
            tvNamaBan = itemView.findViewById(R.id.tvNamaBan);
            tvIdBan = itemView.findViewById(R.id.tvIdBan);
            tvHarga = itemView.findViewById(R.id.tvHarga);
            tvDeskripsi = itemView.findViewById(R.id.tvDeskripsi);
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
                    listener.onItemClick(banModelList.get(getAdapterPosition()));
                }
            });

            // Long click listener
            foregroundCard.setOnLongClickListener(v -> {
                if (listener != null) {
                    animateCardLongClick(foregroundCard);
                    listener.onItemLongClick(banModelList.get(getAdapterPosition()));
                    return true;
                }
                return false;
            });

            // Swipe action listeners
            updateAction.setOnClickListener(v -> {
                if (swipeListener != null) {
                    animateActionClick(updateAction);
                    swipeListener.onUpdateClick(banModelList.get(getAdapterPosition()));
                }
            });

            deleteAction.setOnClickListener(v -> {
                if (swipeListener != null) {
                    animateActionClick(deleteAction);
                    swipeListener.onDeleteClick(banModelList.get(getAdapterPosition()));
                }
            });
        }

        public void bind(BanModel ban) {
            tvNamaBan.setText(ban.getNama_ban());
            tvIdBan.setText("ID: " + ban.getId_ban());
            try {
                NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
                double hargaValue = Double.parseDouble(ban.getHarga());
                String hargaFormatted = formatRupiah.format(hargaValue);
                tvHarga.setText(hargaFormatted);
            } catch (NumberFormatException e) {
                tvHarga.setText("Rp " + ban.getHarga());
            }
            tvNamaBan.setText(ban.getNama_ban());
            tvDeskripsi.setText(ban.getDeskripsi());
            tvCreatedAt.setText(ban.getCreated_at());

            loadProfileImage(ban.getFoto_ban());
            animateItemEntry();
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
