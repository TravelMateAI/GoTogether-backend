package com.example.socialmediaservice.controller;

// import com.example.socialmediaservice.entity.User; // No longer needed if all tests are moved
// import com.example.socialmediaservice.service.UserService; // May still be needed if there are other user-related tests
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.security.core.authority.SimpleGrantedAuthority; // No longer needed
// import org.springframework.security.oauth2.core.user.DefaultOAuth2User; // No longer needed
// import org.springframework.security.oauth2.core.user.OAuth2User; // No longer needed
import org.springframework.test.web.servlet.MockMvc;
import com.example.socialmediaservice.service.UserService; // Keep if any non-auth tests remain or are added

// import java.util.Collections; // No longer needed
// import java.util.HashMap; // No longer needed
// import java.util.Map; // No longer needed
// import java.util.UUID; // No longer needed

// import static org.mockito.ArgumentMatchers.any; // No longer needed
// import static org.mockito.Mockito.mock; // No longer needed
// import static org.mockito.Mockito.when; // No longer needed
// import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login; // No longer needed
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get; // No longer needed
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post; // No longer needed
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*; // No longer needed
// import jakarta.servlet.http.Cookie; // No longer needed

@WebMvcTest(UserController.class) // Still testing UserController but for its specific endpoints
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService; // UserService is still a dependency of UserController

    // All auth-related tests have been moved to AuthControllerTest.
    // If there were tests for UserController's own endpoints (e.g., /register, /profile), they would remain here.
    // For now, this class will be empty of tests until UserController specific tests are added.

    @Test
    void contextLoads() {
        // Basic test to ensure the context loads for UserController, can be expanded later.
        // This is useful if no other tests are present yet.
    }
}
