package com.example.vikorsistemahmadban.activity.admin;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vikorsistemahmadban.R;
import com.example.vikorsistemahmadban.adapter.KriteriaAdapter;
import com.example.vikorsistemahmadban.adapter.SubKriteriaAdapter;
import com.example.vikorsistemahmadban.api.JDBCConnection;
import com.example.vikorsistemahmadban.databinding.ActivityDataKriteriaBinding;
import com.example.vikorsistemahmadban.model.KriteriaModel;
import com.example.vikorsistemahmadban.model.SubKriteriaModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DataKriteriaActivity extends AppCompatActivity implements KriteriaAdapter.OnItemClickListener, KriteriaAdapter.OnSwipeActionListener {
    private ActivityDataKriteriaBinding binding;
    private List<KriteriaModel> kriteriaModelList;
    private JDBCConnection jdbcConnection;
    private KriteriaAdapter kriteriaAdapter;

    // Helper class untuk mengirim data kriteria dan sub kriteria
    private static class KriteriaWithSubKriteria {
        public final KriteriaModel kriteria;
        public final List<SubKriteriaModel> subKriteriaList;

        public KriteriaWithSubKriteria(KriteriaModel kriteria, List<SubKriteriaModel> subKriteriaList) {
            this.kriteria = kriteria;
            this.subKriteriaList = subKriteriaList;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDataKriteriaBinding.inflate(getLayoutInflater());
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
        loadKriteriaData();
    }

    private void initializeViews() {
        jdbcConnection = new JDBCConnection();
        kriteriaModelList = new ArrayList<>();

        // Setup FAB click listener
        binding.fabAddKriteria.setOnClickListener(v -> showAddKriteriaDialog());

        // Initial state
        showLoading(true);
    }

    private void setupRecyclerView() {
        kriteriaAdapter = new KriteriaAdapter(this);
        kriteriaAdapter.setOnItemClickListener(this);
        kriteriaAdapter.setOnSwipeActionListener(this);

        binding.rvKriteria.setLayoutManager(new LinearLayoutManager(this));
        binding.rvKriteria.setAdapter(kriteriaAdapter);

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
                    KriteriaModel kriteria = kriteriaModelList.get(position);

                    if (direction == ItemTouchHelper.RIGHT) {
                        // Swipe right - Update action
                        showEditKriteriaDialog(kriteria);
                    } else if (direction == ItemTouchHelper.LEFT) {
                        // Swipe left - Delete action
                        showDeleteConfirmationDialog(kriteria);
                    }
                }

                // Reset the item position after action
                kriteriaAdapter.notifyItemChanged(position);
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
                        drawSwipeBackground(c, itemView, dX, ContextCompat.getColor(DataKriteriaActivity.this, R.color.success_green),
                                ContextCompat.getDrawable(DataKriteriaActivity.this, R.mipmap.ic_edit_foreground), true, swipeProgress);
                    } else if (dX < 0) {
                        // Swipe left - Delete action
                        drawSwipeBackground(c, itemView, dX, ContextCompat.getColor(DataKriteriaActivity.this, R.color.error_red),
                                ContextCompat.getDrawable(DataKriteriaActivity.this, R.mipmap.ic_delete_foreground), false, swipeProgress);
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

        itemTouchHelper.attachToRecyclerView(binding.rvKriteria);
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
                if (kriteriaAdapter != null) {
                    kriteriaAdapter.filter(s.toString());
                    updateEmptyState();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadKriteriaData() {
        new LoadKriteriasTask().execute();
    }

    private void showLoading(boolean show) {
        if (show) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.rvKriteria.setVisibility(View.GONE);
            binding.emptyStateLayout.setVisibility(View.GONE);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            updateEmptyState();
        }
    }

    private void updateEmptyState() {
        if (kriteriaModelList.isEmpty()) {
            binding.rvKriteria.setVisibility(View.GONE);
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            binding.rvKriteria.setVisibility(View.VISIBLE);
            binding.emptyStateLayout.setVisibility(View.GONE);
        }

        // Update Kriteria count
        binding.tvKriteriaCount.setText(kriteriaModelList.size() + " Kriteria");
    }

    // KriteriaAdapter.OnItemClickListener implementation
    @Override
    public void onItemClick(KriteriaModel kriteria) {
        showKriteriaDetailDialog(kriteria);
    }

    @Override
    public void onItemLongClick(KriteriaModel kriteria) {
        showKriteriaOptionsDialog(kriteria);
    }

    // KriteriaAdapter.OnSwipeActionListener implementation
    @Override
    public void onUpdateClick(KriteriaModel kriteria) {
        showEditKriteriaDialog(kriteria);
    }

    @Override
    public void onDeleteClick(KriteriaModel kriteria) {
        showDeleteConfirmationDialog(kriteria);
    }

    private void showKriteriaDetailDialog(KriteriaModel kriteria) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_kriteria_detail, null);

        // Initialize views
        TextView tvNamaKriteriaDetail = dialogView.findViewById(R.id.tvNamaKriteriaDetail);
        TextView tvBobotDetail = dialogView.findViewById(R.id.tvBobotDetail);
        TextView tvIdKriteriaDetail = dialogView.findViewById(R.id.tvIdKriteriaDetail);
        RecyclerView rvSubKriteria = dialogView.findViewById(R.id.rvSubKriteriaDetail);

        // Set data
        tvNamaKriteriaDetail.setText(kriteria.getNama_kriteria());
        tvIdKriteriaDetail.setText("ID: " + kriteria.getId_kriteria());
        tvBobotDetail.setText(kriteria.getBobot());

        // Load and display sub kriteria
        setupSubKriteriaRecyclerView(rvSubKriteria, kriteria.getId_kriteria(), true);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setView(dialogView)
                .setTitle("Detail Kriteria")
                .setPositiveButton("Tutup", null)
                .setNeutralButton("Edit", (dialog, which) -> showEditKriteriaDialog(kriteria))
                .show();
    }

    private void setupSubKriteriaRecyclerView(RecyclerView recyclerView, String kriteriaId, boolean isReadOnly) {
        SubKriteriaAdapter adapter = new SubKriteriaAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Load sub kriteria data
        new LoadSubKriteriaTask(adapter).execute(kriteriaId);
    }

    private void showKriteriaOptionsDialog(KriteriaModel kriteria) {
        String[] options = {"Edit Kriteria", "Delete Kriteria", "View Details"};

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Pilih Aksi")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showEditKriteriaDialog(kriteria);
                            break;
                        case 1:
                            showDeleteConfirmationDialog(kriteria);
                            break;
                        case 2:
                            showKriteriaDetailDialog(kriteria);
                            break;
                    }
                })
                .show();
    }

    private void showAddKriteriaDialog() {
        showKriteriaFormDialog(null, "Tambah Kriteria Baru");
    }

    private void showEditKriteriaDialog(KriteriaModel kriteria) {
        showKriteriaFormDialog(kriteria, "Edit Kriteria");
    }

    private void showKriteriaFormDialog(KriteriaModel kriteria, String title) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_kriteria_form, null);

        EditText etNamaKriteria = dialogView.findViewById(R.id.etNamaKriteria);
        EditText etBobot = dialogView.findViewById(R.id.etBobot);
        RecyclerView rvSubKriteria = dialogView.findViewById(R.id.rvSubKriteria);
        Button btnAddSubKriteria = dialogView.findViewById(R.id.btnAddSubKriteria);

        // Sub kriteria adapter
        SubKriteriaAdapter subKriteriaAdapter = new SubKriteriaAdapter(this);
        rvSubKriteria.setLayoutManager(new LinearLayoutManager(this));
        rvSubKriteria.setAdapter(subKriteriaAdapter);

        // Fill data if editing
        if (kriteria != null) {
            etNamaKriteria.setText(kriteria.getNama_kriteria());
            etBobot.setText(kriteria.getBobot());
            // Load existing sub kriteria
            loadSubKriteriaForEdit(subKriteriaAdapter, kriteria.getId_kriteria());
        } else {
            // Initialize with default sub kriteria for new kriteria
            initializeDefaultSubKriteria(subKriteriaAdapter);
        }

        // Setup sub kriteria actions
        subKriteriaAdapter.setOnSubKriteriaActionListener(new SubKriteriaAdapter.OnSubKriteriaActionListener() {
            @Override
            public void onEditSubKriteria(SubKriteriaModel subKriteria, int position) {
                showSubKriteriaFormDialog(subKriteria, position, subKriteriaAdapter);
            }

            @Override
            public void onDeleteSubKriteria(SubKriteriaModel subKriteria, int position) {
                showDeleteSubKriteriaDialog(position, subKriteriaAdapter);
            }
        });

        btnAddSubKriteria.setOnClickListener(v -> showSubKriteriaFormDialog(null, -1, subKriteriaAdapter));

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setView(dialogView)
                .setTitle(title)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String namaKriteria = etNamaKriteria.getText().toString().trim();
                    String bobotKriteria = etBobot.getText().toString().trim();

                    if (namaKriteria.isEmpty() || bobotKriteria.isEmpty()) {
                        Toast.makeText(this, "Nama Kriteria dan Bobot harus diisi!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<SubKriteriaModel> subKriteriaList = subKriteriaAdapter.getSubKriteriaList();
                    if (subKriteriaList.isEmpty()) {
                        Toast.makeText(this, "Minimal harus ada 1 sub kriteria!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    KriteriaModel newKriteria = new KriteriaModel(
                            kriteria != null ? kriteria.getId_kriteria() : null,
                            namaKriteria,
                            bobotKriteria
                    );

                    if (kriteria == null) {
                        // Add new kriteria with sub kriteria
                        new AddKriteriaWithSubKriteriaTask().execute(new KriteriaWithSubKriteria(newKriteria, subKriteriaList));
                    } else {
                        // Update kriteria with sub kriteria
                        new UpdateKriteriaWithSubKriteriaTask().execute(new KriteriaWithSubKriteria(newKriteria, subKriteriaList));
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void initializeDefaultSubKriteria(SubKriteriaAdapter adapter) {
        String[] klasifikasi = SubKriteriaModel.getAllKlasifikasi();
        for (String klas : klasifikasi) {
            SubKriteriaModel subKriteria = new SubKriteriaModel(
                    null, null, klas, SubKriteriaModel.getBobotByKlasifikasi(klas)
            );
            adapter.addSubKriteria(subKriteria);
        }
    }

    private void loadSubKriteriaForEdit(SubKriteriaAdapter adapter, String kriteriaId) {
        new LoadSubKriteriaTask(adapter).execute(kriteriaId);
    }

    private void showSubKriteriaFormDialog(SubKriteriaModel subKriteria, int position, SubKriteriaAdapter adapter) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_subkriteria_form, null);

        Spinner spinnerKlasifikasi = dialogView.findViewById(R.id.spinnerKlasifikasi);
        TextView tvBobot = dialogView.findViewById(R.id.tvBobotSubKriteria);

        // Setup spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, SubKriteriaModel.getAllKlasifikasi());
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerKlasifikasi.setAdapter(spinnerAdapter);

        spinnerKlasifikasi.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selectedKlasifikasi = parent.getItemAtPosition(pos).toString();
                int bobot = SubKriteriaModel.getBobotByKlasifikasi(selectedKlasifikasi);
                tvBobot.setText("Bobot: " + bobot);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Fill data if editing
        if (subKriteria != null) {
            String[] klasifikasiArray = SubKriteriaModel.getAllKlasifikasi();
            for (int i = 0; i < klasifikasiArray.length; i++) {
                if (klasifikasiArray[i].equals(subKriteria.getKlasifikasi())) {
                    spinnerKlasifikasi.setSelection(i);
                    break;
                }
            }
        }

        String dialogTitle = subKriteria == null ? "Tambah Sub Kriteria" : "Edit Sub Kriteria";

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setView(dialogView)
                .setTitle(dialogTitle)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String selectedKlasifikasi = spinnerKlasifikasi.getSelectedItem().toString();
                    int bobot = SubKriteriaModel.getBobotByKlasifikasi(selectedKlasifikasi);

                    SubKriteriaModel newSubKriteria = new SubKriteriaModel(
                            subKriteria != null ? subKriteria.getId_subkriteria() : null,
                            subKriteria != null ? subKriteria.getId_kriteria() : null,
                            selectedKlasifikasi,
                            bobot
                    );

                    if (position == -1) {
                        // Add new
                        adapter.addSubKriteria(newSubKriteria);
                    } else {
                        // Update existing
                        adapter.updateSubKriteria(newSubKriteria, position);
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showDeleteSubKriteriaDialog(int position, SubKriteriaAdapter adapter) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Konfirmasi Hapus")
                .setMessage("Apakah Anda yakin ingin menghapus sub kriteria ini?")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    adapter.removeSubKriteria(position);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showDeleteConfirmationDialog(KriteriaModel kriteria) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Konfirmasi Hapus")
                .setMessage("Apakah Anda yakin ingin menghapus data \"" + kriteria.getNama_kriteria() + "\"?\n\nSemua sub kriteria yang terkait juga akan dihapus.")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    new DeleteKriteriaTask().execute(kriteria.getId_kriteria());
                })
                .setNegativeButton("Batal", null)
                .setIcon(R.mipmap.ic_delete_foreground)
                .show();
    }

    private class LoadKriteriasTask extends AsyncTask<Void, Void, List<KriteriaModel>> {
        @Override
        protected List<KriteriaModel> doInBackground(Void... voids) {
            List<KriteriaModel> kriterias = new ArrayList<>();
            Connection conn = null;
            try {
                conn = jdbcConnection.getConnection();
                if (conn != null) {
                    String query = "SELECT * FROM tb_kriteria ORDER BY id_kriteria";

                    PreparedStatement ps = conn.prepareStatement(query);
                    ResultSet rs = ps.executeQuery();

                    while (rs.next()) {
                        KriteriaModel kriteria = new KriteriaModel(
                                rs.getString("id_kriteria"),
                                rs.getString("nama_kriteria"),
                                rs.getString("bobot")
                        );
                        kriterias.add(kriteria);
                    }
                    android.util.Log.d("LoadKriterias", "Data loaded: " + kriterias.size() + " items");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                JDBCConnection.closeConnection(conn);
            }
            return kriterias;
        }

        @Override
        protected void onPostExecute(List<KriteriaModel> kriterias) {
            kriteriaModelList = kriterias;
            kriteriaAdapter.setKriteriaList(kriterias);
            showLoading(false);
        }
    }

    private class LoadSubKriteriaTask extends AsyncTask<String, Void, List<SubKriteriaModel>> {
        private SubKriteriaAdapter adapter;

        public LoadSubKriteriaTask(SubKriteriaAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        protected List<SubKriteriaModel> doInBackground(String... params) {
            String kriteriaId = params[0];
            List<SubKriteriaModel> subKriterias = new ArrayList<>();
            Connection conn = null;
            try {
                conn = jdbcConnection.getConnection();
                if (conn != null) {
                    String query = "SELECT * FROM tb_subkriteria WHERE id_kriteria = ? ORDER BY id_subkriteria";
                    PreparedStatement ps = conn.prepareStatement(query);
                    ps.setString(1, kriteriaId);
                    ResultSet rs = ps.executeQuery();

                    while (rs.next()) {
                        SubKriteriaModel subKriteria = new SubKriteriaModel(
                                rs.getString("id_subkriteria"),
                                rs.getString("id_kriteria"),
                                rs.getString("klasifikasi"),
                                rs.getInt("bobot_sub_kriteria")
                        );
                        subKriterias.add(subKriteria);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                JDBCConnection.closeConnection(conn);
            }
            return subKriterias;
        }

        @Override
        protected void onPostExecute(List<SubKriteriaModel> subKriterias) {
            adapter.setSubKriteriaList(subKriterias);
        }
    }

    private class AddKriteriaWithSubKriteriaTask extends AsyncTask<KriteriaWithSubKriteria, Void, KriteriaModel> {
        private KriteriaWithSubKriteria data;

        @Override
        protected KriteriaModel doInBackground(KriteriaWithSubKriteria... params) {
            data = params[0];
            Connection conn = null;
            try {
                conn = jdbcConnection.getConnection();
                if (conn != null) {
                    // Start transaction
                    conn.setAutoCommit(false);

                    // Insert kriteria
                    String kriteriaQuery = "INSERT INTO tb_kriteria (nama_kriteria, bobot) VALUES (?, ?)";
                    PreparedStatement psKriteria = conn.prepareStatement(kriteriaQuery, PreparedStatement.RETURN_GENERATED_KEYS);
                    psKriteria.setString(1, data.kriteria.getNama_kriteria());
                    psKriteria.setString(2, data.kriteria.getBobot());

                    int affectedRows = psKriteria.executeUpdate();
                    if (affectedRows > 0) {
                        // Get generated kriteria ID
                        ResultSet generatedKeys = psKriteria.getGeneratedKeys();
                        if (generatedKeys.next()) {
                            String kriteriaId = generatedKeys.getString(1);

                            // Insert sub kriteria
                            String subKriteriaQuery = "INSERT INTO tb_subkriteria (id_kriteria, klasifikasi, bobot_sub_kriteria) VALUES (?, ?, ?)";
                            PreparedStatement psSubKriteria = conn.prepareStatement(subKriteriaQuery);

                            for (SubKriteriaModel subKriteria : data.subKriteriaList) {
                                psSubKriteria.setString(1, kriteriaId);
                                psSubKriteria.setString(2, subKriteria.getKlasifikasi());
                                psSubKriteria.setInt(3, subKriteria.getBobot_sub_kriteria());
                                psSubKriteria.addBatch();
                            }

                            psSubKriteria.executeBatch();

                            // Commit transaction
                            conn.commit();

                            return new KriteriaModel(kriteriaId, data.kriteria.getNama_kriteria(), data.kriteria.getBobot());
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                // Rollback transaction if error occurs
                try {
                    if (conn != null) {
                        conn.rollback();
                    }
                } catch (SQLException rollbackException) {
                    rollbackException.printStackTrace();
                }
            } finally {
                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                    }
                } catch (SQLException autoCommitException) {
                    autoCommitException.printStackTrace();
                }
                JDBCConnection.closeConnection(conn);
            }
            return null;
        }

        @Override
        protected void onPostExecute(KriteriaModel newKriteriaWithId) {
            if (newKriteriaWithId != null) {
                kriteriaModelList.add(0, newKriteriaWithId);
                kriteriaAdapter.addKriteria(newKriteriaWithId);
                updateEmptyState();
                showSuccessSnackbar("Kriteria dan Sub Kriteria berhasil ditambahkan");
            } else {
                Toast.makeText(DataKriteriaActivity.this, "Gagal menambahkan kriteria", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class UpdateKriteriaWithSubKriteriaTask extends AsyncTask<KriteriaWithSubKriteria, Void, Boolean> {
        private KriteriaWithSubKriteria data;

        @Override
        protected Boolean doInBackground(KriteriaWithSubKriteria... params) {
            data = params[0];
            Connection conn = null;
            try {
                conn = jdbcConnection.getConnection();
                if (conn != null) {
                    // Start transaction
                    conn.setAutoCommit(false);

                    // Update kriteria
                    String kriteriaQuery = "UPDATE tb_kriteria SET nama_kriteria=?, bobot=? WHERE id_kriteria=?";
                    PreparedStatement psKriteria = conn.prepareStatement(kriteriaQuery);
                    psKriteria.setString(1, data.kriteria.getNama_kriteria());
                    psKriteria.setString(2, data.kriteria.getBobot());
                    psKriteria.setString(3, data.kriteria.getId_kriteria());

                    int affectedRows = psKriteria.executeUpdate();
                    if (affectedRows > 0) {
                        // Delete existing sub kriteria
                        String deleteSubKriteriaQuery = "DELETE FROM tb_subkriteria WHERE id_kriteria = ?";
                        PreparedStatement psDelete = conn.prepareStatement(deleteSubKriteriaQuery);
                        psDelete.setString(1, data.kriteria.getId_kriteria());
                        psDelete.executeUpdate();

                        // Insert updated sub kriteria
                        String subKriteriaQuery = "INSERT INTO tb_subkriteria (id_kriteria, klasifikasi, bobot_sub_kriteria) VALUES (?, ?, ?)";
                        PreparedStatement psSubKriteria = conn.prepareStatement(subKriteriaQuery);

                        for (SubKriteriaModel subKriteria : data.subKriteriaList) {
                            psSubKriteria.setString(1, data.kriteria.getId_kriteria());
                            psSubKriteria.setString(2, subKriteria.getKlasifikasi());
                            psSubKriteria.setInt(3, subKriteria.getBobot_sub_kriteria());
                            psSubKriteria.addBatch();
                        }

                        psSubKriteria.executeBatch();

                        // Commit transaction
                        conn.commit();
                        return true;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                // Rollback transaction if error occurs
                try {
                    if (conn != null) {
                        conn.rollback();
                    }
                } catch (SQLException rollbackException) {
                    rollbackException.printStackTrace();
                }
            } finally {
                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                    }
                } catch (SQLException autoCommitException) {
                    autoCommitException.printStackTrace();
                }
                JDBCConnection.closeConnection(conn);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                // Update local list
                for (int i = 0; i < kriteriaModelList.size(); i++) {
                    if (kriteriaModelList.get(i).getId_kriteria().equals(data.kriteria.getId_kriteria())) {
                        kriteriaModelList.set(i, data.kriteria);
                        break;
                    }
                }
                kriteriaAdapter.updateKriteria(data.kriteria);
                showSuccessSnackbar("Kriteria dan Sub Kriteria berhasil diupdate");
            } else {
                Toast.makeText(DataKriteriaActivity.this, "Gagal mengupdate kriteria", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class DeleteKriteriaTask extends AsyncTask<String, Void, Boolean> {
        private String kriteriaId;

        @Override
        protected Boolean doInBackground(String... ids) {
            kriteriaId = ids[0];
            Connection conn = null;
            try {
                conn = jdbcConnection.getConnection();
                if (conn != null) {
                    // Start transaction
                    conn.setAutoCommit(false);

                    // Delete sub kriteria first (foreign key constraint)
                    String deleteSubKriteriaQuery = "DELETE FROM tb_subkriteria WHERE id_kriteria=?";
                    PreparedStatement psSubKriteria = conn.prepareStatement(deleteSubKriteriaQuery);
                    psSubKriteria.setString(1, kriteriaId);
                    psSubKriteria.executeUpdate();

                    // Delete kriteria
                    String deleteKriteriaQuery = "DELETE FROM tb_kriteria WHERE id_kriteria=?";
                    PreparedStatement psKriteria = conn.prepareStatement(deleteKriteriaQuery);
                    psKriteria.setString(1, kriteriaId);
                    int result = psKriteria.executeUpdate();

                    // Commit transaction
                    conn.commit();
                    return result > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                // Rollback transaction if error occurs
                try {
                    if (conn != null) {
                        conn.rollback();
                    }
                } catch (SQLException rollbackException) {
                    rollbackException.printStackTrace();
                }
            } finally {
                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                    }
                } catch (SQLException autoCommitException) {
                    autoCommitException.printStackTrace();
                }
                JDBCConnection.closeConnection(conn);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                // Remove from local list
                kriteriaModelList.removeIf(kriteria -> kriteria.getId_kriteria().equals(kriteriaId));
                kriteriaAdapter.removeKriteria(kriteriaId);
                updateEmptyState();
                showSuccessSnackbar("Kriteria dan Sub Kriteria berhasil dihapus");
            } else {
                Toast.makeText(DataKriteriaActivity.this, "Gagal menghapus kriteria", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showSuccessSnackbar(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.success_green))
                .setTextColor(Color.WHITE)
                .show();
    }
}