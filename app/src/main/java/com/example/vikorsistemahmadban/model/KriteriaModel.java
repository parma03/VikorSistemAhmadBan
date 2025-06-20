package com.example.vikorsistemahmadban.model;

public class KriteriaModel {
    public String id_kriteria;
    public String nama_kriteria;
    public String bobot;

    public KriteriaModel(String id_kriteria, String nama_kriteria, String bobot) {
        this.id_kriteria = id_kriteria;
        this.nama_kriteria = nama_kriteria;
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

    public String getBobot() {
        return bobot;
    }

    public void setBobot(String bobot) {
        this.bobot = bobot;
    }
}
