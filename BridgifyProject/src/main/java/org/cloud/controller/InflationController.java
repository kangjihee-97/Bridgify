package org.cloud.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.cloud.service.InflationDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inflation")
@RequiredArgsConstructor
public class InflationController {

    private final InflationDataService inflationDataService;

    // 미국 물가상승률(%) — GET /api/inflation/us
    @GetMapping("/us")
    public Map<String, BigDecimal> usInflation() {
        return Map.of("usInflationRate", inflationDataService.getUsInflationRate());
    }

    // 한국 물가상승률(%) — GET /api/inflation/kr
    @GetMapping("/kr")
    public Map<String, BigDecimal> krInflation() {
        return Map.of("krInflationRate", inflationDataService.getKrInflationRate());
    }

    // 둘 다 — GET /api/inflation/all
    @GetMapping("/all")
    public Map<String, BigDecimal> allInflation() {
        return Map.of(
                "usInflationRate", inflationDataService.getUsInflationRate(),
                "krInflationRate", inflationDataService.getKrInflationRate()
        );
    }
}