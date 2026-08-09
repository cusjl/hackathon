package org.hackathon.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hackathon.data.dto.GenerateJwtDTO;
import org.hackathon.data.dto.RegisterStudentDTO;
import org.hackathon.data.vo.LoginVO;
import org.hackathon.data.vo.Result;
import org.hackathon.security.jwt.LocalJwtUtils;
import org.hackathon.security.jwt.SduPassJwtUtils;
import org.hackathon.security.jwt.SduPassJwtPayload;
import org.hackathon.security.sdupass.SduPassClient;
import org.hackathon.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SduPassClient sduPassClient;
    private final SduPassJwtUtils sduPassJwtUtils;
    private final AuthService authService;
    private final LocalJwtUtils localJwtUtils;

    @GetMapping("/sdu-pass-jwt")
    public ResponseEntity<?> sduPassLogin(@RequestParam String code) {
        String sduPassJwt = sduPassClient.getToken(code).token();
        SduPassJwtPayload payload =
                sduPassJwtUtils.parseSduPassJwt(sduPassJwt);
        //在已注册用户中查找对应学生
        Integer id = authService.examineStudent(payload.casID());
        String url = "[前端url占位]";
        if (id == null) {
            String token = localJwtUtils.generateJwt(
                    new GenerateJwtDTO(-1, payload.name(), true, payload.casID())
            );
            url += "/register#token=" + token;
        } else {
            String token = localJwtUtils.generateJwt(
                    new GenerateJwtDTO(id, payload.name(), true, payload.casID())
            );
            url += "/dashboard#token=" + token;
        }

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }

    @PostMapping("/register")
    public ResponseEntity<Result<LoginVO>> studentRegister(@Valid @RequestBody RegisterStudentDTO dto) {
        return authService.studentRegister(dto);
    }
}
