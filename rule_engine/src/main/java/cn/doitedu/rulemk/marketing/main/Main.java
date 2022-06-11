package cn.doitedu.rulemk.marketing.main;

import cn.doitedu.rulemk.marketing.beans.CanalLogBean;
import cn.doitedu.rulemk.marketing.beans.DynamicKeyedBean;
import cn.doitedu.rulemk.marketing.beans.EventBean;
import cn.doitedu.rulemk.marketing.beans.RuleMatchResult;
import cn.doitedu.rulemk.marketing.functions.*;
import cn.doitedu.rulemk.marketing.utils.StateDescContainer;
import com.alibaba.fastjson.JSON;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.*;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.time.Duration;

/**
 * 规则系统第一版
 * 需求：
 *    获得用户事件，计算如下规则，输出结果
 *    规则：
 *         触发事件：  K事件，事件属性（ p2=v1 ）
 *         画像属性条件: tag87=v2, tag26=v1
 *         行为次数条件： 2021-06-18 ~ 当前 , 事件 C [p6=v8,p12=v5] 做过 >= 2次
 */
public class Main {

    public static void main(String[] args) throws Exception {

        // 构建env
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());
        env.setParallelism(1);

        // 读取kafka中的用户行为日志
        KafkaSourceBuilder kafkaSourceBuilder = new KafkaSourceBuilder();
        DataStream<String> dss = env.addSource(kafkaSourceBuilder.build("zenniu_applog"));
        // json解析
        DataStream<EventBean> dsBean = dss.map(new Json2EventBeanMapFunction()).filter(e -> e != null);

        // 添加事件时间分配
        WatermarkStrategy<EventBean> watermarkStrategy = WatermarkStrategy.<EventBean>forBoundedOutOfOrderness(Duration.ofMillis(0))
                .withTimestampAssigner(new SerializableTimestampAssigner<EventBean>() {
                    @Override
                    public long extractTimestamp(EventBean element, long recordTimestamp) {
                        return element.getTimeStamp();
                    }
                });
        SingleOutputStreamOperator<EventBean> eventBeanWithWaterMark = dsBean.assignTimestampsAndWatermarks(watermarkStrategy);



        // 读取kafka中的规则操作数据流canal binlog
        DataStream<String> ruleBinlogDs = env.addSource(kafkaSourceBuilder.build("rule-demo"));
        // 解析 binlog
        DataStream<CanalLogBean> canalLogBeanDs = ruleBinlogDs.map(s -> JSON.parseObject(s, CanalLogBean.class)).returns(CanalLogBean.class);

        // 将binlog数据流广播出去  <keybyFields,List<规则名称>>
        BroadcastStream<CanalLogBean> ruleBroadcast = canalLogBeanDs.broadcast(StateDescContainer.ruleStateDesc);

        // 将 数据流 connect  规则广播流
        BroadcastConnectedStream<EventBean, CanalLogBean> connect1 = eventBeanWithWaterMark.connect(ruleBroadcast);

        // 数据复制（因为有多种keyby需求）
        SingleOutputStreamOperator<DynamicKeyedBean> withDynamicKey = connect1.process(new DynamicKeyByReplicationFunction());


        // keyby: 按deviceId
        KeyedStream<DynamicKeyedBean, String> keyByed = withDynamicKey.keyBy(bean -> bean.getKeyValue());

        // 规则计算
        BroadcastConnectedStream<DynamicKeyedBean, CanalLogBean> connect2 = keyByed.connect(ruleBroadcast);
        DataStream<RuleMatchResult> matchResultDs = connect2.process(new RuleMatchKeyedProcessFunction());

        matchResultDs.print();

        env.execute();

    }
}
