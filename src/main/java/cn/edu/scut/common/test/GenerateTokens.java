package cn.edu.scut.common.test;


import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.client.codec.StringCodec;

import java.io.FileWriter;
import java.util.UUID;

public class GenerateTokens {
    public static void main(String[] args) throws Exception {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://172.24.195.136:6379");
        config.setCodec(new StringCodec());
        RedissonClient redisson = Redisson.create(config);

        FileWriter writer = new FileWriter("tokens.csv");

        long startId = 1015;
        long endId = 1214;

        for (long userId = startId; userId <= endId; userId++) {
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String token = "login:token:" + uuid;
            redisson.getBucket(token).set(String.valueOf(userId));
            writer.write(token + "\n");
        }

        writer.close();
        redisson.shutdown();
        System.out.println("完成");
    }
}
