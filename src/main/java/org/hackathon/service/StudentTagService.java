package org.hackathon.service;

import lombok.RequiredArgsConstructor;
import org.hackathon.data.po.StudentTag;
import org.hackathon.mapper.StudentTagMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentTagService {
    private final StudentTagMapper tagMapper;

    @Cacheable("studentTags")
    public List<String> getAvailableTags() {
        List<StudentTag> list = tagMapper.selectList(null);
        return list.stream().map(StudentTag::getName).toList();
    }
}
