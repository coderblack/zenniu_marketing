package cn.doitedu.rulemk.demos;

import org.apache.commons.lang3.RandomUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.MeterView;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

/**
 * 度量：
 *      g1:
 *        process方法被调用的次数
 *        process方法调用耗费的总时长
 *      g2:
 *        process方法平均每秒调用的次数
 */
public class FlinkMetricDemo {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());
        env.setParallelism(1);
        DataStreamSource<String> ds = env.socketTextStream("localhost", 5656);

        SingleOutputStreamOperator<String> res = ds.process(new ProcessFunction<String, String>() {
            Counter process_call_count;
            Counter process_call_timeamount;
            MeterView process_callcnt_persecond;

            @Override
            public void open(Configuration parameters) throws Exception {
                // 定义两个度量组
                MetricGroup g1 = getRuntimeContext().getMetricGroup().addGroup("g1");
                MetricGroup g2 = getRuntimeContext().getMetricGroup().addGroup("g2");

                // g1组中定义两个counter度量
                process_call_count = g1.counter("process_call_count");
                process_call_timeamount = g1.counter("process_call_timeamount");

                // g2组定义一个meter度量
                process_callcnt_persecond = g2.meter("process_callcnt_persecond", new MeterView(process_call_count, 5));

            }

            @Override
            public void processElement(String value, Context ctx, Collector<String> out) throws Exception {


                long s = System.currentTimeMillis();

                Thread.sleep(RandomUtils.nextInt(500,5000));

                long e = System.currentTimeMillis();

                // 调用次数度量器+1
                process_call_count.inc();
                process_call_timeamount.inc(e-s);

                // 标记本次调用到 meter度量种
                process_callcnt_persecond.markEvent();


                out.collect(value.toUpperCase());

            }
        });

        res.print();
        env.execute();


    }
}
