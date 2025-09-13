package com.example.common.Service;

import com.example.common.Exception.AnyException;
import com.example.common.util.GeminiApiHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

    @Value("${openweathermap.api.key}")
    private String openWeatherApiKey;

    private static final String WEATHER_API_URL = "http://api.openweathermap.org/data/2.5/forecast";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final GeminiApiHelper geminiApiHelper;

    // Fixed 20 crops with icons
    private static final List<String> CROPS = Arrays.asList(
            "Wheat", "Rice", "Sugarcane", "Pulses", "Maize", "Barley", "Cotton", "Soybean",
            "Groundnut", "Sorghum", "Millet", "Sunflower", "Chickpea", "Lentil", "Mustard",
            "Potato", "Tomato", "Onion", "Cauliflower", "Cabbage"
    );

    private static final Map<String, String> CROP_ICONS = new HashMap<>() {{
        put("Wheat", "🌾"); put("Rice", "🌱"); put("Sugarcane", "🍃"); put("Pulses", "🌿");
        put("Maize", "🌽"); put("Barley", "🌾"); put("Cotton", "🧵"); put("Soybean", "🌱");
        put("Groundnut", "🥜"); put("Sorghum", "🌾"); put("Millet", "🌾"); put("Sunflower", "🌻");
        put("Chickpea", "🌿"); put("Lentil", "🌿"); put("Mustard", "🌿"); put("Potato", "🥔");
        put("Tomato", "🍅"); put("Onion", "🧅"); put("Cauliflower", "🥦"); put("Cabbage", "🥬");
    }};

    public WeatherService(RestTemplate restTemplate, ObjectMapper objectMapper, GeminiApiHelper geminiApiHelper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.geminiApiHelper = geminiApiHelper;
    }

    // 🌤 Fetch weather + multi-crop advice
    public Map<String, Object> getWeatherForecast(double lat, double lon, String lang) throws AnyException {
        try {
            if (lat < -90 || lat > 90)
                throw new AnyException(HttpStatus.BAD_REQUEST.value(), "Invalid latitude");
            if (lon < -180 || lon > 180)
                throw new AnyException(HttpStatus.BAD_REQUEST.value(), "Invalid longitude");

            String url = String.format("%s?lat=%.6f&lon=%.6f&appid=%s&units=metric", WEATHER_API_URL, lat, lon, openWeatherApiKey);
            logger.info("Fetching weather from OpenWeatherMap: {}", url);

            String response = restTemplate.getForObject(url, String.class);
            if (response == null)
                throw new AnyException(HttpStatus.NO_CONTENT.value(), "Empty weather response");

            JsonNode data = objectMapper.readTree(response);
            return parseWeatherData(data, lang != null ? lang : "en");

        } catch (AnyException ex) {
            throw ex;
        } catch (Exception e) {
            logger.error("Failed to fetch weather forecast for lat={}, lon={}", lat, lon, e);
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to fetch weather forecast");
        }
    }

    // 🌾 Multi-crop advice
    public List<Map<String, String>> generateCropAdvice(double temp, int humidity, double rainProb,
                                                        String weatherMain, String lang) throws AnyException {
        try {
            // Input validation
            if (temp < -50 || temp > 60)
                throw new AnyException(HttpStatus.BAD_REQUEST.value(), "Temperature must be -50°C to 60°C");
            if (humidity < 0 || humidity > 100)
                throw new AnyException(HttpStatus.BAD_REQUEST.value(), "Humidity must be 0-100%");
            if (rainProb < 0 || rainProb > 1)
                throw new AnyException(HttpStatus.BAD_REQUEST.value(), "Rain probability must be 0-1");
            if (weatherMain == null || weatherMain.isBlank())
                throw new AnyException(HttpStatus.BAD_REQUEST.value(), "Weather condition cannot be empty");

            // Old-style prompt
            String prompt = String.format(
                    "Given the weather: temp=%.2f°C, humidity=%d%%, rainProb=%.2f, weather=%s, " +
                            "provide advice for crops: %s. Return JSON array with fields cropName, status, adviceEn, adviceHi",
                    temp, humidity, rainProb, weatherMain, String.join(", ", CROPS)
            );

            String generatedText = geminiApiHelper.callGeminiApiForJson(prompt);

            List<Map<String, String>> apiAdvice = objectMapper.readValue(generatedText,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() {});

            List<Map<String, String>> advice = new ArrayList<>();
            Set<String> includedCrops = new HashSet<>();

            for (Map<String, String> crop : apiAdvice) {
                String cropName = crop.get("cropName");
                if (!CROPS.contains(cropName))
                    throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Invalid crop in response: " + cropName);

                Map<String, String> map = new HashMap<>();
                map.put("crop", CROP_ICONS.getOrDefault(cropName, "") + " " + cropName);
                map.put("icon", CROP_ICONS.getOrDefault(cropName, ""));
                map.put("cropName", cropName);
                map.put("status", crop.get("status"));
                map.put("adviceEn", crop.get("adviceEn"));
                map.put("adviceHi", crop.get("adviceHi"));
                advice.add(map);
                includedCrops.add(cropName);
            }

            // Missing crops default
            for (String crop : CROPS) {
                if (!includedCrops.contains(crop)) {
                    Map<String, String> map = new HashMap<>();
                    map.put("crop", CROP_ICONS.get(crop) + " " + crop);
                    map.put("icon", CROP_ICONS.get(crop));
                    map.put("cropName", crop);
                    map.put("status", "✅");
                    map.put("adviceEn", "Check weather conditions and proceed with regular care");
                    map.put("adviceHi", "मौसम की स्थिति जांचें और नियमित देखभाल के साथ आगे बढ़ें");
                    advice.add(map);
                }
            }

            return advice;

        } catch (AnyException ex) {
            throw ex;
        } catch (Exception e) {
            logger.error("Failed multi-crop advice", e);
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to generate multi-crop advice");
        }
    }

    // 🍅 Single crop advice
    public Map<String, String> generateSingleCropAdvice(double temp, int humidity, double rainProb,
                                                        String weatherMain, String lang, String cropName) throws AnyException {
        try {
            if (cropName == null || cropName.isBlank())
                throw new AnyException(HttpStatus.BAD_REQUEST.value(), "Crop name cannot be empty");

            String prompt = String.format(
                    "Given the weather: temp=%.2f°C, humidity=%d%%, rainProb=%.2f, weather=%s, " +
                            "provide advice for crop: %s. Return JSON object with cropName, status, adviceEn, adviceHi",
                    temp, humidity, rainProb, weatherMain, cropName
            );

            String generatedText = geminiApiHelper.callGeminiApiForJson(prompt);

            Map<String, String> cropAdvice = objectMapper.readValue(
                    generatedText,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {}
            );

            cropAdvice.put("crop", CROP_ICONS.getOrDefault(cropName, "") + " " + cropName);
            cropAdvice.put("icon", CROP_ICONS.getOrDefault(cropName, ""));

            return cropAdvice;

        } catch (AnyException ex) {
            throw ex;
        } catch (Exception e) {
            logger.error("Failed single-crop advice", e);
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed single-crop advice");
        }
    }

    // Parse OpenWeather + add crop advice
    private Map<String, Object> parseWeatherData(JsonNode data, String lang) throws AnyException {
        try {
            JsonNode city = data.get("city");
            if (city == null || !city.has("name") || !city.has("country"))
                throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Invalid city data");

            Map<String, Object> result = new HashMap<>();
            result.put("city", city.get("name").asText());
            result.put("country", city.get("country").asText());

            JsonNode current = data.get("list").get(0);
            JsonNode main = current.get("main");
            JsonNode weather = current.get("weather").get(0);
            JsonNode wind = current.get("wind");

            if (main == null || weather == null || wind == null)
                throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Invalid weather data");

            double temp = main.get("temp").asDouble();
            int humidity = main.get("humidity").asInt();
            double rainProb = current.get("pop").asDouble();
            String weatherMain = weather.get("main").asText();

            result.put("temperature", Math.round(temp));
            result.put("feelsLike", Math.round(main.get("feels_like").asDouble()));
            result.put("humidity", humidity);
            result.put("pressure", main.get("pressure").asInt());
            result.put("description", weather.get("description").asText());
            result.put("icon", weather.get("icon").asText());
            result.put("weatherMain", weatherMain);
            result.put("windSpeed", Math.round(wind.get("speed").asDouble() * 3.6));
            result.put("windDeg", wind.get("deg").asInt());
            result.put("rainProbability", Math.round(rainProb * 100));
            result.put("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy (EEEE)")));

            result.put("cropAdvice", generateCropAdvice(temp, humidity, rainProb, weatherMain, lang));

            return result;

        } catch (AnyException ex) {
            throw ex;
        } catch (Exception e) {
            logger.error("Failed to parse weather data", e);
            throw new AnyException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to parse weather data");
        }
    }

    // Default weather data
    public Map<String, Object> getDefaultWeatherData(String lang) throws AnyException {
        Map<String, Object> defaultData = new HashMap<>();
        defaultData.put("city", "Basti");
        defaultData.put("country", "IN");
        defaultData.put("temperature", 28);
        defaultData.put("feelsLike", 32);
        defaultData.put("humidity", 75);
        defaultData.put("pressure", 1010);
        defaultData.put("description", "partly cloudy");
        defaultData.put("icon", "02d");
        defaultData.put("weatherMain", "Clouds");
        defaultData.put("windSpeed", 8);
        defaultData.put("windDeg", 180);
        defaultData.put("rainProbability", 40);
        defaultData.put("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy (EEEE)")));

        defaultData.put("cropAdvice", generateCropAdvice(28, 75, 0.4, "Clouds", lang));
        return defaultData;
    }
}