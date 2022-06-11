package cn.doitedu.rulemk.marketing.utils;

/**
 * 配置文件，参数名统一管理类
 */
public class ConfigNames {

    public static final String KAFKA_BOOTSTRAP_SERVERS = "kafka.bootstrap.servers";
    public static final String KAFKA_OFFSET_AUTORESET = "kafka.auto.offset.reset";
    public static final String KAFKA_ACTION_DETAIL_TOPIC = "kafka.action.detail.topic";

    public static final String CK_JDBC_DRIVER = "ck.jdbc.driver";
    public static final String CK_JDBC_URL = "ck.jdbc.url";
    public static final String CK_JDBC_DETAIL_TABLE = "ck.jdbc.detail.table";

    public static final String HBASE_ZK_QUORUM = "hbase.zookeeper.quorum";
    public static final String HBASE_PROFILE_TABLE = "hbase.profile.table";
    public static final String HBASE_PROFILE_FAMILY = "hbase.profile.family";

    public static final String REDIS_HOST = "redis.host";
    public static final String REDIS_PORT = "redis.port";
    public static final String REDIS_BUFFER_TTL = "reids.buffer.ttl";
}
