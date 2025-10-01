package com.example.backend.HotelOwner.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.hotel_reservation.domain.ReservationStatus;

public interface OwnerReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findTop500ByStatusAndExpiresAtBefore(ReservationStatus status, Instant cutoff);

    @Query("SELECT r FROM Reservation r JOIN Room ro ON r.roomId = ro.id JOIN Hotel h ON ro.hotelId = h.id WHERE h.ownerId = :ownerId")
    List<Reservation> findReservationsByHotelOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Reservation r JOIN Room ro ON r.roomId = ro.id JOIN Hotel h ON ro.hotelId = h.id WHERE r.id = :reservationId AND h.ownerId = :ownerId")
    boolean isReservationForOwner(@Param("reservationId") Long reservationId, @Param("ownerId") Long ownerId);

    @Query("SELECT r FROM Reservation r WHERE r.roomId IN " +
           "(SELECT ro.id FROM Room ro WHERE ro.hotel.owner.id = :ownerId) " +
           "ORDER BY r.createdAt DESC")
    List<Reservation> findAllByHotelOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT r FROM Reservation r JOIN Room ro ON r.roomId = ro.id JOIN Hotel h ON ro.hotelId = h.id " +
           "WHERE h.ownerId = :ownerId AND r.createdAt >= :threeDaysAgo ORDER BY r.createdAt DESC")
    List<Reservation> findRecentReservationsForOwner(@Param("ownerId") Long ownerId, @Param("threeDaysAgo") LocalDateTime threeDaysAgo, Pageable pageable);

    // 2. 오늘 체크인 목록을 가져오는 쿼리
    @Query("SELECT r FROM Reservation r JOIN Room ro ON r.roomId = ro.id JOIN Hotel h ON ro.hotelId = h.id " +
           "WHERE h.ownerId = :ownerId AND r.checkInDate = CURRENT_DATE AND r.status = 'COMPLETED'")
    List<Reservation> findCheckInsForOwnerByDateRange(@Param("ownerId") Long ownerId, @Param("startOfDay") Instant startOfDay, @Param("endOfDay") Instant endOfDay);

    // 3. 오늘 체크아웃 목록을 가져오는 쿼리
    @Query("SELECT r FROM Reservation r JOIN Room ro ON r.roomId = ro.id JOIN Hotel h ON ro.hotelId = h.id " +
           "WHERE h.ownerId = :ownerId AND r.checkOutDate = CURRENT_DATE AND r.status = 'COMPLETED'")
    List<Reservation> findCheckOutsForOwnerByDateRange(@Param("ownerId") Long ownerId, @Param("startOfDay") Instant startOfDay, @Param("endOfDay") Instant endOfDay);
}
