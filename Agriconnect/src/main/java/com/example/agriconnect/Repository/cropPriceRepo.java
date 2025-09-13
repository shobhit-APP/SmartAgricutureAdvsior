package com.example.agriconnect.Repository;


import com.example.common.Model.Crop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/*
    Using Jpa Repository is an Interface that Provides a Powerful abstractions Layer For Interacting
    with Database it extends The Crud Repository (Create,Read,Update,Delete)and Paging And Sorting
    Repo And You Should Also Make Your Custom Query Method To Work With Relational Database
 */
/*
    Using @Repository Annotation To Add This Repo Component In Spring Boot Container
    To Make It Object....
 */
@Repository
public interface cropPriceRepo extends JpaRepository<Crop,Long > {

    List<Crop> findByStateAndUserDetails1UserId(String state, Long userId);
    //    Creating A Custom Method To Find The Market Details for Specific Crop
    List<Crop> findByUserDetails1UserId(Long userId);
    @Query("SELECT DISTINCT c.state FROM Crop c WHERE c.userDetails1.id = :userId")
    Set<String> findDistinctStatesByUserId(Long userId);
//    List<Crop> findByStateHiContainingIgnoreCaseAndUserDetails1UserId(String state, Long userId);
    //    List<Crop> findByStateEnContainingIgnoreCaseAndUserDetails1UserId(String state, Long userId);
}
