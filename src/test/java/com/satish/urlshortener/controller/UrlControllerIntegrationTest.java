package com.satish.urlshortener.controller;

import com.satish.urlshortener.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests: the full app runs with the H2 test database
 * (profile "test"), and we send real HTTP requests with MockMvc.
 * This checks controller + service + repository + database together.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UrlControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UrlMappingRepository repository;

    @BeforeEach
    void cleanDatabase() {
        // Empty table before each test, so tests do not affect each other
        repository.deleteAll();
    }

    @Test
    void createShortUrlReturns201WithDetails() throws Exception {
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://example.com/page\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value(matchesPattern("[0-9A-Za-z]{7}")))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/page"))
                .andExpect(jsonPath("$.shortUrl").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void redirectReturns302ToOriginalUrl() throws Exception {
        // First create a short URL
        String body = mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://example.com/target\"}"))
                .andReturn().getResponse().getContentAsString();

        // Read the shortCode from the JSON answer
        String shortCode = body.replaceAll(".*\"shortCode\":\"([^\"]+)\".*", "$1");

        // Then open the short link
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/target"));
    }

    @Test
    void unknownShortCodeReturns404() throws Exception {
        mockMvc.perform(get("/nothere1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void invalidUrlReturns400() throws Exception {
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"not-a-url\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void emptyUrlReturns400() throws Exception {
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}