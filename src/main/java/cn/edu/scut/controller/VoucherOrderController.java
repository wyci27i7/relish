package cn.edu.scut.controller;

import cn.edu.scut.common.result.Result;
import cn.edu.scut.service.VoucherOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/voucher-order")
@RequiredArgsConstructor
public class VoucherOrderController {

    private final VoucherOrderService voucherOrderService;

    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }

    @PostMapping("pay/{orderId}")
    public Result payOrder(@PathVariable("orderId") Long orderId) {
        voucherOrderService.payOrder(orderId);
        return Result.ok();
    }

    @PostMapping("verify/{orderId}")
    public Result verifyOrder(@PathVariable("orderId") Long orderId) {
        voucherOrderService.verifyOrder(orderId);
        return Result.ok();
    }

    @PostMapping("cancel/{orderId}")
    public Result cancelOrder(@PathVariable("orderId") Long orderId) {
        voucherOrderService.cancelOrder(orderId);
        return Result.ok();
    }

    @PostMapping("refund/apply/{orderId}")
    public Result applyRefund(@PathVariable("orderId") Long orderId) {
        voucherOrderService.applyRefund(orderId);
        return Result.ok();
    }

    @PostMapping("refund/approve/{orderId}")
    public Result approveRefund(@PathVariable("orderId") Long orderId) {
        voucherOrderService.approveRefund(orderId);
        return Result.ok();
    }

    @PostMapping("refund/success/{orderId}")
    public Result refundSuccess(@PathVariable("orderId") Long orderId) {
        voucherOrderService.refundSuccess(orderId);
        return Result.ok();
    }


}