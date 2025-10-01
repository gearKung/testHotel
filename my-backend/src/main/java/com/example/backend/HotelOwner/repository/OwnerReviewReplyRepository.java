package com.example.backend.HotelOwner.repository;

import com.example.backend.review.domain.ReviewReply;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerReviewReplyRepository extends JpaRepository<ReviewReply, Long> {
}