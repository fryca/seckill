package yw.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import yw.seckill.entity.Product;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
} 