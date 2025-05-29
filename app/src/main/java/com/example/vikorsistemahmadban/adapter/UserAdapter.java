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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.vikorsistemahmadban.R;
import com.example.vikorsistemahmadban.model.UserModel;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<UserModel> userList;
    private List<UserModel> userListFull; // For search functionality
    private Context context;
    private OnItemClickListener listener;
    private OnSwipeActionListener swipeListener;

    public interface OnItemClickListener {
        void onItemClick(UserModel user);
        void onItemLongClick(UserModel user);
    }

    public interface OnSwipeActionListener {
        void onUpdateClick(UserModel user);
        void onDeleteClick(UserModel user);
    }

    public UserAdapter(Context context) {
        this.context = context;
        this.userList = new ArrayList<>();
        this.userListFull = new ArrayList<>();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnSwipeActionListener(OnSwipeActionListener listener) {
        this.swipeListener = listener;
    }

    public void setUserList(List<UserModel> users) {
        this.userList = new ArrayList<>(users);
        this.userListFull = new ArrayList<>(users);
        notifyDataSetChanged();
    }

    public void addUser(UserModel user) {
        userList.add(0, user);
        userListFull.add(0, user);
        notifyItemInserted(0);
    }

    public void updateUser(UserModel updatedUser) {
        for (int i = 0; i < userList.size(); i++) {
            if (userList.get(i).getId_user().equals(updatedUser.getId_user())) {
                userList.set(i, updatedUser);
                notifyItemChanged(i);
                break;
            }
        }
        // Update full list too
        for (int i = 0; i < userListFull.size(); i++) {
            if (userListFull.get(i).getId_user().equals(updatedUser.getId_user())) {
                userListFull.set(i, updatedUser);
                break;
            }
        }
    }

    public void removeUser(String userId) {
        for (int i = 0; i < userList.size(); i++) {
            if (userList.get(i).getId_user().equals(userId)) {
                userList.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
        // Remove from full list too
        for (int i = 0; i < userListFull.size(); i++) {
            if (userListFull.get(i).getId_user().equals(userId)) {
                userListFull.remove(i);
                break;
            }
        }
    }

    public void filter(String query) {
        userList.clear();
        if (query.isEmpty()) {
            userList.addAll(userListFull);
        } else {
            String filterPattern = query.toLowerCase().trim();
            for (UserModel user : userListFull) {
                if (user.getNama().toLowerCase().contains(filterPattern) ||
                        user.getId_user().toLowerCase().contains(filterPattern) ||
                        user.getRole().toLowerCase().contains(filterPattern)) {
                    userList.add(user);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserModel user = userList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public class UserViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView foregroundCard;
        private ImageView ivProfile;
        private TextView tvNama, tvIdUser, tvStatus;
        private Chip chipRole;
        private View statusIndicator;
        private View updateAction, deleteAction;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);

            foregroundCard = itemView.findViewById(R.id.foregroundCard);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvNama = itemView.findViewById(R.id.tvNama);
            tvIdUser = itemView.findViewById(R.id.tvIdUser);
            chipRole = itemView.findViewById(R.id.chipRole);
            updateAction = itemView.findViewById(R.id.updateAction);
            deleteAction = itemView.findViewById(R.id.deleteAction);

            setupClickListeners();
        }

        private void setupClickListeners() {
            // Card click listener
            foregroundCard.setOnClickListener(v -> {
                if (listener != null) {
                    animateCardClick(foregroundCard);
                    listener.onItemClick(userList.get(getAdapterPosition()));
                }
            });

            // Long click listener
            foregroundCard.setOnLongClickListener(v -> {
                if (listener != null) {
                    animateCardLongClick(foregroundCard);
                    listener.onItemLongClick(userList.get(getAdapterPosition()));
                    return true;
                }
                return false;
            });

            // Swipe action listeners
            updateAction.setOnClickListener(v -> {
                if (swipeListener != null) {
                    animateActionClick(updateAction);
                    swipeListener.onUpdateClick(userList.get(getAdapterPosition()));
                }
            });

            deleteAction.setOnClickListener(v -> {
                if (swipeListener != null) {
                    animateActionClick(deleteAction);
                    swipeListener.onDeleteClick(userList.get(getAdapterPosition()));
                }
            });
        }

        public void bind(UserModel user) {
            tvNama.setText(user.getNama());
            tvIdUser.setText("ID: " + user.getId_user());

            // Set role chip
            chipRole.setText(user.getRole());
            setRoleChipColor(user.getRole());

            // Load profile image
            loadProfileImage(user.getProfile());

            // Add enter animation
            animateItemEntry();
        }

        private void setRoleChipColor(String role) {
            int backgroundColor;
            switch (role.toLowerCase()) {
                case "admin":
                    backgroundColor = ContextCompat.getColor(context, R.color.error_red);
                    break;
                case "pimpinan":
                    backgroundColor = ContextCompat.getColor(context, R.color.success_green);
                    break;
                default:
                    backgroundColor = ContextCompat.getColor(context, R.color.text_secondary);
                    break;
            }
            chipRole.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(backgroundColor));
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
                                .into(ivProfile);
                    } catch (Exception e) {
                        ivProfile.setImageResource(R.mipmap.ic_user_foreground);
                    }
                } else {
                    // URL image
                    Glide.with(context)
                            .load(profileUrl)
                            .apply(new RequestOptions()
                                    .placeholder(R.mipmap.ic_user_foreground)
                                    .error(R.mipmap.ic_user_foreground)
                                    .circleCrop())
                            .into(ivProfile);
                }
            } else {
                // Set default avatar
                ivProfile.setImageResource(R.mipmap.ic_user_foreground);
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