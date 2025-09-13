package com.example.common.Model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crop_diseases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CropDisease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Disease name
    @Column(name = "name_en", nullable = false)
    private String nameEn;

    @Column(name = "name_hi", nullable = false)
    private String nameHi;

    // Cause
    @Column(name = "cause_en")
    private String causeEn;

    @Column(name = "cause_hi")
    private String causeHi;

    // Symptoms
    @Column(name = "symptoms_en", columnDefinition = "TEXT")
    private String symptomsEn;

    @Column(name = "symptoms_hi", columnDefinition = "TEXT")
    private String symptomsHi;

    // Suggestions
    @Column(name = "suggestion_en", columnDefinition = "TEXT")
    private String suggestionEn;

    @Column(name = "suggestion_hi", columnDefinition = "TEXT")
    private String suggestionHi;

    // Crop type in English and Hindi
    @Column(name = "crop_type_en")
    private String cropTypeEn;

    @Column(name = "crop_type_hi")
    private String cropTypeHi;

    // Image path from Azure Blob Storage
    @Column(name = "image_path")
    private String imagePath;

    @ManyToOne
    @JoinColumn(name = "UserId", referencedColumnName = "user_id")
    @ToString.Exclude
    private UserDetails1 userDetails1;

}