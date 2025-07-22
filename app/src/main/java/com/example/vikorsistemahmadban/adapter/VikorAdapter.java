package com.example.vikorsistemahmadban.adapter;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vikorsistemahmadban.R;
import com.example.vikorsistemahmadban.model.VikorResultModel;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VikorAdapter extends RecyclerView.Adapter<VikorAdapter.VikorViewHolder> {

    private Context context;
    private List<VikorResultModel> vikorList;
    private DecimalFormat df = new DecimalFormat("#.####");

    public VikorAdapter(Context context) {
        this.context = context;
        this.vikorList = new ArrayList<>();
    }

    public void setVikorList(List<VikorResultModel> vikorList) {
        this.vikorList = vikorList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VikorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_vikor_result, parent, false);
        return new VikorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VikorViewHolder holder, int position) {
        VikorResultModel vikor = vikorList.get(position);

        // Set ranking text with different styling
        holder.tvRanking.setText("Ranking " + vikor.getRanking());
        holder.tvNamaBan.setText(vikor.getNamaBan());
        holder.tvTipeBan.setText(vikor.getTipe_ban());
        holder.tvTanggal.setText(vikor.getTanggal());

        // Format price to Indonesian Rupiah
        String hargaFormatted = formatToRupiah(vikor.getHargaBan());
        holder.tvHarga.setText(hargaFormatted);

        holder.tvAlternatif.setText(vikor.getAlternatif());
        holder.tvSiValue.setText("Si = " + df.format(vikor.getSiValue()));
        holder.tvRiValue.setText("Ri = " + df.format(vikor.getRiValue()));
        holder.tvQiValue.setText("Qi = " + df.format(vikor.getQiValue()));

        // Set background and trophy icon based on ranking
        setupRankingVisuals(holder, vikor.getRanking());

        // Add entrance animation
        animateItemEntry(holder.itemView, position);

        // Click listener with animation
        holder.itemView.setOnClickListener(v -> {
            animateClick(v);
            showCalculationDetails(vikor);
        });
    }

    private String formatToRupiah(String hargaString) {
        try {
            // Parse string to long
            long harga = Long.parseLong(hargaString);

            // Create NumberFormat for Indonesian locale
            NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

            // Format the number
            return rupiahFormat.format(harga);

        } catch (NumberFormatException e) {
            // If parsing fails, return original string with Rp prefix
            return "Rp " + hargaString;
        }
    }

    private void setupRankingVisuals(VikorViewHolder holder, int ranking) {
        // Set background based on ranking
        int backgroundRes;
        int trophyVisibility = View.VISIBLE;

        switch (ranking) {
            case 1:
                backgroundRes = R.drawable.bg_ranking_gold;
                holder.tvRanking.setBackgroundColor(context.getResources().getColor(R.color.gold));
                holder.tvRanking.setTextColor(context.getResources().getColor(R.color.black));
                break;
            case 2:
                backgroundRes = R.drawable.bg_ranking_silver;
                holder.tvRanking.setBackgroundColor(context.getResources().getColor(R.color.silver));
                holder.tvRanking.setTextColor(context.getResources().getColor(R.color.black));
                break;
            case 3:
                backgroundRes = R.drawable.bg_ranking_bronze;
                holder.tvRanking.setBackgroundColor(context.getResources().getColor(R.color.bronze));
                holder.tvRanking.setTextColor(context.getResources().getColor(R.color.white));
                break;
            default:
                backgroundRes = R.drawable.bg_ranking_default;
                holder.tvRanking.setBackgroundColor(context.getResources().getColor(com.google.android.material.R.color.cardview_dark_background));
                holder.tvRanking.setTextColor(context.getResources().getColor(R.color.white));
                trophyVisibility = View.GONE;
                break;
        }

        holder.itemView.setBackgroundResource(backgroundRes);

        // Show/hide trophy icon if you have one in your layout
        if (holder.ivTrophy != null) {
            holder.ivTrophy.setVisibility(trophyVisibility);
        }
    }

    private void animateItemEntry(View view, int position) {
        // Slide in from right with fade in
        view.setTranslationX(300f);
        view.setAlpha(0f);

        ObjectAnimator slideAnimator = ObjectAnimator.ofFloat(view, "translationX", 300f, 0f);
        ObjectAnimator fadeAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);

        slideAnimator.setDuration(300);
        fadeAnimator.setDuration(300);

        slideAnimator.setStartDelay(position * 50L); // Staggered animation
        fadeAnimator.setStartDelay(position * 50L);

        slideAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        fadeAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        slideAnimator.start();
        fadeAnimator.start();
    }

    private void animateClick(View view) {
        // Scale animation on click
        ObjectAnimator scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 0.95f);
        ObjectAnimator scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1.0f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 0.95f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1.0f);

        scaleDown.setDuration(100);
        scaleUp.setDuration(100);
        scaleDownY.setDuration(100);
        scaleUpY.setDuration(100);

        scaleDown.start();
        scaleDownY.start();

        scaleDown.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                scaleUp.start();
                scaleUpY.start();
            }
        });
    }

    @Override
    public int getItemCount() {
        return vikorList.size();
    }

    private void showCalculationDetails(VikorResultModel vikor) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_calculation_details, null);

        TextView tvMatriks = dialogView.findViewById(R.id.tvMatriksKeputusan);
        TextView tvNormalisasi = dialogView.findViewById(R.id.tvNormalisasi);
        TextView tvPerhitunganSi = dialogView.findViewById(R.id.tvPerhitunganSi);
        TextView tvPerhitunganRi = dialogView.findViewById(R.id.tvPerhitunganRi);
        TextView tvPerhitunganQi = dialogView.findViewById(R.id.tvPerhitunganQi);

        tvMatriks.setText(vikor.getMatriksKeputusan());
        tvNormalisasi.setText(vikor.getNormalisasiDetail());
        tvPerhitunganSi.setText(vikor.getPerhitunganSi());
        tvPerhitunganRi.setText(vikor.getPerhitunganRi());
        tvPerhitunganQi.setText(vikor.getPerhitunganQi());

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Perhitungan VIKOR - " + vikor.getAlternatif())
                .setView(dialogView)
                .setPositiveButton("Tutup", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static class VikorViewHolder extends RecyclerView.ViewHolder {
        TextView tvRanking, tvTanggal, tvHarga, tvNamaBan, tvTipeBan, tvAlternatif, tvSiValue, tvRiValue, tvQiValue;
        ImageView ivTrophy; // Optional trophy icon

        public VikorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRanking = itemView.findViewById(R.id.tvRanking);
            tvTanggal = itemView.findViewById(R.id.tvTanggal);
            tvHarga = itemView.findViewById(R.id.tvHarga);
            tvNamaBan = itemView.findViewById(R.id.tvNamaBan);
            tvTipeBan = itemView.findViewById(R.id.tvTipeBan);
            tvAlternatif = itemView.findViewById(R.id.tvAlternatif);
            tvSiValue = itemView.findViewById(R.id.tvSiValue);
            tvRiValue = itemView.findViewById(R.id.tvRiValue);
            tvQiValue = itemView.findViewById(R.id.tvQiValue);
        }
    }
}