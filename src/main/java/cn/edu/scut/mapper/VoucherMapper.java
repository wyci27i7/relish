package cn.edu.scut.mapper;

import cn.edu.scut.entity.Voucher;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VoucherMapper {

    @Insert("insert into tb_voucher (shop_id, title, sub_title, rules, pay_value, actual_value, type, status) " +
            "values (#{shopId}, #{title}, #{subTitle}, #{rules}, #{payValue}, #{actualValue}, #{type}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void save(Voucher voucher);

    @Select("select v.*, sv.begin_time, sv.end_time, sv.stock " +
            "from tb_voucher v " +
            "left join tb_seckill_voucher sv on v.id = sv.voucher_id " +
            "where v.shop_id = #{shopId};")
    List<Voucher> queryAllVoucherOfShop(Long shopId);


}
