package org.hackathon.service;

import lombok.RequiredArgsConstructor;
import org.hackathon.mapper.EventMapper;
import org.hackathon.mapper.PhaseMapper;
import org.hackathon.mapper.TrackMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PhaseService {

    private final EventMapper eventMapper;
    private final TrackMapper trackMapper;
    private final PhaseMapper phaseMapper;

}
