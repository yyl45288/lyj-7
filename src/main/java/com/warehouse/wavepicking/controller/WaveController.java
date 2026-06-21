package com.warehouse.wavepicking.controller;

import com.warehouse.wavepicking.common.ApiResponse;
import com.warehouse.wavepicking.dto.request.CreateWaveRequest;
import com.warehouse.wavepicking.dto.response.WaveResponse;
import com.warehouse.wavepicking.entity.Wave;
import com.warehouse.wavepicking.service.WaveService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/waves")
public class WaveController {

    private final WaveService waveService;

    public WaveController(WaveService waveService) {
        this.waveService = waveService;
    }

    @GetMapping
    public ApiResponse<List<WaveResponse>> getAllWaves() {
        return ApiResponse.success(waveService.getAllWaves());
    }

    @GetMapping("/active")
    public ApiResponse<List<WaveResponse>> getActiveWaves() {
        return ApiResponse.success(waveService.getActiveWaves());
    }

    @GetMapping("/status/{status}")
    public ApiResponse<List<WaveResponse>> getWavesByStatus(@PathVariable String status) {
        Wave.WaveStatus waveStatus = Wave.WaveStatus.valueOf(status.toUpperCase());
        return ApiResponse.success(waveService.getWavesByStatus(waveStatus));
    }

    @GetMapping("/{id}")
    public ApiResponse<WaveResponse> getWaveById(@PathVariable Long id) {
        return ApiResponse.success(waveService.getWaveById(id));
    }

    @GetMapping("/no/{waveNo}")
    public ApiResponse<WaveResponse> getWaveByWaveNo(@PathVariable String waveNo) {
        return ApiResponse.success(waveService.getWaveByWaveNo(waveNo));
    }

    @PostMapping
    public ApiResponse<WaveResponse> createWave(@Valid @RequestBody CreateWaveRequest request) {
        return ApiResponse.success(waveService.createWave(request));
    }

    @PutMapping("/{id}/release")
    public ApiResponse<WaveResponse> releaseWave(@PathVariable Long id) {
        return ApiResponse.success(waveService.releaseWave(id));
    }

    @PutMapping("/{id}/rollback")
    public ApiResponse<WaveResponse> rollbackWave(@PathVariable Long id) {
        return ApiResponse.success(waveService.rollbackWave(id));
    }

    @PutMapping("/{id}/complete")
    public ApiResponse<WaveResponse> completeWave(@PathVariable Long id) {
        return ApiResponse.success(waveService.completeWave(id));
    }

    @PostMapping("/{waveId}/orders/{orderId}/urgent")
    public ApiResponse<WaveResponse> addUrgentOrder(@PathVariable Long waveId, @PathVariable Long orderId) {
        return ApiResponse.success(waveService.addUrgentOrderToWave(waveId, orderId));
    }
}
