package com.example.backend.HotelOwner.service;

import com.example.backend.HotelOwner.domain.Amenity;
import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.domain.HotelAmenity;
import com.example.backend.HotelOwner.domain.HotelImage;
import com.example.backend.HotelOwner.dto.HotelDto;
import com.example.backend.HotelOwner.repository.AmenityRepository;
import com.example.backend.HotelOwner.repository.HotelAmenityRepository;
import com.example.backend.HotelOwner.repository.HotelImageRepository;
import com.example.backend.HotelOwner.repository.OwnerHotelRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerHotelService {

    private final OwnerHotelRepository hotelRepository;
    private final AmenityRepository amenityRepository;
    private final HotelAmenityRepository hotelAmenityRepository;
    private final HotelImageRepository hotelImageRepository;
    private final FileStorageService fileStorageService;

    public List<HotelDto> getMyHotels(Long ownerId) {
        return hotelRepository.findByOwnerId(ownerId).stream()
                .map(this::mapHotelToDto)
                .collect(Collectors.toList());
    }

    public List<Amenity> getAllAmenities() {
        return amenityRepository.findAll();
    }

    @Transactional
    public Hotel createHotel(HotelDto hotelDto, List<MultipartFile> files, Long ownerId) {
        hotelDto.setOwnerId(ownerId); // 토큰에서 추출한 ownerId 설정
        Hotel hotel = hotelDto.toEntity();
        hotelRepository.save(hotel);

        updateHotelAmenities(hotel, hotelDto.getAmenityIds());
        updateHotelImages(hotel, hotelDto.getImageUrls(), files);

        return hotel;
    }

    @Transactional
    public Hotel updateHotel(Long hotelId, HotelDto hotelDto, List<MultipartFile> files, Long ownerId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid hotel ID: " + hotelId));

        if (!hotel.getOwnerId().equals(ownerId)) {
            throw new SecurityException("호텔을 수정할 권한이 없습니다.");
        }

        hotel.updateDetails(hotelDto);
        updateHotelAmenities(hotel, hotelDto.getAmenityIds());

        // 기존 이미지 중 삭제할 이미지 처리
        List<String> existingUrls = hotel.getImages().stream().map(HotelImage::getImageUrl).collect(Collectors.toList());
        List<String> urlsToKeep = hotelDto.getImageUrls();
        existingUrls.stream()
                .filter(url -> !urlsToKeep.contains(url))
                .forEach(url -> {
                    hotelImageRepository.deleteByImageUrl(url);
                    fileStorageService.deleteFile(url);
                });

        updateHotelImages(hotel, hotelDto.getImageUrls(), files);

        return hotelRepository.save(hotel);
    }

    @Transactional
    public void deleteHotel(Long hotelId, Long ownerId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid hotel ID: " + hotelId));

        if (!hotel.getOwnerId().equals(ownerId)) {
            throw new SecurityException("호텔을 삭제할 권한이 없습니다.");
        }
        // 이미지 파일 먼저 삭제 후 호텔 삭제
        hotel.getImages().forEach(image -> fileStorageService.deleteFile(image.getImageUrl()));
        hotelRepository.delete(hotel);
    }

    private void updateHotelAmenities(Hotel hotel, List<Long> amenityIds) {
        hotelAmenityRepository.deleteByHotel(hotel);
        if (amenityIds != null && !amenityIds.isEmpty()) {
            List<HotelAmenity> hotelAmenities = amenityIds.stream()
                    .map(amenityId -> {
                        Amenity amenity = amenityRepository.findById(amenityId).orElse(null);
                        return (amenity != null) ? new HotelAmenity(hotel, amenity) : null;
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            hotelAmenityRepository.saveAll(hotelAmenities);
        }
    }

    private void updateHotelImages(Hotel hotel, List<String> imageUrls, List<MultipartFile> files) {
        // 기존 이미지 순서 업데이트 및 새 이미지 추가
        hotel.getImages().clear();
        int order = 0;
        if (imageUrls != null) {
            for (String url : imageUrls) {
                hotel.getImages().add(new HotelImage(hotel, url, order++));
            }
        }
        if (files != null) {
            for (MultipartFile file : files) {
                String fileName = fileStorageService.storeFile(file);
                hotel.getImages().add(new HotelImage(hotel, fileName, order++));
            }
        }
    }

    private HotelDto mapHotelToDto(Hotel hotel) {
        HotelDto dto = new HotelDto();
        dto.setId(hotel.getId());
        dto.setName(hotel.getName());
        dto.setAddress(hotel.getAddress());
        // ... 필요한 다른 필드들 ...
        dto.setImageUrls(hotel.getImages().stream()
                .map(image -> "/uploads/" + image.getImageUrl())
                .collect(Collectors.toList()));
        return dto;
    }
}