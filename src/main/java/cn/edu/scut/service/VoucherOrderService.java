package cn.edu.scut.service;

import cn.edu.scut.common.result.Result;
import cn.edu.scut.common.utils.RedisIdWorker;
import cn.edu.scut.common.utils.RedisLock;
import cn.edu.scut.common.utils.UserHolder;
import cn.edu.scut.entity.SeckillVoucher;
import cn.edu.scut.entity.VoucherOrder;
import cn.edu.scut.mapper.SeckillVoucherMapper;
import cn.edu.scut.mapper.VoucherOrderMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherOrderService {

    private final VoucherOrderMapper voucherOrderMapper;
    private final SeckillVoucherMapper seckillVoucherMapper;
    private final RedisIdWorker redisIdWorker;
    private final StringRedisTemplate stringRedisTemplate;


    // 正式开发中肯定是单独再定义一个类
    @Resource
    @Lazy  // 加上这一行，解决循环依赖
    private VoucherOrderService self;

    public Result seckillVoucher(Long voucherId) {
        // 0、查看当前用户
        Long userId = UserHolder.getUserId();
        // 1、查看优惠券信息
        // TODO Select TB_SECKILL_VOUCHER
        SeckillVoucher seckillVoucher = seckillVoucherMapper.findByVoucherId(voucherId);
        if (seckillVoucher == null) {
            return Result.fail("没有改优惠券");
        }
        // 2、看优惠券是否在秒杀时间
        LocalDateTime beginTime = seckillVoucher.getBeginTime();
        LocalDateTime endTime = seckillVoucher.getEndTime();
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(beginTime) || now.isAfter(endTime)) {
            return Result.fail("该时间段不能购买");
        }

        /*
         * 版本1：使用了 synchronized 锁来实现后续事务的互斥执行
         * 但是在多JVM实例中这个锁不能起作用
            synchronized (userId.toString().intern()) {
            return self.createOrder(voucherId, userId, seckillVoucher);
            }
         * */

        String keyName = "VoucherOrder:" + userId;
        RedisLock redisLock = new RedisLock(keyName, stringRedisTemplate);
//        RLock lock = redissonClient.getLock(keyName);

        try {
            boolean flag = redisLock.tryLock(100);
            if (!flag) {
                return Result.ok("已购买了该优惠券");
            }
            return self.createOrder(voucherId, userId, seckillVoucher);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            redisLock.unlock();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Result createOrder(Long voucherId, Long userId, SeckillVoucher seckillVoucher) {
        // 3、减库存
        // 在扣库存前判断一下该用户有没有买过优惠券
        // TODO SELECT TB_VOUCHER_ORDER
        Integer orderNumber = voucherOrderMapper.find(userId, voucherId);
        if (orderNumber > 0) {
            return Result.fail("该优惠券不可重复购买");
        }
        Integer stock = seckillVoucher.getStock();
        if (stock <= 0) {
            return Result.fail("库存不足");
        }
        // TODO UPDATE TB_SECKILL_VOUCHER
        seckillVoucherMapper.decreaseStock(voucherId);
        // 4、插入订单表
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(redisIdWorker.nextId("voucherOrder"));
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(userId);
        // TODO INSERT TB_VOUCHER_ORDER
        voucherOrderMapper.insertVoucherOrder(voucherOrder);
        return Result.ok(voucherOrder.getId());
    }

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setResultType(Long.class);
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
    }

    // 队列名，和Lua脚本里xadd的名字保持一致
    private static final String STREAM_KEY = "stream.orders";
    // 消费者组名
    private static final String GROUP_NAME = "order_group";
    // 当前消费者名字，单消费者就叫c1就行
    private static final String CONSUMER_NAME = "c1";
    // 单线程池：保证订单顺序处理，也不会有并发问题，秒杀场景单线程写库完全够用
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    // 服务启动后自动执行，初始化消费者组 + 启动消费线程
    @PostConstruct
    private void init() {
        // 先检查并创建消费者组，不然后面读消息会报错
        initConsumerGroup();
        // 提交消费任务，后台开始跑
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }


    /**
     * 初始化消费者组：不存在就创建，已存在就跳过
     * 等价原生命令：XGROUP CREATE stream.orders order_group 0 MKSTREAM
     */
    private void initConsumerGroup() {
        try {
            stringRedisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP_NAME);
        } catch (Exception e) {
            // 组已存在会抛出 BUSYGROUP 异常，直接忽略即可
            log.info("消费者组已存在，无需重复创建");
        }
    }


    private class VoucherOrderHandler implements Runnable {
        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            while (true) {
                try {
                    // XREADGROUP GROUP order_group c1 COUNT 1 BLOCK 2000 STREAMS stream.orders >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            // 指定消费者组和消费者
                            Consumer.from(GROUP_NAME, CONSUMER_NAME),
                            // 读取选项：每次读1条，没消息就阻塞等2秒，避免死循环空转耗CPU
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            // 偏移量：lastConsumed() 对应命令里的 ">"，意思是「读取从未消费过的新消息」
                            StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
                    );
                    if (list == null || list.isEmpty()) {
                        // 没消息，继续循环（下一轮又会阻塞等2秒）
                        continue;
                    }
                    // 一次只读1条，直接取第0个 count(1)
                    MapRecord<String, Object, Object> record = list.get(0);
                    // 消息里的字段键值对
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = new VoucherOrder();
                    voucherOrder.setId(Long.parseLong(value.get("id").toString()));
                    voucherOrder.setUserId(Long.parseLong(value.get("userId").toString()));
                    voucherOrder.setVoucherId(Long.parseLong(value.get("voucherId").toString()));
                    // 创建订单
                    self.createVoucherOrder(voucherOrder);
                    // 发送 ACK，确认消息已处理
                    stringRedisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, record.getId());
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                    // 异常后，先去处理待处理列表，补漏
                    handlePendingList();
                }
            }
        }

        @SuppressWarnings("unchecked")
        private void handlePendingList() {
            while (true) {
                // 外层try：兜底「读消息」阶段的异常（Redis连不上、网络超时这类）
                try {
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from(GROUP_NAME, CONSUMER_NAME),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(STREAM_KEY, ReadOffset.from("0"))
                    );

                    if (list == null || list.isEmpty()) {
                        // pending 已经空了，清扫任务结束
                        break;
                    }

                    // 走到这一步，说明成功读到了消息，record 100% 存在
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = new VoucherOrder();
                    voucherOrder.setId(Long.parseLong(value.get("id").toString()));
                    voucherOrder.setUserId(Long.parseLong(value.get("userId").toString()));
                    voucherOrder.setVoucherId(Long.parseLong(value.get("voucherId").toString()));

                    // 内层try：只包「业务落库」这一步，这时候 record 已经确定不为空
                    try {
                        self.createVoucherOrder(voucherOrder);
                        // 处理成功，正常 ACK
                        stringRedisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, record.getId());
                    } catch (Exception e) {
                        // 业务处理失败：打日志留痕 + 强行ACK，把坏消息清出pending，避免卡死
                        log.error("pending订单处理失败，强制ACK，订单id:{}, 消息id:{}", voucherOrder.getId(), record.getId(), e);
                        stringRedisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, record.getId());
                        // 遇到坏消息，本次清扫到此为止，回到主消费循环
                        break;
                    }

                } catch (Exception e) {
                    // 读消息本身失败（Redis连不上等），没有消息ID，不用也不能ACK，直接退出清扫
                    log.error("读取pending列表异常", e);
                    break;
                }
            }
        }


    }

    /*
     * 异步队列的处理逻辑：
     *       只需要减库存和创建订单即可
     *       需要注意的是这里不是主线程来处理这些业务了，不能在线程变量里面取出userId了
     * */
    @Transactional(rollbackFor = Exception.class)
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        // 减库存
        seckillVoucherMapper.decreaseStock(voucherOrder.getVoucherId());
        // 创建订单
        voucherOrderMapper.insertVoucherOrder(voucherOrder);
    }

    /*
     * 预热Redis的库存即可，集合不需要预热
     * 先在Redis里面判断库存和一单一人的判断（这里直接使用LUA脚本）
     * LUA脚本的逻辑：如果库存不足。返回1
     *              如果该商品已经被用户买过 返回2
     *              否则库存 -1
     *              把用户ID插入商品购买的集合里面
     *              向Stream流发送订单消息队列做异步请求
     * */
    public Result _seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUserId();
        long orderId = redisIdWorker.nextId("voucherOrder");
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                String.valueOf(voucherId),
                String.valueOf(userId),
                String.valueOf(orderId)
        );
        int r = result.intValue();
        if (r != 0) {
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        // 下单成功 并且已经把消息插入Stream中
        // 这里直接返回订单ID让用户去付款即可
        return Result.ok(orderId);
    }


}
