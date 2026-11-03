package cn.edu.scut.entity;

import jdk.jshell.execution.LoaderDelegate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoucherOrder {
    private Long id;
    private Long userId;
    private Long voucherId;
    private Integer payType;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime useTime;
    private LocalDateTime cancelTime;
    private LocalDateTime refundTime;
    private LocalDateTime updateTime;
}
