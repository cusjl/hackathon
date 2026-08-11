package org.hackathon.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.annotation.Auth;
import org.hackathon.data.dto.RegisterStudentDTO;
import org.hackathon.data.dto.StudentInfoDTO;
import org.hackathon.data.vo.GetTagsVO;
import org.hackathon.data.vo.LoginVO;
import org.hackathon.data.vo.Result;
import org.hackathon.data.vo.StudentInfoVO;
import org.hackathon.security.jwt.LocalJwt;
import org.hackathon.service.StudentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/student")
public class StudentController {

    private final StudentService studentService;

    /**
     * 学生注册
     * @param dto 注册基本信息，含临时token，密码可选
     * @return token及基本信息
     */
    @PostMapping("/register")
    public ResponseEntity<Result<LoginVO>> register(@Valid @RequestBody RegisterStudentDTO dto) {
        return Result.success(studentService.register(dto), "注册成功");
    }

    /**
     * 获取学生标签
     * @return vo
     */
    @GetMapping("/tags")
    public ResponseEntity<Result<GetTagsVO>> getAvailableTags() {
        return Result.success(new GetTagsVO(studentService.getAvailableTags()), "获取成功");
    }

    /**
     * 获取学生信息
     * @return vo
     */
    @GetMapping("/info")
    @Auth(onlyStudent = true)
    public ResponseEntity<Result<StudentInfoVO>> getStudentInfo(HttpServletRequest request) {
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        return Result.success(studentService.getInfo(jwt.getUserId()), "获取成功");
    }

    /**
     * 更新学生信息
     * @param dto PATCH风格dto，仅修改非null值
     * @return ok
     */
    @PatchMapping("/update")
    @Auth(onlyStudent = true)
    public ResponseEntity<Result<Void>> updateStudentInfo(@Valid @RequestBody StudentInfoDTO dto, HttpServletRequest request) {
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        studentService.updateInfo(dto, jwt.getUserId());
        return Result.ok();
    }
}
