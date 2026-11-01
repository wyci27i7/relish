package cn.edu.scut.controller;

import cn.edu.scut.common.result.Result;
import cn.edu.scut.entity.Shop;
import cn.edu.scut.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @GetMapping("/shop/{id}")
    public Result getShopById(@PathVariable("id") Long id) {
        return shopService._getShopById(id);
    }

    @GetMapping("/shop-type/list")
    public Result getShopTypeList() {
        return shopService.getShopTypeList();
    }

    @PutMapping("/shop")
    public Result updateShop(@RequestBody Shop shop) {
        return shopService.updateShop(shop);
    }


}
