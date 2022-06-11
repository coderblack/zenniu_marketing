- 行为日志topic创建
```shell
[root@hdp01 kafka_2.11-2.0.0]# bin/kafka-topics.sh --create   \
--topic zenniu_applog  \
--partitions 2     \
--replication-factor 1    \
--zookeeper hdp01:2181,hdp02:2181,hdp03:2181
```

- 查看topic
```
[root@hdp01 kafka_2.11-2.0.0]# bin/kafka-topics.sh --list --zookeeper hdp01:2181
```

- 消费测试
```shell
[root@hdp01 kafka_2.11-2.0.0]# bin/kafka-console-consumer.sh --topic zenniu_applog --bootstrap-server hdp01:9092
```