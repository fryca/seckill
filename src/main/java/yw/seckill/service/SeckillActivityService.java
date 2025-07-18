package yw.seckill.service;

import yw.seckill.entity.SeckillActivity;
import java.util.List;

public interface SeckillActivityService {
    List<SeckillActivity> getActiveActivities();
    SeckillActivity getActivityById(Long activityId);
} 