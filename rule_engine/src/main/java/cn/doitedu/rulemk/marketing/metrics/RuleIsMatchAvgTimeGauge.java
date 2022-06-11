package cn.doitedu.rulemk.marketing.metrics;

import org.apache.flink.metrics.Gauge;

public class RuleIsMatchAvgTimeGauge implements Gauge<Float> {

    private long ruleIsMatchCount;
    private long ruleIsMatchTimeAmount;

    @Override
    public Float getValue() {

        return (float)ruleIsMatchTimeAmount/ruleIsMatchCount;
    }


    public void incRuleIsMatchCount(){
        this.ruleIsMatchCount += 1;
    }

    public void incRuleIsMatchTimeAmount(long t){
        this.ruleIsMatchTimeAmount += t;
    }
}
