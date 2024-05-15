package com.task09;

import java.net.HttpURLConnection;

import java.net.URL;

import java.util.Scanner;

public class OpenMeteoAPI {

    public String getWeatherForecast() {

        try {

            URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&hourly=temperature_2m");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");

            conn.connect();

            Scanner scanner = new Scanner(url.openStream());

            StringBuilder response = new StringBuilder();

            while (scanner.hasNext()) {

                response.append(scanner.nextLine());

            }

            scanner.close();

            return response.toString();

        } catch (Exception e) {

            e.printStackTrace();

            return null;

        }

    }

}