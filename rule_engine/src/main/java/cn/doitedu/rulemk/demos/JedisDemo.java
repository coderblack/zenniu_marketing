package cn.doitedu.rulemk.demos;

import cn.doitedu.rulemk.marketing.utils.ConnectionUtils;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;

public class JedisDemo {
    public static void main(String[] args) {
        Jedis jedis = ConnectionUtils.getRedisConnection();
        Map<String, String> m = new HashMap<>();
        m.put("k1","v1");
        m.put("k2","v2");
        String x = jedis.hmset("x", m);
        System.out.println(x);
    }
}
