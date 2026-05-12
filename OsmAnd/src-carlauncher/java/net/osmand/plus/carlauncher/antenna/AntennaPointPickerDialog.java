package net.osmand.plus.carlauncher.antenna;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import net.osmand.Location;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Anten noktasi secimi icin Bottom Sheet Dialog.
 * 4 secenk: GPS konumu, OsmAnd favorileri, haritadan sec, koordinat gir.
 */
public class AntennaPointPickerDialog extends BottomSheetDialogFragment {

    public static final String TAG = "AntennaPointPickerDialog";
    private static final String ARG_MODE = "pick_mode";
    private static final String ARG_IS_SOURCE = "is_source";

    private AntennaManager manager;
    private boolean isSource; // true=Kaynak, false=Hedef

    /** Harita secimi isteginde aranir (Activity kapatilmadan once) */
    public interface OnMapPickRequestListener {
        void onMapPickRequested(boolean isSource);
    }

    private OnMapPickRequestListener mapPickListener;

    // --- Factory ---

    public static AntennaPointPickerDialog forSource() {
        AntennaPointPickerDialog d = new AntennaPointPickerDialog();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_SOURCE, true);
        d.setArguments(args);
        return d;
    }

    public static AntennaPointPickerDialog forTarget() {
        AntennaPointPickerDialog d = new AntennaPointPickerDialog();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_SOURCE, false);
        d.setArguments(args);
        return d;
    }

    // --- Lifecycle ---

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        manager = AntennaManager.getInstance(context);
        if (context instanceof OnMapPickRequestListener) {
            mapPickListener = (OnMapPickRequestListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        isSource = getArguments() != null && getArguments().getBoolean(ARG_IS_SOURCE, true);

        View root = inflater.inflate(net.osmand.plus.R.layout.dialog_antenna_picker, container, false);

        // Baslik
        TextView tvTitle = root.findViewById(net.osmand.plus.R.id.tv_picker_title);
        tvTitle.setText(isSource ? "Kaynak Nokta Sec" : "Hedef Nokta Sec");

        // Kapat
        root.findViewById(net.osmand.plus.R.id.btn_picker_close)
                .setOnClickListener(v -> dismiss());

        // GPS secenegi: iki nokta da seciliyse gizle (swap butonu daha mantikli)
        View optionGps = root.findViewById(net.osmand.plus.R.id.option_gps);
        boolean bothPointsSet = manager.getSource() != null && manager.getTarget() != null;
        optionGps.setVisibility(bothPointsSet ? View.GONE : View.VISIBLE);
        optionGps.setOnClickListener(v -> onGpsSelected());

        // GPS konum bilgisi
        TextView tvGpsSub = root.findViewById(net.osmand.plus.R.id.tv_gps_subtitle);
        updateGpsSubtitle(tvGpsSub);

        // Favoriler
        root.findViewById(net.osmand.plus.R.id.option_favorites)
                .setOnClickListener(v -> onFavoritesSelected());

        // Haritadan sec
        root.findViewById(net.osmand.plus.R.id.option_map)
                .setOnClickListener(v -> onMapSelected());

        // Koordinat gir
        root.findViewById(net.osmand.plus.R.id.option_coordinate)
                .setOnClickListener(v -> onCoordinateSelected());

        // Temizle
        root.findViewById(net.osmand.plus.R.id.option_clear)
                .setOnClickListener(v -> onClearSelected());

        return root;
    }

    // --- GPS ---

    private void updateGpsSubtitle(TextView tv) {
        try {
            OsmandApplication app = (OsmandApplication) requireContext().getApplicationContext();
            Location loc = app.getLocationProvider().getLastKnownLocation();
            if (loc != null) {
                tv.setText(String.format(Locale.US, "%.5f, %.5f", loc.getLatitude(), loc.getLongitude()));
            } else {
                tv.setText("GPS sinyali bekleniyor...");
            }
        } catch (Exception e) {
            tv.setText("Anlik konumunuz");
        }
    }

    private void onGpsSelected() {
        try {
            OsmandApplication app = (OsmandApplication) requireContext().getApplicationContext();
            Location loc = app.getLocationProvider().getLastKnownLocation();
            if (loc != null) {
                LatLon latLon = new LatLon(loc.getLatitude(), loc.getLongitude());
                double altitude = loc.getAltitude();
                if (isSource) {
                    manager.setSource(latLon, "GPS Konumum", altitude);
                } else {
                    manager.setTarget(latLon, "GPS Konumum", altitude);
                }
                Toast.makeText(requireContext(),
                        (isSource ? "Kaynak" : "Hedef") + " GPS konumuna ayarlandi.", Toast.LENGTH_SHORT).show();
                dismiss();
            } else {
                Toast.makeText(requireContext(), "GPS sinyali yok. Bekleyiniz.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "GPS alinirken hata olustu.", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Favoriler ---

    private void onFavoritesSelected() {
        try {
            OsmandApplication app = (OsmandApplication) requireContext().getApplicationContext();
            FavouritesHelper favHelper = app.getFavoritesHelper();
            List<FavouritePoint> favPoints = favHelper.getFavouritePoints();

            if (favPoints == null || favPoints.isEmpty()) {
                Toast.makeText(requireContext(), "Kayitli favori bulunamadi.", Toast.LENGTH_SHORT).show();
                return;
            }

            showFavoritesList(new ArrayList<>(favPoints));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Favoriler yuklenirken hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showFavoritesList(List<FavouritePoint> points) {
        // AlertDialog icinde RecyclerView ile favori listesi goster
        View listView = LayoutInflater.from(requireContext())
                .inflate(android.R.layout.list_content, null, false);

        RecyclerView rv = new RecyclerView(requireContext());
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(new FavoriteAdapter(points, point -> {
            LatLon latLon = new LatLon(point.getLatitude(), point.getLongitude());
            double alt = point.getAltitude();
            if (isSource) {
                manager.setSource(latLon, point.getName(), alt);
            } else {
                manager.setTarget(latLon, point.getName(), alt);
            }
            Toast.makeText(requireContext(),
                    (isSource ? "Kaynak" : "Hedef") + ": " + point.getName(), Toast.LENGTH_SHORT).show();
            dismiss();
        }));

        new AlertDialog.Builder(requireContext())
                .setTitle("Favori Sec")
                .setView(rv)
                .setNegativeButton("Iptal", null)
                .show();
    }

    // --- Haritadan Sec ---

    private void onMapSelected() {
        manager.setPickingMode(isSource ? AntennaManager.PICK_SOURCE : AntennaManager.PICK_TARGET);
        manager.setLayerVisible(true);

        if (mapPickListener != null) {
            mapPickListener.onMapPickRequested(isSource);
        }

        Toast.makeText(requireContext(),
                "Haritaya tiklayin. " + (isSource ? "Kaynak" : "Hedef") + " nokta ayarlanacak.",
                Toast.LENGTH_LONG).show();
        dismiss();
    }

    // --- Koordinat Gir ---

    private void onCoordinateSelected() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(16);
        layout.setPadding(pad, pad, pad, pad);

        EditText etLat = new EditText(requireContext());
        etLat.setHint("Enlem (orn: 41.01234)");
        etLat.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);

        EditText etLon = new EditText(requireContext());
        etLon.setHint("Boylam (orn: 28.97654)");
        etLon.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);

        EditText etAlt = new EditText(requireContext());
        etAlt.setHint("Yukseklik m (opsiyonel, orn: 120)");
        etAlt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);

        layout.addView(etLat);
        layout.addView(etLon);
        layout.addView(etAlt);

        new AlertDialog.Builder(requireContext())
                .setTitle(isSource ? "Kaynak Koordinat" : "Hedef Koordinat")
                .setView(layout)
                .setPositiveButton("Tamam", (dialog, which) -> {
                    try {
                        double lat = Double.parseDouble(etLat.getText().toString().trim());
                        double lon = Double.parseDouble(etLon.getText().toString().trim());
                        String altStr = etAlt.getText().toString().trim();
                        double alt = altStr.isEmpty() ? 0 : Double.parseDouble(altStr);

                        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                            Toast.makeText(requireContext(), "Gecersiz koordinat.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        LatLon latLon = new LatLon(lat, lon);
                        String name = String.format(Locale.US, "%.4f, %.4f", lat, lon);
                        if (isSource) {
                            manager.setSource(latLon, name, alt);
                        } else {
                            manager.setTarget(latLon, name, alt);
                        }
                        Toast.makeText(requireContext(),
                                (isSource ? "Kaynak" : "Hedef") + " koordinat ayarlandi.", Toast.LENGTH_SHORT).show();
                        dismiss();
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "Gecersiz sayi formati.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Iptal", null)
                .show();
    }

    // --- Temizle ---

    private void onClearSelected() {
        if (isSource) {
            manager.setSource(new LatLon(0, 0), "", 0); // dummy sonra null kontrolu
            // Gercek temizleme icin manager'a clearSource eklenebilir, simdilik null ile simule
            manager.clearPoints(); // Ikisini de temizler — sonraki versiyonda ayri metod eklenecek
        } else {
            manager.clearPoints();
        }
        Toast.makeText(requireContext(), "Nokta temizlendi.", Toast.LENGTH_SHORT).show();
        dismiss();
    }

    // --- Yardimci ---

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    // --- Favori Adapter ---

    private static class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.VH> {

        interface OnFavClickListener {
            void onClick(FavouritePoint point);
        }

        private final List<FavouritePoint> items;
        private final OnFavClickListener listener;

        FavoriteAdapter(List<FavouritePoint> items, OnFavClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(net.osmand.plus.R.layout.item_antenna_favorite, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            FavouritePoint p = items.get(position);
            holder.tvName.setText(p.getName());
            holder.tvGroup.setText(p.getCategory() != null ? p.getCategory() : "Favoriler");
            holder.itemView.setOnClickListener(v -> listener.onClick(p));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvGroup;

            VH(@NonNull View v) {
                super(v);
                tvName = v.findViewById(net.osmand.plus.R.id.tv_fav_name);
                tvGroup = v.findViewById(net.osmand.plus.R.id.tv_fav_group);
            }
        }
    }
}
