package com.example.vikorsistemahmadban.activity.admin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vikorsistemahmadban.R;
import com.example.vikorsistemahmadban.adapter.BanAdapter;
import com.example.vikorsistemahmadban.api.JDBCConnection;
import com.example.vikorsistemahmadban.databinding.ActivityDataBanBinding;
import com.example.vikorsistemahmadban.model.BanModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DataBanActivity extends AppCompatActivity implements BanAdapter.OnItemClickListener, BanAdapter.OnSwipeActionListener {
    private ActivityDataBanBinding binding;
    private BanAdapter banAdapter;
    private List<BanModel> banModelList;
    private JDBCConnection jdbcConnection;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ImageView currentImageView;
    private String selectedImageBase64 = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDataBanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupImagePicker();
        setupCameraLauncher();
        setupRecyclerView();
        setupSearchFunctionality();
        loadBanData();
    }

    private void initializeViews() {
        jdbcConnection = new JDBCConnection();
        banModelList = new ArrayList<>();

        // Setup FAB click listener
        binding.fabAddBan.setOnClickListener(v -> showAddBanDialog());

        // Initial state
        showLoading(true);
    }

    private void setupRecyclerView() {
        banAdapter = new BanAdapter(this);
        banAdapter.setOnItemClickListener(this);
        banAdapter.setOnSwipeActionListener(this);

        binding.rvBan.setLayoutManager(new LinearLayoutManager(this));
        binding.rvBan.setAdapter(banAdapter);

        // Setup swipe functionality
        setupSwipeToRevealActions();
    }

    private void setupSwipeToRevealActions() {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            private final float SWIPE_THRESHOLD = 0.3f; // 30% of item width

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    BanModel ban = banModelList.get(position);

                    if (direction == ItemTouchHelper.RIGHT) {
                        // Swipe right - Update action
                        showEditBanDialog(ban);
                    } else if (direction == ItemTouchHelper.LEFT) {
                        // Swipe left - Delete action
                        showDeleteConfirmationDialog(ban);
                    }
                }

                // Reset the item position after action
                banAdapter.notifyItemChanged(position);
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return SWIPE_THRESHOLD;
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    float itemWidth = itemView.getWidth();
                    float swipeProgress = Math.abs(dX) / itemWidth;

                    // Limit swipe distance
                    float maxSwipeDistance = itemWidth * 0.5f; // Maximum 50% of item width
                    if (Math.abs(dX) > maxSwipeDistance) {
                        dX = dX > 0 ? maxSwipeDistance : -maxSwipeDistance;
                    }

                    // Draw background and icons based on swipe direction
                    if (dX > 0) {
                        // Swipe right - Update action
                        drawSwipeBackground(c, itemView, dX, ContextCompat.getColor(DataBanActivity.this, R.color.success_green),
                                ContextCompat.getDrawable(DataBanActivity.this, R.mipmap.ic_edit_foreground), true, swipeProgress);
                    } else if (dX < 0) {
                        // Swipe left - Delete action
                        drawSwipeBackground(c, itemView, dX, ContextCompat.getColor(DataBanActivity.this, R.color.error_red),
                                ContextCompat.getDrawable(DataBanActivity.this, R.mipmap.ic_delete_foreground), false, swipeProgress);
                    }

                    // Apply translation to the item view
                    itemView.setTranslationX(dX);
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            }

            @Override
            public void onChildDrawOver(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                // Add visual feedback when swipe threshold is reached
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && isCurrentlyActive) {
                    View itemView = viewHolder.itemView;
                    float itemWidth = itemView.getWidth();
                    float swipeProgress = Math.abs(dX) / itemWidth;

                    if (swipeProgress >= SWIPE_THRESHOLD) {
                        // Add a subtle vibration effect or visual feedback
                        itemView.setAlpha(0.8f);
                    } else {
                        itemView.setAlpha(1.0f);
                    }
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                // Reset view state
                viewHolder.itemView.setAlpha(1.0f);
                viewHolder.itemView.setTranslationX(0f);
            }
        });

        itemTouchHelper.attachToRecyclerView(binding.rvBan);
    }

    private void drawSwipeBackground(Canvas c, View itemView, float dX, int backgroundColor, Drawable icon, boolean isRightSwipe, float swipeProgress) {
        ColorDrawable background = new ColorDrawable(backgroundColor);

        if (isRightSwipe) {
            // Right swipe background
            background.setBounds(itemView.getLeft(), itemView.getTop(),
                    itemView.getLeft() + (int) Math.abs(dX), itemView.getBottom());
        } else {
            // Left swipe background
            background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(),
                    itemView.getRight(), itemView.getBottom());
        }

        background.draw(c);

        // Draw icon with animation based on swipe progress
        if (icon != null) {
            int iconSize = (int) (48 + (swipeProgress * 16));
            int iconMargin = 48;
            int iconTop = itemView.getTop() + (itemView.getHeight() - iconSize) / 2;
            int iconBottom = iconTop + iconSize;

            if (isRightSwipe) {
                int iconLeft = itemView.getLeft() + iconMargin;
                int iconRight = iconLeft + iconSize;
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            } else {
                int iconRight = itemView.getRight() - iconMargin;
                int iconLeft = iconRight - iconSize;
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            }

            // Set icon alpha based on swipe progress
            icon.setAlpha((int) (255 * Math.min(swipeProgress * 2, 1.0f)));
            icon.draw(c);
            icon.setAlpha(255);
        }
    }

    private void setupSearchFunctionality() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (banAdapter != null) {
                    banAdapter.filter(s.toString());
                    updateEmptyState();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadBanData() {
        new LoadBansTask().execute();
    }

    private void showLoading(boolean show) {
        if (show) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.rvBan.setVisibility(View.GONE);
            binding.emptyStateLayout.setVisibility(View.GONE);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            updateEmptyState();
        }
    }

    private void updateEmptyState() {
        if (banModelList.isEmpty()) {
            binding.rvBan.setVisibility(View.GONE);
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            binding.rvBan.setVisibility(View.VISIBLE);
            binding.emptyStateLayout.setVisibility(View.GONE);
        }

        // Update Ban count
        binding.tvBanCount.setText(banModelList.size() + " Ban");
    }

    // BanAdapter.OnItemClickListener implementation
    @Override
    public void onItemClick(BanModel ban) {
        showBanDetailDialog(ban);
    }

    @Override
    public void onItemLongClick(BanModel ban) {
        showBanOptionsDialog(ban);
    }

    // BanAdapter.OnSwipeActionListener implementation
    @Override
    public void onUpdateClick(BanModel ban) {
        showEditBanDialog(ban);
    }

    @Override
    public void onDeleteClick(BanModel ban) {
        showDeleteConfirmationDialog(ban);
    }

    private void showBanDetailDialog(BanModel ban) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ban_detail, null);

        // Initialize views
        ImageView ivFotoBanDetail = dialogView.findViewById(R.id.ivFotoBanDetail);
        TextView tvNamaBanDetail = dialogView.findViewById(R.id.tvNamaBanDetail);
        TextView tvHargaDetail = dialogView.findViewById(R.id.tvHargaDetail);
        TextView tvIdBanDetail = dialogView.findViewById(R.id.tvIdBanDetail);
        TextView tvDeskripsiDetail = dialogView.findViewById(R.id.tvDeskripsiDetail);
        TextView tvCreatedAtDetail = dialogView.findViewById(R.id.tvCreatedAtDetail);

        // Set data
        tvNamaBanDetail.setText(ban.getNama_ban());
        try {
            NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
            double hargaValue = Double.parseDouble(ban.getHarga());
            String hargaFormatted = formatRupiah.format(hargaValue);
            tvHargaDetail.setText(hargaFormatted);
        } catch (NumberFormatException e) {
            tvHargaDetail.setText("Rp " + ban.getHarga());
        }
        tvIdBanDetail.setText("ID: " + ban.getId_ban());
        tvDeskripsiDetail.setText(ban.getDeskripsi());
        tvCreatedAtDetail.setText(ban.getCreated_at());

        // Load image - Handle null/empty
        if (ban.getFoto_ban() != null && !ban.getFoto_ban().trim().isEmpty()) {
            if (ban.getFoto_ban().startsWith("data:image") || ban.getFoto_ban().length() > 100) {
                // Base64 image
                try {
                    byte[] decodedBytes = Base64.decode(ban.getFoto_ban(), Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    ivFotoBanDetail.setImageBitmap(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                    ivFotoBanDetail.setImageResource(R.mipmap.ic_tire_foreground);
                }
            } else {
                // URL image
                Glide.with(this)
                        .load(ban.getFoto_ban())
                        .placeholder(R.mipmap.ic_tire_foreground)
                        .error(R.mipmap.ic_tire_foreground)
                        .circleCrop()
                        .into(ivFotoBanDetail);
            }
        } else {
            // No image - set default
            ivFotoBanDetail.setImageResource(R.mipmap.ic_tire_foreground);
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setView(dialogView)
                .setTitle("Detail Ban")
                .setPositiveButton("Tutup", null)
                .setNeutralButton("Edit", (dialog, which) -> showEditBanDialog(ban))
                .show();
    }

    private void showBanOptionsDialog(BanModel ban) {
        String[] options = {"Edit Ban", "Delete Ban", "View Details"};

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Pilih Aksi")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showEditBanDialog(ban);
                            break;
                        case 1:
                            showDeleteConfirmationDialog(ban);
                            break;
                        case 2:
                            showBanDetailDialog(ban);
                            break;
                    }
                })
                .show();
    }

    private void showAddBanDialog() {
        showBanFormDialog(null, "Tambah Ban Baru");
    }

    private void showEditBanDialog(BanModel ban) {
        showBanFormDialog(ban, "Edit Ban");
    }

    private void showBanFormDialog(BanModel ban, String title) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ban_form, null);

        EditText etNamaBan = dialogView.findViewById(R.id.etNamaBan);
        EditText etHarga = dialogView.findViewById(R.id.etHarga);
        EditText etDeskripsi = dialogView.findViewById(R.id.etDeskripsi);
        ImageView ivFotoBan = dialogView.findViewById(R.id.ivFotoBan);

        // Setup image click listener
        currentImageView = ivFotoBan;
        selectedImageBase64 = ""; // Reset selected image

        ivFotoBan.setOnClickListener(v -> openImagePicker());

        // Fill data if editing
        if (ban != null) {
            etNamaBan.setText(ban.getNama_ban());
            etHarga.setText(ban.getHarga());
            etDeskripsi.setText(ban.getDeskripsi());

            // Load existing image - Handle null/empty
            if (ban.getFoto_ban() != null && !ban.getFoto_ban().trim().isEmpty()) {
                if (ban.getFoto_ban().startsWith("data:image") || ban.getFoto_ban().length() > 100) {
                    // Base64 image
                    try {
                        byte[] decodedBytes = Base64.decode(ban.getFoto_ban(), Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        ivFotoBan.setImageBitmap(bitmap);
                        selectedImageBase64 = ban.getFoto_ban(); // Keep existing image
                    } catch (Exception e) {
                        e.printStackTrace();
                        ivFotoBan.setImageResource(R.mipmap.ic_tire_foreground);
                        selectedImageBase64 = ""; // Reset if decode fails
                    }
                } else {
                    // URL image
                    Glide.with(this)
                            .load(ban.getFoto_ban())
                            .placeholder(R.mipmap.ic_tire_foreground)
                            .error(R.mipmap.ic_tire_foreground)
                            .circleCrop()
                            .into(ivFotoBan);
                    selectedImageBase64 = ban.getFoto_ban(); // Keep existing image
                }
            } else {
                // No image - set default
                ivFotoBan.setImageResource(R.mipmap.ic_tire_foreground);
                selectedImageBase64 = "";
            }

        } else {
            // New data - set default image
            ivFotoBan.setImageResource(R.mipmap.ic_tire_foreground);
            selectedImageBase64 = "";
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setView(dialogView)
                .setTitle(title)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String namaBan = etNamaBan.getText().toString().trim();
                    String hargaBan = etHarga.getText().toString().trim();
                    String deskripsiBan = etDeskripsi.getText().toString().trim();

                    // Handle image - dapat null/kosong
                    String fotoBan = null;
                    if (ban != null && !selectedImageBase64.isEmpty()) {
                        // Editing with new image selected
                        fotoBan = selectedImageBase64;
                    } else if (ban != null && selectedImageBase64.isEmpty()) {
                        // Editing but no new image selected - keep existing or null
                        fotoBan = (ban.getFoto_ban() != null && !ban.getFoto_ban().trim().isEmpty())
                                ? ban.getFoto_ban() : null;
                    } else if (ban == null && !selectedImageBase64.isEmpty()) {
                        // New data with image selected
                        fotoBan = selectedImageBase64;
                    }
                    // Else: new data without image - remains null

                    if (namaBan.isEmpty() || hargaBan.isEmpty() || deskripsiBan.isEmpty()) {
                        Toast.makeText(this, "Nama Produk Ban, Harga, dan Deskripsi harus diisi!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    BanModel newBan = new BanModel(
                            ban != null ? ban.getId_ban() : null,
                            namaBan,
                            hargaBan,
                            deskripsiBan,
                            fotoBan,
                            ban != null ? ban.getCreated_at() : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date())
                    );

                    if (ban == null) {
                        // Add new ban
                        new AddBanTask().execute(newBan);
                    } else {
                        // Update existing ban
                        new UpdateBanTask().execute(newBan);
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showDeleteConfirmationDialog(BanModel ban) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Konfirmasi Hapus")
                .setMessage("Apakah Anda yakin ingin menghapus data \"" + ban.getNama_ban() + "\"?")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    new DeleteBanTask().execute(ban.getId_ban());
                })
                .setNegativeButton("Batal", null)
                .setIcon(R.mipmap.ic_delete_foreground)
                .show();
    }

    private class LoadBansTask extends AsyncTask<Void, Void, List<BanModel>> {
        @Override
        protected List<BanModel> doInBackground(Void... voids) {
            List<BanModel> bans = new ArrayList<>();
            Connection conn = null;
            try {
                conn = jdbcConnection.getConnection();
                if (conn != null) {
                    String query = "SELECT * " +
                            "FROM tb_ban";

                    PreparedStatement ps = conn.prepareStatement(query);
                    ResultSet rs = ps.executeQuery();

                    while (rs.next()) {
                        BanModel ban = new BanModel(
                                rs.getString("id_ban"),
                                rs.getString("nama_ban"),
                                rs.getString("harga"),
                                rs.getString("deskripsi"),
                                rs.getString("foto_ban"),
                                rs.getString("created_at")
                        );
                        bans.add(ban);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                JDBCConnection.closeConnection(conn);
            }
            return bans;
        }

        @Override
        protected void onPostExecute(List<BanModel> bans) {
            banModelList = bans;
            banAdapter.setBanList(bans);
            showLoading(false);
        }
    }

    private class AddBanTask extends AsyncTask<BanModel, Void, Boolean> {
        private BanModel newBan;

        @Override
        protected Boolean doInBackground(BanModel... bans) {
            newBan = bans[0];
            Connection conn = null;
            try {
                conn = jdbcConnection.getConnection();
                if (conn != null) {
                    String query = "INSERT INTO tb_ban (nama_ban, harga, deskripsi, foto_ban, created_at) VALUES (?, ?, ?, ?, ?)";
                    PreparedStatement ps = conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);
                    ps.setString(1, newBan.getNama_ban());
                    ps.setString(2, newBan.getHarga());
                    ps.setString(3, newBan.getDeskripsi());
                    // Handle image - bisa null
                    if (newBan.getFoto_ban() != null && !newBan.getFoto_ban().trim().isEmpty()) {
                        ps.setString(4, newBan.getFoto_ban());
                    } else {
                        ps.setNull(4, java.sql.Types.LONGVARCHAR); // atau TEXT/CLOB tergantung database
                    }
                    ps.setString(5, newBan.getCreated_at());
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                JDBCConnection.closeConnection(conn);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                banModelList.add(0, newBan);
                banAdapter.addBan(newBan);
                updateEmptyState();
                showSuccessSnackbar("Data Ban berhasil ditambahkan");
            } else {
                Toast.makeText(DataBanActivity.this, "Gagal menambahkan ban", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class UpdateBanTask extends AsyncTask<BanModel, Void, Boolean> {
        private BanModel updatedBan;

        @Override
        protected Boolean doInBackground(BanModel... bans) {
            updatedBan = bans[0];
            Connection conn = null;
            try {
                conn = jdbcConnection.getConnection();
                if (conn != null) {
                    // Update
                    String query = "UPDATE tb_ban SET nama_ban=?, harga=?, deskripsi=?, foto_ban=? WHERE id_ban=?";
                    PreparedStatement ps = conn.prepareStatement(query);
                    ps.setString(1, updatedBan.getNama_ban());
                    ps.setString(2, updatedBan.getHarga());
                    ps.setString(3, updatedBan.getDeskripsi());
                    if (updatedBan.getFoto_ban() != null && !updatedBan.getFoto_ban().trim().isEmpty()) {
                        ps.setString(4, updatedBan.getFoto_ban());
                    } else {
                        ps.setNull(4, java.sql.Types.LONGVARCHAR); // atau TEXT/CLOB tergantung database
                    }
                    ps.setString(5, updatedBan.getId_ban());
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                JDBCConnection.closeConnection(conn);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                // Update local list
                for (int i = 0; i < banModelList.size(); i++) {
                    if (banModelList.get(i).getId_ban().equals(updatedBan.getId_ban())) {
                        banModelList.set(i, updatedBan);
                        break;
                    }
                }
                banAdapter.updateBan(updatedBan);
                showSuccessSnackbar("Ban berhasil diupdate");
            } else {
                Toast.makeText(DataBanActivity.this, "Gagal mengupdate ban", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class DeleteBanTask extends AsyncTask<String, Void, Boolean> {
        private String banId;

        @Override
        protected Boolean doInBackground(String... ids) {
            banId = ids[0];
            Connection conn = null;
            try {
                conn = jdbcConnection.getConnection();
                if (conn != null) {
                    String query = "DELETE FROM tb_ban WHERE id_ban=?";
                    PreparedStatement ps = conn.prepareStatement(query);
                    ps.setString(1, banId);

                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                JDBCConnection.closeConnection(conn);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                // Remove from local list
                banModelList.removeIf(ban -> ban.getId_ban().equals(banId));
                banAdapter.removeBan(banId);
                updateEmptyState();
                showSuccessSnackbar("Ban berhasil dihapus");
            } else {
                Toast.makeText(DataBanActivity.this, "Gagal menghapus ban", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showSuccessSnackbar(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.success_green))
                .setTextColor(Color.WHITE)
                .show();
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            try {
                                // Convert image to base64
                                selectedImageBase64 = convertImageToBase64(selectedImageUri);

                                // Display selected image
                                if (currentImageView != null) {
                                    Glide.with(this)
                                            .load(selectedImageUri)
                                            .circleCrop()
                                            .into(currentImageView);
                                }

                                Toast.makeText(this, "Gambar berhasil dipilih", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );
    }

    // Tambahkan method baru ini
    private String convertImageToBase64(Uri imageUri) throws FileNotFoundException {
        InputStream inputStream = getContentResolver().openInputStream(imageUri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

        // Resize image untuk menghemat storage
        bitmap = resizeBitmap(bitmap, 400, 400);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    // Tambahkan method baru ini
    private Bitmap resizeBitmap(Bitmap originalBitmap, int maxWidth, int maxHeight) {
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxWidth;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxHeight;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(originalBitmap, width, height, true);
    }

    // Tambahkan method baru ini
    private void openImagePicker() {
        // Cek permission untuk Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 100);
                return;
            }
        } else {
            // Untuk Android 12 dan dibawah
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
                return;
            }
        }

        // Tampilkan dialog pilihan untuk memilih sumber gambar
        showImageSourceDialog();
    }

    private void showImageSourceDialog() {
        String[] options = {"Galeri", "Kamera", "Batal"};

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Pilih Sumber Gambar")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openGallery();
                            break;
                        case 1:
                            openCamera();
                            break;
                        case 2:
                            dialog.dismiss();
                            break;
                    }
                })
                .show();
    }

    // Method untuk membuka galeri
    private void openGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            // Alternatif intent jika yang pertama tidak work
            if (intent.resolveActivity(getPackageManager()) == null) {
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
            }

            imagePickerLauncher.launch(Intent.createChooser(intent, "Pilih Gambar"));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Tidak dapat membuka galeri", Toast.LENGTH_SHORT).show();
        }
    }

    // Method untuk membuka kamera (opsional)
    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 101);
            return;
        }

        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                cameraLauncher.launch(intent);
            } else {
                Toast.makeText(this, "Kamera tidak tersedia", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Tidak dapat membuka kamera", Toast.LENGTH_SHORT).show();
        }
    }

    // Tambahkan launcher untuk kamera di bagian deklarasi variabel
    private ActivityResultLauncher<Intent> cameraLauncher;

    // Inisialisasi camera launcher di onCreate atau setupImagePicker
    private void setupCameraLauncher() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null) {
                            Bitmap imageBitmap = (Bitmap) extras.get("data");
                            if (imageBitmap != null) {
                                try {
                                    // Convert bitmap to base64
                                    selectedImageBase64 = convertBitmapToBase64(imageBitmap);

                                    // Display image
                                    if (currentImageView != null) {
                                        Glide.with(this)
                                                .load(imageBitmap)
                                                .circleCrop()
                                                .into(currentImageView);
                                    }

                                    Toast.makeText(this, "Foto berhasil diambil", Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(this, "Gagal memproses foto", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                }
        );
    }

    // Method untuk convert bitmap ke base64
    private String convertBitmapToBase64(Bitmap bitmap) {
        bitmap = resizeBitmap(bitmap, 400, 400);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    // Override onRequestPermissionsResult
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 100: // READ_EXTERNAL_STORAGE atau READ_MEDIA_IMAGES
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showImageSourceDialog();
                } else {
                    Toast.makeText(this, "Permission ditolak. Tidak dapat mengakses gambar.", Toast.LENGTH_SHORT).show();
                }
                break;

            case 101: // CAMERA
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Permission kamera ditolak.", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}