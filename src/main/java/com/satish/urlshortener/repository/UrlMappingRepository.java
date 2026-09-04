package com.satish.urlshortener.repository;

import com.satish.urlshortener.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;

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
}