package com.smartagriculture.community.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "experts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Expert name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Expertise field is required")
    @Column(nullable = false)
    private String field;

    @Email(message = "Email must be valid")
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "organization")
    private String organization;

    @Column(name = "profile_image_url")
    private String profileImageUrl;
}
