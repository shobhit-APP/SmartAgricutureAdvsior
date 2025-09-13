package com.example.common.Model;

import com.example.common.Model.UserDetails1;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "Crop")
public class Crop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    @JsonAlias("फ़सल_आईडी")   // Hindi alias
    private Long id;

    @Column(name = "state")
    @JsonProperty("state")
    @JsonAlias("राज्य")
    private String state;

    @Column(name = "district")
    @JsonProperty("district")
    @JsonAlias("ज़िला")
    private String district;

    @Column(name = "market")
    @JsonProperty("market")
    @JsonAlias("बाज़ार")
    private String market;

    @Column(name = "crop_name")
    @JsonProperty("crop_name")
    @JsonAlias("फ़सल_का_नाम")
    private String cropName;

    @Column(name = "arrival_date")
    @JsonProperty("arrival_date")
    @JsonAlias("आगमन_तारीख़")
    private LocalDate arrivalDate;

    @Column(name = "min_price")
    @JsonProperty("min_price")
    @JsonAlias("न्यूनतम_कीमत")
    private Double minPrice;

    @Column(name = "max_price")
    @JsonProperty("max_price")
    @JsonAlias("अधिकतम_कीमत")
    private Double maxPrice;

    @Column(name = "suggested_price")
    @JsonProperty("suggested_price")
    @JsonAlias("सुझाई_गई_कीमत")
    private Double suggestedPrice;

    @Column(name = "suggested_price_second")
    @JsonProperty("suggested_price_second")
    @JsonAlias("दूसरी_सुझाई_गई_कीमत")
    private Double suggestedPriceSecond;

    @Column(name = "suggested_price_third")
    @JsonProperty("suggested_price_third")
    @JsonAlias("तीसरी_सुझाई_गई_कीमत")
    private Double suggestedPriceThird;

    @Column(name = "best_price")
    @JsonProperty("best_price")
    @JsonAlias("सर्वश्रेष्ठ_कीमत")
    private Double bestPrice;

    @ManyToOne
    @JoinColumn(name = "UserId", referencedColumnName = "user_id")
    @JsonProperty("user_details")
    @JsonAlias("उपयोगकर्ता_विवरण")
    @ToString.Exclude
    private UserDetails1 userDetails1;
}
