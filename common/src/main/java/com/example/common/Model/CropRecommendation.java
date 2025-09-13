package com.example.common.Model;

import com.example.common.Model.UserDetails1;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "CropRecommendation")
public class CropRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    @JsonAlias("फ़सल_सिफारिश_आईडी")
    private int id;

    @Column(name = "N")
    @JsonProperty("N")
    @JsonAlias("नाइट्रोजन")
    private float N;

    @Column(name = "P")
    @JsonProperty("P")
    @JsonAlias("फॉस्फोरस")
    private float P;

    @Column(name = "K")
    @JsonProperty("K")
    @JsonAlias("पोटैशियम")
    private float K;

    @Column(name = "temperature")
    @JsonProperty("temperature")
    @JsonAlias("तापमान")
    private float temperature;

    @Column(name = "humidity")
    @JsonProperty("humidity")
    @JsonAlias("आर्द्रता")
    private float humidity;

    @Column(name = "ph")
    @JsonProperty("ph")
    @JsonAlias("पीएच")
    private float ph;

    @Column(name = "rainfall")
    @JsonProperty("rainfall")
    @JsonAlias("वर्षा")
    private float rainfall;

    @Column(name = "predicted_crop")
    @JsonProperty("predicted_crop")
    @JsonAlias("अनुमानित_फसल")
    private String predictedCrop;

    @Column(name = "predicted_crophindi")
    @JsonProperty("predicted_crophindi")
    @JsonAlias("अनुमानित_फसल_हिंदी")
    private String predictedCropHindi;

    @Column(name = "hindi_description", length = 1000)
    @JsonProperty("hindi_description")
    @JsonAlias("विवरण_हिंदी")
    private String hindiDescription;

    @Column(name = "english_description", length = 1000)
    @JsonProperty("english_description")
    @JsonAlias("विवरण_अंग्रेज़ी")
    private String englishDescription;

    @ManyToOne
    @JoinColumn(name = "UserId", referencedColumnName = "user_id")
    @JsonProperty("user_details")
    @JsonAlias("उपयोगकर्ता_विवरण")
    @ToString.Exclude
    private UserDetails1 userDetails1;
}
