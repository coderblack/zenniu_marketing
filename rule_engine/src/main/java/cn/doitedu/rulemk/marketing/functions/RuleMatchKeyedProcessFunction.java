package cn.doitedu.rulemk.marketing.functions;

import cn.doitedu.rulemk.marketing.beans.*;
import cn.doitedu.rulemk.marketing.metrics.RuleIsMatchAvgTimeGauge;
import cn.doitedu.rulemk.marketing.service.TriggerModeRulelMatchServiceImpl;
import cn.doitedu.rulemk.marketing.utils.StateDescContainer;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.kie.api.runtime.KieSession;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author 涛哥
 * @nick_name "deep as the sea"
 * @contact qq:657270652 wx:doit_edu
 * @site www.doitedu.cn
 * @date 2021-08-02
 * @desc 规则计算的核心function
 */
@Slf4j
public class RuleMatchKeyedProcessFunction extends KeyedBroadcastProcessFunction<String, DynamicKeyedBean, CanalLogBean, RuleMatchResult> {
    ListState<EventBean> listState;
    TriggerModeRulelMatchServiceImpl triggerModeRulelMatchService;
    ReadOnlyBroadcastState<String, List<RuleStateBean>> ruleBeanState;
    ListState<Tuple2<RuleStateBean, Long>> timerInfoState;

    Counter rule_calc_timeAmount;
    Counter rule_match_calc_timeAmount;
    Counter rule_not_match_calc_timeAmount;
    Counter rule_calc_cntAmount;
    Counter rule_is_match_calc_cntAmount;
    Counter rule_not_match_calc_cntAmount;

    RuleIsMatchAvgTimeGauge ruleIsMatchAvgTimeGauge;

    @Override
    public void open(Configuration parameters) throws Exception {
        listState = getRuntimeContext().getListState(StateDescContainer.getEventBeansDesc());
        triggerModeRulelMatchService = new TriggerModeRulelMatchServiceImpl(listState);

        // 记录规则定时信息的state
        timerInfoState = getRuntimeContext().getListState(StateDescContainer.ruleTimerStateDesc);


        rule_calc_timeAmount = getRuntimeContext().getMetricGroup().counter("rule_calc_timeAmount");
        rule_match_calc_timeAmount = getRuntimeContext().getMetricGroup().counter("rule_is_match_calc_timeAmount");
        rule_not_match_calc_timeAmount = getRuntimeContext().getMetricGroup().counter("rule_not_match_calc_timeAmount");

        rule_calc_cntAmount = getRuntimeContext().getMetricGroup().counter("rule_calc_cntAmount");
        rule_is_match_calc_cntAmount = getRuntimeContext().getMetricGroup().counter("rule_is_match_calc_cntAmount");
        rule_not_match_calc_cntAmount = getRuntimeContext().getMetricGroup().counter("rule_not_match_calc_cntAmount");

        ruleIsMatchAvgTimeGauge = getRuntimeContext().getMetricGroup().gauge("计算成立的规则的平均每次计算耗时",  new RuleIsMatchAvgTimeGauge());

    }

