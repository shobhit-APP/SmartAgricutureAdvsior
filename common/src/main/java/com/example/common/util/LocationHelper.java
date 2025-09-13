package com.example.common.util;

import com.example.common.Model.LocationMapping;
import com.example.common.repo.LocationMappingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for handling location-related operations, including state, district, and market resolution.
 * Provides methods to fetch location data from a repository with fallback to default values and calculates
 * the nearest location based on geographic coordinates using the Haversine formula.
 */
@Slf4j
@Component
public class LocationHelper {

    @Autowired
    private LocationMappingRepository locationMappingRepository;

    // Fallback default options
    private final List<String> DEFAULT_STATES = Arrays.asList(
            "Karnataka", "Maharashtra", "Gujarat", "Tamil Nadu", "Andhra Pradesh",
            "Kerala", "Telangana", "Punjab", "Haryana", "Rajasthan",
            "Madhya Pradesh", "Uttar Pradesh", "Bihar", "West Bengal", "Odisha",
            "Jharkhand", "Chhattisgarh", "Assam", "Himachal Pradesh", "Uttarakhand");

    private final List<String> DEFAULT_DISTRICTS = Arrays.asList(
            "Shimoga", "Pune", "Amreli", "Coimbatore", "Bangalore Rural",
            "Mysore", "Hassan", "Mandya", "Tumkur", "Kolar",
            "Nashik", "Aurangabad", "Solapur", "Kolhapur", "Satara",
            "Ahmednagar", "Sangli", "Latur", "Osmanabad", "Beed",
            "Basti", "Gorakhpur", "Varanasi", "Lucknow", "Kanpur",
            "Agra", "Meerut", "Allahabad", "Bareilly", "Moradabad");

    private final List<String> DEFAULT_MARKETS = Arrays.asList(
            "APMC Market", "Mandi", "Wholesale Market", "Farmers Market", "Regulated Market",
            "Primary Market", "Terminal Market", "Collection Center", "Procurement Center", "Trading Hub",
            "Agricultural Market", "Commodity Market", "Produce Market", "Grain Market", "Vegetable Market",
            "Fruit Market", "Spices Market", "Cotton Market", "Sugar Market", "Rice Market");

    // Reference coordinates for districts (for distance calculation)
    private final Map<String, double[]> DISTRICT_COORDINATES = new HashMap<>();

    /**
     * Constructs a LocationHelper instance with the specified repository and initializes
     * the district coordinates map for distance calculations.
     *
     * @param locationMappingRepository The repository for accessing location mappings.
     */
    public LocationHelper(LocationMappingRepository locationMappingRepository) {
        this.locationMappingRepository = locationMappingRepository;
        // Initialize reference coordinates for districts (latitude, longitude)
        DISTRICT_COORDINATES.put("Basti", new double[] { 26.79, 82.76 });
        DISTRICT_COORDINATES.put("Gorakhpur", new double[] { 26.76, 83.37 });
        DISTRICT_COORDINATES.put("Varanasi", new double[] { 25.32, 82.97 });
        DISTRICT_COORDINATES.put("Lucknow", new double[] { 26.85, 80.95 });
        DISTRICT_COORDINATES.put("Pune", new double[] { 18.52, 73.85 });
        DISTRICT_COORDINATES.put("Bangalore Rural", new double[] { 13.23, 77.71 });
        DISTRICT_COORDINATES.put("Coimbatore", new double[] { 11.02, 76.96 });
        DISTRICT_COORDINATES.put("Amreli", new double[] { 21.60, 71.22 });
        DISTRICT_COORDINATES.put("Shimoga", new double[] { 13.93, 75.57 });
        DISTRICT_COORDINATES.put("Patna", new double[] { 25.59, 85.14 });
        DISTRICT_COORDINATES.put("Kolkata", new double[] { 22.57, 88.36 });
        DISTRICT_COORDINATES.put("Bhopal", new double[] { 23.26, 77.40 });
        DISTRICT_COORDINATES.put("Ludhiana", new double[] { 30.90, 75.85 });
        DISTRICT_COORDINATES.put("Gurgaon", new double[] { 28.46, 77.03 });
        DISTRICT_COORDINATES.put("Jaipur", new double[] { 26.91, 75.79 });
    }

    /**
     * Retrieves the list of default states.
     *
     * @return A {@link List} of default state names.
     */
    public List<String> getStates() {
        return DEFAULT_STATES;
    }

    /**
     * Retrieves the list of default districts.
     *
     * @return A {@link List} of default district names.
     */
    public List<String> getDistricts() {
        return DEFAULT_DISTRICTS;
    }

    /**
     * Retrieves the list of default markets.
     *
     * @return A {@link List} of default market names.
     */
    public List<String> getMarkets() {
        return DEFAULT_MARKETS;
    }

    /**
     * Finds the nearest location (state, district, market) based on the provided coordinates.
     * Uses the Haversine formula to calculate distances and falls back to default values if coordinates are null.
     *
     * @param latitude  The latitude coordinate, or null to use default values.
     * @param longitude The longitude coordinate, or null to use default values.
     * @return A {@link LocationMapping} object containing the nearest state, district, and market.
     */
    public LocationMapping getNearestLocation(Double latitude, Double longitude) {
        LocationMapping nearest = new LocationMapping();

        if (latitude != null && longitude != null) {
            // Find the closest district using distance calculation
            String closestDistrict = findClosestDistrict(latitude, longitude);
            String state = getStateForDistrict(closestDistrict);
            String market = getMarketForState(state);

            nearest.setState(state);
            nearest.setDistrict(closestDistrict);
            nearest.setMarket(market);
        } else {
            // Default when coordinates are null
            nearest.setState("Uttar Pradesh");
            nearest.setDistrict("Lucknow");
            nearest.setMarket("Krishi Upaj Mandi");
        }

        return nearest;
    }

