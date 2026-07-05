package org.cloud.controller;

import org.cloud.dto.RealizedProfitResponse;
import org.cloud.dto.SimulationRequest;
import org.cloud.service.RealizedProfitService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/realized-profit")
@RequiredArgsConstructor
public class RealizedProfitController {

    private final RealizedProfitService realizedProfitService;

    // 과거 매수 정보(assets)를 받아 실현손익(시세차익 + 배당 − 세금)을 정산해 반환
    @PostMapping
    public RealizedProfitResponse calculate(@RequestBody SimulationRequest request) {
        return realizedProfitService.calculate(request);
    }
}