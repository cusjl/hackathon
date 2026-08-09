package org.hackathon.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hackathon.security.jwt.LocalJwt;
import org.hackathon.data.dto.LoginDTO;
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

    /**
     * 学生登陆SduPass回调接口
     * @param code SDU统一认证登录返回的code
     * @return 携带短时(5min)token重定向至前端首页或注册页面
     */
    @GetMapping("/sdu-pass-jwt")
    public ResponseEntity<?> sduPassLogin(@RequestParam String code) {
        String sduPassJwt = sduPassClient.getToken(code).token();
        SduPassJwtPayload payload =
                sduPassJwtUtils.parseSduPassJwt(sduPassJwt);
        //在已注册用户中查找对应学生
        Integer id = authService.examineStudent(payload.casID());
        String url = "[前端url占位]";
        if (id == null) {
            String token = localJwtUtils.generateToken(
                    new LocalJwt(-1, payload.name(), true, payload.casID()), true
            );
            url += "/register?token=" + token;
        } else {
            String token = localJwtUtils.generateToken(
                    new LocalJwt(id, payload.name(), true, payload.casID()), true
            );
            url += "/dashboard?token=" + token;
        }

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }

    /**
     * 统一认证登录重定向token兑换接口
     * @param token 短时token
     * @return token及基本信息
     */
    @GetMapping("/exchange")
    public ResponseEntity<Result<LoginVO>> exchangeToken(
            @RequestParam @NotBlank(message = "token不能为空") String token
    ) {
        return authService.exchangeToken(token);
    }
    /**
     * 学生注册
     * @param dto 注册基本信息，含临时token，密码可选
     * @return token及基本信息
     */
    @PostMapping("/register")
    public ResponseEntity<Result<LoginVO>> studentRegister(@Valid @RequestBody RegisterStudentDTO dto) {
        return authService.studentRegister(dto);
    }

    /**
     * 账号密码登录
     * @param dto 支持学号/手机/邮箱登录
     * @return token及基本信息
     */
    @PostMapping("/login")
    public ResponseEntity<Result<LoginVO>> localLogin(@Valid @RequestBody LoginDTO dto) {
        return authService.localLogin(dto);
    }
}
