package com.example.savr.mlayu.Model;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by SAVR on 05/09/2017.
 */

public class Lari {

    private String id;
    private String id_user;
    private long waktu; //dalam millisecond
    private double jarak;
    private double kalori;
    private String tanggal;
    private String jam;


    public Lari(String id_user, String id, long waktu, double jarak, double kalori, String tanggal,String jam) {
        this.id = id;
        this.id_user = id_user;
        this.waktu = waktu;
        this.jarak = jarak;
        this.kalori = kalori;
        this.tanggal = tanggal;
        this.jam = jam;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId_user() {
        return id_user;
    }

    public void setId_user(String id_user) {
        this.id_user = id_user;
    }

    public long getWaktu() {
        return waktu;
    }

    public void setWaktu(long waktu) {
        this.waktu = waktu;
    }

    public double getJarak() {
        return jarak;
    }

    public void setJarak(double jarak) {
        this.jarak = jarak;
    }

    public double getKalori() {
        return kalori;
    }

    public void setKalori(double kalori) {
        this.kalori = kalori;
    }

    public String getTanggal() {
        return tanggal;
    }

    public void setTanggal(String tanggal) {
        this.tanggal = tanggal;
    }

    public String getJam() {
        return jam;
    }

    public void setJam(String jam) {
        this.jam = jam;
    }

    public Lari() {
    }
}
