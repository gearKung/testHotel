package com.example.backend.HotelOwner.controller;

import com.example.backend.HotelOwner.domain.Amenity;
import com.example.backend.HotelOwner.dto.HotelDto;
import com.example.backend.HotelOwner.service.OwnerHotelService;
import com.example.backend.authlogin.config.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/owner/hotels")
@RequiredArgsConstructor
public class OwnerHotelController {

    private final OwnerHotelService hotelService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @GetMapping("/my-hotels")
    public ResponseEntity<List<HotelDto>> getMyHotels(@RequestHeader("Authorization") String authHeader) {
        Long ownerId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(hotelService.getMyHotels(ownerId));
    }

    @GetMapping("/amenities")
    public ResponseEntity<List<Amenity>> getAllAmenities() {
        return ResponseEntity.ok(hotelService.getAllAmenities());
    }

    @PostMapping
    public ResponseEntity<Void> createHotel(@RequestPart("hotel") String hotelDtoString,
                                            @RequestPart(value = "files", required = false) List<MultipartFile> files,
                                            @RequestHeader("Authorization") String authHeader) throws IOException {
        HotelDto hotelDto = objectMapper.readValue(hotelDtoString, HotelDto.class);
        Long ownerId = getUserIdFromToken(authHeader);
        hotelService.createHotel(hotelDto, files, ownerId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{hotelId}")
    public ResponseEntity<Void> updateHotel(@PathVariable Long hotelId,
                                            @RequestPart("hotel") String hotelDtoString,
                                            @RequestPart(value = "files", required = false) List<MultipartFile> files,
                                            @RequestHeader("Authorization") String authHeader) throws IOException {
        HotelDto hotelDto = objectMapper.readValue(hotelDtoString, HotelDto.class);
        Long ownerId = getUserIdFromToken(authHeader);
        hotelService.updateHotel(hotelId, hotelDto, files, ownerId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{hotelId}")
    public ResponseEntity<Void> deleteHotel(@PathVariable Long hotelId,
                                            @RequestHeader("Authorization") String authHeader) {
        Long ownerId = getUserIdFromToken(authHeader);
        hotelService.deleteHotel(hotelId, ownerId);
        return ResponseEntity.ok().build();
    }
    
    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.substring(7);
        Claims claims = jwtUtil.extractAllClaims(token);
        return claims.get("userId", Long.class);
    }
}