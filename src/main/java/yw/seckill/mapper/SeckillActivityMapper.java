package yw.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import yw.seckill.entity.SeckillActivity;

@Mapper
public interface SeckillActivityMapper extends BaseMapper<SeckillActivity> {
    
    @Update("UPDATE t_seckill_activity SET seckill_stock = seckill_stock - 1 WHERE id = #{activityId} AND seckill_stock > 0")
    int decreaseStock(@Param("activityId") Long activityId);
} 