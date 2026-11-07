package cn.edu.scut.mapper;

import cn.edu.scut.entity.Blog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BlogMapper {

    int insert(Blog blog);

    Blog selectById(Long id);

    List<Blog> selectByIds(@Param("ids") List<Long> ids);

}
