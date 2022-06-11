package cn.doitedu.rulemk.marketing.functions;


import cn.doitedu.rulemk.marketing.utils.ConfigNames;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import java.util.Properties;

/**
 * 各类 kafka source的构建工具
 */
public class KafkaSourceBuilder {
    Config config;

    public KafkaSourceBuilder() {
        config = ConfigFactory.load();
    }

    public FlinkKafkaConsumer<String> build(String topic){

        Properties props = new Properties();
        props.setProperty("bootstrap.servers",config.getString(ConfigNames.KAFKA_BOOTSTRAP_SERVERS));
        props.setProperty("auto.offset.reset", config.getString(ConfigNames.KAFKA_OFFSET_AUTORESET));

        FlinkKafkaConsumer<String> kafkaConsumer = new FlinkKafkaConsumer<>(topic, new SimpleStringSchema(), props);

        return kafkaConsumer;

    }


}
