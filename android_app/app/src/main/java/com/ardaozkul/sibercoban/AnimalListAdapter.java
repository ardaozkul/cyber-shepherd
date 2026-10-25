package com.ardaozkul.sibercoban;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter - Hayvan listesini gösterir
 * Her hayvan için durum, konum ve son güncelleme bilgisi
 */
public class AnimalListAdapter extends RecyclerView.Adapter<AnimalListAdapter.AnimalViewHolder> {

    private List<AnimalData> animalList;
    private OnAnimalClickListener clickListener;

    public interface OnAnimalClickListener {
        void onAnimalClick(AnimalData animal);
    }

    public AnimalListAdapter() {
        this.animalList = new ArrayList<>();
    }

    public void setOnAnimalClickListener(OnAnimalClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public AnimalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_animal, parent, false);
        return new AnimalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnimalViewHolder holder, int position) {
        AnimalData animal = animalList.get(position);
        holder.bind(animal, clickListener);
    }

    @Override
    public int getItemCount() {
        return animalList.size();
    }

    /**
     * Listeyi güncelle
     */
    public void updateList(List<AnimalData> newList) {
        this.animalList.clear();
        this.animalList.addAll(newList);
        notifyDataSetChanged();
    }

    /**
     * Tek bir hayvan ekle veya güncelle
     */
    public void addOrUpdateAnimal(AnimalData animal) {
        int existingIndex = -1;
        for (int i = 0; i < animalList.size(); i++) {
            if (animalList.get(i).getDeviceId().equals(animal.getDeviceId())) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex >= 0) {
            // Güncelle
            animalList.set(existingIndex, animal);
            notifyItemChanged(existingIndex);
        } else {
            // Yeni ekle
            animalList.add(animal);
            notifyItemInserted(animalList.size() - 1);
        }
    }

    /**
     * Tüm listeyi temizle
     */
    public void clearAll() {
        animalList.clear();
        notifyDataSetChanged();
    }

    static class AnimalViewHolder extends RecyclerView.ViewHolder {
        private View statusIndicator;
        private TextView txtDeviceId;
        private TextView txtBarnId;
        private TextView txtStatus;
        private TextView txtLastUpdate;
        private TextView txtLocation;

        public AnimalViewHolder(@NonNull View itemView) {
            super(itemView);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
            txtDeviceId = itemView.findViewById(R.id.txtDeviceId);
            txtBarnId = itemView.findViewById(R.id.txtBarnId);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtLastUpdate = itemView.findViewById(R.id.txtLastUpdate);
            txtLocation = itemView.findViewById(R.id.txtLocation);
        }

        public void bind(AnimalData animal, OnAnimalClickListener listener) {
            txtDeviceId.setText(animal.getDeviceId());
            txtBarnId.setText("Ahır: " + animal.getBarnId());
            txtLastUpdate.setText(animal.getFormattedLastUpdate());
            txtLocation.setText(String.format("%.5f, %.5f", 
                animal.getLatitude(), animal.getLongitude()));

            // Durum rengini ve metnini ayarla
            int statusColor;
            String statusText;

            switch (animal.getStatus()) {
                case AnimalData.STATUS_INSIDE:
                    statusColor = Color.parseColor("#4CAF50"); // Yeşil
                    statusText = "Sınır içi ✓";
                    break;
                case AnimalData.STATUS_OUTSIDE:
                    statusColor = Color.parseColor("#F44336"); // Kırmızı
                    statusText = "Sınır dışı ⚠";
                    break;
                case AnimalData.STATUS_WARNING:
                    statusColor = Color.parseColor("#FF9800"); // Turuncu
                    statusText = "Uyarı ⚡";
                    break;
                default:
                    statusColor = Color.parseColor("#999999"); // Gri
                    statusText = "Bilinmiyor";
                    break;
            }

            // Durum göstergesini renklendir
            GradientDrawable drawable = (GradientDrawable) statusIndicator.getBackground();
            if (drawable != null) {
                drawable.setColor(statusColor);
            }

            txtStatus.setText("Durum: " + statusText);
            txtStatus.setTextColor(statusColor);

            // Veri eskimişse solgun göster
            if (animal.isStale()) {
                itemView.setAlpha(0.6f);
                txtStatus.setText("Durum: Sinyal yok (" + animal.getSecondsSinceUpdate() + "s)");
            } else {
                itemView.setAlpha(1.0f);
            }

            // Tıklama eventi
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAnimalClick(animal);
                }
            });
        }
    }
}
