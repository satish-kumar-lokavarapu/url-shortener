package com.satish.urlshortener.repository;

import com.satish.urlshortener.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Database access for the url_mapping table.
 * Spring Data JPA writes the SQL for us at runtime,
 * we only declare the methods we need.
 */
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    /**
     * Finds a mapping by its short code.
     * Used when a user opens a short link.
     * Optional means: result can be empty if the code does not exist.
     */
    Optional<UrlMapping> findByShortCode(String shortCode);

    /**
     * Checks if a short code is already used.
     * Used while generating a new code.
     */
    boolean existsByShortCode(String shortCode);

    /**
     * Adds 1 to the click count and sets the last access time,
     * in ONE database statement.
     *
     * Why one statement: if we instead read the count, add 1 in Java,
     * and save it back, two clicks at the same moment could both read
     * the same number and one click would be lost.
     * "click_count = click_count + 1" is done by the database itself,
     * so every click is counted, even under heavy parallel traffic.
     */
    @Modifying
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + 1, u.lastAccessedAt = :accessTime WHERE u.shortCode = :shortCode")
    void incrementClickCount(@Param("shortCode") String shortCode, @Param("accessTime") Instant accessTime);
}