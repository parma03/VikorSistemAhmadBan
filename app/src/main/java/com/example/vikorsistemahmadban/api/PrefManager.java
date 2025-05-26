package com.example.vikorsistemahmadban.api;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context context;

    // shared pref mode
    int PRIVATE_MODE = 0;

    // Shared preferences file name
    private static final String PREF_NAME = "data_app";

    private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";

    public PrefManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {
        editor.putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime);
        editor.commit();
    }

    public boolean isFirstTimeLaunch() {
        return pref.getBoolean(IS_FIRST_TIME_LAUNCH, true);
    }

    public void setId(String id){
        editor.putString("id", id);
        editor.apply();
    }
    public String getId(){
        return pref.getString("id", "");
    }

    public void setUsername(String username){
        editor.putString("username", username);
        editor.apply();
    }
    public String getUsername(){
        return pref.getString("username", "");
    }

    public void setPassword(String password){
        editor.putString("password", password);
        editor.apply();
    }
    public String getPassword(){
        return pref.getString("password", "");
    }

    public void setNama(String nama){
        editor.putString("nama", nama);
        editor.apply();
    }
    public String getNama(){
        return pref.getString("nama", "");
    }

    public void setNohp(String nohp){
        editor.putString("nohp", nohp);
        editor.apply();
    }
    public String getNohp(){
        return pref.getString("nohp", "");
    }

    public void setImg(String img){
        editor.putString("img", img);
        editor.apply();
    }
    public String getImg(){
        return pref.getString("img", "");
    }

    public void setTipe(String tipe){
        editor.putString("tipe", tipe);
        editor.apply();
    }
    public String getTipe(){
        return pref.getString("tipe", "");
    }

    public void setEmail(String email){
        editor.putString("email", email);
        editor.apply();
    }
    public String getEmail(){
        return pref.getString("email", "");
    }

    public void setLoginStatus(boolean islogin){
        editor.putBoolean("login", islogin);
        editor.apply();
    }

    public boolean getLoginStatus(){
        return pref.getBoolean("login", false);
    }
}