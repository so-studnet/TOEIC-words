package com.toeicquiz.backend.controller;

import com.toeicquiz.backend.dto.MasteryDto;
import com.toeicquiz.backend.security.AppUserPrincipal;
import com.toeicquiz.backend.service.MasteryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class MasteryController {

    private final MasteryService masteryService;

    public MasteryController(MasteryService masteryService) {
        this.masteryService = masteryService;
    }

    @GetMapping("/api/mastery")
    public Map<String, List<MasteryDto>> getMastery(@RequestParam(required = false) Integer level,
                                                      @AuthenticationPrincipal AppUserPrincipal principal) {
        return Map.of("words", masteryService.getMasteryList(principal.getId(), level));
    }
}
