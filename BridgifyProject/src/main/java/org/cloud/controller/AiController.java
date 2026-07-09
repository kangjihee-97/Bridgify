package org.cloud.controller;

import java.util.Map;

import org.cloud.dto.AiSummaryRequest;
import org.cloud.service.AiSummaryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiSummaryService aiSummaryService;

    // 시뮬레이션 결과 AI 해설 — POST /api/ai/summary
    @PostMapping("/summary")
    public Map<String, String> summary(@RequestBody AiSummaryRequest request) {
        return Map.of("summary", aiSummaryService.summarize(request));
    }
}