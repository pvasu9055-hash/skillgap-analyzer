package com.skillgap.skillgap.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@CrossOrigin
public class SkillGapController {

    @PostMapping("/analyze")
    public List<String> analyzeSkills(@RequestBody Map<String,String> skills) {

        String student = skills.get("studentSkills");
        String job = skills.get("jobSkills");

        List<String> studentSkills = Arrays.stream(student.split(","))
                .map(s -> s.trim().toLowerCase())
                .toList();

        List<String> jobSkills = Arrays.stream(job.split(","))
                .map(s -> s.trim().toLowerCase())
                .toList();

        List<String> missing = new ArrayList<>();

        for(String skill : jobSkills){
            if(!studentSkills.contains(skill)){
                missing.add(skill);
            }
        }

        return missing;
    }
}