package com.example.vikorsistemahmadban.model;

public class BanModel {
    public String id_ban;
    public String nama_ban;
    public String harga;
    public String deskripsi;
    public String foto_ban;
    public String created_at;

    public BanModel(String id_ban, String nama_ban, String harga, String deskripsi, String foto_ban, String created_at) {
        this.id_ban = id_ban;
        this.nama_ban = nama_ban;
        this.harga = harga;
        this.deskripsi = deskripsi;
        this.foto_ban = foto_ban;
        this.created_at = created_at;
    }

    public String getId_ban() {
        return id_ban;
    }

    public void setId_ban(String id_ban) {
        this.id_ban = id_ban;
    }

    public String getNama_ban() {
        return nama_ban;
    }

    public void setNama_ban(String nama_ban) {
        this.nama_ban = nama_ban;
    }

    public String getHarga() {
        return harga;
    }

    public void setHarga(String harga) {
        this.harga = harga;
    }

    public String getDeskripsi() {
        return deskripsi;
    }

    public void setDeskripsi(String deskripsi) {
        this.deskripsi = deskripsi;
    }

    public String getFoto_ban() {
        return foto_ban;
    }

    public void setFoto_ban(String foto_ban) {
        this.foto_ban = foto_ban;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }
}
