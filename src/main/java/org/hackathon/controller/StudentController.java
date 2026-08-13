package org.hackathon.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.annotation.Auth;
import org.hackathon.data.dto.CreateStudentDTO;
import org.hackathon.data.dto.UpdateStudentDTO;
import org.hackathon.data.vo.*;
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
    @PostMapping
    public ResponseEntity<Result<CreateStudentVO>> createStudent(@Valid @RequestBody CreateStudentDTO dto) {
        return Result.success(studentService.createStudent(dto), "注册成功");
    }

    /**
     * 获取学生标签
     * @return vo
     */
    @GetMapping("/tags")
    public ResponseEntity<Result<StudentTagsVO>> getAvailableTags() {
        return Result.success(new StudentTagsVO(studentService.getAvailableTags()), "获取成功");
    }

    /**
     * 学生自查信息
     * @return vo
     */
    @GetMapping
    @Auth(onlyStudent = true)
    public ResponseEntity<Result<StudentInfoVO>> getStudent(HttpServletRequest request) {
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        return Result.success(studentService.getStudent(jwt.getUserId()), "获取成功");
    }

    /**
     * 更新学生信息
     * @param dto dto
     * @return ok
     */
    @PutMapping
    @Auth(onlyStudent = true)
    public ResponseEntity<Result<Void>> updateStudent(
            @Valid @RequestBody UpdateStudentDTO dto, HttpServletRequest request) {
        LocalJwt jwt = (LocalJwt) request.getAttribute("jwt");
        studentService.updateStudent(dto, jwt.getUserId());
        return Result.ok();
    }
}
