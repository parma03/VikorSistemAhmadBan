package com.example.vikorsistemahmadban.activity.admin;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vikorsistemahmadban.R;
import com.example.vikorsistemahmadban.adapter.ProsesAdapter;
import com.example.vikorsistemahmadban.api.JDBCConnection;
import com.example.vikorsistemahmadban.databinding.ActivityVikorProcesBinding;
import com.example.vikorsistemahmadban.model.BanModel;
import com.example.vikorsistemahmadban.model.KriteriaModel;
import com.example.vikorsistemahmadban.model.ProsesModel;
import com.example.vikorsistemahmadban.model.SubKriteriaModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VikorProcesActivity extends AppCompatActivity implements ProsesAdapter.OnItemClickListener, ProsesAdapter.OnSwipeActionListener {
    private ActivityVikorProcesBinding binding;
    private ProsesAdapter prosesAdapter;
    private List<ProsesModel> prosesModelList;
    private List<BanModel> banModelList;
    private List<KriteriaModel> kriteriaModelList;
    private JDBCConnection jdbcConnection;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVikorProcesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupRecyclerView();
        setupSearchFunctionality();
        loadInitialData();
    }

    private void initializeViews() {
        jdbcConnection = new JDBCConnection();
        prosesModelList = new ArrayList<>();
        banModelList = new ArrayList<>();
        kriteriaModelList = new ArrayList<>();

        // Setup FAB click listener
        binding.fabAddProses.setOnClickListener(v -> showAddProsesDialog());

        // Initial state
        showLoading(true);
    }

    private void setupRecyclerView() {
        prosesAdapter = new ProsesAdapter(this);
        prosesAdapter.setOnItemClickListener(this);
        prosesAdapter.setOnSwipeActionListener(this);

        binding.rvProses.setLayoutManager(new LinearLayoutManager(this));
        binding.rvProses.setAdapter(prosesAdapter);

        // Setup swipe functionality
        setupSwipeToRevealActions();
    }

    private void setupSwipeToRevealActions() {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            private final float SWIPE_THRESHOLD = 0.3f;

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    ProsesModel proses = prosesModelList.get(position);

                    if (direction == ItemTouchHelper.RIGHT) {
                        showEditProsesDialog(proses);
                    } else if (direction == ItemTouchHelper.LEFT) {
                        showDeleteConfirmationDialog(proses);
                    }
                }
                prosesAdapter.notifyItemChanged(position);
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

                    float maxSwipeDistance = itemWidth * 0.5f;
                    if (Math.abs(dX) > maxSwipeDistance) {
                        dX = dX > 0 ? maxSwipeDistance : -maxSwipeDistance;
                    }

                    if (dX > 0) {
                        drawSwipeBackground(c, itemView, dX, ContextCompat.getColor(VikorProcesActivity.this, R.color.success_green),
                                ContextCompat.getDrawable(VikorProcesActivity.this, R.mipmap.ic_edit_foreground), true, swipeProgress);
                    } else if (dX < 0) {
                        drawSwipeBackground(c, itemView, dX, ContextCompat.getColor(VikorProcesActivity.this, R.color.error_red),
                                ContextCompat.getDrawable(VikorProcesActivity.this, R.mipmap.ic_delete_foreground), false, swipeProgress);
                    }

                    itemView.setTranslationX(dX);
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setAlpha(1.0f);
                viewHolder.itemView.setTranslationX(0f);
            }
        });

        itemTouchHelper.attachToRecyclerView(binding.rvProses);
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
                if (prosesAdapter != null) {
                    prosesAdapter.filter(s.toString());
                    updateEmptyState();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadInitialData() {
        new LoadInitialDataTask().execute();
    }

    private void showLoading(boolean show) {
        if (show) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.rvProses.setVisibility(View.GONE);
            binding.emptyStateLayout.setVisibility(View.GONE);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            updateEmptyState();
        }
    }

    private void updateEmptyState() {
        if (prosesModelList.isEmpty()) {
            binding.rvProses.setVisibility(View.GONE);
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            binding.rvProses.setVisibility(View.VISIBLE);
            binding.emptyStateLayout.setVisibility(View.GONE);
        }
        binding.tvProsesCount.setText(prosesModelList.size() + " Proses");
    }

    @Override
    public void onItemClick(ProsesModel proses) {
        showProsesDetailDialog(proses);
    }

    @Override
    public void onItemLongClick(ProsesModel proses) {
        showProsesOptionsDialog(proses);
    }

    @Override
    public void onUpdateClick(ProsesModel proses) {
        showEditProsesDialog(proses);
    }

    @Override
    public void onDeleteClick(ProsesModel proses) {
        showDeleteConfirmationDialog(proses);
    }

    private void showProsesDetailDialog(ProsesModel proses) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_proses_detail, null);

        ImageView ivFotoBanDetail = dialogView.findViewById(R.id.ivFotoBanDetail);
        TextView tvNamaBanDetail = dialogView.findViewById(R.id.tvNamaBanDetail);
        TextView tvIdProsesDetail = dialogView.findViewById(R.id.tvIdProsesDetail);
        TextView tvCreatedAtDetail = dialogView.findViewById(R.id.tvCreatedAtDetail);
        LinearLayout llKriteriaDetailContainer = dialogView.findViewById(R.id.llKriteriaDetailContainer);
        ProgressBar pbKriteriaDetailLoading = dialogView.findViewById(R.id.pbKriteriaDetailLoading);

        // Find ban data
        BanModel banData = findBanById(proses.getId_ban());
        if (banData != null) {
            tvNamaBanDetail.setText(banData.getNama_ban());
            loadImageIntoView(ivFotoBanDetail, banData.getFoto_ban());
        } else {
            tvNamaBanDetail.setText("Ban ID: " + proses.getId_ban());
            ivFotoBanDetail.setImageResource(R.mipmap.ic_tire_foreground);
        }

        tvIdProsesDetail.setText(proses.getId_proses());
        tvCreatedAtDetail.setText(proses.getCreated_at());

        // Load kriteria and subkriteria data
        new LoadProsesDetailTask(
                llKriteriaDetailContainer,
                pbKriteriaDetailLoading,
                proses.getId_ban(),
                proses.getCreated_at()
        ).execute();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setView(dialogView)
                .setTitle("Detail Proses")
                .setPositiveButton("Tutup", null)
                .setNeutralButton("Edit", (dialog, which) -> showEditProsesDialog(proses))
                .show();
    }

    private class LoadProsesDetailTask extends AsyncTask<Void, Void, List<ProsesDetailModel>> {
        private LinearLayout container;
        private ProgressBar loading;
        private String banId;
        private String createdAt; // Tambahkan parameter tanggal

        public LoadProsesDetailTask(LinearLayout container, ProgressBar loading, String banId, String createdAt) {
            this.container = container;
            this.loading = loading;
            this.banId = banId;
            this.createdAt = createdAt;
        }

        @Override
        protected void onPreExecute() {
            loading.setVisibility(View.VISIBLE);
            container.setVisibility(View.GONE);
        }

        @Override
        protected List<ProsesDetailModel> doInBackground(Void... voids) {
            Connection conn = null;
            List<ProsesDetailModel> detailList = new ArrayList<>();

            try {
                conn = jdbcConnection.getConnection();
                if (conn != null) {
                    String query = "SELECT p.id_proses, p.id_ban, p.id_kriteria, p.id_subkriteria, " +
                            "k.nama_kriteria, k.bobot, " +
                            "sk.klasifikasi, sk.bobot_sub_kriteria " +
                            "FROM tb_proses p " +
                            "JOIN tb_kriteria k ON p.id_kriteria = k.id_kriteria " +
                            "JOIN tb_subkriteria sk ON p.id_subkriteria = sk.id_subkriteria " +
                            "WHERE p.id_ban = ? AND DATE(p.created_at) = DATE(?) " +
                            "ORDER BY k.id_kriteria";

                    PreparedStatement ps = conn.prepareStatement(query);
                    ps.setString(1, banId);
                    ps.setString(2, createdAt);
                    ResultSet rs = ps.executeQuery();

                    while (rs.next()) {
                        ProsesDetailModel detail = new ProsesDetailModel(
                                rs.getString("id_proses"),
                                rs.getString("id_ban"),
                                rs.getString("id_kriteria"),
                                rs.getString("id_subkriteria"),
                                rs.getString("nama_kriteria"),
                                rs.getString("bobot"),
                                rs.getString("klasifikasi"),
                                rs.getInt("bobot_sub_kriteria")
                        );
                        detailList.add(detail);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                JDBCConnection.closeConnection(conn);
            }
            return detailList;
        }

        @Override
        protected void onPostExecute(List<ProsesDetailModel> detailList) {
            loading.setVisibility(View.GONE);
            container.setVisibility(View.VISIBLE);
            container.removeAllViews();

            if (detailList.isEmpty()) {
                TextView tvEmpty = new TextView(VikorProcesActivity.this);
                tvEmpty.setText("Tidak ada data kriteria");
                tvEmpty.setTextColor(ContextCompat.getColor(VikorProcesActivity.this, R.color.text_secondary));
                tvEmpty.setGravity(Gravity.CENTER);
                tvEmpty.setPadding(0, 32, 0, 32);
                container.addView(tvEmpty);
                return;
            }

            for (ProsesDetailModel detail : detailList) {
                View kriteriaView = LayoutInflater.from(VikorProcesActivity.this)
                        .inflate(R.layout.row_kriteria_detail, container, false);

                TextView tvKriteriaNama = kriteriaView.findViewById(R.id.tvKriteriaNama);
                TextView tvKriteriaBobot = kriteriaView.findViewById(R.id.tvKriteriaBobot);
                TextView tvSubkriteriaLabel = kriteriaView.findViewById(R.id.tvSubkriteriaLabel);
                TextView tvSubkriteriaNilai = kriteriaView.findViewById(R.id.tvSubkriteriaNilai);

                tvKriteriaNama.setText(detail.getNamaKriteria());
                tvKriteriaBobot.setText("Bobot: " + detail.getBobotKriteria());
                tvSubkriteriaLabel.setText("Pilihan:");
                tvSubkriteriaNilai.setText(detail.getKlasifikasiSubkriteria() +
                        " (Nilai: " + detail.getBobotSubkriteria() + ")");

                container.addView(kriteriaView);
            }
        }
    }

    private static class ProsesDetailModel {
        private String idProses;
        private String idBan;
        private String idKriteria;
        private String idSubkriteria;
        private String namaKriteria;
        private String bobotKriteria;
        private String klasifikasiSubkriteria;
        private int bobotSubkriteria;

        public ProsesDetailModel(String idProses, String idBan, String idKriteria, String idSubkriteria,
                                 String namaKriteria, String bobotKriteria, String klasifikasiSubkriteria,
                                 int bobotSubkriteria) {
            this.idProses = idProses;
            this.idBan = idBan;
            this.idKriteria = idKriteria;
            this.idSubkriteria = idSubkriteria;
            this.namaKriteria = namaKriteria;
            this.bobotKriteria = bobotKriteria;
            this.klasifikasiSubkriteria = klasifikasiSubkriteria;
            this.bobotSubkriteria = bobotSubkriteria;
        }

        // Getters
        public String getIdProses() { return idProses; }
        public String getIdBan() { return idBan; }
        public String getIdKriteria() { return idKriteria; }
        public String getIdSubkriteria() { return idSubkriteria; }
        public String getNamaKriteria() { return namaKriteria; }
        public String getBobotKriteria() { return bobotKriteria; }
        public String getKlasifikasiSubkriteria() { return klasifikasiSubkriteria; }
        public int getBobotSubkriteria() { return bobotSubkriteria; }
    }

    private void showProsesOptionsDialog(ProsesModel proses) {
        String[] options = {"Edit Proses", "Delete Proses", "View Details"};

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Pilih Aksi")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showEditProsesDialog(proses);
                            break;
                        case 1:
                            showDeleteConfirmationDialog(proses);
                            break;
                        case 2:
                            showProsesDetailDialog(proses);
                            break;
                    }
                })
                .show();
    }

    private void showAddProsesDialog() {
        showProsesFormDialog(null, "Tambah Proses Baru");
    }

    private void showEditProsesDialog(ProsesModel proses) {
        showProsesFormDialog(proses, "Edit Proses");
    }

    private void showProsesFormDialog(ProsesModel proses, String title) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_proses_form, null);

        AutoCompleteTextView actvBan = dialogView.findViewById(R.id.actvBan);
        LinearLayout llKriteriaContainer = dialogView.findViewById(R.id.llKriteriaContainer);
        ProgressBar pbKriteriaLoading = dialogView.findViewById(R.id.pbKriteriaLoading);
        LinearLayout llEmptyKriteria = dialogView.findViewById(R.id.llEmptyKriteria);

        // Setup Ban Dropdown
        setupBanDropdown(actvBan, proses);

        // Setup Kriteria Container
        setupKriteriaContainer(llKriteriaContainer, pbKriteriaLoading, llEmptyKriteria, proses);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        AlertDialog dialog = builder.setView(dialogView)
                .setTitle(title)
                .setPositiveButton("Simpan", null)
                .setNegativeButton("Batal", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                if (validateAndSaveProses(dialogView, proses)) {
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private void setupBanDropdown(AutoCompleteTextView actvBan, ProsesModel proses) {
        if (banModelList.isEmpty()) {
            actvBan.setEnabled(false);
            actvBan.setHint("Loading ban data...");
            return;
        }

        List<String> banNames = new ArrayList<>();
        for (BanModel ban : banModelList) {
            banNames.add(ban.getNama_ban());
        }

        ArrayAdapter<String> banAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, banNames);
        actvBan.setAdapter(banAdapter);

        if (proses != null) {
            // Edit mode - disable ban selection and set existing ban
            BanModel selectedBan = findBanById(proses.getId_ban());
            if (selectedBan != null) {
                actvBan.setText(selectedBan.getNama_ban());
            }

            // Disable the AutoCompleteTextView in edit mode
            actvBan.setEnabled(false);
            actvBan.setFocusable(false);
            actvBan.setClickable(false);

            // Optional: Add a hint or helper text to indicate why it's disabled
            actvBan.setHint("Ban tidak dapat diubah saat mengedit");
        } else {
            // Add mode - keep ban selection enabled
            actvBan.setEnabled(true);
            actvBan.setFocusable(true);
            actvBan.setClickable(true);
        }
    }

    private void setupKriteriaContainer(LinearLayout container, ProgressBar loading, LinearLayout emptyState, ProsesModel proses) {
        if (kriteriaModelList.isEmpty()) {
            loading.setVisibility(View.VISIBLE);
            container.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
            return;
        }

        loading.setVisibility(View.GONE);
        container.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        container.removeAllViews();

        for (KriteriaModel kriteria : kriteriaModelList) {
            View kriteriaView = LayoutInflater.from(this).inflate(R.layout.row_kriteria_selection, container, false);

            TextView tvKriteriaNama = kriteriaView.findViewById(R.id.tvKriteriaNama);
            TextView tvKriteriaBobot = kriteriaView.findViewById(R.id.tvKriteriaBobot);
            AutoCompleteTextView actvSubkriteria = kriteriaView.findViewById(R.id.actvSubkriteria);

            tvKriteriaNama.setText(kriteria.getNama_kriteria());
            tvKriteriaBobot.setText("Bobot: " + kriteria.getBobot());

            // Load subkriteria tanpa pre-selection untuk edit mode
            new LoadSubkriteriaTask(actvSubkriteria, kriteria.getId_kriteria(), null).execute();

            container.addView(kriteriaView);
        }
    }

    private ProsesModel findProsesForKriteria(String banId, String kriteriaId) {
        Connection conn = null;
        try {
            conn = jdbcConnection.getConnection();
            if (conn != null) {
                String query = "SELECT * FROM tb_proses WHERE id_ban = ? AND id_kriteria = ?";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setString(1, banId);
                ps.setString(2, kriteriaId);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    return new ProsesModel(
                            rs.getString("id_proses"),
                            rs.getString("id_ban"),
                            rs.getString("id_kriteria"),
                            rs.getString("id_subkriteria"),
                            rs.getString("created_at")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            JDBCConnection.closeConnection(conn);
        }
        return null;
    }

    private boolean validateAndSaveProses(View dialogView, ProsesModel existingProses) {
        AutoCompleteTextView actvBan = dialogView.findViewById(R.id.actvBan);
        LinearLayout llKriteriaContainer = dialogView.findViewById(R.id.llKriteriaContainer);

        String selectedBanName = actvBan.getText().toString().trim();
        if (selectedBanName.isEmpty()) {
            Toast.makeText(this, "Pilih ban terlebih dahulu", Toast.LENGTH_SHORT).show();
            return false;
        }

        BanModel selectedBan = findBanByName(selectedBanName);
        if (selectedBan == null) {
            Toast.makeText(this, "Ban tidak valid", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Collect basic validation data first
        List<ValidationData> validationDataList = new ArrayList<>();
        for (int i = 0; i < llKriteriaContainer.getChildCount(); i++) {
            View kriteriaView = llKriteriaContainer.getChildAt(i);
            AutoCompleteTextView actvSubkriteria = kriteriaView.findViewById(R.id.actvSubkriteria);

            String selectedSubkriteriaText = actvSubkriteria.getText().toString().trim();
            if (selectedSubkriteriaText.isEmpty()) {
                Toast.makeText(this, "Pilih semua subkriteria", Toast.LENGTH_SHORT).show();
                return false;
            }

            ValidationData validationData = new ValidationData();
            validationData.subkriteriaName = selectedSubkriteriaText;
            validationData.kriteriaId = kriteriaModelList.get(i).getId_kriteria();
            validationData.banId = selectedBan.getId_ban();
            // Remove the created_at assignment - will be handled in AsyncTask
            validationDataList.add(validationData);
        }

        // Now validate and save using AsyncTask
        new ValidateAndSaveProsesTask(existingProses, validationDataList).execute();
        return true;
    }

    // Helper class for validation data
    private static class ValidationData {
        String subkriteriaName;
        String kriteriaId;
        String banId;
    }

    // New AsyncTask for validation and saving
    private class ValidateAndSaveProsesTask extends AsyncTask<Void, Void, Boolean> {
        private ProsesModel existingProses;
        private List<ValidationData> validationDataList;
        private List<ProsesModel> prosesDataList;
        private String errorMessage;

        public ValidateAndSaveProsesTask(ProsesModel existingProses, List<ValidationData> validationDataList) {
            this.existingProses = existingProses;
            this.validationDataList = validationDataList;
            this.prosesDataList = new ArrayList<>();
        }

        @Override
        protected void onPreExecute() {
            // Show loading indicator
            Toast.makeText(VikorProcesActivity.this, "Memvalidasi data...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            Connection conn = null;
            try {
                conn = jdbcConnection.getConnection();
                if (conn == null) {
                    errorMessage = "Gagal terhubung ke database";
                    return false;
                }

                // Get current date for new/updated records
                String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                // For validation, we need the ban ID
                String banId = validationDataList.get(0).banId;

                // Check if data already exists for this ban on current date (for add mode)
                // Or if data exists for different ban on current date (for edit mode)
                if (!validateDuplicateData(conn, banId, currentDate)) {
                    return false;
                }

                // Validate all subkriteria selections
                for (int i = 0; i < validationDataList.size(); i++) {
                    ValidationData validationData = validationDataList.get(i);

                    SubKriteriaModel selectedSubkriteria = findSubkriteriaByNameInBackground(
                            conn, validationData.subkriteriaName, validationData.kriteriaId);

                    if (selectedSubkriteria == null) {
                        errorMessage = "Subkriteria tidak valid: " + validationData.subkriteriaName;
                        return false;
                    }

                    ProsesModel prosesData = new ProsesModel(
                            existingProses != null ? existingProses.getId_proses() : null,
                            validationData.banId,
                            validationData.kriteriaId,
                            selectedSubkriteria.getId_subkriteria(),
                            currentDate // Always use current date
                    );
                    prosesDataList.add(prosesData);
                }

                // Save the data
                if (existingProses != null) {
                    return updateProsesInBackground(conn, prosesDataList);
                } else {
                    return insertProsesInBackground(conn, prosesDataList);
                }

            } catch (SQLException e) {
                e.printStackTrace();
                errorMessage = "Error database: " + e.getMessage();
                return false;
            } finally {
                JDBCConnection.closeConnection(conn);
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                showSuccessSnackbar(existingProses != null ? "Proses berhasil diupdate" : "Proses berhasil ditambahkan");
                loadInitialData(); // Reload data
            } else {
                Toast.makeText(VikorProcesActivity.this,
                        errorMessage != null ? errorMessage : "Gagal menyimpan proses",
                        Toast.LENGTH_LONG).show();
            }
        }

        /**
         * Validate if data already exists for the same ban on the same date
         */
        /**
         * Validate if data already exists for the same ban on the same date
         */
        private boolean validateDuplicateData(Connection conn, String banId, String currentDate) throws SQLException {
            String query;
            PreparedStatement ps;

            if (existingProses != null) {
                // For update mode: check if there's another record with same ban_id on current date
                // that is NOT the record being updated (different created_at)
                query = "SELECT COUNT(*) as count FROM tb_proses WHERE id_ban = ? AND DATE(created_at) = DATE(?) AND DATE(created_at) != DATE(?)";
                ps = conn.prepareStatement(query);
                ps.setString(1, banId);
                ps.setString(2, currentDate);
                ps.setString(3, existingProses.getCreated_at()); // Exclude the existing record's date

                System.out.println("DEBUG UPDATE - Checking ban: " + banId + ", current date: " + currentDate + ", existing date: " + existingProses.getCreated_at());
            } else {
                // For add mode: check if there's any record with same ban_id on current date
                query = "SELECT COUNT(*) as count FROM tb_proses WHERE id_ban = ? AND DATE(created_at) = DATE(?)";
                ps = conn.prepareStatement(query);
                ps.setString(1, banId);
                ps.setString(2, currentDate);

                System.out.println("DEBUG ADD - Checking ban: " + banId + ", current date: " + currentDate);
            }

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int count = rs.getInt("count");
                System.out.println("DEBUG - Count result: " + count);

                if (count > 0) {
                    BanModel banData = findBanById(banId);
                    String banName = banData != null ? banData.getNama_ban() : "Ban ID: " + banId;

                    if (existingProses != null) {
                        errorMessage = "Data untuk ban \"" + banName + "\" sudah ada pada tanggal hari ini dengan tanggal yang berbeda dari data yang sedang diedit.";
                    } else {
                        errorMessage = "Data untuk ban \"" + banName + "\" sudah ada pada tanggal hari ini. Pilih ban lain atau coba lagi besok.";
                    }
                    return false;
                }
            }
            return true;
        }

        private SubKriteriaModel findSubkriteriaByNameInBackground(Connection conn, String subkriteriaName, String kriteriaId) throws SQLException {
            String query = "SELECT * FROM tb_subkriteria WHERE id_kriteria = ? AND klasifikasi = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, kriteriaId);
            ps.setString(2, subkriteriaName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new SubKriteriaModel(
                        rs.getString("id_subkriteria"),
                        rs.getString("id_kriteria"),
                        rs.getString("klasifikasi"),
                        rs.getInt("bobot_sub_kriteria")
                );
            }
            return null;
        }

        private boolean insertProsesInBackground(Connection conn, List<ProsesModel> prosesDataList) throws SQLException {
            conn.setAutoCommit(false);
            String query = "INSERT INTO tb_proses (id_ban, id_kriteria, id_subkriteria, created_at) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(query);

            try {
                for (ProsesModel proses : prosesDataList) {
                    ps.setString(1, proses.getId_ban());
                    ps.setString(2, proses.getId_kriteria());
                    ps.setString(3, proses.getId_subkriteria());
                    ps.setString(4, proses.getCreated_at());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }

        private boolean updateProsesInBackground(Connection conn, List<ProsesModel> prosesDataList) throws SQLException {
            conn.setAutoCommit(false);

            try {
                // First, delete existing records for this ban and date
                String deleteQuery = "DELETE FROM tb_proses WHERE id_ban = ? AND DATE(created_at) = DATE(?)";
                PreparedStatement deletePs = conn.prepareStatement(deleteQuery);
                deletePs.setString(1, existingProses.getId_ban());
                deletePs.setString(2, existingProses.getCreated_at());
                deletePs.executeUpdate();

                // Then insert new records with updated created_at
                String insertQuery = "INSERT INTO tb_proses (id_ban, id_kriteria, id_subkriteria, created_at) VALUES (?, ?, ?, ?)";
                PreparedStatement insertPs = conn.prepareStatement(insertQuery);

                for (ProsesModel proses : prosesDataList) {
                    insertPs.setString(1, proses.getId_ban());
                    insertPs.setString(2, proses.getId_kriteria());
                    insertPs.setString(3, proses.getId_subkriteria());
                    insertPs.setString(4, proses.getCreated_at()); // This is now current date
                    insertPs.addBatch();
                }
                insertPs.executeBatch();
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private BanModel findBanById(String banId) {
        for (BanModel ban : banModelList) {
            if (ban.getId_ban().equals(banId)) {
                return ban;
            }
        }
        return null;
    }

    private BanModel findBanByName(String banName) {
        for (BanModel ban : banModelList) {
            if (ban.getNama_ban().equals(banName)) {
                return ban;
            }
        }
        return null;
    }

    private void loadImageIntoView(ImageView imageView, String imageUrl) {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            if (imageUrl.startsWith("data:image") || imageUrl.length() > 100) {
                try {
                    byte[] decodedBytes = Base64.decode(imageUrl, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    imageView.setImageBitmap(bitmap);
                } catch (Exception e) {
                    imageView.setImageResource(R.mipmap.ic_tire_foreground);
                }
            } else {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.mipmap.ic_tire_foreground)
                        .error(R.mipmap.ic_tire_foreground)
                        .circleCrop()
                        .into(imageView);
            }
        } else {
            imageView.setImageResource(R.mipmap.ic_tire_foreground);
        }
    }

    private void showDeleteConfirmationDialog(ProsesModel proses) {
        BanModel banData = findBanById(proses.getId_ban());
        String banName = banData != null ? banData.getNama_ban() : "Ban ID: " + proses.getId_ban();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Konfirmasi Hapus")
                .setMessage("Apakah Anda yakin ingin menghapus proses untuk \"" + banName + "\"?")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    new DeleteProsesTask().execute(proses.getId_proses());
                })
                .setNegativeButton("Batal", null)
                .setIcon(R.mipmap.ic_delete_foreground)
                .show();
    }

    private void showSuccessSnackbar(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.success_green))
                .setTextColor(Color.WHITE)
                .show();
    }

    // AsyncTask Classes
    private class LoadInitialDataTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            Connection conn = null;
            try {
                conn = jdbcConnection.getConnection();
                if (conn != null) {
                    loadBanData(conn);
                    loadKriteriaData(conn);
                    loadProsesData(conn);
                    return true;
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
            showLoading(false);
            if (success) {
                prosesAdapter.setProsesModelList(prosesModelList);
                prosesAdapter.setBanList(banModelList);
                updateEmptyState();
            } else {
                Toast.makeText(VikorProcesActivity.this, "Gagal memuat data", Toast.LENGTH_SHORT).show();
            }
        }

        private void loadBanData(Connection conn) throws SQLException {
            String query = "SELECT * FROM tb_ban ORDER BY created_at DESC";
            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            banModelList.clear();
            while (rs.next()) {
                BanModel ban = new BanModel(
                        rs.getString("id_ban"),
                        rs.getString("nama_ban"),
                        rs.getString("harga"),
                        rs.getString("deskripsi"),
                        rs.getString("foto_ban"),
                        rs.getString("created_at")
                );
                banModelList.add(ban);
            }
        }

        private void loadKriteriaData(Connection conn) throws SQLException {
            String query = "SELECT * FROM tb_kriteria ORDER BY id_kriteria";
            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            kriteriaModelList.clear();
            while (rs.next()) {
                KriteriaModel kriteria = new KriteriaModel(
                        rs.getString("id_kriteria"),
                        rs.getString("nama_kriteria"),
                        rs.getString("nilai"),
                        rs.getString("bobot")
                );
                kriteriaModelList.add(kriteria);
            }
        }

        private void loadProsesData(Connection conn) throws SQLException {
            String query = "SELECT id_ban, created_at, " +
                    "GROUP_CONCAT(id_proses) as proses_ids, " +
                    "GROUP_CONCAT(id_kriteria) as kriteria_ids, " +
                    "GROUP_CONCAT(id_subkriteria) as subkriteria_ids " +
                    "FROM tb_proses " +
                    "GROUP BY id_ban, DATE(created_at) " +
                    "ORDER BY created_at DESC";

            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            prosesModelList.clear();
            while (rs.next()) {
                // Buat satu representasi proses per grup (id_ban + tanggal)
                String[] prosesIds = rs.getString("proses_ids").split(",");

                ProsesModel proses = new ProsesModel(
                        prosesIds[0],
                        rs.getString("id_ban"),
                        "",
                        "",
                        rs.getString("created_at")
                );
                prosesModelList.add(proses);
            }
        }
    }

    private class LoadSubkriteriaTask extends AsyncTask<Void, Void, List<SubKriteriaModel>> {
        private AutoCompleteTextView actvSubkriteria;
        private String kriteriaId;
        private ProsesModel existingProses;

        public LoadSubkriteriaTask(AutoCompleteTextView actvSubkriteria, String kriteriaId, ProsesModel existingProses) {
            this.actvSubkriteria = actvSubkriteria;
            this.kriteriaId = kriteriaId;
            this.existingProses = existingProses;
        }

        @Override
        protected List<SubKriteriaModel> doInBackground(Void... voids) {
            Connection conn = null;
            List<SubKriteriaModel> subkriteriaList = new ArrayList<>();
            try {
                conn = jdbcConnection.getConnection();
                if (conn != null) {
                    String query = "SELECT * FROM tb_subkriteria WHERE id_kriteria = ? ORDER BY klasifikasi";
                    PreparedStatement ps = conn.prepareStatement(query);
                    ps.setString(1, kriteriaId);
                    ResultSet rs = ps.executeQuery();

                    while (rs.next()) {
                        SubKriteriaModel subkriteria = new SubKriteriaModel(
                                rs.getString("id_subkriteria"),
                                rs.getString("id_kriteria"),
                                rs.getString("klasifikasi"),
                                rs.getInt("bobot_sub_kriteria")
                        );
                        subkriteriaList.add(subkriteria);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                JDBCConnection.closeConnection(conn);
            }
            return subkriteriaList;
        }

        @Override
        protected void onPostExecute(List<SubKriteriaModel> subkriteriaList) {
            if (!subkriteriaList.isEmpty()) {
                List<String> subkriteriaNames = new ArrayList<>();
                for (SubKriteriaModel subkriteria : subkriteriaList) {
                    subkriteriaNames.add(subkriteria.getKlasifikasi());
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(VikorProcesActivity.this,
                        android.R.layout.simple_dropdown_item_1line, subkriteriaNames);
                actvSubkriteria.setAdapter(adapter);

                // Set existing selection if editing
                if (existingProses != null && existingProses.getId_kriteria().equals(kriteriaId)) {
                    for (SubKriteriaModel subkriteria : subkriteriaList) {
                        if (subkriteria.getId_subkriteria().equals(existingProses.getId_subkriteria())) {
                            actvSubkriteria.setText(subkriteria.getKlasifikasi());
                            break;
                        }
                    }
                }
            }
        }
    }

    // AsyncTask untuk Delete Proses
    private class DeleteProsesTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... prosesIds) {
            Connection conn = null;
            try {
                conn = jdbcConnection.getConnection();
                if (conn != null) {
                    String query = "DELETE FROM tb_proses WHERE id_proses = ?";
                    PreparedStatement ps = conn.prepareStatement(query);
                    ps.setString(1, prosesIds[0]);
                    int result = ps.executeUpdate();
                    return result > 0;
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
                showSuccessSnackbar("Proses berhasil dihapus");
                loadInitialData(); // Reload data
            } else {
                Toast.makeText(VikorProcesActivity.this, "Gagal menghapus proses", Toast.LENGTH_SHORT).show();
            }
        }
    }
}