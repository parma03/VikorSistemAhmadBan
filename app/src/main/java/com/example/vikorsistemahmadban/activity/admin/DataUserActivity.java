package com.example.vikorsistemahmadban.activity.admin;

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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
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
import android.Manifest;
import com.example.vikorsistemahmadban.R;
import com.example.vikorsistemahmadban.adapter.UserAdapter;
import com.example.vikorsistemahmadban.api.JDBCConnection;
import com.example.vikorsistemahmadban.databinding.ActivityDataUserBinding;
import com.example.vikorsistemahmadban.model.UserModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class DataUserActivity extends AppCompatActivity implements UserAdapter.OnItemClickListener, UserAdapter.OnSwipeActionListener {
    private ActivityDataUserBinding binding;
    private UserAdapter userAdapter;
    private List<UserModel> userList;
    private JDBCConnection jdbcConnection;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ImageView currentProfileImageView;
    private String selectedImageBase64 = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDataUserBinding.inflate(getLayoutInflater());
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
        loadUserData();
    }

    private void initializeViews() {
        jdbcConnection = new JDBCConnection();
        userList = new ArrayList<>();

        // Setup FAB click listener
        binding.fabAddUser.setOnClickListener(v -> showAddUserDialog());

        // Initial state
        showLoading(true);
    }

    private void setupRecyclerView() {
        userAdapter = new UserAdapter(this);
        userAdapter.setOnItemClickListener(this);
        userAdapter.setOnSwipeActionListener(this);

        binding.rvUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUsers.setAdapter(userAdapter);

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
                    UserModel user = userList.get(position);

                    if (direction == ItemTouchHelper.RIGHT) {
                        // Swipe right - Update action
                        showEditUserDialog(user);
                    } else if (direction == ItemTouchHelper.LEFT) {
                        // Swipe left - Delete action
                        showDeleteConfirmationDialog(user);
                    }
                }

                // Reset the item position after action
                userAdapter.notifyItemChanged(position);
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
                        drawSwipeBackground(c, itemView, dX, ContextCompat.getColor(DataUserActivity.this, R.color.success_green),
                                ContextCompat.getDrawable(DataUserActivity.this, R.mipmap.ic_edit_foreground), true, swipeProgress);
                    } else if (dX < 0) {
                        // Swipe left - Delete action
                        drawSwipeBackground(c, itemView, dX, ContextCompat.getColor(DataUserActivity.this, R.color.error_red),
                                ContextCompat.getDrawable(DataUserActivity.this, R.mipmap.ic_delete_foreground), false, swipeProgress);
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

        itemTouchHelper.attachToRecyclerView(binding.rvUsers);
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
            int iconSize = (int) (48 + (swipeProgress * 16)); // Icon grows as user swipes
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
            icon.setAlpha(255); // Reset alpha
        }
    }

    private void setupSearchFunctionality() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (userAdapter != null) {
                    userAdapter.filter(s.toString());
                    updateEmptyState();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadUserData() {
        new LoadUsersTask().execute();
    }

    private void showLoading(boolean show) {
        if (show) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.rvUsers.setVisibility(View.GONE);
            binding.emptyStateLayout.setVisibility(View.GONE);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            updateEmptyState();
        }
    }

    private void updateEmptyState() {
        if (userList.isEmpty()) {
            binding.rvUsers.setVisibility(View.GONE);
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            binding.rvUsers.setVisibility(View.VISIBLE);
            binding.emptyStateLayout.setVisibility(View.GONE);
        }

        // Update user count
        binding.tvUserCount.setText(userList.size() + " Users");
    }

    // UserAdapter.OnItemClickListener implementation
    @Override
    public void onItemClick(UserModel user) {
        showUserDetailDialog(user);
    }

    @Override
    public void onItemLongClick(UserModel user) {
        showUserOptionsDialog(user);
    }

    // UserAdapter.OnSwipeActionListener implementation
    @Override
    public void onUpdateClick(UserModel user) {
        showEditUserDialog(user);
    }

    @Override
    public void onDeleteClick(UserModel user) {
        showDeleteConfirmationDialog(user);
    }

    private void showUserDetailDialog(UserModel user) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_detail, null);

        // Initialize views
        ImageView ivProfile = dialogView.findViewById(R.id.ivProfileDetail);
        TextView tvNama = dialogView.findViewById(R.id.tvNamaDetail);
        TextView tvIdUser = dialogView.findViewById(R.id.tvIdUserDetail);

        // Set data
        tvNama.setText(user.getNama());
        tvIdUser.setText("ID: " + user.getId_user());

        // Load profile image - Handle null/empty profile
        if (user.getProfile() != null && !user.getProfile().trim().isEmpty()) {
            if (user.getProfile().startsWith("data:image") || user.getProfile().length() > 100) {
                // Base64 image
                try {
                    byte[] decodedBytes = Base64.decode(user.getProfile(), Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    ivProfile.setImageBitmap(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                    ivProfile.setImageResource(R.mipmap.ic_user_foreground);
                }
            } else {
                // URL image
                Glide.with(this)
                        .load(user.getProfile())
                        .placeholder(R.mipmap.ic_user_foreground)
                        .error(R.mipmap.ic_user_foreground)
                        .circleCrop()
                        .into(ivProfile);
            }
        } else {
            // No profile image - set default
            ivProfile.setImageResource(R.mipmap.ic_user_foreground);
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setView(dialogView)
                .setTitle("Detail User")
                .setPositiveButton("Tutup", null)
                .setNeutralButton("Edit", (dialog, which) -> showEditUserDialog(user))
                .show();
    }

    private void showUserOptionsDialog(UserModel user) {
        String[] options = {"Edit User", "Delete User", "View Details"};

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Pilih Aksi")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showEditUserDialog(user);
                            break;
                        case 1:
                            showDeleteConfirmationDialog(user);
                            break;
                        case 2:
                            showUserDetailDialog(user);
                            break;
                    }
                })
                .show();
    }

    private void showAddUserDialog() {
        showUserFormDialog(null, "Tambah User Baru");
    }

    private void showEditUserDialog(UserModel user) {
        showUserFormDialog(user, "Edit User");
    }

    private void showUserFormDialog(UserModel user, String title) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_form, null);

        EditText etNama = dialogView.findViewById(R.id.etNama);
        EditText etUsername = dialogView.findViewById(R.id.etUsername);
        EditText etPassword = dialogView.findViewById(R.id.etPassword);
        Spinner spinnerRole = dialogView.findViewById(R.id.spinnerRole);
        ImageView ivProfile = dialogView.findViewById(R.id.ivProfile);

        // Setup profile image click listener
        currentProfileImageView = ivProfile;
        selectedImageBase64 = ""; // Reset selected image

        ivProfile.setOnClickListener(v -> openImagePicker());

        // Setup spinner
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"admin", "pengguna", "pimpinan"});
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(roleAdapter);

        // Fill data if editing
        if (user != null) {
            etNama.setText(user.getNama());
            etUsername.setText(user.getUsername());
            etPassword.setText(user.getPassword());

            // Load existing profile image - Handle null/empty profile
            if (user.getProfile() != null && !user.getProfile().trim().isEmpty()) {
                if (user.getProfile().startsWith("data:image") || user.getProfile().length() > 100) {
                    // Base64 image
                    try {
                        byte[] decodedBytes = Base64.decode(user.getProfile(), Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        ivProfile.setImageBitmap(bitmap);
                        selectedImageBase64 = user.getProfile(); // Keep existing image
                    } catch (Exception e) {
                        e.printStackTrace();
                        ivProfile.setImageResource(R.mipmap.ic_user_foreground);
                        selectedImageBase64 = ""; // Reset if decode fails
                    }
                } else {
                    // URL image
                    Glide.with(this)
                            .load(user.getProfile())
                            .placeholder(R.mipmap.ic_user_foreground)
                            .error(R.mipmap.ic_user_foreground)
                            .circleCrop()
                            .into(ivProfile);
                    selectedImageBase64 = user.getProfile(); // Keep existing image
                }
            } else {
                // No profile image - set default
                ivProfile.setImageResource(R.mipmap.ic_user_foreground);
                selectedImageBase64 = "";
            }

            // Set spinner selection
            String[] roles = {"admin", "pengguna", "pimpinan"};
            for (int i = 0; i < roles.length; i++) {
                if (roles[i].equals(user.getRole())) {
                    spinnerRole.setSelection(i);
                    break;
                }
            }
        } else {
            // New user - set default profile image
            ivProfile.setImageResource(R.mipmap.ic_user_foreground);
            selectedImageBase64 = "";
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setView(dialogView)
                .setTitle(title)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String nama = etNama.getText().toString().trim();
                    String username = etUsername.getText().toString().trim();
                    String password = etPassword.getText().toString().trim();
                    String role = spinnerRole.getSelectedItem().toString();

                    // Handle profile image - dapat null/kosong
                    String profile = null;
                    if (user != null && !selectedImageBase64.isEmpty()) {
                        // Editing with new image selected
                        profile = selectedImageBase64;
                    } else if (user != null && selectedImageBase64.isEmpty()) {
                        // Editing but no new image selected - keep existing or null
                        profile = (user.getProfile() != null && !user.getProfile().trim().isEmpty())
                                ? user.getProfile() : null;
                    } else if (user == null && !selectedImageBase64.isEmpty()) {
                        // New user with image selected
                        profile = selectedImageBase64;
                    }
                    // Else: new user without image - profile remains null

                    if (nama.isEmpty() || username.isEmpty() || password.isEmpty()) {
                        Toast.makeText(this, "Nama, Username, dan Password harus diisi!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    UserModel newUser = new UserModel(
                            user != null ? user.getId_user() : null,
                            nama,
                            username,
                            password,
                            role,
                            profile
                    );

                    if (user == null) {
                        // Add new user
                        new AddUserTask().execute(newUser);
                    } else {
                        // Update existing user
                        new UpdateUserTask().execute(newUser);
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showDeleteConfirmationDialog(UserModel user) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Konfirmasi Hapus")
                .setMessage("Apakah Anda yakin ingin menghapus user \"" + user.getNama() + "\"?")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    new DeleteUserTask().execute(user.getId_user());
                })
                .setNegativeButton("Batal", null)
                .setIcon(R.mipmap.ic_delete_foreground)
                .show();
    }

    // AsyncTask classes for database operations
    private class LoadUsersTask extends AsyncTask<Void, Void, List<UserModel>> {
        @Override
        protected List<UserModel> doInBackground(Void... voids) {
            List<UserModel> users = new ArrayList<>();
            Connection conn = null;
            try {
                conn = jdbcConnection.getConnection();
                if (conn != null) {
                    String query = "SELECT u.*, " +
                            "CASE " +
                            "WHEN u.role = 'admin' THEN a.nama " +
                            "WHEN u.role = 'pimpinan' THEN p.nama " +
                            "WHEN u.role = 'pengguna' THEN pg.nama " +
                            "END as nama_user, " +
                            "CASE " +
                            "WHEN u.role = 'admin' THEN a.profile " +
                            "WHEN u.role = 'pimpinan' THEN p.profile " +
                            "WHEN u.role = 'pengguna' THEN pg.profile " +
                            "END as profile_user " +
                            "FROM tb_user u " +
                            "LEFT JOIN tb_admin a ON u.id_user = a.id_user " +
                            "LEFT JOIN tb_pengguna a ON u.id_user = pg.id_user " +
                            "LEFT JOIN tb_pimpinan p ON u.id_user = p.id_user ";

                    PreparedStatement ps = conn.prepareStatement(query);
                    ResultSet rs = ps.executeQuery();

                    while (rs.next()) {
                        UserModel user = new UserModel(
                                rs.getString("id_user"),
                                rs.getString("nama_user"),
                                rs.getString("username"),
                                rs.getString("password"),
                                rs.getString("role"),
                                rs.getString("profile_user")
                        );
                        users.add(user);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                JDBCConnection.closeConnection(conn);
            }
            return users;
        }

        @Override
        protected void onPostExecute(List<UserModel> users) {
            userList = users;
            userAdapter.setUserList(users);
            showLoading(false);
        }
    }

    private class AddUserTask extends AsyncTask<UserModel, Void, Boolean> {
        private UserModel newUser;

        @Override
        protected Boolean doInBackground(UserModel... users) {
            newUser = users[0];
            Connection conn = null;
            try {
                conn = jdbcConnection.getConnection();
                if (conn != null) {
                    // Insert ke tb_user tanpa id_user karena autoincrement
                    String query = "INSERT INTO tb_user (username, password, role) VALUES (?, ?, ?)";
                    PreparedStatement ps = conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);
                    ps.setString(1, newUser.getUsername());
                    ps.setString(2, newUser.getPassword());
                    ps.setString(3, newUser.getRole());

                    int result = ps.executeUpdate();

                    if (result > 0) {
                        // Ambil generated id_user
                        ResultSet generatedKeys = ps.getGeneratedKeys();
                        String generatedId = null;
                        if (generatedKeys.next()) {
                            generatedId = generatedKeys.getString(1);
                            newUser.setId_user(generatedId); // Set id yang baru di-generate
                        }

                        if (generatedId != null) {
                            // Insert ke tabel role-specific
                            String roleTable = "";
                            switch (newUser.getRole().toLowerCase()) {
                                case "admin":
                                    roleTable = "tb_admin";
                                    break;
                                case "pimpinan":
                                    roleTable = "tb_pimpinan";
                                    break;
                                case "pengguna":
                                    roleTable = "tb_pengguna";
                                    break;
                                default:
                                    roleTable = "tb_admin";
                                    break;
                            }

                            String insertRoleQuery = "INSERT INTO " + roleTable + " (id_user, nama, profile) VALUES (?, ?, ?)";
                            PreparedStatement psRole = conn.prepareStatement(insertRoleQuery);
                            psRole.setString(1, generatedId);
                            psRole.setString(2, newUser.getNama());

                            // Handle profile - bisa null
                            if (newUser.getProfile() != null && !newUser.getProfile().trim().isEmpty()) {
                                psRole.setString(3, newUser.getProfile());
                            } else {
                                psRole.setNull(3, java.sql.Types.LONGVARCHAR); // atau TEXT/CLOB tergantung database
                            }

                            return psRole.executeUpdate() > 0;
                        }
                    }
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
                userList.add(0, newUser);
                userAdapter.addUser(newUser);
                updateEmptyState();
                showSuccessSnackbar("User berhasil ditambahkan");
            } else {
                Toast.makeText(DataUserActivity.this, "Gagal menambahkan user", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class UpdateUserTask extends AsyncTask<UserModel, Void, Boolean> {
        private UserModel updatedUser;

        @Override
        protected Boolean doInBackground(UserModel... users) {
            updatedUser = users[0];
            Connection conn = null;
            try {
                conn = jdbcConnection.getConnection();
                if (conn != null) {
                    // Update tb_user
                    String query = "UPDATE tb_user SET username=?, password=?, role=? WHERE id_user=?";
                    PreparedStatement ps = conn.prepareStatement(query);
                    ps.setString(1, updatedUser.getUsername());
                    ps.setString(2, updatedUser.getPassword());
                    ps.setString(3, updatedUser.getRole());
                    ps.setString(4, updatedUser.getId_user());

                    int result = ps.executeUpdate();

                    if (result > 0) {
                        // Update role-specific table
                        String roleTable = "";
                        switch (updatedUser.getRole().toLowerCase()) {
                            case "admin":
                                roleTable = "tb_admin";
                                break;
                            case "pimpinan":
                                roleTable = "tb_pimpinan";
                                break;
                            case "pengguna":
                                roleTable = "tb_pengguna";
                                break;
                            default:
                                roleTable = "tb_admin";
                                break;
                        }

                        String updateRoleQuery = "UPDATE " + roleTable + " SET nama=?, profile=? WHERE id_user=?";
                        PreparedStatement psRole = conn.prepareStatement(updateRoleQuery);
                        psRole.setString(1, updatedUser.getNama());

                        // Handle profile - bisa null
                        if (updatedUser.getProfile() != null && !updatedUser.getProfile().trim().isEmpty()) {
                            psRole.setString(2, updatedUser.getProfile());
                        } else {
                            psRole.setNull(2, java.sql.Types.LONGVARCHAR); // atau TEXT/CLOB tergantung database
                        }

                        psRole.setString(3, updatedUser.getId_user());

                        return psRole.executeUpdate() > 0;
                    }
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
                for (int i = 0; i < userList.size(); i++) {
                    if (userList.get(i).getId_user().equals(updatedUser.getId_user())) {
                        userList.set(i, updatedUser);
                        break;
                    }
                }
                userAdapter.updateUser(updatedUser);
                showSuccessSnackbar("User berhasil diupdate");
            } else {
                Toast.makeText(DataUserActivity.this, "Gagal mengupdate user", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class DeleteUserTask extends AsyncTask<String, Void, Boolean> {
        private String userId;

        @Override
        protected Boolean doInBackground(String... ids) {
            userId = ids[0];
            Connection conn = null;
            try {
                conn = jdbcConnection.getConnection();
                if (conn != null) {
                    String query = "DELETE FROM tb_user WHERE id_user=?";
                    PreparedStatement ps = conn.prepareStatement(query);
                    ps.setString(1, userId);

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
                userList.removeIf(user -> user.getId_user().equals(userId));
                userAdapter.removeUser(userId);
                updateEmptyState();
                showSuccessSnackbar("User berhasil dihapus");
            } else {
                Toast.makeText(DataUserActivity.this, "Gagal menghapus user", Toast.LENGTH_SHORT).show();
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
                                if (currentProfileImageView != null) {
                                    Glide.with(this)
                                            .load(selectedImageUri)
                                            .circleCrop()
                                            .into(currentProfileImageView);
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
                                    if (currentProfileImageView != null) {
                                        Glide.with(this)
                                                .load(imageBitmap)
                                                .circleCrop()
                                                .into(currentProfileImageView);
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