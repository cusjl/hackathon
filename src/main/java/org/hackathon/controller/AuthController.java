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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String SDU_PASS_LOGIN_PAGE =
            "https://i.sdu.edu.cn/pass-api/login/page";

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
     * 跳转到 SDU Pass 统一认证登录页。
     * SDU Pass 登录成功后会携带 code 回调到 callbackUrl。
     */
    @GetMapping("/sdu-pass/login")
    public ResponseEntity<Void> startSduPassLogin() {
        try {
            String encodedCallback = URLEncoder.encode(globalProperties.callbackUrl(), StandardCharsets.UTF_8);
            URI loginUrl = URI.create(SDU_PASS_LOGIN_PAGE + "?forward=" + encodedCallback);
            return redirect(loginUrl);
        } catch (Exception e) {
            log.warn("构造 SDU Pass 登录跳转地址失败", e);
            return redirectToFrontendError(errorMessage(e));
        }
    }

    /**
     * SduPass回调接口
     * @param code SDU统一认证登录返回的code
     * @return 携带短时(5min)token重定向至前端首页或注册页面
     */
    @GetMapping("/sdu-pass-jwt")
    public ResponseEntity<Void> sduPassLogin(@RequestParam(required = false) String code) {
        try {
            if (code == null || code.isBlank()) {
                return redirectToFrontendError("缺少授权码");
            }
            String sduPassJwt = sduPassClient.getToken(code).token();
            SduPassJwtPayload payload =
                    sduPassJwtUtils.parseSduPassJwt(sduPassJwt);
            //在已注册用户中查找对应学生
            Integer id = authService.examineStudent(payload.casID());
            if (id == null) {
                String token = localJwtUtils.generateToken(
                        new LocalJwt(null, payload.name(), true, payload.casID()), LocalJwt.Type.REGISTER
                );
                return redirectToFrontend(globalProperties.registerPath(), "token", token);
            }
            String token = localJwtUtils.generateToken(
                    new LocalJwt(id, payload.name(), true, payload.casID()), LocalJwt.Type.EXCHANGE
            );
            return redirectToFrontend(globalProperties.redirectPath(), "token", token);
        } catch (Exception e) {
            log.warn("SDU Pass 登录回调处理失败", e);
            return redirectToFrontendError(errorMessage(e));
        }
    }

    private ResponseEntity<Void> redirectToFrontend(String path, String parameter, String value) {
        URI uri = UriComponentsBuilder.fromUriString(globalProperties.frontendUrl())
                .path(path)
                .queryParam(parameter, value)
                .build()
                .encode()
                .toUri();
        return redirect(uri);
    }

    private ResponseEntity<Void> redirectToFrontendError(String message) {
        return redirectToFrontend("", "error", message);
    }

    private ResponseEntity<Void> redirect(URI location) {
        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }

    private String errorMessage(Exception e) {
        return e.getMessage() == null || e.getMessage().isBlank()
                ? "统一认证登录失败" : e.getMessage();
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
