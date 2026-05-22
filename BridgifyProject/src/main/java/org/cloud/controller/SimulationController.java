package org.cloud.controller;

import org.cloud.dto.SimulationRequest;
import org.cloud.dto.SimulationResponse;
import org.cloud.service.SimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @PostMapping("/run")
    public ResponseEntity<SimulationResponse> run(@RequestBody SimulationRequest request) {
        return ResponseEntity.ok(simulationService.runSimulation(request));
    }
}