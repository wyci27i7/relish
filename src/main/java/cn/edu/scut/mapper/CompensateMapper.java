package cn.edu.scut.mapper;

import cn.edu.scut.entity.Compensate;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CompensateMapper {

    @Insert("INSERT INTO tb_order_compensate (id, user_id, voucher_id, " +
            "redis_user_count, db_order_count, reason, status) " +
            "VALUES (#{id}, #{userId}, #{voucherId}, " +
            "#{redisUserCount}, #{dbOrderCount}, #{reason}, #{status})")
    void insert(Compensate compensate);
}
