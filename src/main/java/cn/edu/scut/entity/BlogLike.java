package cn.edu.scut.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlogLike {
    private Long id;
    private Long userId;
    private Long blogId;
    private LocalDateTime createTime;
}
