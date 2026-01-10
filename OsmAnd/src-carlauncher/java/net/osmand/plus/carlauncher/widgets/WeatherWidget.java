package net.osmand.plus.carlauncher.widgets;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.carlauncher.widgets.weather.WeatherManager;

import java.util.List;

public class WeatherWidget extends BaseWidget implements WeatherManager.WeatherListener {

    private TextView startTempText;
    private TextView minMaxText;
    private ImageView weatherIcon;
    private LinearLayout forecastContainer;
    private View currentContainer;

    public WeatherWidget(Context context, OsmandApplication app) {
        super(context, app);
    }

    @Override
    public String getId() {
        return "weather_widget";
    }

    @Override
    public String getTitle() {
        return "Hava Durumu";
    }

    @Override
    public View createView(ViewGroup parent) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.widget_weather, parent, false);

        currentContainer = view.findViewById(R.id.weather_current_container);
        startTempText = view.findViewById(R.id.weather_temp_text);
        minMaxText = view.findViewById(R.id.weather_minmax_text);
        weatherIcon = view.findViewById(R.id.weather_icon);
        forecastContainer = view.findViewById(R.id.weather_forecast_container);

        // Initial update with listener
        WeatherManager.getInstance(getContext()).addListener(this);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        WeatherManager.getInstance(getContext()).removeListener(this);
    }

    @Override
    public void update() {
        // Triggered periodically, we rely on WeatherManager callback mainly.
        // But we can check cache.
        onWeatherUpdated(WeatherManager.getInstance(getContext()).getCachedWeather());
    }

    @Override
    protected void onSizeChanged(WidgetSize newSize) {
        if (newSize == WidgetSize.LARGE) {
            forecastContainer.setVisibility(View.VISIBLE);
            minMaxText.setVisibility(View.VISIBLE);
            // Adjust weights if needed
        } else if (newSize == WidgetSize.MEDIUM) {
            forecastContainer.setVisibility(View.GONE);
            minMaxText.setVisibility(View.VISIBLE);
        } else {
            // SMALL
            forecastContainer.setVisibility(View.GONE);
            minMaxText.setVisibility(View.GONE);
        }
        
        // Refresh data display
        update();
    }

    @Override
    public void onWeatherUpdated(WeatherManager.WeatherData data) {
        if (data == null || startTempText == null) return;

        startTempText.setText(String.format("%.0f°", data.temp));
        minMaxText.setText(String.format("H: %.0f° L: %.0f°", data.maxTemp, data.minTemp));
        
        String iconName = data.getIconName();
        int resId = getContext().getResources().getIdentifier(iconName, "drawable", getContext().getPackageName());
        if (resId != 0) {
            weatherIcon.setImageResource(resId);
        }

        // Forecast for L size
        if (forecastContainer.getVisibility() == View.VISIBLE && data.forecast != null) {
            forecastContainer.removeAllViews();
            
            for (WeatherManager.DailyForecast day : data.forecast) {
                View dayView = createDayView(day);
                forecastContainer.addView(dayView);
            }
        }
    }

    @Override
    public void onWeatherError(String error) {
        // Handle error display
    }
    
    private View createDayView(WeatherManager.DailyForecast day) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        layout.setLayoutParams(params);
        layout.setGravity(Gravity.CENTER);
        
        // Day Name (e.g. Mon) - Parsing full date is expensive, simpler to show basic
        // Or show temp
        
        ImageView icon = new ImageView(getContext());
        int resId = getContext().getResources().getIdentifier(day.getIconName(), "drawable", getContext().getPackageName());
        if (resId == 0) resId = R.drawable.ic_weather_cloudy;
        icon.setImageResource(resId);
        icon.setLayoutParams(new LinearLayout.LayoutParams(32, 32)); // pixel values approximately
        
        TextView temp = new TextView(getContext());
        temp.setText(String.format("%.0f°", day.maxTemp));
        temp.setTextSize(12);
        temp.setTextColor(0xFFFFFFFF);
        temp.setGravity(Gravity.CENTER);
        
        layout.addView(icon);
        layout.addView(temp);
        
        return layout;
    }
}
