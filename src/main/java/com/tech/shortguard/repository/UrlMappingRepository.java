package com.tech.shortguard.repository;

import com.tech.shortguard.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long>{

    Optional<UrlMapping> findByShortCode(String shortCode);


    boolean existsByShortCode(String shortCode);

    Optional<UrlMapping> findByLongUrl(String longUrl);

    void deleteByShortCode(String shortCode);

    @Modifying
    @Transactional
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + 1 WHERE u.shortCode = :shortCode")
    void incrementClickCount(@Param("shortCode") String shortCode);
}
