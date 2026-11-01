package cn.edu.scut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeckillVoucher {
    private Long voucherId;
    private Integer stock;
    private LocalDateTime createTime;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
    private LocalDateTime updateTime;
}
