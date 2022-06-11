package cn.doitedu.rulemk.marketing.controller;

import cn.doitedu.rulemk.marketing.beans.*;
import cn.doitedu.rulemk.marketing.service.TriggerModeRulelMatchServiceImpl;
import cn.doitedu.rulemk.marketing.utils.EventUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.ListState;

import java.util.List;
import java.util.Map;

@Slf4j
public class TriggerModelRuleMatchController {

    TriggerModeRulelMatchServiceImpl triggerModeRulelMatchService;

    /**
     * 构造函数
     *
     * @param listState flink的状态句柄
     * @throws Exception 异常
     */
    public TriggerModelRuleMatchController(ListState<EventBean> listState) throws Exception {
        triggerModeRulelMatchService = new TriggerModeRulelMatchServiceImpl(listState);
    }

    /**
     * @param rule      营销规则封装对象
     * @param eventBean 数据流中的事件
     * @return 规则是否匹配
     * rule{
     * 触发条件
     * 画像条件
     * 行为组合条件
     * // 定时条件：{
     * 行为组合条件
     * } // 本方法不包含定时条件的计算
     * <p>
     * }
     */
    public boolean ruleIsMatch(MarketingRule rule, EventBean eventBean) throws Exception {

        //  判断当前数据bean是否满足规则的触发事件条件
        EventCondition triggerEventCondition = rule.getTriggerEventCondition();
        if (!EventUtil.eventMatchCondition(eventBean, triggerEventCondition)) {
            return false;
        }

        log.debug("规则:{},分组依据:{},分组key:{},事件:{},满足触发条件",rule.getRuleId(),rule.getKeyByFields(),eventBean.getKeyByValue(),eventBean.getEventId());
        // 判断规则中是否有画像条件，并进行计算
        Map<String, String> userProfileConditions = rule.getUserProfileConditions();
        if (userProfileConditions != null && userProfileConditions.size() > 0) {
            boolean b = triggerModeRulelMatchService.matchProfileCondition(userProfileConditions, eventBean.getKeyByValue());
            log.debug("规则:{},分组依据:{},分组key:{},画像条件匹配结果:{}",rule.getRuleId(),rule.getKeyByFields(),eventBean.getKeyByValue(),b);
            if (!b) return false;
        }

        // 判断规则中是否有行为组合条件，并进行计算  ①  ②  ③  ④     。。。  p1 ||  （ ① || ②  &&  ③  || ④ ）
        List<EventCombinationCondition> eventCombinationConditionList = rule.getEventCombinationConditionList();
        if (eventCombinationConditionList != null && eventCombinationConditionList.size() > 0) {
            // 循环遍历一次取一个“事件组合条件”进行计算
            for (EventCombinationCondition eventCombinationCondition : eventCombinationConditionList) {
                boolean b = triggerModeRulelMatchService.matchEventCombinationCondition(eventCombinationCondition, eventBean);
                log.debug("规则:{},分组依据:{},分组key:{},事件组合条件:{},计算结果:{}",rule.getRuleId(),rule.getKeyByFields(),eventBean.getKeyByValue(),eventCombinationCondition.getCacheId(),b);
                // 暂时写死（多个组合条件之间的关系是“且”）
                if (!b) {
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * 检查定时条件是否满足
     *
     * @param keyByValue
     * @param timerCondition
     * @param queryStartTime
     * @param queryEndTime
     * @return
     */
    public boolean isMatchTimeCondition(String keyByValue, TimerCondition timerCondition, long queryStartTime, long queryEndTime) throws Exception {

        List<EventCombinationCondition> eventCombinationConditionList = timerCondition.getEventCombinationConditionList();

        for (EventCombinationCondition eventCombinationCondition : eventCombinationConditionList) {

            eventCombinationCondition.setTimeRangeStart(queryStartTime);
            eventCombinationCondition.setTimeRangeEnd(queryEndTime);

            EventBean eventBean = new EventBean();

            // 因为service的条件计算方法中，需要知道查询deviceId，还需要一个时间戳来计算条件查询时用的分界点
            eventBean.setKeyByValue(keyByValue);
            eventBean.setTimeStamp(queryEndTime);

            boolean b = triggerModeRulelMatchService.matchEventCombinationCondition(eventCombinationCondition, eventBean);
            if (!b) return false;
        }

        return true;

    }

}
