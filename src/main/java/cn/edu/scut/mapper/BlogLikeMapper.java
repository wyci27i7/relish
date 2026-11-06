package cn.edu.scut.mapper;

import cn.edu.scut.entity.BlogLike;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlogLikeMapper {

    @Insert("INSERT INTO tb_blog_like (user_id, blog_id, create_time) " +
            "VALUES (#{userId}, #{blogId}, NOW())")
    int insert(BlogLike blogLike);

    @Delete("DELETE FROM tb_blog_like WHERE user_id = #{userId} AND blog_id = #{blogId}")
    int delete(Long userId, Long blogId);
}
