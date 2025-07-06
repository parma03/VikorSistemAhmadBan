package com.example.vikorsistemahmadban.activity.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.vikorsistemahmadban.R;
import com.example.vikorsistemahmadban.adapter.VikorAdapter;
import com.example.vikorsistemahmadban.api.JDBCConnection;
import com.example.vikorsistemahmadban.databinding.ActivityDataVikorBinding;
import com.example.vikorsistemahmadban.model.BanModel;
import com.example.vikorsistemahmadban.model.KriteriaModel;
import com.example.vikorsistemahmadban.model.ProsesModel;
import com.example.vikorsistemahmadban.model.SubKriteriaModel;
import com.example.vikorsistemahmadban.model.VikorResultModel;
import com.itextpdf.layout.properties.TextAlignment;
import com.mysql.jdbc.Connection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

// 3. Modifikasi DataVikorActivity.java - Tambahkan import statements
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// iText PDF
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;

// Apache POI Excel
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DataVikorActivity extends AppCompatActivity {
    private ActivityDataVikorBinding binding;
    private VikorAdapter vikorAdapter;
    private List<BanModel> banList = new ArrayList<>();
    private List<KriteriaModel> kriteriaList = new ArrayList<>();
    private List<SubKriteriaModel> subKriteriaList = new ArrayList<>();
    private List<ProsesModel> prosesList = new ArrayList<>();
    private JDBCConnection jdbcConnection;
    private DecimalFormat df = new DecimalFormat("#.####");
    private static final int REQUEST_WRITE_PERMISSION = 1001;
    private static final int REQUEST_STORAGE_PERMISSION = 1002;
    private static final int REQUEST_CREATE_PDF_FILE = 1003;
    private static final int REQUEST_CREATE_EXCEL_FILE = 1004;
    private List<VikorResultModel> currentVikorResults = new ArrayList<>();

    // Role management
    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_PIMPINAN = "pimpinan";
    private static final String ROLE_PENGGUNA = "pengguna";
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDataVikorBinding.inflate(getLayoutInflater());
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
        setupRecyclerView();
        setupExportButtons();
        loadDataAndCalculateVikor();
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
            case ROLE_PIMPINAN:
                // Sembunyikan FAB untuk pimpinan
                binding.btnExportExcel.setVisibility(View.VISIBLE);
                binding.btnExportPDF.setVisibility(View.VISIBLE);
                break;
            case ROLE_PENGGUNA:
                // Sembunyikan FAB untuk pengguna
                binding.btnExportExcel.setVisibility(View.GONE);
                binding.btnExportPDF.setVisibility(View.GONE);
                break;
            case ROLE_ADMIN:
                // Admin bisa akses semua
                binding.btnExportExcel.setVisibility(View.VISIBLE);
                binding.btnExportPDF.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void setupRecyclerView() {
        vikorAdapter = new VikorAdapter(this);
        binding.rvVikorResults.setLayoutManager(new LinearLayoutManager(this));
        binding.rvVikorResults.setAdapter(vikorAdapter);
    }

    private void loadDataAndCalculateVikor() {
        showLoading(true);
        new LoadAllDataTask().execute();
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.rvVikorResults.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private class LoadAllDataTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            return loadBanData() && loadKriteriaData() && loadSubKriteriaData() && loadProsesData();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                calculateVikorRanking();
            }
            showLoading(false);
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

                while (rs.next()) {
                    // PERBAIKAN: Mapping field yang benar
                    KriteriaModel kriteria = new KriteriaModel(
                            rs.getString("id_kriteria"),
                            rs.getString("nama_kriteria"),
                            rs.getString("kategori"),        // kategori (benefit/cost)
                            rs.getString("nilai"),           // nilai (untuk perhitungan bobot)
                            rs.getString("bobot")            // bobot final
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
                String query = "SELECT * FROM tb_proses ORDER BY id_proses";
                PreparedStatement ps = conn.prepareStatement(query);
                ResultSet rs = ps.executeQuery();

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

    private void calculateVikorRanking() {
        List<VikorResultModel> vikorResults = new ArrayList<>();

        // Kelompokkan data berdasarkan tanggal
        Map<String, List<ProsesModel>> prosesGroupedByDate = new HashMap<>();

        for (ProsesModel proses : prosesList) {
            String date = proses.getCreated_at();
            if (!prosesGroupedByDate.containsKey(date)) {
                prosesGroupedByDate.put(date, new ArrayList<>());
            }
            prosesGroupedByDate.get(date).add(proses);
        }

        // Hitung VIKOR untuk setiap tanggal
        for (String date : prosesGroupedByDate.keySet()) {
            List<ProsesModel> prosesForDate = prosesGroupedByDate.get(date);

            Set<String> banIdsForDate = new HashSet<>();
            for (ProsesModel proses : prosesForDate) {
                banIdsForDate.add(proses.getId_ban());
            }

            List<VikorResultModel> resultsForDate = calculateVikorForDate(date, prosesForDate, banIdsForDate);
            vikorResults.addAll(resultsForDate);
        }

        // Ranking global
        vikorResults.sort((a, b) -> Double.compare(a.getQiValue(), b.getQiValue()));
        for (int i = 0; i < vikorResults.size(); i++) {
            vikorResults.get(i).setRanking(i + 1);
        }

        currentVikorResults = vikorResults;
        vikorAdapter.setVikorList(vikorResults);
    }

    private List<VikorResultModel> calculateVikorForDate(String date, List<ProsesModel> prosesForDate, Set<String> banIdsForDate) {
        List<VikorResultModel> results = new ArrayList<>();

        // 1. Menyusun Matriks Keputusan
        Map<String, Map<String, Double>> decisionMatrix = buildDecisionMatrixForDate(prosesForDate, banIdsForDate);

        if (decisionMatrix.isEmpty()) {
            return results;
        }

        // 2. Mencari nilai terbaik (f*) dan terburuk (f-) untuk setiap kriteria
        Map<String, Double> bestValues = new HashMap<>(); // f*
        Map<String, Double> worstValues = new HashMap<>(); // f-

        for (KriteriaModel kriteria : kriteriaList) {
            String kriteriaId = kriteria.getId_kriteria();
            String kriteriaType = kriteria.getKategori(); // PERBAIKAN: Menggunakan kategori, bukan nilai

            double bestVal, worstVal;
            List<Double> values = new ArrayList<>();

            // Kumpulkan semua nilai untuk kriteria ini
            for (String banId : banIdsForDate) {
                if (decisionMatrix.containsKey(banId) && decisionMatrix.get(banId).containsKey(kriteriaId)) {
                    values.add(decisionMatrix.get(banId).get(kriteriaId));
                }
            }

            if (!values.isEmpty()) {
                if ("benefit".equalsIgnoreCase(kriteriaType)) {
                    // Untuk kriteria benefit: f* = max, f- = min
                    bestVal = Collections.max(values);
                    worstVal = Collections.min(values);
                } else {
                    // Untuk kriteria cost: f* = min, f- = max
                    bestVal = Collections.min(values);
                    worstVal = Collections.max(values);
                }

                bestValues.put(kriteriaId, bestVal);
                worstValues.put(kriteriaId, worstVal);
            }
        }

        // 3. Normalisasi menggunakan rumus VIKOR yang benar
        Map<String, Map<String, Double>> normalizedMatrix = new HashMap<>();

        for (String banId : banIdsForDate) {
            if (decisionMatrix.containsKey(banId)) {
                normalizedMatrix.put(banId, new HashMap<>());

                for (KriteriaModel kriteria : kriteriaList) {
                    String kriteriaId = kriteria.getId_kriteria();
                    String kriteriaType = kriteria.getKategori(); // PERBAIKAN: Menggunakan kategori

                    if (decisionMatrix.get(banId).containsKey(kriteriaId) &&
                            bestValues.containsKey(kriteriaId) && worstValues.containsKey(kriteriaId)) {

                        double originalValue = decisionMatrix.get(banId).get(kriteriaId);
                        double bestVal = bestValues.get(kriteriaId);
                        double worstVal = worstValues.get(kriteriaId);

                        double normalized = 0.0;
                        if (Math.abs(bestVal - worstVal) > 1e-10) { // Menghindari pembagian dengan nol
                            // Rumus VIKOR: Rij = (fj* - fij) / (fj* - fj-) untuk semua jenis kriteria
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

        for (String banId : banIdsForDate) {
            if (normalizedMatrix.containsKey(banId)) {
                double si = 0.0;
                double ri = 0.0;

                for (KriteriaModel kriteria : kriteriaList) {
                    String kriteriaId = kriteria.getId_kriteria();
                    if (normalizedMatrix.get(banId).containsKey(kriteriaId)) {
                        double bobot = Double.parseDouble(kriteria.getBobot());
                        double normalizedValue = normalizedMatrix.get(banId).get(kriteriaId);

                        // Si = Σ(wj * Rij)
                        si += bobot * normalizedValue;

                        // Ri = max(wj * Rij)
                        ri = Math.max(ri, bobot * normalizedValue);
                    }
                }

                siValues.put(banId, si);
                riValues.put(banId, ri);
            }
        }

        // 5. Mencari S*, S-, R*, R-
        if (siValues.isEmpty() || riValues.isEmpty()) {
            return results;
        }

        double sPlus = Collections.min(siValues.values());  // S* = min(Si)
        double sMinus = Collections.max(siValues.values()); // S- = max(Si)
        double rPlus = Collections.min(riValues.values());  // R* = min(Ri)
        double rMinus = Collections.max(riValues.values()); // R- = max(Ri)

        // 6. Hitung Qi dengan v = 0.5
        double v = 0.5;

        for (String banId : banIdsForDate) {
            if (siValues.containsKey(banId) && riValues.containsKey(banId)) {
                double si = siValues.get(banId);
                double ri = riValues.get(banId);

                double qi = 0.0;
                if (Math.abs(sMinus - sPlus) > 1e-10 && Math.abs(rMinus - rPlus) > 1e-10) {
                    // Qi = v * (Si - S*) / (S- - S*) + (1-v) * (Ri - R*) / (R- - R*)
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
                    result.setNamaBan(ban.getNama_ban() + " (" + date + ")");
                    result.setAlternatif("A" + banId + "_" + date.replace("-", ""));
                    result.setSiValue(si);
                    result.setRiValue(ri);
                    result.setQiValue(qi);

                    // Generate detail perhitungan
                    result.setMatriksKeputusan(generateMatriksKeputusan(ban, decisionMatrix, date));
                    result.setNormalisasiDetail(generateNormalisasiDetail(ban, normalizedMatrix, bestValues, worstValues, date));
                    result.setPerhitunganSi(generatePerhitunganSi(ban, normalizedMatrix, si, date));
                    result.setPerhitunganRi(generatePerhitunganRi(ban, normalizedMatrix, ri, date));
                    result.setPerhitunganQi(generatePerhitunganQi(si, ri, qi, sPlus, sMinus, rPlus, rMinus, v, date));

                    results.add(result);
                }
            }
        }

        // 7. Sorting berdasarkan Qi (ascending)
        results.sort((a, b) -> Double.compare(a.getQiValue(), b.getQiValue()));

        // 8. Set ranking
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRanking(i + 1);
        }

        return results;
    }

    private Map<String, Map<String, Double>> buildDecisionMatrixForDate(List<ProsesModel> prosesForDate, Set<String> banIdsForDate) {
        Map<String, Map<String, Double>> matrix = new HashMap<>();

        for (String banId : banIdsForDate) {
            matrix.put(banId, new HashMap<>());

            for (ProsesModel proses : prosesForDate) {
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

    // Helper methods untuk generate detail perhitungan
    private String generateMatriksKeputusan(BanModel ban, Map<String, Map<String, Double>> matrix, String date) {
        StringBuilder sb = new StringBuilder();
        sb.append("Matriks Keputusan untuk ").append(ban.getNama_ban()).append(" (").append(date).append("):\n\n");

        Map<String, Double> banValues = matrix.get(ban.getId_ban());
        if (banValues != null) {
            for (KriteriaModel kriteria : kriteriaList) {
                String kriteriaId = kriteria.getId_kriteria();
                if (banValues.containsKey(kriteriaId)) {
                    sb.append(kriteria.getNama_kriteria()).append(" (").append(kriteria.getKategori()).append("): ")
                            .append(banValues.get(kriteriaId)).append("\n");
                }
            }
        }

        return sb.toString();
    }

    private String generateNormalisasiDetail(BanModel ban, Map<String, Map<String, Double>> normalizedMatrix,
                                             Map<String, Double> bestValues, Map<String, Double> worstValues, String date) {
        StringBuilder sb = new StringBuilder();
        sb.append("Normalisasi untuk ").append(ban.getNama_ban()).append(" (").append(date).append("):\n\n");

        Map<String, Double> banValues = normalizedMatrix.get(ban.getId_ban());
        if (banValues != null) {
            int index = 1;

            for (KriteriaModel kriteria : kriteriaList) {
                String kriteriaId = kriteria.getId_kriteria();
                if (banValues.containsKey(kriteriaId)) {
                    double bestVal = bestValues.get(kriteriaId);
                    double worstVal = worstValues.get(kriteriaId);
                    double normalized = banValues.get(kriteriaId);
                    String kriteriaType = kriteria.getKategori();

                    sb.append("R").append(index).append(" (").append(kriteriaType).append(") = ");
                    sb.append("(").append(df.format(bestVal)).append(" - nilai) / (").append(df.format(bestVal))
                            .append(" - ").append(df.format(worstVal)).append(")");
                    sb.append(" = ").append(df.format(normalized)).append("\n");
                    index++;
                }
            }
        }

        return sb.toString();
    }

    private String generatePerhitunganSi(BanModel ban, Map<String, Map<String, Double>> normalizedMatrix, double si, String date) {
        StringBuilder sb = new StringBuilder();
        sb.append("Perhitungan Si untuk ").append(ban.getNama_ban()).append(" (").append(date).append("):\n\n");
        sb.append("Si = Σ(wj × Rij)\nSi = ");

        Map<String, Double> banValues = normalizedMatrix.get(ban.getId_ban());
        if (banValues != null) {
            boolean first = true;

            for (KriteriaModel kriteria : kriteriaList) {
                String kriteriaId = kriteria.getId_kriteria();
                if (banValues.containsKey(kriteriaId)) {
                    double bobot = Double.parseDouble(kriteria.getBobot());
                    double normalized = banValues.get(kriteriaId);

                    if (!first) sb.append(" + ");
                    sb.append("(").append(bobot).append(" × ").append(df.format(normalized)).append(")");
                    first = false;
                }
            }
        }

        sb.append("\nSi = ").append(df.format(si));
        return sb.toString();
    }

    private String generatePerhitunganRi(BanModel ban, Map<String, Map<String, Double>> normalizedMatrix, double ri, String date) {
        StringBuilder sb = new StringBuilder();
        sb.append("Perhitungan Ri untuk ").append(ban.getNama_ban()).append(" (").append(date).append("):\n\n");
        sb.append("Ri = max{wj × Rij}\nRi = max{");

        Map<String, Double> banValues = normalizedMatrix.get(ban.getId_ban());
        if (banValues != null) {
            boolean first = true;

            for (KriteriaModel kriteria : kriteriaList) {
                String kriteriaId = kriteria.getId_kriteria();
                if (banValues.containsKey(kriteriaId)) {
                    double bobot = Double.parseDouble(kriteria.getBobot());
                    double normalized = banValues.get(kriteriaId);
                    double value = bobot * normalized;

                    if (!first) sb.append(", ");
                    sb.append(df.format(value));
                    first = false;
                }
            }
        }

        sb.append("}\nRi = ").append(df.format(ri));
        return sb.toString();
    }

    private String generatePerhitunganQi(double si, double ri, double qi, double sPlus, double sMinus,
                                         double rPlus, double rMinus, double v, String date) {
        StringBuilder sb = new StringBuilder();
        sb.append("Perhitungan Qi untuk tanggal ").append(date).append(" (v = ").append(v).append("):\n\n");
        sb.append("Qi = v × (Si - S*) / (S- - S*) + (1-v) × (Ri - R*) / (R- - R*)\n\n");
        sb.append("Dengan:\n");
        sb.append("S* = ").append(df.format(sPlus)).append(" (min Si)\n");
        sb.append("S- = ").append(df.format(sMinus)).append(" (max Si)\n");
        sb.append("R* = ").append(df.format(rPlus)).append(" (min Ri)\n");
        sb.append("R- = ").append(df.format(rMinus)).append(" (max Ri)\n\n");
        sb.append("Qi = ").append(v).append(" × ((").append(df.format(si)).append(" - ").append(df.format(sPlus))
                .append(") / (").append(df.format(sMinus)).append(" - ").append(df.format(sPlus)).append(")) + ")
                .append(1-v).append(" × ((").append(df.format(ri)).append(" - ").append(df.format(rPlus))
                .append(") / (").append(df.format(rMinus)).append(" - ").append(df.format(rPlus)).append("))\n");
        sb.append("Qi = ").append(df.format(qi));

        return sb.toString();
    }

    private void setupExportButtons() {
        binding.btnExportPDF.setOnClickListener(v -> {
            if (checkPermission()) {
                createPDFFile();
            } else {
                requestPermission();
            }
        });

        binding.btnExportExcel.setOnClickListener(v -> {
            if (checkPermission()) {
                createExcelFile();
            } else {
                requestPermission();
            }
        });
    }

    private void createPDFFile() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "VIKOR_Report_" + timestamp + ".pdf";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        try {
            startActivityForResult(intent, REQUEST_CREATE_PDF_FILE);
        } catch (Exception e) {
            // Fallback ke metode lama jika SAF tidak tersedia
            exportToPDFLegacy();
        }
    }

    private void createExcelFile() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "VIKOR_Report_" + timestamp + ".xlsx";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        try {
            startActivityForResult(intent, REQUEST_CREATE_EXCEL_FILE);
        } catch (Exception e) {
            // Fallback ke metode lama jika SAF tidak tersedia
            exportToExcelLegacy();
        }
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ menggunakan Environment.isExternalStorageManager()
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-10 menggunakan WRITE_EXTERNAL_STORAGE
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ request manage storage permission
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_STORAGE_PERMISSION);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, REQUEST_STORAGE_PERMISSION);
            }
        } else {
            // Android 6-10 request write external storage
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == REQUEST_CREATE_PDF_FILE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                exportToPDFWithUri(uri);
            }
        } else if (requestCode == REQUEST_CREATE_EXCEL_FILE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                exportToExcelWithUri(uri);
            }
        }
    }

    private void exportToPDFWithUri(Uri uri) {
        try {
            // Buka output stream dari URI
            getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            PdfWriter writer = new PdfWriter(getContentResolver().openOutputStream(uri));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Font
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            // Title
            Paragraph title = new Paragraph("LAPORAN PERHITUNGAN VIKOR")
                    .setFont(boldFont)
                    .setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);

            document.add(new Paragraph("Tanggal Export: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()))
                    .setFont(font)
                    .setFontSize(10));

            document.add(new Paragraph("\n"));

            // Tabel Hasil Ranking
            document.add(new Paragraph("HASIL RANKING VIKOR")
                    .setFont(boldFont)
                    .setFontSize(14));

            Table rankingTable = new Table(6);
            rankingTable.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Ranking").setFont(boldFont)));
            rankingTable.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Alternatif").setFont(boldFont)));
            rankingTable.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Nama Ban").setFont(boldFont)));
            rankingTable.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Si").setFont(boldFont)));
            rankingTable.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Ri").setFont(boldFont)));
            rankingTable.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Qi").setFont(boldFont)));

            for (VikorResultModel result : currentVikorResults) {
                rankingTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.valueOf(result.getRanking())).setFont(font)));
                rankingTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(result.getAlternatif()).setFont(font)));
                rankingTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(result.getNamaBan()).setFont(font)));
                rankingTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(df.format(result.getSiValue())).setFont(font)));
                rankingTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(df.format(result.getRiValue())).setFont(font)));
                rankingTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(df.format(result.getQiValue())).setFont(font)));
            }

            document.add(rankingTable);
            document.add(new Paragraph("\n"));

            // Detail Perhitungan untuk setiap alternatif
            document.add(new Paragraph("DETAIL PERHITUNGAN VIKOR")
                    .setFont(boldFont)
                    .setFontSize(14));

            for (VikorResultModel result : currentVikorResults) {
                document.add(new Paragraph("\n=== " + result.getNamaBan() + " ===")
                        .setFont(boldFont)
                        .setFontSize(12));

                // Matriks Keputusan
                document.add(new Paragraph("1. MATRIKS KEPUTUSAN")
                        .setFont(boldFont)
                        .setFontSize(10));
                document.add(new Paragraph(result.getMatriksKeputusan())
                        .setFont(font)
                        .setFontSize(9));

                // Normalisasi
                document.add(new Paragraph("2. NORMALISASI")
                        .setFont(boldFont)
                        .setFontSize(10));
                document.add(new Paragraph(result.getNormalisasiDetail())
                        .setFont(font)
                        .setFontSize(9));

                // Perhitungan Si
                document.add(new Paragraph("3. PERHITUNGAN Si")
                        .setFont(boldFont)
                        .setFontSize(10));
                document.add(new Paragraph(result.getPerhitunganSi())
                        .setFont(font)
                        .setFontSize(9));

                // Perhitungan Ri
                document.add(new Paragraph("4. PERHITUNGAN Ri")
                        .setFont(boldFont)
                        .setFontSize(10));
                document.add(new Paragraph(result.getPerhitunganRi())
                        .setFont(font)
                        .setFontSize(9));

                // Perhitungan Qi
                document.add(new Paragraph("5. PERHITUNGAN Qi")
                        .setFont(boldFont)
                        .setFontSize(10));
                document.add(new Paragraph(result.getPerhitunganQi())
                        .setFont(font)
                        .setFontSize(9));

                document.add(new Paragraph("\n"));
            }

            document.close();
            Toast.makeText(this, "PDF berhasil disimpan di lokasi yang dipilih", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e("ExportPDF", "Error: " + e.getMessage());
            Toast.makeText(this, "Error saat membuat PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportToExcelWithUri(Uri uri) {
        try {
            // Buka output stream dari URI
            getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            Workbook workbook = new XSSFWorkbook();

            // Sheet 1: Hasil Ranking
            Sheet rankingSheet = workbook.createSheet("Hasil Ranking");
            createRankingSheet(rankingSheet);

            // Sheet 2: Detail Perhitungan
            Sheet detailSheet = workbook.createSheet("Detail Perhitungan");
            createDetailSheet(detailSheet);

            // Sheet 3: Matriks Keputusan
            Sheet matrixSheet = workbook.createSheet("Matriks Keputusan");
            createMatrixSheet(matrixSheet);

            // Simpan file ke URI yang dipilih user
            FileOutputStream fileOut = (FileOutputStream) getContentResolver().openOutputStream(uri);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();

            Toast.makeText(this, "Excel berhasil disimpan di lokasi yang dipilih", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e("ExportExcel", "Error: " + e.getMessage());
            Toast.makeText(this, "Error saat membuat Excel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportToPDFLegacy() {
        // Gunakan kode exportToPDF() yang sudah ada sebelumnya
        exportToPDF();
    }

    private void exportToExcelLegacy() {
        // Gunakan kode exportToExcel() yang sudah ada sebelumnya
        exportToExcel();
    }

    private void exportToPDF() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "VIKOR_Report_" + timestamp + ".pdf";

            File pdfFile;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ gunakan app-specific directory
                File documentsDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "VIKOR_Reports");
                if (!documentsDir.exists()) {
                    documentsDir.mkdirs();
                }
                pdfFile = new File(documentsDir, fileName);
            } else {
                // Android 9 dan sebelumnya
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                pdfFile = new File(downloadsDir, fileName);
            }

            // Sisa kode PDF tetap sama...
            PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Font
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            // Title
            Paragraph title = new Paragraph("LAPORAN PERHITUNGAN VIKOR")
                    .setFont(boldFont)
                    .setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);

            document.add(new Paragraph("Tanggal Export: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()))
                    .setFont(font)
                    .setFontSize(10));

            document.add(new Paragraph("\n"));

            // Tabel Hasil Ranking
            document.add(new Paragraph("HASIL RANKING VIKOR")
                    .setFont(boldFont)
                    .setFontSize(14));

            Table rankingTable = new Table(6);
            rankingTable.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Ranking").setFont(boldFont)));
            rankingTable.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Alternatif").setFont(boldFont)));
            rankingTable.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Nama Ban").setFont(boldFont)));
            rankingTable.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Si").setFont(boldFont)));
            rankingTable.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Ri").setFont(boldFont)));
            rankingTable.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Qi").setFont(boldFont)));

            for (VikorResultModel result : currentVikorResults) {
                rankingTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.valueOf(result.getRanking())).setFont(font)));
                rankingTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(result.getAlternatif()).setFont(font)));
                rankingTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(result.getNamaBan()).setFont(font)));
                rankingTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(df.format(result.getSiValue())).setFont(font)));
                rankingTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(df.format(result.getRiValue())).setFont(font)));
                rankingTable.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(df.format(result.getQiValue())).setFont(font)));
            }

            document.add(rankingTable);
            document.add(new Paragraph("\n"));

            // Detail Perhitungan untuk setiap alternatif
            document.add(new Paragraph("DETAIL PERHITUNGAN VIKOR")
                    .setFont(boldFont)
                    .setFontSize(14));

            for (VikorResultModel result : currentVikorResults) {
                document.add(new Paragraph("\n=== " + result.getNamaBan() + " ===")
                        .setFont(boldFont)
                        .setFontSize(12));

                // Matriks Keputusan
                document.add(new Paragraph("1. MATRIKS KEPUTUSAN")
                        .setFont(boldFont)
                        .setFontSize(10));
                document.add(new Paragraph(result.getMatriksKeputusan())
                        .setFont(font)
                        .setFontSize(9));

                // Normalisasi
                document.add(new Paragraph("2. NORMALISASI")
                        .setFont(boldFont)
                        .setFontSize(10));
                document.add(new Paragraph(result.getNormalisasiDetail())
                        .setFont(font)
                        .setFontSize(9));

                // Perhitungan Si
                document.add(new Paragraph("3. PERHITUNGAN Si")
                        .setFont(boldFont)
                        .setFontSize(10));
                document.add(new Paragraph(result.getPerhitunganSi())
                        .setFont(font)
                        .setFontSize(9));

                // Perhitungan Ri
                document.add(new Paragraph("4. PERHITUNGAN Ri")
                        .setFont(boldFont)
                        .setFontSize(10));
                document.add(new Paragraph(result.getPerhitunganRi())
                        .setFont(font)
                        .setFontSize(9));

                // Perhitungan Qi
                document.add(new Paragraph("5. PERHITUNGAN Qi")
                        .setFont(boldFont)
                        .setFontSize(10));
                document.add(new Paragraph(result.getPerhitunganQi())
                        .setFont(font)
                        .setFontSize(9));

                document.add(new Paragraph("\n"));
            }

            document.close();

            Toast.makeText(this, "PDF berhasil disimpan di: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e("ExportPDF", "Error: " + e.getMessage());
            Toast.makeText(this, "Error saat membuat PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportToExcel() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "VIKOR_Report_" + timestamp + ".xlsx";

            File excelFile;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ gunakan app-specific directory
                File documentsDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "VIKOR_Reports");
                if (!documentsDir.exists()) {
                    documentsDir.mkdirs();
                }
                excelFile = new File(documentsDir, fileName);
            } else {
                // Android 9 dan sebelumnya
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                excelFile = new File(downloadsDir, fileName);
            }

            // Sisa kode Excel tetap sama...
            Workbook workbook = new XSSFWorkbook();

            // Sheet 1: Hasil Ranking
            Sheet rankingSheet = workbook.createSheet("Hasil Ranking");
            createRankingSheet(rankingSheet);

            // Sheet 2: Detail Perhitungan
            Sheet detailSheet = workbook.createSheet("Detail Perhitungan");
            createDetailSheet(detailSheet);

            // Sheet 3: Matriks Keputusan
            Sheet matrixSheet = workbook.createSheet("Matriks Keputusan");
            createMatrixSheet(matrixSheet);

            // Simpan file
            FileOutputStream fileOut = new FileOutputStream(excelFile);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();

            Toast.makeText(this, "Excel berhasil disimpan di: " + excelFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e("ExportExcel", "Error: " + e.getMessage());
            Toast.makeText(this, "Error saat membuat Excel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void createRankingSheet(Sheet sheet) {
        // Header
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Ranking", "Alternatif", "Nama Ban", "Si", "Ri", "Qi"};

        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data
        int rowNum = 1;
        for (VikorResultModel result : currentVikorResults) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(result.getRanking());
            row.createCell(1).setCellValue(result.getAlternatif());
            row.createCell(2).setCellValue(result.getNamaBan());
            row.createCell(3).setCellValue(result.getSiValue());
            row.createCell(4).setCellValue(result.getRiValue());
            row.createCell(5).setCellValue(result.getQiValue());
        }

        // Manual column width setting instead of autoSizeColumn
        // Set column widths manually (in units of 1/256th of a character width)
        sheet.setColumnWidth(0, 2560);  // Ranking
        sheet.setColumnWidth(1, 3840);  // Alternatif
        sheet.setColumnWidth(2, 5120);  // Nama Ban
        sheet.setColumnWidth(3, 2560);  // Si
        sheet.setColumnWidth(4, 2560);  // Ri
        sheet.setColumnWidth(5, 2560);  // Qi
    }

    private void createDetailSheet(Sheet sheet) {
        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("DETAIL PERHITUNGAN VIKOR");

        CellStyle titleStyle = sheet.getWorkbook().createCellStyle();
        Font titleFont = sheet.getWorkbook().createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        rowNum++;

        for (VikorResultModel result : currentVikorResults) {
            // Alternatif name
            Row nameRow = sheet.createRow(rowNum++);
            Cell nameCell = nameRow.createCell(0);
            nameCell.setCellValue("=== " + result.getNamaBan() + " ===");

            CellStyle nameStyle = sheet.getWorkbook().createCellStyle();
            Font nameFont = sheet.getWorkbook().createFont();
            nameFont.setBold(true);
            nameStyle.setFont(nameFont);
            nameCell.setCellStyle(nameStyle);

            // Matriks Keputusan
            Row matrixHeaderRow = sheet.createRow(rowNum++);
            matrixHeaderRow.createCell(0).setCellValue("1. MATRIKS KEPUTUSAN");

            String[] matrixLines = result.getMatriksKeputusan().split("\n");
            for (String line : matrixLines) {
                if (!line.trim().isEmpty()) {
                    Row lineRow = sheet.createRow(rowNum++);
                    lineRow.createCell(0).setCellValue(line);
                }
            }

            // Normalisasi
            Row normHeaderRow = sheet.createRow(rowNum++);
            normHeaderRow.createCell(0).setCellValue("2. NORMALISASI");

            String[] normLines = result.getNormalisasiDetail().split("\n");
            for (String line : normLines) {
                if (!line.trim().isEmpty()) {
                    Row lineRow = sheet.createRow(rowNum++);
                    lineRow.createCell(0).setCellValue(line);
                }
            }

            // Perhitungan Si
            Row siHeaderRow = sheet.createRow(rowNum++);
            siHeaderRow.createCell(0).setCellValue("3. PERHITUNGAN Si");

            String[] siLines = result.getPerhitunganSi().split("\n");
            for (String line : siLines) {
                if (!line.trim().isEmpty()) {
                    Row lineRow = sheet.createRow(rowNum++);
                    lineRow.createCell(0).setCellValue(line);
                }
            }

            // Perhitungan Ri
            Row riHeaderRow = sheet.createRow(rowNum++);
            riHeaderRow.createCell(0).setCellValue("4. PERHITUNGAN Ri");

            String[] riLines = result.getPerhitunganRi().split("\n");
            for (String line : riLines) {
                if (!line.trim().isEmpty()) {
                    Row lineRow = sheet.createRow(rowNum++);
                    lineRow.createCell(0).setCellValue(line);
                }
            }

            // Perhitungan Qi
            Row qiHeaderRow = sheet.createRow(rowNum++);
            qiHeaderRow.createCell(0).setCellValue("5. PERHITUNGAN Qi");

            String[] qiLines = result.getPerhitunganQi().split("\n");
            for (String line : qiLines) {
                if (!line.trim().isEmpty()) {
                    Row lineRow = sheet.createRow(rowNum++);
                    lineRow.createCell(0).setCellValue(line);
                }
            }

            rowNum++; // Empty row between alternatives
        }

        // Set column width manually instead of autoSizeColumn
        sheet.setColumnWidth(0, 15360); // Wider for detail text
    }

    private void createMatrixSheet(Sheet sheet) {
        // Header
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Alternatif");

        // Add criteria headers
        int colNum = 1;
        for (KriteriaModel kriteria : kriteriaList) {
            Cell cell = headerRow.createCell(colNum++);
            cell.setCellValue(kriteria.getNama_kriteria());
        }

        // Data dari hasil perhitungan
        int rowNum = 1;
        for (VikorResultModel result : currentVikorResults) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(result.getAlternatif());

            // Untuk setiap kriteria, ambil nilai dari matriks keputusan
            // Ini memerlukan parsing dari string detail perhitungan
            // Atau lebih baik, simpan matriks keputusan sebagai data terpisah
        }

        // Set column widths manually instead of autoSizeColumn
        sheet.setColumnWidth(0, 4096); // Alternatif column
        for (int i = 1; i <= kriteriaList.size(); i++) {
            sheet.setColumnWidth(i, 3072); // Criteria columns
        }
    }
}