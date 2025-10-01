package com.example.backend.HotelOwner.service;

import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.domain.Room;
import com.example.backend.HotelOwner.repository.HotelRepository;
import com.example.backend.HotelOwner.repository.OwnerHotelRepository;
import com.example.backend.HotelOwner.repository.OwnerReservationRepository;
import com.example.backend.HotelOwner.repository.OwnerRoomRepository;
import com.example.backend.HotelOwner.repository.OwnerUserRepository;
import com.example.backend.HotelOwner.repository.RoomRepository;
import com.example.backend.authlogin.domain.User;
import com.example.backend.authlogin.repository.LoginRepository;
import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.hotel_reservation.domain.ReservationStatus;
import com.example.backend.hotel_reservation.dto.ReservationDtos;
import com.example.backend.hotel_reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OwnerReservationService {

    private final OwnerReservationRepository reservationRepository;
    private final OwnerUserRepository userRepository;
    private final OwnerRoomRepository roomRepository;
    private final OwnerHotelRepository hotelRepository;

    public List<ReservationDtos.OwnerReservationResponse> getReservationsForOwner(Long ownerId) {
        List<Reservation> reservations = reservationRepository.findReservationsByHotelOwnerId(ownerId);

        List<Long> userIds = reservations.stream().map(Reservation::getUserId).distinct().collect(Collectors.toList());
        List<Long> roomIds = reservations.stream().map(Reservation::getRoomId).distinct().collect(Collectors.toList());

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream() // 수정
                .collect(Collectors.toMap(User::getId, user -> user));
        Map<Long, Room> roomMap = roomRepository.findAllById(roomIds).stream()
                .collect(Collectors.toMap(Room::getId, room -> room));

        List<Long> hotelIds = roomMap.values().stream().map(Room::getHotelId).distinct().collect(Collectors.toList());
        Map<Long, Hotel> hotelMap = hotelRepository.findAllById(hotelIds).stream() // 수정
                .collect(Collectors.toMap(Hotel::getId, hotel -> hotel));

        return reservations.stream()
            .filter(r -> r.getStatus() != ReservationStatus.PENDING)
            .map(r -> {
                User user = userMap.get(r.getUserId());
                Room room = roomMap.get(r.getRoomId());
                if (room != null) {
                    Hotel hotel = hotelMap.get(room.getHotelId());
                    // Reservation, User, Room, Hotel 정보를 모두 사용하여 DTO 생성
                    return ReservationDtos.OwnerReservationResponse.fromEntity(r, user, room, hotel);
                }
                return null;
            })
            .filter(dto -> dto != null)
            .collect(Collectors.toList());
    }

    @Transactional
    public void cancelReservationByOwner(Long reservationId, Long ownerId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 예약을 찾을 수 없습니다."));

        // 해당 업주의 예약이 맞는지 확인
        boolean isOwnerReservation = reservationRepository.isReservationForOwner(reservationId, ownerId);
        if (!isOwnerReservation) {
            throw new SecurityException("예약을 취소할 권한이 없습니다.");
        }

        // 체크인 날짜가 오늘 또는 과거이면 취소 불가
        if (reservation.getCheckInDate().isBefore(LocalDate.now().plusDays(1))) {
            throw new IllegalStateException("체크인 날짜가 지났거나 오늘인 예약은 취소할 수 없습니다.");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        // (필요 시) 재고 반환 로직 추가
    }
}