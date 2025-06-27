package com.example.vikorsistemahmadban.activity.admin;

import android.os.AsyncTask;
import android.os.Bundle;
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

public class DataVikorActivity extends AppCompatActivity {
    private ActivityDataVikorBinding binding;
    private VikorAdapter vikorAdapter;
    private List<BanModel> banList = new ArrayList<>();
    private List<KriteriaModel> kriteriaList = new ArrayList<>();
    private List<SubKriteriaModel> subKriteriaList = new ArrayList<>();
    private List<ProsesModel> prosesList = new ArrayList<>();
    private JDBCConnection jdbcConnection;
    private DecimalFormat df = new DecimalFormat("#.####");

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
        setupRecyclerView();
        loadDataAndCalculateVikor();
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
                    KriteriaModel kriteria = new KriteriaModel(
                            rs.getString("id_kriteria"),
                            rs.getString("nama_kriteria"),
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

        // Kelompokkan data berdasarkan tanggal untuk memungkinkan perbandingan
        Map<String, List<ProsesModel>> prosesGroupedByDate = new HashMap<>();

        // Group proses by date
        for (ProsesModel proses : prosesList) {
            String date = proses.getCreated_at();
            if (!prosesGroupedByDate.containsKey(date)) {
                prosesGroupedByDate.put(date, new ArrayList<>());
            }
            prosesGroupedByDate.get(date).add(proses);
        }

        // Untuk setiap tanggal, buat perhitungan VIKOR terpisah
        for (String date : prosesGroupedByDate.keySet()) {
            List<ProsesModel> prosesForDate = prosesGroupedByDate.get(date);

            // Dapatkan semua ban yang memiliki data pada tanggal ini
            Set<String> banIdsForDate = new HashSet<>();
            for (ProsesModel proses : prosesForDate) {
                banIdsForDate.add(proses.getId_ban());
            }

            // Hitung VIKOR untuk tanggal ini
            List<VikorResultModel> resultsForDate = calculateVikorForDate(date, prosesForDate, banIdsForDate);
            vikorResults.addAll(resultsForDate);
        }

        // Jika ingin ranking global (semua tanggal digabung), uncomment baris ini:
         vikorResults.sort((a, b) -> Double.compare(a.getQiValue(), b.getQiValue()));
         for (int i = 0; i < vikorResults.size(); i++) {
             vikorResults.get(i).setRanking(i + 1);
         }

        // Update adapter
        vikorAdapter.setVikorList(vikorResults);
    }