    @Override
    public void processElement(DynamicKeyedBean dynamicKeyedBean, ReadOnlyContext ctx, Collector<RuleMatchResult> out) throws Exception {

        log.debug("接收到动态key数据, 分组key为： {}" ,dynamicKeyedBean.getKeyValue());
        if(ruleBeanState == null) ruleBeanState = ctx.getBroadcastState(StateDescContainer.ruleStateDesc);
        Iterable<Map.Entry<String, List<RuleStateBean>>> entries = ruleBeanState.immutableEntries();
        for (Map.Entry<String, List<RuleStateBean>> entry : entries) {
            log.debug("从广播状态中拿到一条规则，动态keyby字段为：{},规则条数为：{}" ,entry.getKey(),entry.getValue().size());
        }


        List<RuleStateBean> ruleStateBeans = ruleBeanState.get(dynamicKeyedBean.getKeyNames());
        log.debug("从广播状态中拿到规则state list的大小： {} " ,ruleStateBeans == null?null:ruleStateBeans.size()+"");

        for (RuleStateBean ruleStateBean : ruleStateBeans) {
            MarketingRule marketingRule = ruleStateBean.getMarketingRule();
            KieSession kieSession = ruleStateBean.getKieSession();

            RuleControllerFact ruleControllerFact = new RuleControllerFact(marketingRule, triggerModeRulelMatchService, dynamicKeyedBean.getEventBean(), false,null);

            long s = System.currentTimeMillis();
            // 调用drools引擎，执行计算
            kieSession.insert(ruleControllerFact);
            kieSession.fireAllRules();
            // 获取计算结果
            boolean matchResult = ruleControllerFact.isMatchResult();
            long e = System.currentTimeMillis();

            rule_calc_timeAmount.inc(e-s);
            if(matchResult){
                rule_match_calc_timeAmount.inc(e-s);
                rule_is_match_calc_cntAmount.inc();

                ruleIsMatchAvgTimeGauge.incRuleIsMatchCount();
                ruleIsMatchAvgTimeGauge.incRuleIsMatchTimeAmount(e-s);

            }else{
                rule_not_match_calc_timeAmount.inc(e-s);
                rule_not_match_calc_cntAmount.inc();
            }

            // 判断计算结果是否为true, 并继续判断是否有定时条件
            if(matchResult){
                // 再判断这个规则是否是一个带定时条件的规则
                if (marketingRule.isOnTimer()) {  // 是带定时条件的规则
                    // 从规则中取出所有定时条件
                    List<TimerCondition> timerConditionList = marketingRule.getTimerConditionList();
                    // 目前限定一个规则中只有一个时间条件
                    TimerCondition timerCondition = timerConditionList.get(0);
                    // 注册定时器
                    long triggerTime = dynamicKeyedBean.getEventBean().getTimeStamp() + timerCondition.getTimeLate();
                    ctx.timerService().registerEventTimeTimer(triggerTime);

                    // 在 定时信息state中进行记录
                    timerInfoState.add(Tuple2.of(ruleStateBean, triggerTime));

                } else {  // 不带定时条件
                    // 输出规则成立的结果
                    out.collect(new RuleMatchResult(dynamicKeyedBean.getKeyValue(),marketingRule.getRuleId(),dynamicKeyedBean.getEventBean().getTimeStamp(),System.currentTimeMillis()));
                }
            }
        }
    }

    @Override
    public void processBroadcastElement(CanalLogBean canalLogBean, Context ctx, Collector<RuleMatchResult> out) throws Exception {


    }


    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<RuleMatchResult> out) throws Exception {

        Iterator<Tuple2<RuleStateBean, Long>> iterator = timerInfoState.get().iterator();

        while (iterator.hasNext()) {
            // 拿到每一个 <规则  及其 注册定时器触发时间点>
            Tuple2<RuleStateBean, Long> tp = iterator.next();

            // 判断这个 “(规则:定时点)”，是否对应本次触发点
            if (tp.f1 == timestamp) {
                // 如果对应，检查该规则的定时条件(定时条件中包含的就是行为组合条件列表）
                RuleStateBean ruleStateBean = tp.f0;

                MarketingRule marketingRule = ruleStateBean.getMarketingRule();
                KieSession kieSession = ruleStateBean.getKieSession();

                TimerCondition timerCondition = marketingRule.getTimerConditionList().get(0);

                // 调用drools 计算
                TimerMatchDroolsFact timerMatchDroolsFact = new TimerMatchDroolsFact(ctx.getCurrentKey(), timerCondition, timestamp - timerCondition.getTimeLate(), timestamp,false);
                RuleControllerFact ruleControllerFact = new RuleControllerFact();
                ruleControllerFact.setTimerMatchDroolsFact(timerMatchDroolsFact);
                kieSession.insert(ruleControllerFact);
                kieSession.fireAllRules();

                boolean b = timerMatchDroolsFact.isTimerMatchResult();

                // 清除已经检查完毕的规则定时点state信息
                iterator.remove();

                if (b) {
                    RuleMatchResult ruleMatchResult = new RuleMatchResult(ctx.getCurrentKey(), marketingRule.getRuleId(), timestamp, ctx.timerService().currentProcessingTime());
                    out.collect(ruleMatchResult);
                }
            }

            // 增加删除过期定时信息的逻辑 - 双保险（一般情况下不会出现<的记录）
            if (tp.f1 < timestamp) {
                iterator.remove();
            }
        }

    }
}
