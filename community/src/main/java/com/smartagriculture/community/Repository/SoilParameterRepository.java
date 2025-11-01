package com.smartagriculture.community.Repository;


import com.smartagriculture.community.Model.SoilParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SoilParameterRepository extends JpaRepository<SoilParameter, Long> {
}