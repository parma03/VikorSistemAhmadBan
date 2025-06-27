package com.example.vikorsistemahmadban.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vikorsistemahmadban.R;
import com.example.vikorsistemahmadban.model.VikorResultModel;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

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

        holder.tvRanking.setText("Ranking " + vikor.getRanking());
        holder.tvNamaBan.setText(vikor.getNamaBan());
        holder.tvAlternatif.setText(vikor.getAlternatif());
        holder.tvSiValue.setText("Si = " + df.format(vikor.getSiValue()));
        holder.tvRiValue.setText("Ri = " + df.format(vikor.getRiValue()));
        holder.tvQiValue.setText("Qi = " + df.format(vikor.getQiValue()));

        // Set background color based on ranking
        if (vikor.getRanking() == 1) {
            holder.itemView.setBackgroundResource(R.drawable.bg_ranking_gold);
        } else if (vikor.getRanking() == 2) {
            holder.itemView.setBackgroundResource(R.drawable.bg_ranking_silver);
        } else if (vikor.getRanking() == 3) {
            holder.itemView.setBackgroundResource(R.drawable.bg_ranking_bronze);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_ranking_default);
        }

        // Click listener to show calculation details
        holder.itemView.setOnClickListener(v -> showCalculationDetails(vikor));
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
        TextView tvRanking, tvNamaBan, tvAlternatif, tvSiValue, tvRiValue, tvQiValue;

        public VikorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRanking = itemView.findViewById(R.id.tvRanking);
            tvNamaBan = itemView.findViewById(R.id.tvNamaBan);
            tvAlternatif = itemView.findViewById(R.id.tvAlternatif);
            tvSiValue = itemView.findViewById(R.id.tvSiValue);
            tvRiValue = itemView.findViewById(R.id.tvRiValue);
            tvQiValue = itemView.findViewById(R.id.tvQiValue);
        }
    }
}
