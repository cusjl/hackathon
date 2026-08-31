package org.hackathon.controller;

import org.hackathon.config.GlobalProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthControllerTest {

    private final AuthController controller = new AuthController(
            null, null, null, null,
            new GlobalProperties(
                    5,
                    "http://localhost:5180",
                    "/onboarding",
                    "/auth/callback",
                    "http://localhost:8080/auth/sdu-pass-jwt"
            )
    );

    @Test
    void startsSduPassLoginWithConfiguredBackendCallback() {
        String location = controller.startSduPassLogin().getHeaders().getFirst(HttpHeaders.LOCATION);

        assertEquals(
                "https://i.sdu.edu.cn/pass-api/login/page?forward=http%3A%2F%2Flocalhost%3A8080%2Fauth%2Fsdu-pass-jwt",
                location
        );
    }

    @Test
    void redirectsMissingCallbackCodeToFrontendError() {
        URI location = controller.sduPassLogin(null).getHeaders().getLocation();

        assertEquals("http://localhost:5180?error=%E7%BC%BA%E5%B0%91%E6%8E%88%E6%9D%83%E7%A0%81", location.toString());
    }
}