    private List<VikorResultModel> calculateVikorForDate(String date, List<ProsesModel> prosesForDate, Set<String> banIdsForDate) {
        List<VikorResultModel> results = new ArrayList<>();

        // 1. Menyusun Matriks Keputusan untuk tanggal tertentu
        Map<String, Map<String, Double>> decisionMatrix = buildDecisionMatrixForDate(prosesForDate, banIdsForDate);

        // Jika tidak ada data yang lengkap, return empty
        if (decisionMatrix.isEmpty()) {
            return results;
        }

        // 2. Mencari nilai maksimum dan minimum untuk setiap kriteria
        Map<String, Double> maxValues = new HashMap<>();
        Map<String, Double> minValues = new HashMap<>();

        for (KriteriaModel kriteria : kriteriaList) {
            String kriteriaId = kriteria.getId_kriteria();
            double maxVal = Double.MIN_VALUE;
            double minVal = Double.MAX_VALUE;

            for (String banId : banIdsForDate) {
                if (decisionMatrix.containsKey(banId) && decisionMatrix.get(banId).containsKey(kriteriaId)) {
                    double value = decisionMatrix.get(banId).get(kriteriaId);
                    maxVal = Math.max(maxVal, value);
                    minVal = Math.min(minVal, value);
                }
            }

            // Hanya tambahkan jika ada data valid
            if (maxVal != Double.MIN_VALUE && minVal != Double.MAX_VALUE) {
                maxValues.put(kriteriaId, maxVal);
                minValues.put(kriteriaId, minVal);
            }
        }

        // 3. Normalisasi Rij
        Map<String, Map<String, Double>> normalizedMatrix = new HashMap<>();
        for (String banId : banIdsForDate) {
            if (decisionMatrix.containsKey(banId)) {
                normalizedMatrix.put(banId, new HashMap<>());

                for (KriteriaModel kriteria : kriteriaList) {
                    String kriteriaId = kriteria.getId_kriteria();
                    if (decisionMatrix.get(banId).containsKey(kriteriaId) &&
                            maxValues.containsKey(kriteriaId) && minValues.containsKey(kriteriaId)) {

                        double originalValue = decisionMatrix.get(banId).get(kriteriaId);
                        double maxVal = maxValues.get(kriteriaId);
                        double minVal = minValues.get(kriteriaId);

                        double normalized = 0.0;
                        if (maxVal != minVal) {
                            normalized = (maxVal - originalValue) / (maxVal - minVal);
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

                        si += bobot * normalizedValue;
                        ri = Math.max(ri, bobot * normalizedValue);
                    }
                }

                siValues.put(banId, si);
                riValues.put(banId, ri);
            }
        }

        // 5. Mencari S* dan S-, R* dan R-
        if (siValues.isEmpty() || riValues.isEmpty()) {
            return results;
        }

        double sPlus = Collections.min(siValues.values());
        double sMinus = Collections.max(siValues.values());
        double rPlus = Collections.min(riValues.values());
        double rMinus = Collections.max(riValues.values());

        // 6. Hitung Qi dengan v = 0.5
        double v = 0.5;

        for (String banId : banIdsForDate) {
            if (siValues.containsKey(banId) && riValues.containsKey(banId)) {
                double si = siValues.get(banId);
                double ri = riValues.get(banId);

                double qi = 0.0;
                if (sMinus != sPlus && rMinus != rPlus) {
                    qi = v * ((si - sPlus) / (sMinus - sPlus)) + (1 - v) * ((ri - rPlus) / (rMinus - rPlus));
                }

                // Find ban information
                BanModel ban = null;
                for (BanModel b : banList) {
                    if (b.getId_ban().equals(banId)) {
                        ban = b;
                        break;
                    }
                }

                if (ban != null) {
                    // Create result model with detailed calculations
                    VikorResultModel result = new VikorResultModel();
                    result.setNamaBan(ban.getNama_ban() + " (" + date + ")");
                    result.setAlternatif("A" + banId + "_" + date.replace("-", ""));
                    result.setSiValue(si);
                    result.setRiValue(ri);
                    result.setQiValue(qi);

                    // Generate detailed calculation strings
                    result.setMatriksKeputusan(generateMatriksKeputusanForDate(ban, decisionMatrix, date));
                    result.setNormalisasiDetail(generateNormalisasiDetailForDate(ban, normalizedMatrix, maxValues, minValues, date));
                    result.setPerhitunganSi(generatePerhitunganSiForDate(ban, normalizedMatrix, si, date));
                    result.setPerhitunganRi(generatePerhitunganRiForDate(ban, normalizedMatrix, ri, date));
                    result.setPerhitunganQi(generatePerhitunganQiForDate(si, ri, qi, sPlus, sMinus, rPlus, rMinus, v, date));

                    results.add(result);
                }
            }
        }

        // 7. Sorting berdasarkan Qi (ascending) untuk tanggal ini
        results.sort((a, b) -> Double.compare(a.getQiValue(), b.getQiValue()));

        // 8. Set ranking untuk tanggal ini
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRanking(i + 1);
        }

        return results;
    }

