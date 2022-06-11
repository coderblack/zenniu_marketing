package cn.doitedu.rulemk.demos;

import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.*;
import org.apache.flink.runtime.metrics.DescriptiveStatisticsHistogram;
import org.apache.flink.runtime.metrics.DescriptiveStatisticsHistogramStatistics;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

/**
 * @author 涛哥
 * @nick_name "deep as the sea"
 * @contact qq:657270652 wx:doit_edu
 * @site www.doitedu.cn
 * @date 2021-08-01
 * @desc
 *
 * 可以在继承自 RichFunction 的函数中通过 getRuntimeContext().getMetricGroup() 获取 Metric 信息
 * 常见的 Metrics 的类型有 Counter、Gauge、Histogram、Meter。
 *
 */
public class MetricDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());
        env.setParallelism(1);
        DataStreamSource<String> ds = env.socketTextStream("localhost", 5656);

        ds.process(new ProcessFunction<String, String>() {


            Counter doitedu_counter;
            Histogram doitedu_histogram;
            MeterView doitedu_meter;
            DoiteduGauge doitedu_gauge;

            @Override
            public void open(Configuration parameters) throws Exception {

                // Counter 用于计数，当前值可以使用 inc()/inc(long n) 递增和 dec()/dec(long n) 递减
                doitedu_counter = getRuntimeContext().getMetricGroup().counter("doitedu_counter");

                // Gauge 根据需要提供任何类型的值，要使用 Gauge 的话，需要实现 Gauge 接口，返回值没有规定类型。
                doitedu_gauge = getRuntimeContext().getMetricGroup().gauge("doitedu_gauge", new DoiteduGauge());

                // Histogram 统计数据的分布情况，比如最小值，最大值，平均值，还有分位数等。使用情况如下：
                doitedu_histogram = getRuntimeContext().getMetricGroup().histogram("doitedu_histogram", new DescriptiveStatisticsHistogram(100));

                // Meter 代表平均吞吐量，使用情况如下：
                doitedu_meter = getRuntimeContext().getMetricGroup().meter("doitedu_meter", new MeterView(doitedu_counter, 5000));


            }

            @Override
            public void processElement(String value, Context ctx, Collector<String> out) throws Exception {

                doitedu_counter.inc(1);
                // counter_1.dec();

                doitedu_histogram.update(100);

                doitedu_meter.markEvent();

                String[] split = value.split(",");
                doitedu_gauge.addAmount1(1);
                doitedu_gauge.addAmount2(split.length);

                out.collect(value.toUpperCase());

            }
        }).print();

        env.execute();
    }
}










