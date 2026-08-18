package org.hackathon.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hackathon.config.GlobalProperties;
import org.hackathon.data.dto.SduPassTestDTO;
import org.hackathon.security.jwt.LocalJwt;
import org.hackathon.data.dto.LoginDTO;
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
    private final GlobalProperties globalProperties;

    /**
     * 生成临时token（仅测试）
     * @param dto payload信息
     * @return 临时token
     */
    @PostMapping("/test")
    public ResponseEntity<Result<String>> getTempToken(@RequestBody @Valid SduPassTestDTO dto) {
        Integer id = authService.examineStudent(dto.getCasId());
        LocalJwt jwt = new LocalJwt(id, dto.getName(), true, dto.getCasId());
        LocalJwt.Type type = id == null ? LocalJwt.Type.REGISTER : LocalJwt.Type.EXCHANGE;
        return Result.success(localJwtUtils.generateToken(jwt, type), "生成成功");
    }

    /**
     * SduPass回调接口
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
        String url = globalProperties.frontendUrl();
        if (id == null) {
            String token = localJwtUtils.generateToken(
                    new LocalJwt(null, payload.name(), true, payload.casID()), LocalJwt.Type.REGISTER
            );
            url += globalProperties.registerPath() + "?token=" + token;
        } else {
            String token = localJwtUtils.generateToken(
                    new LocalJwt(id, payload.name(), true, payload.casID()), LocalJwt.Type.EXCHANGE
            );
            url += globalProperties.redirectPath() + "?token=" + token;
        }

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }

    /**
     * token兑换
     * @param token 短时token
     * @return token及基本信息
     */
    @GetMapping("/exchange")
    public ResponseEntity<Result<LoginVO>> exchangeToken(
            @RequestParam @NotBlank(message = "token不能为空") String token
    ) {
        return Result.success(authService.exchangeToken(token), "兑换成功");
    }

    /**
     * 账号密码登录
     * @param dto 支持学号/手机/邮箱登录
     * @return token及基本信息
     */
    @PostMapping("/login")
    public ResponseEntity<Result<LoginVO>> localLogin(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.localLogin(dto), "登录成功");
    }
}
