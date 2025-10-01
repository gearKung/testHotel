package com.example.backend.HotelOwner.controller;

import com.example.backend.HotelOwner.service.OwnerReservationService;
import com.example.backend.authlogin.config.JwtUtil;
import com.example.backend.hotel_reservation.dto.ReservationDtos;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/owner/reservations")
@RequiredArgsConstructor
public class OwnerReservationController {

    private final OwnerReservationService reservationService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<List<ReservationDtos.OwnerReservationResponse>> getMyReservations(@RequestHeader("Authorization") String authHeader) {
        Long ownerId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(reservationService.getReservationsForOwner(ownerId));
    }

    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable Long reservationId,
            @RequestHeader("Authorization") String authHeader) {
        Long ownerId = getUserIdFromToken(authHeader);
        reservationService.cancelReservationByOwner(reservationId, ownerId);
        return ResponseEntity.ok().build();
    }

    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.substring(7);
        Claims claims = jwtUtil.extractAllClaims(token);
        return claims.get("userId", Long.class);
    }
}