package com.example.backend.HotelOwner.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.example.backend.review.domain.Review;;

@Data
@Builder
public class OwnerReviewDto {
    private Long id;
    private Long replyId;
    private String authorName;
    private String hotelName;
    private int rating;
    private String content;
    private List<String> imageUrls;
    private String replyContent;
    private LocalDateTime createdAt;
    private boolean replied;
    private String status;

    public static OwnerReviewDto fromEntity(Review review) {
        return OwnerReviewDto.builder()
                .id(review.getId())
                .replyId(review.getReply() != null ? review.getReply().getId() : null)
                .authorName(review.getUser().getName())
                .hotelName(review.getHotel().getName())
                .rating(review.getRating())
                .content(review.getContent())
                .imageUrls(review.getImages().stream()
                        .map(image -> image.getImageUrl())
                        .collect(Collectors.toList()))
                .replyContent(review.getReply() != null ? review.getReply().getContent() : null)
                .replied(review.getReply() != null)
                .createdAt(review.getCreatedAt())
                .status(review.getStatus().name())
                .build();
    }
}