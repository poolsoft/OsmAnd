package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.carlauncher.widgets.weather.WeatherManager;

import java.util.Locale;

/**
 * Hava Durumu Wiget'i.
 * WeatherManager'dan veri alir ve gosterir.
 */
public class WeatherWidget extends BaseWidget implements WeatherManager.WeatherListener {

    private TextView tvLocation;
    private TextView tvTemp;
    private TextView tvDesc;
    private ImageView ivIcon;
    private ProgressBar progressBar;
    
    private final WeatherManager weatherManager;

    public WeatherWidget(@NonNull Context context, OsmandApplication app) {
        super(context, "weather", "Hava Durumu");
        this.weatherManager = WeatherManager.getInstance(context);
        this.order = 10; // Default order towards end
    }

    @Override
    public View createView() {
        View view = LayoutInflater.from(context).inflate(net.osmand.plus.R.layout.widget_weather, null);

        tvLocation = view.findViewById(net.osmand.plus.R.id.weather_location);
        tvTemp = view.findViewById(net.osmand.plus.R.id.weather_temp);
        tvDesc = view.findViewById(net.osmand.plus.R.id.weather_desc);
        ivIcon = view.findViewById(net.osmand.plus.R.id.weather_icon);
        progressBar = view.findViewById(net.osmand.plus.R.id.weather_loading);

        rootView = view;
        
        rootView = view;
        
        rootView.setOnClickListener(v -> {
            net.osmand.plus.carlauncher.CarLauncherInterface activity = null;
            Context ctx = context;
            
            // Traverse ContextWrapper to find Activity
            while (ctx instanceof android.content.ContextWrapper) {
                if (ctx instanceof net.osmand.plus.carlauncher.CarLauncherInterface) {
                    activity = (net.osmand.plus.carlauncher.CarLauncherInterface) ctx;
                    break;
                }
                if (ctx instanceof android.app.Activity) {
                     // Check if this activity implements interface
                     if (ctx instanceof net.osmand.plus.carlauncher.CarLauncherInterface) {
                         activity = (net.osmand.plus.carlauncher.CarLauncherInterface) ctx;
                     }
                     break; // Stop at Activity level
                }
                ctx = ((android.content.ContextWrapper) ctx).getBaseContext();
            }
            
            if (activity != null) {
                activity.openWeatherDashboard();
            } else {
                android.util.Log.e("WeatherWidget", "Context is NOT CarLauncherInterface: " + context.getClass().getName());
            }
        });
        
        updateUI(weatherManager.getCachedWeather());
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        weatherManager.addListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        weatherManager.removeListener(this);
    }

    @Override
    public void onWeatherUpdated(WeatherManager.WeatherData data) {
         new Handler(Looper.getMainLooper()).post(() -> updateUI(data));
    }

    @Override
    public void onWeatherError(String error) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (tvDesc != null) tvDesc.setText(error);
            if (progressBar != null) progressBar.setVisibility(View.GONE);
        });
    }

    @Override
    public void update() {
        // Redundant if listener works, but can force refresh UI
        updateUI(weatherManager.getCachedWeather());
    }
    
    @Override
    protected void onSizeChanged(WidgetSize newSize) {
        if (rootView == null) return;
        
        // Adjust UI based on size
        if (newSize == WidgetSize.SMALL) {
            // Compact Mode
            if (tvDesc != null) tvDesc.setVisibility(View.GONE);
            if (progressBar != null) progressBar.setVisibility(View.GONE); // Hide loading in small to save space?
            
            // Icon smaller
            if (ivIcon != null) {
                ivIcon.setVisibility(View.VISIBLE);
                // Layout params could be adjusted here if needed, 
                // but simpler to rely on layout constraints or GONE handling
            }
            
            if (tvLocation != null) tvLocation.setVisibility(View.GONE); // Hide location in small
        } else {
            // Normal Mode
            if (tvDesc != null) tvDesc.setVisibility(View.VISIBLE);
            if (tvLocation != null) tvLocation.setVisibility(View.VISIBLE);
            // Loading bar handled by data state
        }
        
        // Refresh data display
        updateUI(weatherManager.getCachedWeather());
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public void openConfig(androidx.fragment.app.FragmentManager fragmentManager) {
        net.osmand.plus.carlauncher.ui.WeatherConfigDialog dialog = new net.osmand.plus.carlauncher.ui.WeatherConfigDialog(this);
        dialog.show(fragmentManager, "WeatherConfig");
    }

    private void updateUI(WeatherManager.WeatherData data) {
        if (rootView == null) return;
        
        if (data == null) {
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            if (tvTemp != null) tvTemp.setText("--");
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.GONE);

        if (tvTemp != null) {
            tvTemp.setText(String.format(Locale.US, "%.0f°", data.temp));
        }
        
        if (tvDesc != null) {
            tvDesc.setText(getWeatherDescription(data.weatherCode));
        }

        if (ivIcon != null) {
            int iconRes = getIconResource(data.getIconName());
            ivIcon.setImageResource(iconRes);
        }
        
        if (tvLocation != null) {
             tvLocation.setText("Konum");
        }
        
        // Enforcement for specific sizes if layout reset properties
        if (size == WidgetSize.SMALL) {
             if (tvDesc != null) tvDesc.setVisibility(View.GONE);
             if (tvLocation != null) tvLocation.setVisibility(View.GONE);
        }
    }
    
    private String getWeatherDescription(int code) {
        if (code == 0) return "Açık";
        if (code >= 1 && code <= 3) return "Parçalı Bulutlu";
        if (code >= 45 && code <= 48) return "Sisli";
        if (code >= 51 && code <= 67) return "Yağmurlu";
        if (code >= 71 && code <= 77) return "Karlı";
        if (code >= 80 && code <= 82) return "Sağanak";
        if (code >= 95) return "Fırtına";
        return "Bilinmiyor";
    }

    private int getIconResource(String iconName) {
        int resId = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
        if (resId == 0) {
            if (iconName.contains("clear")) return net.osmand.plus.R.drawable.ic_action_sun; 
            if (iconName.contains("cloud")) return net.osmand.plus.R.drawable.ic_action_cloud;
            return net.osmand.plus.R.drawable.ic_action_umbrella;
        }
        return resId;
    }
}
