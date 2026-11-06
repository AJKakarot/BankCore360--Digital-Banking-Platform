package com.bankcore360.controller;

import com.bankcore360.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DatabaseHealthController.class)
@Import(SecurityConfig.class)
class DatabaseHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser
    @DisplayName("GET /api/health/db should return 200 UP when database query succeeds")
    void shouldReturnUpWhenDatabaseQuerySucceeds() throws Exception {
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(1);

        mockMvc.perform(get("/api/health/db")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("PostgreSQL"))
                .andExpect(jsonPath("$.connection").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Database connection and query verification succeeded"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/health/db should return 503 DOWN without leaking secrets when query fails")
    void shouldReturnDownSafelyWhenQueryFails() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
                .thenThrow(new DataAccessResourceFailureException("Connection refused to secret-db-host"));

        mockMvc.perform(get("/api/health/db")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.database").value("PostgreSQL"))
                .andExpect(jsonPath("$.connection").value("FAILED"))
                .andExpect(jsonPath("$.message").value("Database connectivity check failed. Check server logs for details."));
    }
}
