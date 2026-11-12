package com.gymnasion.tcc.repository;

import com.gymnasion.tcc.domain.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {
    Optional<Lead> findByEmail(String email);
}
