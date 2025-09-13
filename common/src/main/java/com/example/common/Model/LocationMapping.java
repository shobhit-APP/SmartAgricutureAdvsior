package com.example.common.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Table(name = "location_mapping")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "state")
    private String state;

    @Column(name = "district")
    private String district;

    @Column(name = "market")
    private String market;

    @Column(name = "crop_name")
    private String cropName;

    @Column(name = "arrival_date")
    private String arrivalDate;

    @Column(name = "min_price")
    private Double minPrice;

    @Column(name = "max_price")
    private Double maxPrice;

    @Column(name = "latitude")
    private Double lat;

    @Column(name = "longitude")
    private Double lon;
}