    /**
     * Finds the closest district to the given coordinates using the Haversine formula.
     *
     * @param latitude  The latitude coordinate.
     * @param longitude The longitude coordinate.
     * @return The name of the closest district, or "Lucknow" if no closer district is found.
     */
    private String findClosestDistrict(double latitude, double longitude) {
        String closestDistrict = "Lucknow"; // Default district
        double minDistance = Double.MAX_VALUE;

        for (Map.Entry<String, double[]> entry : DISTRICT_COORDINATES.entrySet()) {
            String district = entry.getKey();
            double[] coords = entry.getValue();
            double distance = calculateDistance(latitude, longitude, coords[0], coords[1]);
            if (distance < minDistance) {
                minDistance = distance;
                closestDistrict = district;
            }
        }

        return closestDistrict;
    }

    /**
     * Maps a district to its corresponding state.
     *
     * @param district The name of the district.
     * @return The name of the state, or "Uttar Pradesh" if the district is not mapped.
     */
    private String getStateForDistrict(String district) {
        return switch (district) {
            case "Basti", "Gorakhpur", "Varanasi", "Lucknow" -> "Uttar Pradesh";
            case "Patna" -> "Bihar";
            case "Kolkata" -> "West Bengal";
            case "Bhopal" -> "Madhya Pradesh";
            case "Bangalore Rural", "Shimoga" -> "Karnataka";
            case "Pune" -> "Maharashtra";
            case "Amreli" -> "Gujarat";
            case "Coimbatore" -> "Tamil Nadu";
            case "Ludhiana" -> "Punjab";
            case "Gurgaon" -> "Haryana";
            case "Jaipur" -> "Rajasthan";
            default -> "Uttar Pradesh";
        };
    }

    /**
     * Maps a state to a representative market.
     *
     * @param state The name of the state.
     * @return The name of the market, or "Krishi Upaj Mandi" if the state is not mapped.
     */
    private String getMarketForState(String state) {
        return switch (state) {
            case "Uttar Pradesh" -> "Krishi Upaj Mandi";
            case "Bihar" -> "Regulated Market";
            case "West Bengal", "Gujarat" -> "Wholesale Market";
            case "Madhya Pradesh", "Maharashtra" -> "Mandi";
            case "Karnataka" -> "APMC Market";
            case "Tamil Nadu" -> "Farmers Market";
            case "Punjab" -> "Grain Market";
            case "Haryana" -> "Produce Market";
            case "Rajasthan" -> "Agricultural Market";
            default -> "Krishi Upaj Mandi";
        };
    }

    /**
     * Calculates the distance between two geographic points using the Haversine formula.
     *
     * @param lat1 The latitude of the first point.
     * @param lon1 The longitude of the first point.
     * @param lat2 The latitude of the second point.
     * @param lon2 The longitude of the second point.
     * @return The distance in kilometers between the two points.
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Resolves a location based on latitude and longitude, querying the database first and falling back to
     * the nearest location if the database query fails or returns no results.
     *
     * @param lat The latitude coordinate, or null to return an empty {@link LocationMapping}.
     * @param lon The longitude coordinate, or null to return an empty {@link LocationMapping}.
     * @return A {@link LocationMapping} object containing the state, district, and market, or an empty
     *         {@link LocationMapping} if coordinates are null.
     */
    public LocationMapping resolveLocation(Double lat, Double lon) {
        if (lat == null || lon == null) {
            return new LocationMapping();
        }
        try {
            List<LocationMapping> mappings = locationMappingRepository.findByLatitudeAndLongitude(lat, lon);
            if (mappings != null && !mappings.isEmpty()) {
                return mappings.getFirst();
            }
            return getNearestLocation(lat, lon);
        } catch (Exception e) {
            log.error("DB error while fetching location, using fallback: {}", e.getMessage());
            return getNearestLocation(lat, lon);
        }
    }

    /**
     * Retrieves a list of distinct states from the database, falling back to the default states if the query fails.
     *
     * @return A {@link List} of distinct state names.
     */
    public List<String> GetStates() {
        try {
            return locationMappingRepository.findDistinctStates();
        } catch (Exception e) {
            log.error("DB failed for states, using helper: {}", e.getMessage());
            return getStates();
        }
    }

    /**
     * Retrieves a list of distinct districts from the database, falling back to the default districts if the query fails.
     *
     * @return A {@link List} of distinct district names.
     */
    public List<String> GetDistricts() {
        try {
            return locationMappingRepository.findDistinctDistricts();
        } catch (Exception e) {
            log.error("DB failed for districts, using helper: {}", e.getMessage());
            return getDistricts();
        }
    }

    /**
     * Retrieves a list of distinct markets from the database, falling back to the default markets if the query fails.
     *
     * @return A {@link List} of distinct market names.
     */
    public List<String> GetMarkets() {
        try {
            return locationMappingRepository.findDistinctMarkets();
        } catch (Exception e) {
            log.error("DB failed for markets, using helper: {}", e.getMessage());
            return getMarkets();
        }
    }
}