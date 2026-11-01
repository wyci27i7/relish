package cn.edu.scut.mapper;

import cn.edu.scut.entity.SeckillVoucher;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SeckillVoucherMapper {

    @Insert("insert into tb_seckill_voucher (voucher_id, stock, begin_time, end_time) " +
            "values (#{voucherId}, #{stock}, #{beginTime}, #{endTime});")
    void save(SeckillVoucher seckillVoucher);

    @Select("select * from tb_seckill_voucher where voucher_id = #{voucherId};")
    SeckillVoucher findByVoucherId(Long voucherId);

    @Update("update tb_seckill_voucher set stock = stock - 1 where voucher_id = #{voucherId} and stock > 0;")
    void decreaseStock(Long voucherId);

}
