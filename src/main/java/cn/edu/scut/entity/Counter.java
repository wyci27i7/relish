package cn.edu.scut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Counter {
    private Long id;
    private Integer bizType;
    private Long bizId;
    private Integer countType;
    private Long value;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
