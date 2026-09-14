package com.lucasmarques.authapi.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lucasmarques.authapi.dto.LoginRequest;
import com.lucasmarques.authapi.dto.LoginResponse;
import com.lucasmarques.authapi.dto.RegisterRequest;
import com.lucasmarques.authapi.entity.User;
import com.lucasmarques.authapi.enums.UserRole;
import com.lucasmarques.authapi.service.AuthService;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@Testcontainers
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    AuthService  authService;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    RabbitTemplate rabbitTemplate;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("authapi")
            .withUsername("postgres-test")
            .withPassword("authapi-test");

    @DynamicPropertySource
    public static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    public void shouldRegisterUserWithValidData() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("Pedro", "pedro123", "pedro@gmail.com");

        String jsonPayload = jsonMapper.writeValueAsString(registerRequest);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated());
    }

    @Test
    public void shouldRegisterLoginAndAccessProtectedRoute() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("Lucas", "lucas123", "lucas@gmail.com");
        String jsonPayload = jsonMapper.writeValueAsString(registerRequest);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
               .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("Lucas", "lucas123");
        String jsonPayload2 = jsonMapper.writeValueAsString(loginRequest);

        MvcResult resultado = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload2))
               .andExpect(status().isOk()).andReturn();

        String resultadoBody = resultado.getResponse().getContentAsString();

        LoginResponse loginResponse = jsonMapper.readValue(resultadoBody, LoginResponse.class);

        mockMvc.perform(get("/user/me")
                .header("Authorization", "Bearer " + loginResponse.token()))
                .andExpect(status().isOk())
                .andReturn();
    }
}
