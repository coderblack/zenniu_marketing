package cn.doitedu.rulemk.demos;


import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
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
public class OnTimerDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());
        env.setParallelism(1);
        DataStreamSource<String> ds = env.socketTextStream("localhost", 5656);
        SingleOutputStreamOperator<String> watermarked = ds.assignTimestampsAndWatermarks(WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofMillis(0)).withTimestampAssigner(new SerializableTimestampAssigner<String>() {
            @Override
            public long extractTimestamp(String element, long recordTimestamp) {
                return Long.parseLong(element.split(",")[2]);
            }
        }));

        SingleOutputStreamOperator<String> res = watermarked.keyBy(s -> s.split(",")[0])
                .process(new KeyedProcessFunction<String, String, String>() {

                    ListState<Tuple2<String, Long>> listState;

                    @Override
                    public void open(Configuration parameters) throws Exception {

                        listState = getRuntimeContext().getListState(new ListStateDescriptor<Tuple2<String, Long>>("tmp", TypeInformation.of(new TypeHint<Tuple2<String, Long>>() {})));

                    }

                    @Override
                    public void processElement(String value, Context ctx, Collector<String> out) throws Exception {

                        String[] split = value.split(",");
                        if("A".equals(split[1])){
                            // 注册一个定时器
                            System.out.println("遇到了事件A，注册了定时器1");
                            ctx.timerService().registerEventTimeTimer( Long.parseLong(split[2]) + 3000);
                            System.out.println("遇到了事件A，注册了定时器2");
                            ctx.timerService().registerEventTimeTimer( Long.parseLong(split[2]) + 3000);
                            System.out.println("遇到了事件A，注册了定时器3");
                            ctx.timerService().registerEventTimeTimer( Long.parseLong(split[2]) + 4000);
                        }
                    }

                    @Override
                    public void onTimer(long timestamp, OnTimerContext ctx, Collector<String> out) throws Exception {
                        System.out.println("定时器被触发，触发事件戳为： " + timestamp);
                        /*boolean flag = true;
                        Iterator<Tuple2<String, Long>> iterator = listState.get().iterator();
                        while(iterator.hasNext()){
                            Tuple2<String, Long> tp = iterator.next();
                            if(tp.f1 < timestamp) {
                                // 如果发现了3秒内有C，则把flag置为false
                                flag = false;
                                iterator.remove();
                            }
                        }

                        if(flag) out.collect(ctx.getCurrentKey() + ",在A事件后，3秒内没做C");*/
                    }
                });

        res.print();
        env.execute();
    }
}
