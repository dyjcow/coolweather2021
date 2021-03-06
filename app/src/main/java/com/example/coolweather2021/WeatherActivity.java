package com.example.coolweather2021;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


import com.bumptech.glide.Glide;
import com.example.coolweather2021.gson.AQI;
import com.example.coolweather2021.gson.Daily;
import com.example.coolweather2021.gson.Now;
import com.example.coolweather2021.gson.Suggestion;
import com.example.coolweather2021.gson.Weather;
import com.example.coolweather2021.service.AutoUpdateService;
import com.example.coolweather2021.util.HttpUtil;
import com.example.coolweather2021.util.Utility;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 本类使用了ButterKnife注解
 */
public class WeatherActivity extends AppCompatActivity {

    @BindView(R.id.weather_layout)
    ScrollView weatherLayout;
    @BindView(R.id.title_city)
    TextView titleCity;
    @BindView(R.id.title_update_time)
    TextView titleUpdateTime;
    @BindView(R.id.degree_text)
    TextView degreeText;
    @BindView(R.id.weather_info_text)
    TextView weatherInfoText;
    @BindView(R.id.forecast_layout)
    LinearLayout forecastLayout;
    @BindView(R.id.aqi_text)
    TextView aqiText;
    @BindView(R.id.pm25_text)
    TextView pm25Text;
    @BindView(R.id.health_text)
    TextView healthText;
    @BindView(R.id.face_painting_text)
    TextView paintingText;
    @BindView(R.id.wear_text)
    TextView wearText;
    @BindView(R.id.icon_now)
    ImageView weatherImage;
    @BindView(R.id.bing_pic_img)
    ImageView bingPicImg;
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefresh;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;
    @BindView(R.id.nav_button)
    Button navButton;

