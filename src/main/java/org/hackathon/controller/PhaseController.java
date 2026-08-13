package org.hackathon.controller;

import lombok.RequiredArgsConstructor;
import org.hackathon.service.PhaseService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/phase")
@RequiredArgsConstructor
public class PhaseController {

    private final PhaseService phaseService;

}
