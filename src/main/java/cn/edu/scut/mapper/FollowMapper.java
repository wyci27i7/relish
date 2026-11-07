package cn.edu.scut.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FollowMapper {

    @Select("SELECT follow_user_id FROM tb_follow " +
            "WHERE user_id = #{userId}")
    List<Long> selectFollowerIds(Long userId);

}
