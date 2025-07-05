package com.example.vikorsistemahmadban;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.vikorsistemahmadban.activity.admin.DataUserActivity;
import com.example.vikorsistemahmadban.activity.admin.MainAdminActivity;
import com.example.vikorsistemahmadban.activity.pengguna.MainPenggunaActivity;
import com.example.vikorsistemahmadban.activity.pimpinan.MainPimpinanActivity;
import com.example.vikorsistemahmadban.api.JDBCConnection;
import com.example.vikorsistemahmadban.api.PrefManager;
import com.example.vikorsistemahmadban.databinding.ActivityMainBinding;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private PrefManager prefManager;
    private JDBCConnection jdbcConnection;
    private ProgressDialog progressDialog;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        prefManager = new PrefManager(this);
        jdbcConnection = new JDBCConnection();

        // Initialize progress dialog
        initializeProgressDialog();

        // Cek apakah user sudah login
        checkLoginStatus();

        // Set click listener untuk tombol login
        binding.btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        binding.btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initializeProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Mohon Tunggu");
        progressDialog.setMessage("Sedang memproses login...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
    }

    private void checkLoginStatus() {
        if (prefManager.getLoginStatus()) {
            String role = prefManager.getTipe();
            redirectToMainActivity(role);
        }
    }

    private void performLogin() {
        String username = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // Validasi input
        if (username.isEmpty()) {
            binding.tilEmail.setError("Username tidak boleh kosong");
            return;
        }
        if (password.isEmpty()) {
            binding.tilPassword.setError("Password tidak boleh kosong");
            return;
        }

        // Clear error
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        // Jalankan login dalam background thread
        new LoginTask().execute(username, password);
    }

    private class LoginTask extends AsyncTask<String, String, LoginResult> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Tampilkan loading dialog
            showLoadingDialog("Menghubungkan ke server...");
        }

        @Override
        protected LoginResult doInBackground(String... params) {
            String username = params[0];
            String password = params[1];

            // Update progress message
            publishProgress("Memverifikasi kredensial...");

            try {
                // Simulasi delay untuk menunjukkan loading (opsional)
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return authenticateUser(username, password);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            // Update pesan loading dialog
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.setMessage(values[0]);
            }
        }

        @Override
        protected void onPostExecute(LoginResult result) {
            // Sembunyikan loading dialog
            hideLoadingDialog();

            if (result.success) {
                showSuccessAlert(result);
            } else {
                showErrorAlert(result.message);
            }
        }
    }

    private void showLoadingDialog(String message) {
        if (progressDialog != null && !isFinishing()) {
            progressDialog.setMessage(message);
            if (!progressDialog.isShowing()) {
                progressDialog.show();
            }
        }
    }

    private void hideLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            try {
                progressDialog.dismiss();
            } catch (Exception e) {
                Log.e(TAG, "Error dismissing progress dialog: " + e.getMessage());
            }
        }
    }

    private LoginResult authenticateUser(String username, String password) {
        LoginResult result = new LoginResult();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = jdbcConnection.getConnection();
            if (conn == null) {
                result.message = "Gagal terhubung ke database";
                return result;
            }

            // Query untuk login berdasarkan role
            String query = "SELECT u.*, " +
                    "CASE " +
                    "WHEN u.role = 'admin' THEN a.nama " +
                    "WHEN u.role = 'pimpinan' THEN p.nama " +
                    "END as nama_user, " +
                    "CASE " +
                    "WHEN u.role = 'admin' THEN a.profile " +
                    "WHEN u.role = 'pimpinan' THEN p.profile " +
                    "END as profile_user " +
                    "FROM tb_user u " +
                    "LEFT JOIN tb_admin a ON u.id_user = a.id_user " +
                    "LEFT JOIN tb_pimpinan p ON u.id_user = p.id_user " +
                    "WHERE u.username = ? AND u.password = ?";

            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            rs = pstmt.executeQuery();

            if (rs.next()) {
                // Login berhasil
                result.success = true;
                result.userId = rs.getString("id_user");
                result.username = rs.getString("username");
                result.role = rs.getString("role");
                result.nama = rs.getString("nama_user");
                result.profile = rs.getString("profile_user");
                result.message = "Login berhasil!";

                Log.d(TAG, "Login successful for user: " + username + " with role: " + result.role);
            } else {
                // Login gagal
                result.success = false;
                result.message = "Username atau password salah!";
                Log.d(TAG, "Login failed for user: " + username);
            }

        } catch (SQLException e) {
            result.success = false;
            result.message = "Error database: " + e.getMessage();
            Log.e(TAG, "SQL Exception during login: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            result.success = false;
            result.message = "Error sistem: " + e.getMessage();
            Log.e(TAG, "General Exception during login: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Tutup koneksi
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                Log.e(TAG, "Error closing connection: " + e.getMessage());
            }
        }

        return result;
    }

    private void showSuccessAlert(LoginResult result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Login Berhasil")
                .setMessage("Selamat datang, " + (result.nama != null ? result.nama : result.username) + "!")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("OK", (dialog, which) -> {
                    // Tampilkan loading saat menyiapkan halaman utama
                    showLoadingDialog("Menyiapkan halaman utama...");

                    // Simpan data user ke SharedPreferences
                    saveUserData(result);

                    // Delay sedikit untuk efek loading
                    new android.os.Handler().postDelayed(() -> {
                        hideLoadingDialog();
                        // Redirect ke halaman sesuai role
                        redirectToMainActivity(result.role);
                    }, 1000);
                })
                .setCancelable(false)
                .show();
    }

    private void showErrorAlert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Login Gagal")
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();

        // Juga tampilkan toast
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void saveUserData(LoginResult result) {
        prefManager.setId(result.userId);
        prefManager.setUsername(result.username);
        prefManager.setTipe(result.role);
        prefManager.setNama(result.nama != null ? result.nama : result.username);
        prefManager.setImg(result.profile != null ? result.profile : "");
        prefManager.setLoginStatus(true);

        Log.d(TAG, "User data saved to preferences");
    }

    private void redirectToMainActivity(String role) {
        Intent intent = null;

        switch (role.toLowerCase()) {
            case "admin":
                intent = new Intent(MainActivity.this, MainAdminActivity.class);
                break;
            case "pimpinan":
                intent = new Intent(MainActivity.this, MainPimpinanActivity.class);
                break;
            case "pengguna":
                intent = new Intent(MainActivity.this, MainPenggunaActivity.class);
                break;
            default:
                showErrorAlert("Role tidak valid: " + role);
                return;
        }

        if (intent != null) {
            startActivity(intent);
            finish(); // Tutup activity login
            Log.d(TAG, "Redirected to " + role + " main activity");
        }
    }

    // Class untuk menyimpan hasil login
    private static class LoginResult {
        boolean success = false;
        String message = "";
        String userId = "";
        String username = "";
        String role = "";
        String nama = "";
        String profile = "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Pastikan dialog ditutup untuk mencegah memory leak
        if (progressDialog != null) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            progressDialog = null;
        }

        if (binding != null) {
            binding = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Sembunyikan dialog saat activity di-pause untuk mencegah WindowLeaked
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}