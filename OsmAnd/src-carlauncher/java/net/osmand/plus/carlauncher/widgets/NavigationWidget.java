package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.NextDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.router.TurnType;

/**
 * Navigasyon widget - Sonraki donus ve rota bilgisi.
 * RoutingHelper kullanarak navigasyon bilgilerini gosterir.
 */
public class NavigationWidget extends BaseWidget {

    private ImageView turnIconView;
    private TextView distanceText;
    private TextView instructionText;
    private TextView etaText;

    private final OsmandApplication app;
    private final RoutingHelper routingHelper;

    private Runnable updateRunnable;

    public NavigationWidget(@NonNull Context context, @NonNull OsmandApplication app) {
        super(context, "navigation", "Navigasyon");
        this.app = app;
        this.routingHelper = app.getRoutingHelper();
        this.order = 3; // Hiz/yon'den sonra
    }

    @NonNull
    @Override
    public View createView() {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(16, 16, 16, 16);
        container.setBackgroundColor(Color.TRANSPARENT);

        // Label
        TextView label = new TextView(context);
        label.setText("🎯 NAVIGASYON");
        label.setTextColor(context.getResources().getColor(net.osmand.plus.R.color.cyber_text_secondary));
        label.setTextSize(12);
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, 0, 0, 8);
        container.addView(label);

        // Sonraki donus mesafesi
        distanceText = new TextView(context);
        distanceText.setTextColor(context.getResources().getColor(net.osmand.plus.R.color.cyber_text_primary));
        distanceText.setTextSize(32);
        distanceText.setGravity(Gravity.CENTER);
        distanceText.setText("--");
        container.addView(distanceText);

        // Donus ikonu
        turnIconView = new ImageView(context);
        turnIconView.setLayoutParams(new LinearLayout.LayoutParams(96, 96));
        turnIconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        turnIconView.setColorFilter(context.getResources().getColor(net.osmand.plus.R.color.cyber_neon_blue)); // Neon
                                                                                                               // mavi
                                                                                                               // ikon
        turnIconView.setPadding(0, 8, 0, 8);
        container.addView(turnIconView);

        // Donus talimati
        instructionText = new TextView(context);
        instructionText.setTextColor(context.getResources().getColor(net.osmand.plus.R.color.cyber_text_primary));
        instructionText.setTextSize(16);
        instructionText.setGravity(Gravity.CENTER);
        instructionText.setText("");
        instructionText.setMaxLines(2);
        instructionText.setPadding(0, 0, 0, 12);
        container.addView(instructionText);

        // Kalan sure ve mesafe
        etaText = new TextView(context);
        etaText.setTextColor(context.getResources().getColor(net.osmand.plus.R.color.cyber_text_tertiary));
        etaText.setTextSize(12);
        etaText.setGravity(Gravity.CENTER);
        etaText.setText("");
        container.addView(etaText);

        rootView = container;
        update();

