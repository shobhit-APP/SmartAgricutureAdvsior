
   package com.example.Authentication.repository;

import com.example.Authentication.Model.Expert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

   @Repository
   public interface ExpertRepository extends JpaRepository<Expert, Long> {
       Optional<Expert> findByUserId(Long userId);
       List<Expert> findByPendingReviewTrue();
       List<Expert> findByIsVerifiedTrue();
       List<Expert> findByIsVerifiedFalseAndPendingReviewFalse();
       boolean existsByUserId(Long userId);
       List<Expert> findByIsVerifiedTrueAndPendingReviewFalse();
       int countByPendingReviewTrue();
       int countByIsVerifiedTrue();
   }