    private Map<String, Map<String, Double>> buildDecisionMatrixForDate(List<ProsesModel> prosesForDate, Set<String> banIdsForDate) {
        Map<String, Map<String, Double>> matrix = new HashMap<>();

        for (String banId : banIdsForDate) {
            matrix.put(banId, new HashMap<>());

            // Get values from proses table for this date
            for (ProsesModel proses : prosesForDate) {
                if (proses.getId_ban().equals(banId)) {
                    String kriteriaId = proses.getId_kriteria();
                    String subKriteriaId = proses.getId_subkriteria();

                    // Find subkriteria bobot
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

    // Updated helper methods to include date information
    private String generateMatriksKeputusanForDate(BanModel ban, Map<String, Map<String, Double>> matrix, String date) {
        StringBuilder sb = new StringBuilder();
        sb.append("Matriks Keputusan untuk ").append(ban.getNama_ban()).append(" (").append(date).append("):\n\n");

        Map<String, Double> banValues = matrix.get(ban.getId_ban());
        if (banValues != null) {
            for (KriteriaModel kriteria : kriteriaList) {
                String kriteriaId = kriteria.getId_kriteria();
                if (banValues.containsKey(kriteriaId)) {
                    sb.append(kriteria.getNama_kriteria()).append(": ")
                            .append(banValues.get(kriteriaId)).append("\n");
                }
            }
        }

        return sb.toString();
    }

    private String generateNormalisasiDetailForDate(BanModel ban, Map<String, Map<String, Double>> normalizedMatrix,
                                                    Map<String, Double> maxValues, Map<String, Double> minValues, String date) {
        StringBuilder sb = new StringBuilder();
        sb.append("Normalisasi Rij untuk ").append(ban.getNama_ban()).append(" (").append(date).append("):\n\n");

        Map<String, Double> banValues = normalizedMatrix.get(ban.getId_ban());
        if (banValues != null) {
            int index = 1;

            for (KriteriaModel kriteria : kriteriaList) {
                String kriteriaId = kriteria.getId_kriteria();
                if (banValues.containsKey(kriteriaId)) {
                    double maxVal = maxValues.get(kriteriaId);
                    double minVal = minValues.get(kriteriaId);
                    double normalized = banValues.get(kriteriaId);

                    sb.append("K").append(index).append(" = (").append(maxVal).append(" - ").append("nilai")
                            .append(") / (").append(maxVal).append(" - ").append(minVal).append(") = ")
                            .append(df.format(normalized)).append("\n");
                    index++;
                }
            }
        }

        return sb.toString();
    }

    private String generatePerhitunganSiForDate(BanModel ban, Map<String, Map<String, Double>> normalizedMatrix, double si, String date) {
        StringBuilder sb = new StringBuilder();
        sb.append("Perhitungan Si untuk ").append(ban.getNama_ban()).append(" (").append(date).append("):\n\n");
        sb.append("Si = ");

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

    private String generatePerhitunganRiForDate(BanModel ban, Map<String, Map<String, Double>> normalizedMatrix, double ri, String date) {
        StringBuilder sb = new StringBuilder();
        sb.append("Perhitungan Ri untuk ").append(ban.getNama_ban()).append(" (").append(date).append("):\n\n");
        sb.append("Ri = max{");

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

    private String generatePerhitunganQiForDate(double si, double ri, double qi, double sPlus, double sMinus,
                                                double rPlus, double rMinus, double v, String date) {
        StringBuilder sb = new StringBuilder();
        sb.append("Perhitungan Qi untuk tanggal ").append(date).append(" (v = ").append(v).append("):\n\n");
        sb.append("Qi = v × ((Si - S+) / (S- - S+)) + (1-v) × ((Ri - R+) / (R- - R+))\n\n");
        sb.append("Dengan:\n");
        sb.append("S+ = ").append(df.format(sPlus)).append("\n");
        sb.append("S- = ").append(df.format(sMinus)).append("\n");
        sb.append("R+ = ").append(df.format(rPlus)).append("\n");
        sb.append("R- = ").append(df.format(rMinus)).append("\n\n");
        sb.append("Qi = ").append(v).append(" × ((").append(df.format(si)).append(" - ").append(df.format(sPlus))
                .append(") / (").append(df.format(sMinus)).append(" - ").append(df.format(sPlus)).append(")) + ")
                .append(1-v).append(" × ((").append(df.format(ri)).append(" - ").append(df.format(rPlus))
                .append(") / (").append(df.format(rMinus)).append(" - ").append(df.format(rPlus)).append("))\n");
        sb.append("Qi = ").append(df.format(qi));

        return sb.toString();
    }
}