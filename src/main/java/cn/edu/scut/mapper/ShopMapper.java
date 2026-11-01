package cn.edu.scut.mapper;

import cn.edu.scut.entity.Shop;
import cn.edu.scut.entity.ShopType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ShopMapper {

    @Select("select * from tb_shop where id = #{id};")
    Shop selectShopById(Long id);

    @Select("select * from tb_shop_type order by sort;")
    List<ShopType> selectShopTypeList();

    void updateShop(Shop shop);

}
