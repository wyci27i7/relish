package cn.edu.scut.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CounterMapper {

    @Update("UPDATE tb_counter SET value = value + #{delta} " +
            "WHERE biz_type = #{bizType} AND biz_id = #{bizId} AND count_type = #{countType}")
    void updateCount(@Param("bizType") Integer bizType,
                    @Param("bizId") Long bizId,
                    @Param("countType") Integer countType,
                    @Param("delta") Long delta);

    @Select("SELECT value FROM tb_counter " +
            "WHERE biz_type = #{bizType} AND biz_id = #{bizId} AND count_type = #{countType}")
    Long selectCount(@Param("bizType") Integer bizType,
                     @Param("bizId") Long bizId,
                     @Param("countType") Integer countType);
}
