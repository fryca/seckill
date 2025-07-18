package yw.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long activityId;
    private Long productId;
    private String orderNo;
    private BigDecimal amount;
    private Integer status; // 0-待支付 1-已支付 2-已取消 3-已退款
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
} 