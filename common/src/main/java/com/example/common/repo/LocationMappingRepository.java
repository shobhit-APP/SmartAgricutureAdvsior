package com.example.common.repo;

import com.example.common.Model.LocationMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationMappingRepository extends JpaRepository<LocationMapping, Long> {

        @Query("SELECT l FROM LocationMapping l WHERE " +
                        "ABS(l.lat - :latitude) < 0.1 AND ABS(l.lon - :longitude) < 0.1 " +
                        "ORDER BY (ABS(l.lat - :latitude) + ABS(l.lon - :longitude)) ASC")
        List<LocationMapping> findByLatitudeAndLongitude(
                        @Param("latitude") Double latitude,
                        @Param("longitude") Double longitude);

        @Query("SELECT DISTINCT l.state FROM LocationMapping l ORDER BY l.state ASC")
        List<String> findDistinctStates();

        @Query("SELECT DISTINCT l.district FROM LocationMapping l ORDER BY l.district ASC")
        List<String> findDistinctDistricts();

        @Query("SELECT DISTINCT l.market FROM LocationMapping l ORDER BY l.market ASC")
        List<String> findDistinctMarkets();
}
