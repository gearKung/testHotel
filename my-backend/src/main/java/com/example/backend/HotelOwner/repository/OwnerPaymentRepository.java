package com.example.backend.HotelOwner.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.HotelOwner.domain.Room;
import com.example.backend.HotelOwner.dto.DailySalesDto;
import com.example.backend.HotelOwner.dto.MonthlySalesDto;
import com.example.backend.payment.domain.Payment;

public interface OwnerPaymentRepository extends JpaRepository<Payment, Long>{

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "JOIN Reservation res ON p.reservation.id = res.id " +
            "JOIN Room r ON res.roomId = r.id " +
            "JOIN Hotel h ON r.hotelId = h.id " +
            "WHERE h.ownerId = :ownerId AND p.status = 'COMPLETED' AND p.approvedAt >= :start AND p.approvedAt < :end")
    long sumCompletedPaymentsByOwnerAndDateRange(@Param("ownerId") Long ownerId, @Param("start") Instant start, @Param("end") Instant end);


    @Query("SELECT new com.example.backend.HotelOwner.dto.DailySalesDto(FUNCTION('DATE_FORMAT', p.approvedAt, '%Y-%m-%d'), SUM(p.amount)) " +
            "FROM Payment p JOIN p.reservation res JOIN Room r ON res.roomId = r.id JOIN Hotel h ON r.hotelId = h.id " +
            "WHERE h.ownerId = :ownerId AND p.status = 'COMPLETED' AND p.approvedAt >= :start AND p.approvedAt < :end " +
            "AND (:hotelId IS NULL OR h.id = :hotelId) " +
            "AND (:roomType IS NULL OR r.roomType = :roomType) " +
            "GROUP BY FUNCTION('DATE_FORMAT', p.approvedAt, '%Y-%m-%d')")
    List<DailySalesDto> findDailySalesByOwner(@Param("ownerId") Long ownerId, @Param("start") Instant start, @Param("end") Instant end, @Param("hotelId") Long hotelId, @Param("roomType") Room.RoomType roomType);


    @Query("SELECT new com.example.backend.HotelOwner.dto.MonthlySalesDto(FUNCTION('DATE_FORMAT', p.approvedAt, '%Y-%m'), SUM(p.amount)) " +
            "FROM Payment p JOIN p.reservation res JOIN Room r ON res.roomId = r.id JOIN Hotel h ON r.hotelId = h.id " +
            "WHERE h.ownerId = :ownerId AND p.status = 'COMPLETED' AND p.approvedAt >= :start AND p.approvedAt < :end " +
            "AND (:hotelId IS NULL OR h.id = :hotelId) " +
            "AND (:roomType IS NULL OR r.roomType = :roomType) " +
            "GROUP BY FUNCTION('DATE_FORMAT', p.approvedAt, '%Y-%m')")
    List<MonthlySalesDto> findMonthlySalesByOwner(@Param("ownerId") Long ownerId, @Param("start") Instant start, @Param("end") Instant end, @Param("hotelId") Long hotelId, @Param("roomType") Room.RoomType roomType);
}
