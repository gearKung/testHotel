package com.example.backend.HotelOwner.controller;

import com.example.backend.HotelOwner.dto.DailySalesDto;
import com.example.backend.HotelOwner.dto.MonthlySalesDto;
import com.example.backend.HotelOwner.dto.OwnerDashboardSummaryDto;
import com.example.backend.HotelOwner.dto.OwnerReviewDto;
import com.example.backend.HotelOwner.dto.OwnerSalesChartRequestDto;
import com.example.backend.HotelOwner.service.OwnerDashboardService;
import com.example.backend.authlogin.config.JwtUtil;
import com.example.backend.hotel_reservation.dto.ReservationDtos;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/owner/dashboard")
@RequiredArgsConstructor
public class OwnerDashboardController {

    private final OwnerDashboardService dashboardService;
    private final JwtUtil jwtUtil;

    @GetMapping("/summary")
    public ResponseEntity<OwnerDashboardSummaryDto> getSalesSummary(@RequestHeader("Authorization") String authHeader) {
        Long ownerId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(dashboardService.getSalesSummary(ownerId));
    }

    @PostMapping("/daily-sales")
    public ResponseEntity<List<DailySalesDto>> getDailySales(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody OwnerSalesChartRequestDto requestDto) {
        Long ownerId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(dashboardService.getDailySales(ownerId, requestDto));
    }

    @PostMapping("/monthly-sales")
    public ResponseEntity<List<MonthlySalesDto>> getMonthlySales(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody OwnerSalesChartRequestDto requestDto) {
        Long ownerId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(dashboardService.getMonthlySales(ownerId, requestDto));
    }
    
    @GetMapping("/activity")
    public ResponseEntity<ReservationDtos.DashboardActivityResponse> getDashboardActivity(@RequestHeader("Authorization") String authHeader) {
        Long ownerId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(dashboardService.getDashboardActivity(ownerId));
    }

    @GetMapping("/recent-reviews")
    public ResponseEntity<List<OwnerReviewDto>> getRecentReviews(@RequestHeader("Authorization") String authHeader) {
        Long ownerId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(dashboardService.getRecentReviewsByOwner(ownerId));
    }

    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.substring(7);
        Claims claims = jwtUtil.extractAllClaims(token);
        return claims.get("userId", Long.class);
    }
}