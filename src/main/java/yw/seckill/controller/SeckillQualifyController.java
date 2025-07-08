package yw.seckill.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import yw.seckill.config.ApiResponse;
import yw.seckill.dto.QualifyRequestDTO;
import yw.seckill.service.SeckillQualifyService;

@RestController
@RequestMapping("/seckill/qualify")
public class SeckillQualifyController {
    @Autowired
    private SeckillQualifyService seckillQualifyService;

    @PostMapping("")
    public ApiResponse<Boolean> checkQualification(@RequestBody QualifyRequestDTO request, @RequestParam String signature) {
        try {
            boolean qualified = seckillQualifyService.checkSeckillQualification(request.userId(), request.activityId(), request.timestamp(), signature);
            return ApiResponse.success(qualified);
        } catch (Exception e) {
            throw e;
        }
    }
} 