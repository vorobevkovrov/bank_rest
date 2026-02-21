package com.example.bankcards.controller;

import com.example.bankcards.config.JwtAuthenticationFilter;
import com.example.bankcards.dto.request.AuthenticationRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.response.AuthenticationResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.security.UserDetailsServiceImpl;
import com.example.bankcards.service.AuthenticationService;
import com.example.bankcards.util.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthenticationController.class)
@ActiveProfiles("test")
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private JwtService jwtService;

    // Добавляем MockBean для зависимостей безопасности
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private RegisterRequest registerRequest;
    private AuthenticationRequest authRequest;
    private AuthenticationResponse authResponse;
    private User mockUser;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .username("testuser")
                .password("Password123")
                .role(Role.USER)
                .build();

        authRequest = AuthenticationRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        authResponse = AuthenticationResponse.builder()
                .accessToken("access-token-here")
                .refreshToken("refresh-token-here")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .username("testuser")
                .role(Role.USER)
                .message("Authentication successful")
                .build();

        mockUser = User.builder()
                .id(1L)
                .username("testuser")
                .role(Role.USER)
                .build();
    }

    @Test
    void register_ShouldReturnAuthenticationResponse() throws Exception {
        when(authenticationService.register(any(RegisterRequest.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-here"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-here"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(authenticationService).register(any(RegisterRequest.class));
    }

    @Test
    void authenticate_ShouldReturnAuthenticationResponse() throws Exception {
        when(authenticationService.authenticate(any(AuthenticationRequest.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-here"));

        verify(authenticationService).authenticate(any(AuthenticationRequest.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void registerAdmin_ShouldCreateAdminWhenCurrentUserIsAdmin() throws Exception {
        when(jwtService.extractUsername(anyString()))
                .thenReturn("admin");
        when(authenticationService.findByUsername(anyString()))
                .thenReturn(User.builder().role(Role.ADMIN).build());
        when(authenticationService.register(any(RegisterRequest.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register/admin")
                        .with(csrf())
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        verify(authenticationService).register(any(RegisterRequest.class));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void registerAdmin_ShouldReturnForbiddenWhenCurrentUserIsNotAdmin() throws Exception {
        when(jwtService.extractUsername(anyString()))
                .thenReturn("user");
        when(authenticationService.findByUsername(anyString()))
                .thenReturn(User.builder().role(Role.USER).build());

        mockMvc.perform(post("/api/v1/auth/register/admin")
                        .with(csrf())
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void getCurrentUser_ShouldReturnUserWithoutPassword() throws Exception {
        when(jwtService.extractUsername(anyString()))
                .thenReturn("testuser");
        when(authenticationService.findByUsername(anyString()))
                .thenReturn(mockUser);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(authenticationService).findByUsername("testuser");
    }

    @Test
    void logout_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }
}