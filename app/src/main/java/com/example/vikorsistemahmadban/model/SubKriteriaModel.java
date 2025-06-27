package com.example.vikorsistemahmadban.model;

public class SubKriteriaModel {
    private String id_subkriteria;
    private String id_kriteria;
    private String klasifikasi;
    private int bobot_sub_kriteria;

    // Constructor
    public SubKriteriaModel() {}

    public SubKriteriaModel(String id_subkriteria, String id_kriteria, String klasifikasi, int bobot_sub_kriteria) {
        this.id_subkriteria = id_subkriteria;
        this.id_kriteria = id_kriteria;
        this.klasifikasi = klasifikasi;
        this.bobot_sub_kriteria = bobot_sub_kriteria;
    }

    // Getters and Setters
    public String getId_subkriteria() {
        return id_subkriteria;
    }

    public void setId_subkriteria(String id_subkriteria) {
        this.id_subkriteria = id_subkriteria;
    }

    public String getId_kriteria() {
        return id_kriteria;
    }

    public void setId_kriteria(String id_kriteria) {
        this.id_kriteria = id_kriteria;
    }

    public String getKlasifikasi() {
        return klasifikasi;
    }

    public void setKlasifikasi(String klasifikasi) {
        this.klasifikasi = klasifikasi;
    }

    public int getBobot_sub_kriteria() {
        return bobot_sub_kriteria;
    }

    public void setBobot_sub_kriteria(int bobot_sub_kriteria) {
        this.bobot_sub_kriteria = bobot_sub_kriteria;
    }
}