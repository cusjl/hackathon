package org.hackathon.security.sdupass;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Component
public class SduPassClient {

    private final RestClient sduPass;
    private final ObjectMapper objectMapper;

    public SduPassClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.sduPass = RestClient.builder()
                .baseUrl("https://i.sdu.edu.cn/pass-api")
                .build();
    }

    public StudentInfo getToken(String callbackCode) {
        SduPassResult rs = sduPass.post()
                .uri("/auth/token")
                .body(new GetTokenDTO(callbackCode))
                .retrieve()
                .body(SduPassResult.class);

        return objectMapper.convertValue(rs.data(), StudentInfo.class);
    }


}
