package cn.doitedu.rulemk.demos;

import org.apache.flink.metrics.Gauge;

class DoiteduGauge implements Gauge<Float> {

    private int amount1;
    private int amount2;

    @Override
    public Float getValue() {
        return (float)amount1/amount2;
    }

    public void addAmount1(int n){amount1 += n;}
    public void addAmount2(int n){amount2 += n;}
}