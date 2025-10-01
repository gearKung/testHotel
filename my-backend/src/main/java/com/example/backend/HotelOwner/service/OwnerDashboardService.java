package com.example.backend.HotelOwner.service;

import com.example.backend.HotelOwner.domain.Room;
import com.example.backend.HotelOwner.dto.DailySalesDto;
import com.example.backend.HotelOwner.dto.MonthlySalesDto;
import com.example.backend.HotelOwner.dto.OwnerDashboardSummaryDto;
import com.example.backend.HotelOwner.dto.OwnerReviewDto;
import com.example.backend.HotelOwner.dto.OwnerSalesChartRequestDto;
import com.example.backend.HotelOwner.repository.OwnerPaymentRepository;
import com.example.backend.HotelOwner.repository.OwnerReservationRepository;
import com.example.backend.HotelOwner.repository.OwnerReviewRepository;
import com.example.backend.HotelOwner.repository.OwnerRoomRepository;
import com.example.backend.HotelOwner.repository.OwnerUserRepository;
import com.example.backend.authlogin.domain.User;
import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.hotel_reservation.dto.ReservationDtos;
import com.example.backend.hotel_reservation.repository.ReservationRepository;
import com.example.backend.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OwnerDashboardService {

    private final OwnerUserRepository userRepository;
    private final OwnerReservationRepository reservationRepository;
    private final OwnerPaymentRepository paymentRepository;
    private final OwnerReviewRepository reviewRepository;
    private final OwnerRoomRepository roomRepository; // Room 정보 조회를 위해 추가

    // 대시보드 상단 카드 (매출 요약)
    public OwnerDashboardSummaryDto getSalesSummary(Long ownerId) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        long todaySales = getSalesForDate(ownerId, today);
        long yesterdaySales = getSalesForDate(ownerId, yesterday);

        LocalDate thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate thisWeekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        long thisWeekSales = getSalesForDateRange(ownerId, thisWeekStart, thisWeekEnd);

        LocalDate lastWeekStart = thisWeekStart.minusWeeks(1);
        LocalDate lastWeekEnd = thisWeekEnd.minusWeeks(1);
        long lastWeekSales = getSalesForDateRange(ownerId, lastWeekStart, lastWeekEnd);

        LocalDate thisMonthStart = today.withDayOfMonth(1);
        LocalDate thisMonthEnd = today.with(TemporalAdjusters.lastDayOfMonth());
        long thisMonthSales = getSalesForDateRange(ownerId, thisMonthStart, thisMonthEnd);

        LocalDate lastMonth = today.minusMonths(1);
        LocalDate lastMonthStart = lastMonth.withDayOfMonth(1);
        LocalDate lastMonthEnd = lastMonth.with(TemporalAdjusters.lastDayOfMonth());
        long lastMonthSales = getSalesForDateRange(ownerId, lastMonthStart, lastMonthEnd);

        return OwnerDashboardSummaryDto.builder()
                .todaySales(todaySales)
                .thisWeekSales(thisWeekSales)
                .thisMonthSales(thisMonthSales)
                .salesChangeVsYesterday(calculateChangePercentage(todaySales, yesterdaySales))
                .salesChangeVsLastWeek(calculateChangePercentage(thisWeekSales, lastWeekSales))
                .salesChangeVsLastMonth(calculateChangePercentage(thisMonthSales, lastMonthSales))
                .build();
    }

    private long getSalesForDate(Long ownerId, LocalDate date) {
        Instant start = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return paymentRepository.sumCompletedPaymentsByOwnerAndDateRange(ownerId, start, end, Instant.now());
    }

    private long getSalesForDateRange(Long ownerId, LocalDate startDate, LocalDate endDate) {
        Instant start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return paymentRepository.sumCompletedPaymentsByOwnerAndDateRange(ownerId, start, end, Instant.now());
    }

    private double calculateChangePercentage(long current, long previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return ((double) (current - previous) / previous) * 100;
    }

    // 일별 매출 차트 데이터
    public List<DailySalesDto> getDailySales(Long ownerId, OwnerSalesChartRequestDto requestDto) {
        Instant start = requestDto.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = requestDto.getEndDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return paymentRepository.findDailySalesByOwner(ownerId, start, end, requestDto.getHotelId(), requestDto.getRoomType());
    }

    // 월별 매출 차트 데이터
    public List<MonthlySalesDto> getMonthlySales(Long ownerId, OwnerSalesChartRequestDto requestDto) {
        Instant start = requestDto.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = requestDto.getEndDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return paymentRepository.findMonthlySalesByOwner(ownerId, start, end, requestDto.getHotelId(), requestDto.getRoomType());
    }

    // 오늘의 현황, 최근 예약 데이터
    public ReservationDtos.DashboardActivityResponse getDashboardActivity(Long ownerId) {
        LocalDate today = LocalDate.now();
        Instant startOfDay = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Reservation> checkIns = reservationRepository.findCheckInsForOwnerByDateRange(ownerId, startOfDay, endOfDay);
        List<Reservation> checkOuts = reservationRepository.findCheckOutsForOwnerByDateRange(ownerId, startOfDay, endOfDay);
        LocalDateTime threeDaysAgo = LocalDateTime.now().minus(3, ChronoUnit.DAYS);
        List<Reservation> recentReservations = reservationRepository.findRecentReservationsForOwner(ownerId, threeDaysAgo, PageRequest.of(0, 5));

        List<Reservation> allReservations = Stream.of(checkIns, checkOuts, recentReservations).flatMap(List::stream).collect(Collectors.toList());
        if (allReservations.isEmpty()) {
            return new ReservationDtos.DashboardActivityResponse(List.of(), List.of(), List.of());
        }

        List<Long> userIds = allReservations.stream().map(Reservation::getUserId).distinct().collect(Collectors.toList());
        List<Long> roomIds = allReservations.stream().map(Reservation::getRoomId).distinct().collect(Collectors.toList());

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Room> roomMap = roomRepository.findAllById(roomIds).stream().collect(Collectors.toMap(Room::getId, Function.identity()));

        Function<Reservation, ReservationDtos.OwnerReservationResponse> toDto = r -> {
            User user = userMap.get(r.getUserId());
            Room room = roomMap.get(r.getRoomId());
            return (user != null && room != null) ? ReservationDtos.OwnerReservationResponse.fromEntity(r, user, room) : null;
        };
        
        return new ReservationDtos.DashboardActivityResponse(
            checkIns.stream().map(toDto).filter(dto -> dto != null).collect(Collectors.toList()),
            checkOuts.stream().map(toDto).filter(dto -> dto != null).collect(Collectors.toList()),
            recentReservations.stream().map(toDto).filter(dto -> dto != null).collect(Collectors.toList())
        );
    }

    // 최근 리뷰 데이터
    public List<OwnerReviewDto> getRecentReviewsByOwner(Long ownerId) {
        return reviewRepository.findByHotelOwnerId(ownerId).stream()
                .map(OwnerReviewDto::fromEntity)
                .collect(Collectors.toList());
    }
}