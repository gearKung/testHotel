package com.example.backend.HotelOwner.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.authlogin.domain.User;

@Repository
public interface OwnerUserRepository extends JpaRepository<User, Long> {

    public Optional<User> findById(Long userId);
}
