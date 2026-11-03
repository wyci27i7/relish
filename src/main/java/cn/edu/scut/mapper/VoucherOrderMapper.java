package cn.edu.scut.mapper;

import cn.edu.scut.entity.VoucherOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Set;

@Mapper
public interface VoucherOrderMapper {

    @Insert("INSERT INTO tb_voucher_order (id, voucher_id, user_id) " +
            "VALUES (#{id}, #{voucherId}, #{userId});")
    void insertVoucherOrder(VoucherOrder voucherOrder);

    @Select("SELECT count(*) FROM tb_voucher_order " +
            "WHERE user_id = #{userId} AND voucher_id = #{voucherId};")
    Integer find(Long userId, Long voucherId);

    @Select("SELECT id, user_id, voucher_id, status, create_time " +
            "FROM tb_voucher_order WHERE id = #{id}")
    VoucherOrder selectById(Long id);

    @Select("SELECT user_id FROM tb_voucher_order WHERE voucher_id = #{voucherId}")
    Set<Long> selectUserIdsByVoucherId(Long voucherId);

    @Update("UPDATE tb_voucher_order SET status = 2, pay_time = NOW() " +
            "WHERE id = #{orderId} AND user_id = #{userId} AND status = 1")
    Integer payOrder(Long orderId, Long userId);

    @Update("UPDATE tb_voucher_order SET status = 3, use_time = NOW() " +
            "WHERE id = #{orderId} AND user_id = #{userId} AND status = 2")
    Integer verifyOrder(Long orderId, Long userId);

    @Update("UPDATE tb_voucher_order SET status = 4, cancel_time = NOW() " +
            "WHERE id = #{orderId} AND user_id = #{userId} AND status = 1")
    int cancelOrder(Long orderId, Long userId);

    // （已支付→退款中）
    @Update("UPDATE tb_voucher_order SET status = 5 " +
            "WHERE id = #{orderId} AND user_id = #{userId} AND status = 2")
    int applyRefund(Long orderId, Long userId);

    // （已核销→退款中）
    @Update("UPDATE tb_voucher_order SET status = 5 " +
            "WHERE id = #{orderId} AND status = 3")
    int approveRefund(Long orderId);

    // （退款中→已退款）
    @Update("UPDATE tb_voucher_order SET status = 6, refund_time = NOW() " +
            "WHERE id = #{orderId} AND status = 5")
    int refundSuccess(Long orderId);


}
