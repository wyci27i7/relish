package cn.edu.scut.mapper;

import cn.edu.scut.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("select id from tb_user where phone = #{phone};")
    Long getUserIdByPhone(@Param("phone") String phone);

    @Select("select * from tb_user where id = #{id};")
    User getUserById(@Param("id") Long id);

    void addUser(User user);

}
