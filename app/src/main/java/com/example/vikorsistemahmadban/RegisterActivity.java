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

import com.example.vikorsistemahmadban.api.JDBCConnection;
import com.example.vikorsistemahmadban.databinding.ActivityRegisterBinding;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private JDBCConnection jdbcConnection;
    private ProgressDialog progressDialog;
    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        jdbcConnection = new JDBCConnection();

        // Initialize progress dialog
        initializeProgressDialog();

        // Set click listener untuk tombol register
        binding.btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegister();
            }
        });

        // Set click listener untuk tombol login (redirect ke MainActivity)
        binding.btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void initializeProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Mohon Tunggu");
        progressDialog.setMessage("Sedang memproses registrasi...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
    }

    private void performRegister() {
        String fullName = binding.etFullName.getText().toString().trim();
        String username = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        // Validasi input
        if (fullName.isEmpty()) {
            binding.teFullName.setError("Nama lengkap tidak boleh kosong");
            return;
        }
        if (username.isEmpty()) {
            binding.tilEmail.setError("Username tidak boleh kosong");
            return;
        }
        if (password.isEmpty()) {
            binding.tilPassword.setError("Password tidak boleh kosong");
            return;
        }
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.setError("Konfirmasi password tidak boleh kosong");
            return;
        }

        // Validasi panjang nama
        if (fullName.length() < 2) {
            binding.teFullName.setError("Nama lengkap minimal 2 karakter");
            return;
        }

        // Validasi panjang username
        if (username.length() < 4) {
            binding.tilEmail.setError("Username minimal 4 karakter");
            return;
        }

        // Validasi panjang password
        if (password.length() < 6) {
            binding.tilPassword.setError("Password minimal 6 karakter");
            return;
        }

        // Validasi konfirmasi password
        if (!password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError("Password tidak cocok");
            return;
        }

        // Clear error
        binding.teFullName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        // Jalankan registrasi dalam background thread
        new RegisterTask().execute(fullName, username, password);
    }

    private class RegisterTask extends AsyncTask<String, String, RegisterResult> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Tampilkan loading dialog
            showLoadingDialog("Menghubungkan ke server...");
        }

        @Override
        protected RegisterResult doInBackground(String... params) {
            String fullName = params[0];
            String username = params[1];
            String password = params[2];

            // Update progress message
            publishProgress("Memeriksa ketersediaan username...");

            try {
                // Simulasi delay untuk menunjukkan loading (opsional)
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Update progress message
            publishProgress("Membuat akun pengguna...");

            return registerUser(fullName, username, password);
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
        protected void onPostExecute(RegisterResult result) {
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

    private RegisterResult registerUser(String fullName, String username, String password) {
        RegisterResult result = new RegisterResult();
        Connection conn = null;
        PreparedStatement pstmtCheckUser = null;
        PreparedStatement pstmtInsertUser = null;
        PreparedStatement pstmtInsertPengguna = null;
        ResultSet rs = null;

        try {
            conn = jdbcConnection.getConnection();
            if (conn == null) {
                result.message = "Gagal terhubung ke database";
                return result;
            }

            // Mulai transaksi
            conn.setAutoCommit(false);

            // 1. Cek apakah username sudah ada
            String checkUserQuery = "SELECT COUNT(*) FROM tb_user WHERE username = ?";
            pstmtCheckUser = conn.prepareStatement(checkUserQuery);
            pstmtCheckUser.setString(1, username);
            rs = pstmtCheckUser.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                result.success = false;
                result.message = "Username sudah digunakan. Silakan pilih username lain.";
                return result;
            }

            // 2. Insert ke tb_user
            String insertUserQuery = "INSERT INTO tb_user (username, password, role) VALUES (?, ?, 'pengguna')";
            pstmtInsertUser = conn.prepareStatement(insertUserQuery, Statement.RETURN_GENERATED_KEYS);
            pstmtInsertUser.setString(1, username);
            pstmtInsertUser.setString(2, password);

            int userRowsAffected = pstmtInsertUser.executeUpdate();
            if (userRowsAffected == 0) {
                result.success = false;
                result.message = "Gagal membuat akun pengguna";
                return result;
            }

            // Dapatkan ID user yang baru dibuat
            ResultSet generatedKeys = pstmtInsertUser.getGeneratedKeys();
            long userId = 0;
            if (generatedKeys.next()) {
                userId = generatedKeys.getLong(1);
            } else {
                result.success = false;
                result.message = "Gagal mendapatkan ID pengguna";
                return result;
            }

            // 3. Insert ke tb_pengguna
            String insertPenggunaQuery = "INSERT INTO tb_pengguna (id_user, nama, profile) VALUES (?, ?, NULL)";
            pstmtInsertPengguna = conn.prepareStatement(insertPenggunaQuery);
            pstmtInsertPengguna.setLong(1, userId);
            pstmtInsertPengguna.setString(2, fullName); // Gunakan nama lengkap

            int penggunaRowsAffected = pstmtInsertPengguna.executeUpdate();
            if (penggunaRowsAffected == 0) {
                result.success = false;
                result.message = "Gagal membuat profil pengguna";
                return result;
            }

            // Commit transaksi
            conn.commit();

            // Registrasi berhasil
            result.success = true;
            result.userId = String.valueOf(userId);
            result.username = username;
            result.fullName = fullName;
            result.message = "Registrasi berhasil!";

            Log.d(TAG, "Registration successful for user: " + username + " with ID: " + userId);

        } catch (SQLException e) {
            // Rollback transaksi jika terjadi error
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException rollbackEx) {
                Log.e(TAG, "Error during rollback: " + rollbackEx.getMessage());
            }

            result.success = false;
            result.message = "Error database: " + e.getMessage();
            Log.e(TAG, "SQL Exception during registration: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // Rollback transaksi jika terjadi error
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException rollbackEx) {
                Log.e(TAG, "Error during rollback: " + rollbackEx.getMessage());
            }

            result.success = false;
            result.message = "Error sistem: " + e.getMessage();
            Log.e(TAG, "General Exception during registration: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Tutup koneksi
            try {
                if (rs != null) rs.close();
                if (pstmtCheckUser != null) pstmtCheckUser.close();
                if (pstmtInsertUser != null) pstmtInsertUser.close();
                if (pstmtInsertPengguna != null) pstmtInsertPengguna.close();
                if (conn != null) {
                    conn.setAutoCommit(true); // Reset auto commit
                    conn.close();
                }
            } catch (SQLException e) {
                Log.e(TAG, "Error closing connection: " + e.getMessage());
            }
        }

        return result;
    }

    private void showSuccessAlert(RegisterResult result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Registrasi Berhasil")
                .setMessage("Akun Anda berhasil dibuat!\nUsername: " + result.username + "\n\nSilakan login untuk melanjutkan.")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("Login Sekarang", (dialog, which) -> {
                    // Redirect ke halaman login
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Nanti", (dialog, which) -> {
                    // Tutup dialog saja
                    dialog.dismiss();
                    // Clear form
                    binding.etEmail.setText("");
                    binding.etPassword.setText("");
                })
                .setCancelable(false)
                .show();
    }

    private void showErrorAlert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Registrasi Gagal")
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();

        // Juga tampilkan toast
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Class untuk menyimpan hasil registrasi
    private static class RegisterResult {
        boolean success = false;
        String message = "";
        String userId = "";
        String username = "";
        String fullName = "";
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