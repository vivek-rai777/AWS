package com.task08;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class OpenMeteoApi {
    private static final OkHttpClient client = new OkHttpClient();

    public String getWeatherForecast(String apiKey, String city) throws IOException {
        Request request = new Request.Builder()
                .url("https://api.open-meteo.com/weather?city=" + city + "&key=" + apiKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }
}

