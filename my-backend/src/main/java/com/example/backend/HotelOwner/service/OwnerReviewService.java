package com.example.backend.HotelOwner.service;

import com.example.backend.HotelOwner.dto.OwnerReviewDto;
import com.example.backend.HotelOwner.dto.OwnerReviewReplyDto;
import com.example.backend.HotelOwner.repository.OwnerReviewReplyRepository;
import com.example.backend.HotelOwner.repository.OwnerReviewRepository;
import com.example.backend.review.domain.Review;
import com.example.backend.review.domain.ReviewReply;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OwnerReviewService {

    private final OwnerReviewRepository reviewRepository;
    private final OwnerReviewReplyRepository reviewReplyRepository;

    public List<OwnerReviewDto> getReviewsForOwner(Long ownerId) {
        return reviewRepository.findByHotelOwnerId(ownerId).stream()
                .map(OwnerReviewDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void createReviewReply(Long reviewId, Long ownerId, OwnerReviewReplyDto replyDto) {
        Review review = findReviewByIdAndOwner(reviewId, ownerId);
        if (review.getReviewReply() != null) {
            throw new IllegalStateException("이미 답변이 등록된 리뷰입니다.");
        }

        ReviewReply reply = ReviewReply.builder()
                .review(review)
                .content(replyDto.getContent())
                .createdAt(LocalDateTime.now())
                .build();
        
        review.setReviewReply(reply);
        reviewReplyRepository.save(reply);
    }

    @Transactional
    public void updateReviewReply(Long replyId, Long ownerId, OwnerReviewReplyDto replyDto) {
        ReviewReply reply = reviewReplyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("해당 답변을 찾을 수 없습니다."));

        if (!reply.getReview().getHotel().getOwnerId().equals(ownerId)) {
            throw new SecurityException("답변을 수정할 권한이 없습니다.");
        }

        reply.setContent(replyDto.getContent());
        reviewReplyRepository.save(reply);
    }

    @Transactional
    public void reportReview(Long reviewId, Long ownerId) {
        Review review = findReviewByIdAndOwner(reviewId, ownerId);
        if (review.getStatus() != Review.ReviewStatus.VISIBLE) {
            throw new IllegalStateException("이미 신고되었거나 처리된 리뷰입니다.");
        }
        review.setStatus(Review.ReviewStatus.REPORTED);
        reviewRepository.save(review);
    }

    private Review findReviewByIdAndOwner(Long reviewId, Long ownerId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("해당 리뷰를 찾을 수 없습니다."));

        if (!review.getHotel().getOwnerId().equals(ownerId)) {
            throw new SecurityException("리뷰에 접근할 권한이 없습니다.");
        }
        return review;
    }
}