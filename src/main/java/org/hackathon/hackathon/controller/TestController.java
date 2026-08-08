package org.hackathon.hackathon.controller;

import jakarta.annotation.Resource;
import org.hackathon.hackathon.data.po.User;
import org.hackathon.hackathon.mapper.UserMapper;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@CrossOrigin
@RequestMapping("/test")
public class TestController {

    @Resource
    public UserMapper userMapper;
    @GetMapping
    public void test() {
        User user = new User(
                null, "adsfsa", true, "141231", "erfsa",
                LocalDateTime.now(), LocalDateTime.now()
        );
        userMapper.insert(user);
    }
}
