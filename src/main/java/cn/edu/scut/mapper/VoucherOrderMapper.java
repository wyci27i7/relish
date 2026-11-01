package cn.edu.scut.mapper;

import cn.edu.scut.entity.VoucherOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VoucherOrderMapper {

    @Insert("insert into tb_voucher_order (id, voucher_id, user_id) " +
            "values (#{id}, #{voucherId}, #{userId});")
    void insertVoucherOrder(VoucherOrder voucherOrder);

    @Select("select count(*) from tb_voucher_order " +
            "where user_id = #{userId} and voucher_id = #{voucherId};")
    Integer find(Long userId, Long voucherId);

}
