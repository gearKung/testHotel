package com.example.backend.HotelOwner.controller;

import com.example.backend.HotelOwner.dto.OwnerReviewDto;
import com.example.backend.HotelOwner.dto.OwnerReviewReplyDto;
import com.example.backend.HotelOwner.service.OwnerReviewService;
import com.example.backend.authlogin.config.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/owner/reviews")
@RequiredArgsConstructor
public class OwnerReviewController {

    private final OwnerReviewService reviewService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<List<OwnerReviewDto>> getMyReviews(@RequestHeader("Authorization") String authHeader) {
        Long ownerId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(reviewService.getReviewsForOwner(ownerId));
    }

    @PostMapping("/{reviewId}/reply")
    public ResponseEntity<Void> createReply(@PathVariable Long reviewId, @RequestBody OwnerReviewReplyDto replyDto, @RequestHeader("Authorization") String authHeader) {
        Long ownerId = getUserIdFromToken(authHeader);
        reviewService.createReviewReply(reviewId, ownerId, replyDto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/replies/{replyId}")
    public ResponseEntity<Void> updateReply(@PathVariable Long replyId, @RequestBody OwnerReviewReplyDto replyDto, @RequestHeader("Authorization") String authHeader) {
        Long ownerId = getUserIdFromToken(authHeader);
        reviewService.updateReviewReply(replyId, ownerId, replyDto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{reviewId}/report")
    public ResponseEntity<Void> reportReview(@PathVariable Long reviewId, @RequestHeader("Authorization") String authHeader) {
        Long ownerId = getUserIdFromToken(authHeader);
        reviewService.reportReview(reviewId, ownerId);
        return ResponseEntity.ok().build();
    }

    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.substring(7);
        Claims claims = jwtUtil.extractAllClaims(token);
        return claims.get("userId", Long.class);
    }
}