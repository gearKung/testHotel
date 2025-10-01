package com.example.backend.HotelOwner.dto;

import lombok.Data;

@Data
public class OwnerReviewReplyDto {
    private String content;
    private Long ownerId;
}