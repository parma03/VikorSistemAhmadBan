package com.example.vikorsistemahmadban.activity.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.vikorsistemahmadban.LogoutActivity;
import com.example.vikorsistemahmadban.R;
import com.example.vikorsistemahmadban.api.JDBCConnection;
import com.example.vikorsistemahmadban.databinding.ActivityMainAdminBinding;
import com.example.vikorsistemahmadban.model.BanModel;
import com.example.vikorsistemahmadban.model.KriteriaModel;
import com.example.vikorsistemahmadban.model.ProsesModel;
import com.example.vikorsistemahmadban.model.SubKriteriaModel;
import com.example.vikorsistemahmadban.model.VikorResultModel;
import com.mysql.jdbc.Connection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainAdminActivity extends AppCompatActivity {
    private ActivityMainAdminBinding binding;
    private boolean isDropdownVisible = false;
    private JDBCConnection jdbcConnection;
    private DecimalFormat df = new DecimalFormat("#.####");

    // Role management
    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_PIMPINAN = "pimpinan";
    private static final String ROLE_PENGGUNA = "pengguna";
    private String userRole;

    // Data untuk perhitungan VIKOR
    private List<BanModel> banList = new ArrayList<>();
    private List<KriteriaModel> kriteriaList = new ArrayList<>();
    private List<SubKriteriaModel> subKriteriaList = new ArrayList<>();
    private List<ProsesModel> prosesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        jdbcConnection = new JDBCConnection();
        getUserRole();
        setupMenuBasedOnRole();
        setupClickListeners();
        loadTop3VikorRankings();
    }

    private void getUserRole() {
        // Prioritas 1: Ambil role dari Intent yang dikirim dari LoginActivity
        userRole = getIntent().getStringExtra("USER_ROLE");

        // Prioritas 2: Jika tidak ada di Intent, ambil dari SharedPreferences
        if (userRole == null || userRole.isEmpty()) {
            SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
            userRole = sharedPreferences.getString("USER_ROLE", "");
        }

        // Prioritas 3: Jika masih tidak ada, ambil dari PrefManager
        if (userRole == null || userRole.isEmpty()) {
            com.example.vikorsistemahmadban.api.PrefManager prefManager =
                    new com.example.vikorsistemahmadban.api.PrefManager(this);
            userRole = prefManager.getTipe();
        }

        // Default ke pengguna jika masih null
        if (userRole == null || userRole.isEmpty()) {
            userRole = ROLE_PENGGUNA;
        }
    }

    private void setupMenuBasedOnRole() {
        switch (userRole) {
            case ROLE_ADMIN:
                // Admin bisa akses semua menu
                binding.tvWelcome.setText("Admin Dashboard");
                binding.cardDataUser.setVisibility(View.VISIBLE);
                binding.cardDataBan.setVisibility(View.VISIBLE);
                binding.cardDataKriteria.setVisibility(View.VISIBLE);
                binding.cardDataProcessing.setVisibility(View.VISIBLE);
                binding.cardDataVikor.setVisibility(View.VISIBLE);
                break;

            case ROLE_PIMPINAN:
                // Pimpinan hanya bisa akses Data Ban, Data Kriteria, dan Data Vikor
                binding.tvWelcome.setText("Pimpinan Dashboard");
                binding.cardDataUser.setVisibility(View.VISIBLE);
                binding.cardDataBan.setVisibility(View.VISIBLE);
                binding.cardDataKriteria.setVisibility(View.VISIBLE);
                binding.cardDataProcessing.setVisibility(View.VISIBLE);
                binding.cardDataVikor.setVisibility(View.VISIBLE);
                break;

            case ROLE_PENGGUNA:
                // Pengguna hanya bisa akses Data Ban dan Data Vikor
                binding.tvWelcome.setText("Pengguna Dashboard");
                binding.cardDataUser.setVisibility(View.GONE);
                binding.cardDataBan.setVisibility(View.VISIBLE);
                binding.cardDataKriteria.setVisibility(View.VISIBLE);
                binding.cardDataProcessing.setVisibility(View.VISIBLE);
                binding.cardDataVikor.setVisibility(View.VISIBLE);
                adjustLayoutForMissingCard();
                break;

            default:
                break;
        }
    }

    private void adjustLayoutForMissingCard() {
        // Mengatur margin untuk card Data Ban agar memenuhi ruang yang tersedia
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) binding.cardDataBan.getLayoutParams();
        params.setMarginStart(0); // Hilangkan margin start
        params.setMarginEnd(0);   // Hilangkan margin end
        binding.cardDataBan.setLayoutParams(params);
    }

    private void setupClickListeners() {
        // Profile container click listener untuk toggle dropdown
        binding.profileContainer.setOnClickListener(v -> toggleDropdownMenu());

        // Menu cards click listeners
        binding.cardDataUser.setOnClickListener(v -> {
            Intent intent = new Intent(MainAdminActivity.this, DataUserActivity.class);
            startActivity(intent);
        });

        binding.cardDataBan.setOnClickListener(v -> {
            Intent intent = new Intent(MainAdminActivity.this, DataBanActivity.class);
            startActivity(intent);
        });

        binding.cardDataKriteria.setOnClickListener(v -> {
            Intent intent = new Intent(MainAdminActivity.this, DataKriteriaActivity.class);
            startActivity(intent);
        });

        binding.cardDataProcessing.setOnClickListener(v -> {
            Intent intent = new Intent(MainAdminActivity.this, VikorProcesActivity.class);
            startActivity(intent);
        });

        binding.cardDataVikor.setOnClickListener(v -> {
            Intent intent = new Intent(MainAdminActivity.this, DataVikorActivity.class);
            startActivity(intent);
        });

        // Dropdown menu items click listeners
        binding.menuProfile.setOnClickListener(v -> {
            hideDropdownMenu();
            // Intent ke ProfileSettingActivity
            // Intent intent = new Intent(MainAdminActivity.this, ProfileSettingActivity.class);
            // startActivity(intent);
        });

        binding.menuLogout.setOnClickListener(v -> {
            hideDropdownMenu();
            // Panggil LogoutHelper untuk menampilkan konfirmasi dan logout
            LogoutActivity.showLogoutConfirmation(MainAdminActivity.this);
        });

        // Click listener untuk area lain (untuk menutup dropdown)
        binding.main.setOnClickListener(v -> {
            if (isDropdownVisible) {
                hideDropdownMenu();
            }
        });
    }

    private void toggleDropdownMenu() {
        if (isDropdownVisible) {
            hideDropdownMenu();
        } else {
            showDropdownMenu();
        }
    }

    private void showDropdownMenu() {
        binding.dropdownMenu.setVisibility(View.VISIBLE);

        // Animasi slide down
        Animation slideDown = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        slideDown.setDuration(200);
        binding.dropdownMenu.startAnimation(slideDown);

        // Rotasi icon dropdown
        binding.dropdownIcon.animate()
                .rotation(180f)
                .setDuration(200)
                .start();

        isDropdownVisible = true;
    }

    private void hideDropdownMenu() {
        // Animasi slide up
        Animation slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
        slideUp.setDuration(200);
        slideUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                binding.dropdownMenu.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        binding.dropdownMenu.startAnimation(slideUp);

        // Reset rotasi icon dropdown
        binding.dropdownIcon.animate()
                .rotation(0f)
                .setDuration(200)
                .start();

        isDropdownVisible = false;
    }

    private void loadTop3VikorRankings() {
        new LoadTop3VikorTask().execute();
    }

    private class LoadTop3VikorTask extends AsyncTask<Void, Void, List<VikorResultModel>> {
        @Override
        protected List<VikorResultModel> doInBackground(Void... voids) {
            // Load semua data yang diperlukan
            if (loadBanData() && loadKriteriaData() && loadSubKriteriaData() && loadProsesData()) {
                return calculateTop3VikorRanking();
            }
            return new ArrayList<>();
        }

        @Override
        protected void onPostExecute(List<VikorResultModel> top3Results) {
            displayTop3Rankings(top3Results);
        }
    }

    private boolean loadBanData() {
        Connection conn = null;
        try {
            conn = (Connection) jdbcConnection.getConnection();
            if (conn != null) {
                String query = "SELECT * FROM tb_ban ORDER BY id_ban";
                PreparedStatement ps = conn.prepareStatement(query);
                ResultSet rs = ps.executeQuery();

                banList.clear();
                while (rs.next()) {
                    BanModel ban = new BanModel(
                            rs.getString("id_ban"),
                            rs.getString("nama_ban"),
                            rs.getString("harga"),
                            rs.getString("deskripsi"),
                            rs.getString("foto_ban"),
                            rs.getString("created_at")
                    );
                    banList.add(ban);
                }
                return true;
            }
        } catch (SQLException e) {
            Log.e("LoadBanData", "Error: " + e.getMessage());
        } finally {
            JDBCConnection.closeConnection(conn);
        }
        return false;
    }

    private boolean loadKriteriaData() {
        Connection conn = null;
        try {
            conn = (Connection) jdbcConnection.getConnection();
            if (conn != null) {
                String query = "SELECT * FROM tb_kriteria ORDER BY id_kriteria";
                PreparedStatement ps = conn.prepareStatement(query);
                ResultSet rs = ps.executeQuery();

                kriteriaList.clear();
                while (rs.next()) {
                    KriteriaModel kriteria = new KriteriaModel(
                            rs.getString("id_kriteria"),
                            rs.getString("nama_kriteria"),
                            rs.getString("kategori"),
                            rs.getString("nilai"),
                            rs.getString("bobot")
                    );
                    kriteriaList.add(kriteria);
                }
                return true;
            }
        } catch (SQLException e) {
            Log.e("LoadKriteriaData", "Error: " + e.getMessage());
        } finally {
            JDBCConnection.closeConnection(conn);
        }
        return false;
    }

    private boolean loadSubKriteriaData() {
        Connection conn = null;
        try {
            conn = (Connection) jdbcConnection.getConnection();
            if (conn != null) {
                String query = "SELECT * FROM tb_subkriteria ORDER BY id_subkriteria";
                PreparedStatement ps = conn.prepareStatement(query);
                ResultSet rs = ps.executeQuery();

                subKriteriaList.clear();
                while (rs.next()) {
                    SubKriteriaModel subKriteria = new SubKriteriaModel(
                            rs.getString("id_subkriteria"),
                            rs.getString("id_kriteria"),
                            rs.getString("klasifikasi"),
                            rs.getInt("bobot_sub_kriteria")
                    );
                    subKriteriaList.add(subKriteria);
                }
                return true;
            }
        } catch (SQLException e) {
            Log.e("LoadSubKriteriaData", "Error: " + e.getMessage());
        } finally {
            JDBCConnection.closeConnection(conn);
        }
        return false;
    }

    private boolean loadProsesData() {
        Connection conn = null;
        try {
            conn = (Connection) jdbcConnection.getConnection();
            if (conn != null) {
                // Ambil data proses dari tanggal terbaru saja
                String query = "SELECT * FROM tb_proses WHERE created_at = (SELECT MAX(created_at) FROM tb_proses) ORDER BY id_proses";
                PreparedStatement ps = conn.prepareStatement(query);
                ResultSet rs = ps.executeQuery();

                prosesList.clear();
                while (rs.next()) {
                    ProsesModel proses = new ProsesModel(
                            rs.getString("id_proses"),
                            rs.getString("id_ban"),
                            rs.getString("id_kriteria"),
                            rs.getString("id_subkriteria"),
                            rs.getString("created_at")
                    );
                    prosesList.add(proses);
                }
                return true;
            }
        } catch (SQLException e) {
            Log.e("LoadProsesData", "Error: " + e.getMessage());
        } finally {
            JDBCConnection.closeConnection(conn);
        }
        return false;
    }

    private List<VikorResultModel> calculateTop3VikorRanking() {
        List<VikorResultModel> vikorResults = new ArrayList<>();

        if (prosesList.isEmpty()) {
            return vikorResults;
        }

        // Ambil tanggal terbaru
        String latestDate = prosesList.get(0).getCreated_at();

        // Kelompokkan ban berdasarkan tanggal terbaru
        Set<String> banIds = new HashSet<>();
        for (ProsesModel proses : prosesList) {
            if (proses.getCreated_at().equals(latestDate)) {
                banIds.add(proses.getId_ban());
            }
        }

        // 1. Menyusun Matriks Keputusan
        Map<String, Map<String, Double>> decisionMatrix = buildDecisionMatrix(banIds);

        if (decisionMatrix.isEmpty()) {
            return vikorResults;
        }

        // 2. Mencari nilai terbaik (f*) dan terburuk (f-) untuk setiap kriteria
        Map<String, Double> bestValues = new HashMap<>();
        Map<String, Double> worstValues = new HashMap<>();

        for (KriteriaModel kriteria : kriteriaList) {
            String kriteriaId = kriteria.getId_kriteria();
            String kriteriaType = kriteria.getKategori();

            List<Double> values = new ArrayList<>();
            for (String banId : banIds) {
                if (decisionMatrix.containsKey(banId) && decisionMatrix.get(banId).containsKey(kriteriaId)) {
                    values.add(decisionMatrix.get(banId).get(kriteriaId));
                }
            }

            if (!values.isEmpty()) {
                if ("benefit".equalsIgnoreCase(kriteriaType)) {
                    bestValues.put(kriteriaId, Collections.max(values));
                    worstValues.put(kriteriaId, Collections.min(values));
                } else {
                    bestValues.put(kriteriaId, Collections.min(values));
                    worstValues.put(kriteriaId, Collections.max(values));
                }
            }
        }

        // 3. Normalisasi
        Map<String, Map<String, Double>> normalizedMatrix = new HashMap<>();
        for (String banId : banIds) {
            if (decisionMatrix.containsKey(banId)) {
                normalizedMatrix.put(banId, new HashMap<>());

                for (KriteriaModel kriteria : kriteriaList) {
                    String kriteriaId = kriteria.getId_kriteria();
                    if (decisionMatrix.get(banId).containsKey(kriteriaId) &&
                            bestValues.containsKey(kriteriaId) && worstValues.containsKey(kriteriaId)) {

                        double originalValue = decisionMatrix.get(banId).get(kriteriaId);
                        double bestVal = bestValues.get(kriteriaId);
                        double worstVal = worstValues.get(kriteriaId);

                        double normalized = 0.0;
                        if (Math.abs(bestVal - worstVal) > 1e-10) {
                            normalized = (bestVal - originalValue) / (bestVal - worstVal);
                        }
                        normalizedMatrix.get(banId).put(kriteriaId, normalized);
                    }
                }
            }
        }

        // 4. Hitung Si dan Ri
        Map<String, Double> siValues = new HashMap<>();
        Map<String, Double> riValues = new HashMap<>();

        for (String banId : banIds) {
            if (normalizedMatrix.containsKey(banId)) {
                double si = 0.0;
                double ri = 0.0;

                for (KriteriaModel kriteria : kriteriaList) {
                    String kriteriaId = kriteria.getId_kriteria();
                    if (normalizedMatrix.get(banId).containsKey(kriteriaId)) {
                        double bobot = Double.parseDouble(kriteria.getBobot());
                        double normalizedValue = normalizedMatrix.get(banId).get(kriteriaId);

                        si += bobot * normalizedValue;
                        ri = Math.max(ri, bobot * normalizedValue);
                    }
                }

                siValues.put(banId, si);
                riValues.put(banId, ri);
            }
        }

        // 5. Mencari S*, S-, R*, R-
        if (siValues.isEmpty() || riValues.isEmpty()) {
            return vikorResults;
        }

        double sPlus = Collections.min(siValues.values());
        double sMinus = Collections.max(siValues.values());
        double rPlus = Collections.min(riValues.values());
        double rMinus = Collections.max(riValues.values());

        // 6. Hitung Qi
        double v = 0.5;
        for (String banId : banIds) {
            if (siValues.containsKey(banId) && riValues.containsKey(banId)) {
                double si = siValues.get(banId);
                double ri = riValues.get(banId);

                double qi = 0.0;
                if (Math.abs(sMinus - sPlus) > 1e-10 && Math.abs(rMinus - rPlus) > 1e-10) {
                    qi = v * ((si - sPlus) / (sMinus - sPlus)) + (1 - v) * ((ri - rPlus) / (rMinus - rPlus));
                }

                // Cari informasi ban
                BanModel ban = null;
                for (BanModel b : banList) {
                    if (b.getId_ban().equals(banId)) {
                        ban = b;
                        break;
                    }
                }

                if (ban != null) {
                    VikorResultModel result = new VikorResultModel();
                    result.setNamaBan(ban.getNama_ban());
                    result.setAlternatif("A" + banId);
                    result.setSiValue(si);
                    result.setRiValue(ri);
                    result.setQiValue(qi);
                    vikorResults.add(result);
                }
            }
        }

        // 7. Sorting berdasarkan Qi (ascending) dan ambil top 3
        vikorResults.sort((a, b) -> Double.compare(a.getQiValue(), b.getQiValue()));

        // Set ranking dan ambil top 3
        for (int i = 0; i < vikorResults.size(); i++) {
            vikorResults.get(i).setRanking(i + 1);
        }

        return vikorResults.size() > 3 ? vikorResults.subList(0, 3) : vikorResults;
    }

    private Map<String, Map<String, Double>> buildDecisionMatrix(Set<String> banIds) {
        Map<String, Map<String, Double>> matrix = new HashMap<>();

        for (String banId : banIds) {
            matrix.put(banId, new HashMap<>());

            for (ProsesModel proses : prosesList) {
                if (proses.getId_ban().equals(banId)) {
                    String kriteriaId = proses.getId_kriteria();
                    String subKriteriaId = proses.getId_subkriteria();

                    for (SubKriteriaModel subKriteria : subKriteriaList) {
                        if (subKriteria.getId_subkriteria().equals(subKriteriaId)) {
                            double value = subKriteria.getBobot_sub_kriteria();
                            matrix.get(banId).put(kriteriaId, value);
                            break;
                        }
                    }
                }
            }
        }

        return matrix;
    }

    private void displayTop3Rankings(List<VikorResultModel> top3Results) {
        // Reset semua text view ke default
        binding.tvRank1Name.setText("No Data");
        binding.tvRank1Score.setText("0.0000");
        binding.tvRank2Name.setText("No Data");
        binding.tvRank2Score.setText("0.0000");
        binding.tvRank3Name.setText("No Data");
        binding.tvRank3Score.setText("0.0000");

        // Tampilkan data jika ada
        if (top3Results.size() >= 1) {
            VikorResultModel rank1 = top3Results.get(0);
            binding.tvRank1Name.setText(rank1.getNamaBan());
            binding.tvRank1Score.setText(df.format(rank1.getQiValue()));
        }

        if (top3Results.size() >= 2) {
            VikorResultModel rank2 = top3Results.get(1);
            binding.tvRank2Name.setText(rank2.getNamaBan());
            binding.tvRank2Score.setText(df.format(rank2.getQiValue()));
        }

        if (top3Results.size() >= 3) {
            VikorResultModel rank3 = top3Results.get(2);
            binding.tvRank3Name.setText(rank3.getNamaBan());
            binding.tvRank3Score.setText(df.format(rank3.getQiValue()));
        }
    }

    @Override
    public void onBackPressed() {
        if (isDropdownVisible) {
            hideDropdownMenu();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh top 3 rankings ketika kembali ke activity ini
        loadTop3VikorRankings();
    }
}