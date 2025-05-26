package com.example.vikorsistemahmadban;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;

import com.example.vikorsistemahmadban.api.PrefManager;

public class LogoutActivity {

    public static void showLogoutConfirmation(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Konfirmasi Logout")
                .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Ya", (dialog, which) -> {
                    performLogout(context);
                })
                .setNegativeButton("Tidak", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static void performLogout(Context context) {
        // Clear semua data user dari SharedPreferences
        PrefManager prefManager = new PrefManager(context);
        prefManager.setId("");
        prefManager.setUsername("");
        prefManager.setPassword("");
        prefManager.setNama("");
        prefManager.setNohp("");
        prefManager.setImg("");
        prefManager.setTipe("");
        prefManager.setEmail("");
        prefManager.setLoginStatus(false);

        // Redirect ke halaman login
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }
}
