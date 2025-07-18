package yw.seckill.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import yw.seckill.config.ApiResponse;
import yw.seckill.entity.SeckillActivity;
import yw.seckill.service.SeckillActivityService;
import java.util.List;

@RestController
@RequestMapping("/seckill/activity")
public class SeckillActivityController {
    
    @Autowired
    private SeckillActivityService seckillActivityService;
    
    @GetMapping("/list")
    public ApiResponse<List<SeckillActivity>> getActivityList() {
        List<SeckillActivity> activities = seckillActivityService.getActiveActivities();
        return ApiResponse.success(activities);
    }
    
    @GetMapping("/{activityId}")
    public ApiResponse<SeckillActivity> getActivityById(@PathVariable Long activityId) {
        SeckillActivity activity = seckillActivityService.getActivityById(activityId);
        return ApiResponse.success(activity);
    }
    
    @PostMapping("/test")
    public ApiResponse<String> testEndpoint() {
        return ApiResponse.success("活动服务正常运行");
    }
} 