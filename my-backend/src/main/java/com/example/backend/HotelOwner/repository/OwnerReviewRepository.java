package com.example.backend.HotelOwner.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.review.domain.Review;

public interface OwnerReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT r FROM Review r WHERE r.hotel.ownerId = :ownerId ORDER BY r.createdAt DESC")
    List<Review> findByHotelOwnerId(@Param("ownerId") Long ownerId);
}
