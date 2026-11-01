package cn.edu.scut.service;

import cn.edu.scut.common.result.Result;
import cn.edu.scut.common.utils.SmsCodeUtil;
import cn.edu.scut.common.utils.UserHolder;
import cn.edu.scut.entity.User;
import cn.edu.scut.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserMapper userMapper;

    private final StringRedisTemplate stringRedisTemplate;

    public Result sendCode(String phone) {
        // 生成验证码
        String code = SmsCodeUtil.generateCode();
        // 这里就不弄短信认证服务了，没区别，后续可以接入aliyun平台实现
        String key = "user:sentCode:" + phone;
        log.info("{}:{}", key, code);
        // 把验证码存入Redis，每个验证码的有效期的5分钟
        // 原子写入 + 设置5分钟过期，兼容所有Redis版本（2.6+都支持）
        stringRedisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
        return Result.ok();
    }

    public Result login(String phone, String code) {
        // 先查看验证码是否正确
        String key = "user:sentCode:" + phone;
        String cacheCode = stringRedisTemplate.opsForValue().get(key);
        if (cacheCode == null || cacheCode.isEmpty()) {
            return Result.fail("请先获取验证码");
        }
        if (!cacheCode.equals(code)) {
            return Result.fail("验证码输入错误");
        }
        // 把验证码在Redis中删掉
        stringRedisTemplate.delete(key);
        // 查看当前用户是否已经注册
        Long userId = userMapper.getUserIdByPhone(phone);
        if (userId == null) {
            // 当前用户不存在，自动帮用户注册
            User user = new User();
            user.setPhone(phone);
            // 默认ID
            user.setNickName("user_" + phone);
            // 密码和头像先不帮用户设定了
            // 确保userId不为空
            userMapper.addUser(user);
            // 主键已经被 MyBatis 自动回填到 user 对象里了
            userId = user.getId();
        }
        // 把userId放进Redis里面
        String token = "login:token:" + UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(token, userId.toString(), 30, TimeUnit.DAYS);
        return Result.ok(token);
    }


    public Result getUserInfo() {
        User user = userMapper.getUserById(UserHolder.getUserId());
        return Result.ok(user);
    }
}
