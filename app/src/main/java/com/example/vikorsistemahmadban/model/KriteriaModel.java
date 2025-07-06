package com.example.vikorsistemahmadban.model;

public class KriteriaModel {
    private String id_kriteria;
    private String nama_kriteria;
    private String kategori;
    private String bobot;
    private String nilai;

    public KriteriaModel(String id_kriteria, String nama_kriteria, String kategori, String nilai, String bobot) {
        this.id_kriteria = id_kriteria;
        this.nama_kriteria = nama_kriteria;
        this.kategori = kategori;
        this.nilai = nilai;
        this.bobot = bobot;
    }

    public String getId_kriteria() {
        return id_kriteria;
    }

    public void setId_kriteria(String id_kriteria) {
        this.id_kriteria = id_kriteria;
    }

    public String getNama_kriteria() {
        return nama_kriteria;
    }

    public void setNama_kriteria(String nama_kriteria) {
        this.nama_kriteria = nama_kriteria;
    }

    public String getKategori() {
        return kategori;
    }

    public void setKategori(String kategori) {
        this.kategori = kategori;
    }

    public String getBobot() {
        return bobot;
    }

    public void setBobot(String bobot) {
        this.bobot = bobot;
    }

    public String getNilai() {
        return nilai;
    }

    public void setNilai(String nilai) {
        this.nilai = nilai;
    }

    // Method helper untuk mengecek apakah kriteria ini benefit atau cost
    public boolean isBenefit() {
        return "benefit".equalsIgnoreCase(this.kategori);
    }

    public boolean isCost() {
        return "cost".equalsIgnoreCase(this.kategori);
    }
}