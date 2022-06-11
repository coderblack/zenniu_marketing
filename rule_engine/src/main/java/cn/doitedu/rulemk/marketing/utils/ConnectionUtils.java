package cn.doitedu.rulemk.marketing.utils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.sql.DriverManager;

/**
 * @author 涛哥
 * @nick_name "deep as the sea"
 * @contact qq:657270652 wx:doit_edu
 * @site www.doitedu.cn
 * @date 2021-07-24
 * @desc 各类查询库的客户端连接创建工具
 */

@Slf4j
public class ConnectionUtils {

    static Config config = ConfigFactory.load();

    // 获取hbase连接的方法
    public static Connection getHbaseConnection() throws IOException {
        org.apache.hadoop.conf.Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum",config.getString(ConfigNames.HBASE_ZK_QUORUM));
        Connection hbaseConn = ConnectionFactory.createConnection(conf);
        log.debug("hbase connection successfully created");
        return hbaseConn;
    }


    // 获取clickhouse连接的方法
    public static java.sql.Connection getClickhouseConnection() throws Exception {

        String ckDriver = config.getString(ConfigNames.CK_JDBC_DRIVER);
        String ckUrl = config.getString(ConfigNames.CK_JDBC_URL);

        Class.forName(ckDriver);
        java.sql.Connection conn = DriverManager.getConnection(ckUrl);
        log.debug("clickhouse connection successfully created");

        return conn;
    }

    // 获取redis连接的方法
    public static Jedis getRedisConnection() {
        String host = config.getString(ConfigNames.REDIS_HOST);
        int port = config.getInt(ConfigNames.REDIS_PORT);
        Jedis jedis = new Jedis(host, port);
        String ping = jedis.ping();
        if(StringUtils.isNotBlank(ping)){
            log.debug("redis connection successfully created");
        }else{
            log.error("redis connection creation failed");
        }

        return jedis;
    }
}