    static final String key = "&key=98c2e401cf4b46908da304061da6bc16";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
        |View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_weather);
        ButterKnife.bind(this);//Use butterKnife

        String weatherId= getIntent().getStringExtra("weather_id");

        swipeRefresh.setColorSchemeResources(R.color.design_default_color_secondary_variant);

        SharedPreferences nowWeather = getSharedPreferences("weatherNow", MODE_PRIVATE);
        SharedPreferences aqiWeather = getSharedPreferences("weatherAQI", MODE_PRIVATE);
        SharedPreferences dailyWeather = getSharedPreferences("weatherDaily", MODE_PRIVATE);
        SharedPreferences suggestionWeather = getSharedPreferences("weatherSuggestion", MODE_PRIVATE);
        SharedPreferences BingPic = getSharedPreferences("BingPic",MODE_PRIVATE);

        String weatherNowString = nowWeather.getString("Now", null);
        String weatherAQIString = aqiWeather.getString("AQI", null);
        String weatherDailyString = dailyWeather.getString("Daily", null);
        String weatherSuggestionString = suggestionWeather.getString("Suggestion", null);
        String Name = nowWeather.getString("Name",null);
        String Id = nowWeather.getString("ID",null);
        String bingPic = BingPic.getString("bing_pic",null);

        //WeatherId weatherData = new WeatherId();
        //weatherData.weatherId = weatherId1;
        //String weatherId = splitData(weatherData);//都没用

        String weatherName = getIntent().getStringExtra("weather_name");

        if (bingPic != null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else {
            loadBingPic();
        }

        if (weatherNowString != null && weatherAQIString != null && weatherDailyString != null
                && weatherSuggestionString != null){
            Weather weather = new Weather();
            weather.now = Utility.handlerWeatherNowResponse(weatherNowString);
            weather.aqi = Utility.handlerWeatherAQIResponse(weatherAQIString);
            weather.daily = Utility.handlerWeatherDailyResponse(weatherDailyString);
            weather.suggestion = Utility.handlerWeatherSuggestionResponse(weatherSuggestionString);
            showWeatherInfo(weather,Name);
        }else {
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeatherNow(weatherId,weatherName);
            requestWeatherAQI(weatherId);
            requestWeatherDaily(weatherId);
            requestWeatherSuggestion(weatherId);
        }


        //不知为何，这里还无法更新（解决了）
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
//                SharedPreferences nowWeather = getSharedPreferences("weatherNow", MODE_PRIVATE);
                String Id = nowWeather.getString("ID",null);//重新传入当下的ID，解决无法更新的问题
                String Name = nowWeather.getString("Name",null);
                requestWeatherNow(Id,Name);
                requestWeatherAQI(Id);
                requestWeatherDaily(Id);
                requestWeatherSuggestion(Id);

//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                            try {
//                                Thread.sleep(1000);
//
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                    }
//                }).start();

            }
        });

        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

    }

    /**
     * 去和风天气服务器获取实时天气信息Now
     * @param weatherId 传入城市ID
     */
    public void requestWeatherNow(final String weatherId,final String weatherName){
        String weatherNowUrl = "https://devapi.qweather.com/v7/weather/now?location="+weatherId+
                key;
        HttpUtil.sendOkHttpRequest(weatherNowUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showToast("获取天气信息失败");
                    }
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = new Weather();
                weather.now = Utility.handlerWeatherNowResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather.now != null && "200".equals(weather.now.status)){
                            SharedPreferences.Editor editor = getSharedPreferences("weatherNow",
                                    MODE_PRIVATE).edit();
                            editor.putString("Now",responseText);
                            editor.putString("Name",weatherName);//借这里储存城市名
                            editor.putString("ID",weatherId);
                            editor.apply();
                            showWeatherNowInfo(weather.now,weatherName);
                        }else {
                            showToast("获取天气信息失败");
                        }
                    }
                });
            }
        });

    }

    /**
     * 去和风天气获取多天天气信息Daily
     * @param weatherId 传入城市ID
     */
    public void requestWeatherDaily(final String weatherId){
        String weatherNowUrl = "https://devapi.qweather.com/v7/weather/7d?location="+weatherId+
                key;
        HttpUtil.sendOkHttpRequest(weatherNowUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showToast("获取天气信息失败");
                        //swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = new Weather();
                weather.daily = Utility.handlerWeatherDailyResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather.daily != null && "200".equals(weather.daily.status)){
                            SharedPreferences.Editor editor = getSharedPreferences("weatherDaily",
                                    MODE_PRIVATE).edit();
                            editor.putString("Daily",responseText);
                            editor.apply();
                            showWeatherDailyInfo(weather.daily);
                        }else {
                            showToast("获取天气信息失败");
                        }
                        //swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
    }

    /**
     *去和风天气获取空气质量信息AQI
     * @param weatherId 传入城市ID
     */
    public void requestWeatherAQI(final String weatherId){
        String weatherNowUrl = "https://devapi.qweather.com/v7/air/now?location="+weatherId+
                key;
        HttpUtil.sendOkHttpRequest(weatherNowUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showToast("获取天气信息失败");
                        //swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = new Weather();
                weather.aqi = Utility.handlerWeatherAQIResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather.aqi != null && "200".equals(weather.aqi.status)){
                            SharedPreferences.Editor editor = getSharedPreferences("weatherAQI",
                                    MODE_PRIVATE).edit();
                            editor.putString("AQI",responseText);
                            editor.apply();
                            showWeatherAQIInfo(weather.aqi);
                        }else {
                            showToast("获取天气信息失败");
                        }
                        //swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
    }

    /**
     * 去和风天气获取生活建议信息Suggestion
     * @param weatherId 传入城市ID
     */
    public void requestWeatherSuggestion(final String weatherId){
        String weatherNowUrl = "https://devapi.qweather.com/v7/indices/1d?type=3,9,13&location="+weatherId+
                key;
        HttpUtil.sendOkHttpRequest(weatherNowUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showToast("获取天气信息失败");
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = new Weather();
                weather.suggestion = Utility.handlerWeatherSuggestionResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather.suggestion != null && "200".equals(weather.suggestion.status)){
                            SharedPreferences.Editor editor = getSharedPreferences("weatherSuggestion",
                                    MODE_PRIVATE).edit();
                            editor.putString("Suggestion",responseText);
                            editor.apply();
                            showWeatherSuggestion(weather.suggestion);
                        }else {
                            showToast("获取天气信息失败");
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
    }


    /**
     * 更新Now状态
     * @param now Now类的传入
     * @param localName 地点名
     */
    public void showWeatherNowInfo(Now now,String localName){
        String updateTime = now.now.checkTime.split("T|\\+")[1];
        String degree = now.now.temperature + "°C";
        String weatherInfo = now.now.information;
        String weatherIcon = now.now.iconImage;

        titleCity.setText(localName);
        titleUpdateTime.setText(updateTime);
        weatherInfoText.setText(weatherInfo);
        degreeText.setText(degree);
        weatherImage.setImageResource(weatherIcon(weatherIcon));
    }

    /**
     * 更新Daily状态
     * @param daily Daily类的传入
     */
    public void showWeatherDailyInfo(Daily daily){
        forecastLayout.removeAllViews();
        for(Daily.Forecast forecast : daily.forecastList){
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView dateText = view.findViewById(R.id.date_text);
            TextView infoText = view.findViewById(R.id.info_text);
            TextView maxText = view.findViewById(R.id.max_text);
            TextView minText = view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.information);
            maxText.setText(String.format("%s°C", forecast.TemperatureMax));//直接在后面加“°C”有警告，换成这个就没了
            minText.setText(new StringBuilder().append(forecast.TemperatureMin).append("°C").toString());
            forecastLayout.addView(view);
        }
    }

    /**
     * 更新AQI类
     * @param aqi AQI类的传入
     */
    public void showWeatherAQIInfo(AQI aqi){
        if (aqi != null){
            aqiText.setText(aqi.aqiNow.aqi);
            pm25Text.setText(aqi.aqiNow.pm25);
        }
    }

    /**
     * 更新Weather类
     * @param suggestionId Weather类的传入
     */
    public void showWeatherSuggestion(Suggestion suggestionId){

        for (Suggestion.SuggestionTo suggestion : suggestionId.suggestionToList){
            String suggestText = suggestion.name + ": " + suggestion.suggestionText;
            switch (suggestion.name) {
                case "穿衣指数":
                    wearText.setText(suggestText);
                    break;
                case "感冒指数":
                    healthText.setText(suggestText);
                    break;
                case "化妆指数":
                    paintingText.setText(suggestText);
                    break;
            }
        }
        weatherLayout.setVisibility(View.VISIBLE);
    }

    /**
     * 界面信息展示逻辑
     * @param weather 打包好的weather类
     * @param localName 传入选择了的城市名
     */
    private void showWeatherInfo(@NotNull Weather weather, String localName){
        String updateTime = weather.now.now.checkTime.split("T|\\+")[1];
        String degree = weather.now.now.temperature + "°C";
        String weatherInfo = weather.now.now.information;
        String weatherIcon = weather.now.now.iconImage;

        titleCity.setText(localName);
        titleUpdateTime.setText(updateTime);
        weatherInfoText.setText(weatherInfo);
        degreeText.setText(degree);
        weatherImage.setImageResource(weatherIcon(weatherIcon));


        forecastLayout.removeAllViews();
        for(Daily.Forecast forecast : weather.daily.forecastList){
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView dateText = view.findViewById(R.id.date_text);
            TextView infoText = view.findViewById(R.id.info_text);
            TextView maxText = view.findViewById(R.id.max_text);
            TextView minText = view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.information);
            maxText.setText(String.format("%s°C", forecast.TemperatureMax));//直接在后面加“°C”有警告，换成这个就没了
            minText.setText(new StringBuilder().append(forecast.TemperatureMin).append("°C").toString());
            forecastLayout.addView(view);
        }


        if (weather.aqi != null){
            aqiText.setText(weather.aqi.aqiNow.aqi);
            pm25Text.setText(weather.aqi.aqiNow.pm25);
        }



        for (Suggestion.SuggestionTo suggestion : weather.suggestion.suggestionToList){
            String suggestText = suggestion.name + ": " + suggestion.suggestionText;
            switch (suggestion.name) {
                case "穿衣指数":
                    wearText.setText(suggestText);
                    break;
                case "感冒指数":
                    healthText.setText(suggestText);
                    break;
                case "化妆指数":
                    paintingText.setText(suggestText);
                    break;
            }
        }

        weatherLayout.setVisibility(View.VISIBLE);

        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }

    /**
     * 从网络载入背景图
     */
    private void loadBingPic(){
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = getSharedPreferences("BingPic",MODE_PRIVATE).edit();
                editor.putString("bingPic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }

    /**
     * 弹出框的封装
     * @param msg 弹出框的内容
     */
    public void showToast(String msg){
        Toast.makeText(WeatherActivity.this,msg,Toast.LENGTH_SHORT).show();
    }

    /**
     * 对天气图标的选择
     * @param icon 天气图标参数
     * @return 天气图标的int值
     */
    private int weatherIcon(String icon){
        switch (icon){
            case "100":
                return R.drawable.icon_100;
            case "101":
                return R.drawable.icon_101;
            case "102":
                return R.drawable.icon_102;
            case "103":
                return R.drawable.icon_103;
            case "104":
                return R.drawable.icon_104;
            case "150":
                return R.drawable.icon_150;
            case "153":
                return R.drawable.icon_153;
            case "154":
                return R.drawable.icon_154;
            case "300":
                return R.drawable.icon_300;
            case "301":
                return R.drawable.icon_301;
            case "302":
                return R.drawable.icon_302;
            case "303":
                return R.drawable.icon_303;
            case "304":
                return R.drawable.icon_304;
            case "305":
                return R.drawable.icon_305;
            case "306":
                return R.drawable.icon_306;
            case "307":
                return R.drawable.icon_307;
            case "308":
                return R.drawable.icon_308;
            case "309":
                return R.drawable.icon_309;
            case "310":
                return R.drawable.icon_310;
            case "311":
                return R.drawable.icon_311;
            case "312":
                return R.drawable.icon_312;
            case "313":
                return R.drawable.icon_313;
            case "314":
                return R.drawable.icon_314;
            case "315":
                return R.drawable.icon_315;
            case "316":
                return R.drawable.icon_316;
            case "317":
                return R.drawable.icon_317;
            case "318":
                return R.drawable.icon_318;
            case "350":
                return R.drawable.icon_350;
            case "351":
                return R.drawable.icon_351;
            case "399":
                return R.drawable.icon_399;
            case "400":
                return R.drawable.icon_400;
            case "401":
                return R.drawable.icon_401;
            case "402":
                return R.drawable.icon_402;
            case "403":
                return R.drawable.icon_403;
            case "404":
                return R.drawable.icon_404;
            case "405":
                return R.drawable.icon_405;
            case "406":
                return R.drawable.icon_406;
            case "407":
                return R.drawable.icon_407;
            case "408":
                return R.drawable.icon_408;
            case "409":
                return R.drawable.icon_409;
            case "410":
                return R.drawable.icon_410;
            case "456":
                return R.drawable.icon_456;
            case "457":
                return R.drawable.icon_457;
            case "499":
                return R.drawable.icon_499;
            case "500":
                return R.drawable.icon_500;
            case "501":
                return R.drawable.icon_501;
            case "502":
                return R.drawable.icon_502;
            case "503":
                return R.drawable.icon_503;
            case "504":
                return R.drawable.icon_504;
            case "507":
                return R.drawable.icon_507;
            case "508":
                return R.drawable.icon_509;
            case "510":
                return R.drawable.icon_510;
            case "511":
                return R.drawable.icon_511;
            case "512":
                return R.drawable.icon_512;
            case "513":
                return R.drawable.icon_513;
            case "514":
                return R.drawable.icon_514;
            case "515":
                return R.drawable.icon_515;
            case "900":
                return R.drawable.icon_900;
            case "901":
                return R.drawable.icon_901;
            case "999":
                return R.drawable.icon_999;
        }

        return 100;
    }

}