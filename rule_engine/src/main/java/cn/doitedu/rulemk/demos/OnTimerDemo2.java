package cn.doitedu.rulemk.demos;


import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.time.Duration;

/**
 * @author 涛哥
 * @nick_name "deep as the sea"
 * @contact qq:657270652 wx:doit_edu
 * @site www.doitedu.cn
 * @date 2021-07-28
 * @desc
 *
 * 1,A,1677777771000
 * 1,B,1677777771050
 * 1,B,1677777772000
 * 1,C,1677777773000
 * 1,U,1677777774000
 * 1,B,1677777774001
 * 1,X,1677777775000
 * 功能：如果一个用户做了A事件后，3秒内没有做C，则输出一条消息
 *
 *
 */
public class OnTimerDemo2 {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());
        env.setParallelism(2);
        DataStreamSource<String> ds = env.socketTextStream("hdp01", 5656).setParallelism(2);
        SingleOutputStreamOperator<String> watermarked = ds.assignTimestampsAndWatermarks(WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofMillis(0)).withTimestampAssigner(new SerializableTimestampAssigner<String>() {
            @Override
            public long extractTimestamp(String element, long recordTimestamp) {
                return Long.parseLong(element.split(",")[2]);
            }
        }));

        SingleOutputStreamOperator<String> res = watermarked.keyBy(s -> s.split(",")[0])
                .process(new KeyedProcessFunction<String, String, String>() {

                    @Override
                    public void processElement(String value, Context ctx, Collector<String> out) throws Exception {
                        System.out.println("ctx中timestamp： "  + ctx.timestamp());
                        System.out.println("watermark:  " + ctx.timerService().currentWatermark());
                        System.out.println("process time:  " + ctx.timerService().currentProcessingTime());

                    }

                });

        res.print();
        env.execute();
    }
}
