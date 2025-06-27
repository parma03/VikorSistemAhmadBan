package com.example.vikorsistemahmadban.model;

public class VikorResultModel {
    private String namaBan;
    private String alternatif;
    private double siValue;
    private double riValue;
    private double qiValue;
    private int ranking;
    private String matriksKeputusan;
    private String normalisasiDetail;
    private String perhitunganSi;
    private String perhitunganRi;
    private String perhitunganQi;

    public VikorResultModel() {}

    public VikorResultModel(String namaBan, String alternatif, double siValue, double riValue,
                            double qiValue, int ranking) {
        this.namaBan = namaBan;
        this.alternatif = alternatif;
        this.siValue = siValue;
        this.riValue = riValue;
        this.qiValue = qiValue;
        this.ranking = ranking;
    }

    // Getters and Setters
    public String getNamaBan() {
        return namaBan;
    }

    public void setNamaBan(String namaBan) {
        this.namaBan = namaBan;
    }

    public String getAlternatif() {
        return alternatif;
    }

    public void setAlternatif(String alternatif) {
        this.alternatif = alternatif;
    }

    public double getSiValue() {
        return siValue;
    }

    public void setSiValue(double siValue) {
        this.siValue = siValue;
    }

    public double getRiValue() {
        return riValue;
    }

    public void setRiValue(double riValue) {
        this.riValue = riValue;
    }

    public double getQiValue() {
        return qiValue;
    }

    public void setQiValue(double qiValue) {
        this.qiValue = qiValue;
    }

    public int getRanking() {
        return ranking;
    }

    public void setRanking(int ranking) {
        this.ranking = ranking;
    }

    public String getMatriksKeputusan() {
        return matriksKeputusan;
    }

    public void setMatriksKeputusan(String matriksKeputusan) {
        this.matriksKeputusan = matriksKeputusan;
    }

    public String getNormalisasiDetail() {
        return normalisasiDetail;
    }

    public void setNormalisasiDetail(String normalisasiDetail) {
        this.normalisasiDetail = normalisasiDetail;
    }

    public String getPerhitunganSi() {
        return perhitunganSi;
    }

    public void setPerhitunganSi(String perhitunganSi) {
        this.perhitunganSi = perhitunganSi;
    }

    public String getPerhitunganRi() {
        return perhitunganRi;
    }

    public void setPerhitunganRi(String perhitunganRi) {
        this.perhitunganRi = perhitunganRi;
    }

    public String getPerhitunganQi() {
        return perhitunganQi;
    }

    public void setPerhitunganQi(String perhitunganQi) {
        this.perhitunganQi = perhitunganQi;
    }
}