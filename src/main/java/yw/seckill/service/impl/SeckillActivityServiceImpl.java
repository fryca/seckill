package yw.seckill.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import yw.seckill.entity.SeckillActivity;
import yw.seckill.mapper.SeckillActivityMapper;
import yw.seckill.service.SeckillActivityService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;

@Service
public class SeckillActivityServiceImpl implements SeckillActivityService {
    
    @Autowired
    private SeckillActivityMapper seckillActivityMapper;
    
    @Override
    public List<SeckillActivity> getActiveActivities() {
        QueryWrapper<SeckillActivity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1); // 进行中的活动
        return seckillActivityMapper.selectList(queryWrapper);
    }
    
    @Override
    public SeckillActivity getActivityById(Long activityId) {
        return seckillActivityMapper.selectById(activityId);
    }
} 