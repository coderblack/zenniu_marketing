package cn.doitedu.rulemk.demos.flink_drools;

import cn.doitedu.rulemk.marketing.utils.ConfigNames;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.streaming.api.datastream.*;
import org.apache.flink.streaming.api.environment.LocalStreamEnvironment;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.util.Collector;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author 涛哥
 * @nick_name "deep as the sea"
 * @contact qq:657270652 wx:doit_edu
 * @site www.doitedu.cn
 * @date 2021-07-31
 * @desc flink整合drools实战demo
 */
@Slf4j
public class FlinkDrools {
    public static void main(String[] args) throws Exception {

        // 读取业务数据
        LocalStreamEnvironment env = StreamExecutionEnvironment.createLocalEnvironment();
        env.setParallelism(1);
        DataStream<String> dataStream = env.socketTextStream("localhost", 5656);
        DataStream<DataBean> dataBeanStream = dataStream.map(s -> new DataBean(s, null));

        // 读取规则
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "hdp01:9092,hdp02:9092,hdp03:9092");
        props.setProperty("auto.offset.reset", "latest");
        DataStreamSource<String> drlStream = env.addSource(new FlinkKafkaConsumer<String>("rule-demo", new SimpleStringSchema(), props));

        MapStateDescriptor<String, KieSession> mapStateDescriptor = new MapStateDescriptor<>("rule_state", String.class, KieSession.class);
        BroadcastStream<String> broadcast = drlStream.broadcast(mapStateDescriptor);

        BroadcastConnectedStream<DataBean, String> connect = dataBeanStream.connect(broadcast);


        connect.process(new BroadcastProcessFunction<DataBean, String, String>() {
            @Override
            public void processElement(DataBean dataBean, ReadOnlyContext ctx, Collector<String> out) throws Exception {

                ReadOnlyBroadcastState<String, KieSession> state = ctx.getBroadcastState(mapStateDescriptor);
                Iterable<Map.Entry<String, KieSession>> entries = state.immutableEntries();
                for (Map.Entry<String, KieSession> entry : entries) {
                    KieSession kieSession = entry.getValue();

                    // 调用drools引擎，对进来的业务数据data进行处理
                    kieSession.insert(dataBean);
                    kieSession.fireAllRules();

                    // 输出处理结果
                    out.collect(dataBean.getResult());
                }
            }

            @Override
            public void processBroadcastElement(String canalBinlog, Context ctx, Collector<String> out) throws Exception {
                CanalBean canalBean = JSON.parseObject(canalBinlog, CanalBean.class);

                BroadcastState<String, KieSession> state = ctx.getBroadcastState(mapStateDescriptor);

                List<DbRecord> dbRecordList = canalBean.getData();
                for (DbRecord dbRecord : dbRecordList) {
                    // drools规则名称
                    String rule_name = dbRecord.getRule_name();

                    // drools规则内容
                    String drl_string = dbRecord.getDrl_string();

                    // 将drools规则字符串，构造成KieSession对象
                    KieHelper kieHelper = new KieHelper();
                    kieHelper.addContent(drl_string, ResourceType.DRL);
                    KieSession kieSession = kieHelper.build().newKieSession();

                    // 将构造好的KieSession对象放入广播state
                    String operationType = canalBean.getType();
                    String online = dbRecord.getOnline();
                    if ("INSERT".equals(operationType) || ("UPDATE".equals(operationType) && "1".equals(online))) {
                        log.info("注入一条规则,rule_name: {}",rule_name);
                        state.put(rule_name, kieSession);
                    } else if ("DELETE".equals(operationType) || ("UPDATE".equals(operationType) && "0".equals(online))) {
                        log.info("删除一条规则,rule_name: {}",rule_name);
                        state.remove(rule_name);
                    }
                }

            }
        }).print();

        env.execute();

    }

}
