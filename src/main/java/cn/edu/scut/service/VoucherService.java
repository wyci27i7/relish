package cn.edu.scut.service;

import cn.edu.scut.common.result.Result;
import cn.edu.scut.entity.SeckillVoucher;
import cn.edu.scut.entity.Voucher;
import cn.edu.scut.mapper.SeckillVoucherMapper;
import cn.edu.scut.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherService {
    private final VoucherMapper voucherMapper;
    private final SeckillVoucherMapper seckillVoucherMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Transactional(rollbackFor = Exception.class)
    public void addSeckillVoucher(Voucher voucher) {
        voucherMapper.save(voucher);
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherMapper.save(seckillVoucher);

        // 这里要把库存存入Redis里面 -- 方便后续的Redis查询
        String key = "seckill:stock:" + seckillVoucher.getVoucherId();
        stringRedisTemplate.opsForValue().set(key, String.valueOf(voucher.getStock()));
    }

    public Result queryVoucherOfShop(Long shopId) {
        List<Voucher> vouchers = voucherMapper.queryAllVoucherOfShop(shopId);
        return Result.ok(vouchers);
    }
}