        return rootView;
    }

    @Override
    public void update() {
        if (routingHelper == null)
            return;

        // Navigasyon aktif mi?
        if (routingHelper.isFollowingMode() && routingHelper.isRouteCalculated()) {
            updateNavigationInfo();
        } else {
            showNoNavigation();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        startUpdating();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopUpdating();
    }

    /**
     * Navigasyon bilgilerini guncelle.
     */
    private void updateNavigationInfo() {
        try {
            // Sonraki donus bilgisi
            NextDirectionInfo nextDirection = routingHelper.getNextRouteDirectionInfo(new NextDirectionInfo(), true);

            if (nextDirection != null && nextDirection.distanceTo > 0) {
                // Mesafe
                int distance = nextDirection.distanceTo;
                String distanceStr = OsmAndFormatter.getFormattedDistance(distance, app);

                if (distanceText != null) {
                    distanceText.post(() -> distanceText.setText(distanceStr));
                }

                // Donus tipi ve ikonu
                if (nextDirection.directionInfo != null) {
                    TurnType turnType = nextDirection.directionInfo.getTurnType();

                    if (turnType != null) {
                        updateTurnIcon(turnType);
                        updateInstruction(turnType, nextDirection.directionInfo.getStreetName());
                    }
                }
            } else {
                showNoNextTurn();
            }

            // Toplam kalan sure ve mesafe
            updateETA();

        } catch (Exception e) {
            e.printStackTrace();
            showNoNavigation();
        }
    }

    /**
     * Donus ikonunu guncelle.
     */
    private void updateTurnIcon(TurnType turnType) {
        if (turnIconView == null || turnType == null)
            return;

        int iconRes = getTurnIcon(turnType);

        if (iconRes != 0) {
            turnIconView.post(() -> {
                turnIconView.setImageResource(iconRes);
                turnIconView.setVisibility(View.VISIBLE);
            });
        } else {
            turnIconView.post(() -> turnIconView.setVisibility(View.GONE));
        }
    }

    /**
     * Donus ikonunu al.
     */
    private int getTurnIcon(TurnType turnType) {
        if (turnType.isRoundAbout()) {
            return android.R.drawable.ic_menu_rotate;
        }

        switch (turnType.getValue()) {
            case TurnType.C: // Duz git
                return android.R.drawable.arrow_up_float;
            case TurnType.TL: // Sola don
            case TurnType.TSLL: // Keskin sola
                return android.R.drawable.ic_menu_revert;
            case TurnType.TR: // Saga don
            case TurnType.TSLR: // Keskin saga
                return android.R.drawable.ic_menu_always_landscape_portrait;
            case TurnType.TU: // U donus
                return android.R.drawable.ic_menu_rotate;
            case TurnType.KL: // Sola devam
                return android.R.drawable.ic_menu_revert;
            case TurnType.KR: // Saga devam
                return android.R.drawable.ic_menu_always_landscape_portrait;
            default:
                return android.R.drawable.arrow_up_float;
        }
    }

    /**
     * Talimat metnini guncelle.
     */
    private void updateInstruction(TurnType turnType, String streetName) {
        if (instructionText == null)
            return;

        String instruction = getTurnInstruction(turnType);

        if (streetName != null && !streetName.isEmpty()) {
            instruction += "\n" + streetName;
        }

        final String finalInstruction = instruction;
        instructionText.post(() -> instructionText.setText(finalInstruction));
    }

    /**
     * Donus talimatini al.
     */
    private String getTurnInstruction(TurnType turnType) {
        if (turnType.isRoundAbout()) {
            int exit = turnType.getExitOut();
            return "Doneleden " + exit + ". cikis";
        }

        switch (turnType.getValue()) {
            case TurnType.C:
                return "Duz git";
            case TurnType.TL:
                return "Sola don";
            case TurnType.TSLL:
                return "Keskin sola don";
            case TurnType.TR:
                return "Saga don";
            case TurnType.TSLR:
                return "Keskin saga don";
            case TurnType.TU:
                return "U donus yap";
            case TurnType.KL:
                return "Sola devam et";
            case TurnType.KR:
                return "Saga devam et";
            default:
                return "Devam et";
        }
    }

    /**
     * ETA (tahmini varis) bilgisini guncelle.
     */
    private void updateETA() {
        if (etaText == null || routingHelper == null)
            return;

        try {
            int remainingDistance = routingHelper.getLeftDistance();
            int remainingTime = routingHelper.getLeftTime();

            if (remainingDistance > 0 && remainingTime > 0) {
                String distanceStr = OsmAndFormatter.getFormattedDistance(remainingDistance, app);
                String timeStr = OsmAndFormatter.getFormattedDuration(remainingTime, app);

                String eta = "Kalan: " + timeStr + " (" + distanceStr + ")";

                etaText.post(() -> etaText.setText(eta));
            } else {
                etaText.post(() -> etaText.setText(""));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sonraki donus yok.
     */
    private void showNoNextTurn() {
        if (distanceText != null) {
            distanceText.post(() -> distanceText.setText("--"));
        }
        if (instructionText != null) {
            instructionText.post(() -> instructionText.setText("Duz git"));
        }
        if (turnIconView != null) {
            turnIconView.post(() -> {
                turnIconView.setImageResource(android.R.drawable.arrow_up_float);
                turnIconView.setVisibility(View.VISIBLE);
            });
        }
    }

    /**
     * Navigasyon aktif degil.
     */
    private void showNoNavigation() {
        if (distanceText != null) {
            distanceText.post(() -> distanceText.setText("--"));
        }
        if (instructionText != null) {
            instructionText.post(() -> instructionText.setText("Navigasyon yok"));
        }
        if (etaText != null) {
            etaText.post(() -> etaText.setText(""));
        }
        if (turnIconView != null) {
            turnIconView.post(() -> turnIconView.setVisibility(View.GONE));
        }
    }

    /**
     * Otomatik guncellemeyi baslat.
     */
    private void startUpdating() {
        if (updateRunnable == null && rootView != null) {
            updateRunnable = new Runnable() {
                @Override
                public void run() {
                    update();
                    if (rootView != null && isStarted()) {
                        rootView.postDelayed(this, 1000); // Her saniye
                    }
                }
            };
            rootView.post(updateRunnable);
        }
    }

    /**
     * Otomatik guncellemeyi durdur.
     */
    private void stopUpdating() {
        if (updateRunnable != null && rootView != null) {
            rootView.removeCallbacks(updateRunnable);
            updateRunnable = null;
        }
    }
}
