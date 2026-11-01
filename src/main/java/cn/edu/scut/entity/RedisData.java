package cn.edu.scut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RedisData<T> {
    private LocalDateTime expireTime;
    private T data;
}

