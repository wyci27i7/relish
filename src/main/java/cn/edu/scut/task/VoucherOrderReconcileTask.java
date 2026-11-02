package cn.edu.scut.task;

import cn.edu.scut.common.utils.RedisIdWorker;
import cn.edu.scut.entity.Compensate;
import cn.edu.scut.entity.SeckillVoucher;
import cn.edu.scut.mapper.CompensateMapper;
import cn.edu.scut.mapper.SeckillVoucherMapper;
import cn.edu.scut.mapper.VoucherOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoucherOrderReconcileTask {

    private final StringRedisTemplate stringRedisTemplate;
    private final SeckillVoucherMapper seckillVoucherMapper;
    private final VoucherOrderMapper voucherOrderMapper;
    private final CompensateMapper compensateMapper;
    private final RedisIdWorker redisIdWorker;

    @Scheduled(fixedDelay = 300000, initialDelay = 10000)
    public void reconcile() {
        log.info("========== 开始库存对账 ==========");

        List<SeckillVoucher> vouchers = seckillVoucherMapper.selectAll();

        for (SeckillVoucher voucher : vouchers) {
            Long voucherId = voucher.getVoucherId();

            // ① Redis 侧剩余库存
            String stockKey = "seckill:stock:" + voucherId;
            String redisStockStr = stringRedisTemplate.opsForValue().get(stockKey);
            if (redisStockStr == null) {
                continue; // Redis 没这个 key，没秒杀过
            }
            Long redisStock = Long.parseLong(redisStockStr);

            // ② DB 侧剩余库存
            Long dbStock = Long.valueOf(voucher.getStock());

            // ③ 一致就跳过
            if (redisStock.equals(dbStock)) {
                continue;
            }

            log.error("库存不一致，voucherId={}, Redis库存={}, DB库存={}", voucherId, redisStock, dbStock);

            // ④ 不一致，详细对比用户
            String userKey = "seckill:order:" + voucherId;
            Set<String> redisUsers = stringRedisTemplate.opsForSet().members(userKey);
            Set<Long> dbUsers = voucherOrderMapper.selectUserIdsByVoucherId(voucherId);

            for (String userIdStr : redisUsers) {
                Long userId = Long.parseLong(userIdStr);
                if (!dbUsers.contains(userId)) {
                    try {
                        Compensate compensate = new Compensate();
                        compensate.setId(redisIdWorker.nextId("compensate"));
                        compensate.setUserId(userId);
                        compensate.setVoucherId(voucherId);
                        compensate.setRedisUserCount((long) redisUsers.size());
                        compensate.setDbOrderCount((long) dbUsers.size());
                        compensate.setReason("Redis库存" + redisStock + "与DB库存" + dbStock + "不一致");
                        compensate.setStatus(0);
                        compensateMapper.insert(compensate);
                        log.error("记录丢单补偿，userId={}, voucherId={}", userId, voucherId);
                    } catch (DuplicateKeyException e) {
                        // 已经报过警了，忽略
                    }
                }
            }
        }

        log.info("========== 库存对账结束 ==========");
    }

}
