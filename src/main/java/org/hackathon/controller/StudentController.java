package org.hackathon.controller;

import lombok.RequiredArgsConstructor;
import org.hackathon.annotation.Auth;
import org.hackathon.annotation.EventAuth;
import org.hackathon.data.vo.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/student")
public class StudentController {

    @Auth(onlyStudent = true, onlySuper = true)
    @GetMapping
    public ResponseEntity<Result<Void>> authTest() {
        return Result.ok();
    }

    @EventAuth("JUDGE")
    @GetMapping("/{eventId}")
    public ResponseEntity<Result<Void>> eventAuthAdmin(@PathVariable String eventId) {
        return Result.ok();
    }
}
