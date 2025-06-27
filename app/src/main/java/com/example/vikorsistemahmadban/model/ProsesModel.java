package com.example.vikorsistemahmadban.model;

public class ProsesModel {
    private String id_proses;
    private String id_ban;
    private String id_kriteria;
    private String id_subkriteria;
    private String created_at;

    public ProsesModel(String id_proses, String id_ban, String id_kriteria, String id_subkriteria, String created_at) {
        this.id_proses = id_proses;
        this.id_ban = id_ban;
        this.id_kriteria = id_kriteria;
        this.id_subkriteria = id_subkriteria;
        this.created_at = created_at;
    }

    public String getId_proses() {
        return id_proses;
    }

    public void setId_proses(String id_proses) {
        this.id_proses = id_proses;
    }

    public String getId_ban() {
        return id_ban;
    }

    public void setId_ban(String id_ban) {
        this.id_ban = id_ban;
    }

    public String getId_kriteria() {
        return id_kriteria;
    }

    public void setId_kriteria(String id_kriteria) {
        this.id_kriteria = id_kriteria;
    }

    public String getId_subkriteria() {
        return id_subkriteria;
    }

    public void setId_subkriteria(String id_subkriteria) {
        this.id_subkriteria = id_subkriteria;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }
}
