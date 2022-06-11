package cn.doitedu.rulemk.marketing.dao;

import cn.doitedu.rulemk.marketing.beans.EventBean;
import cn.doitedu.rulemk.marketing.beans.EventCombinationCondition;
import cn.doitedu.rulemk.marketing.beans.EventCondition;
import cn.doitedu.rulemk.marketing.utils.EventUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.ListState;

import java.util.List;

@Slf4j
public class StateQuerier {

    ListState<EventBean> listState;

    public StateQuerier(ListState<EventBean> listState) {
        this.listState = listState;
    }


    /**
     * 在state中，根据组合条件及查询的时间范围，得到返回结果的1213形式字符串序列
     *
     * @param eventCombinationCondition 行为组合条件
     * @param queryRangeStart           查询时间范围其实
     * @param queryRangeEnd             查询时间范围结束
     * @return 用户做过的组合条件中的事件的字符串序列
     */
    public String getEventCombinationConditionStr(
                                                  EventCombinationCondition eventCombinationCondition,
                                                  long queryRangeStart,
                                                  long queryRangeEnd) throws Exception {

        // 获取状态state中的数据迭代器
        Iterable<EventBean> eventBeans = listState.get();

        // 获取事件组合条件中的感兴趣的事件
        List<EventCondition> eventConditionList = eventCombinationCondition.getEventConditionList();


        // 迭代state中的数据
        StringBuilder sb = new StringBuilder();
        for (EventBean eventBean : eventBeans) {
            if (eventBean.getTimeStamp() >= queryRangeStart
                    && eventBean.getTimeStamp() <= queryRangeEnd) {
                for (int i = 1; i <= eventConditionList.size(); i++) {
                    // 判断当前迭代到的bean，是否是条件中感兴趣的事件
                    if (EventUtil.eventMatchCondition(eventBean, eventConditionList.get(i - 1))) {
                        sb.append(i);
                        break;
                    }

                }

            }
        }

        return sb.toString();
    }


    /**
     * 在state中，根据组合条件及查询的时间范围，查询该组合出现的次数
     *
     * @param eventCombinationCondition 行为组合条件
     * @param queryRangeStart           查询时间范围其实
     * @param queryRangeEnd             查询时间范围结束
     * @return 出现的次数
     */
    public int queryEventCombinationConditionCount(
                                                   EventCombinationCondition eventCombinationCondition,
                                                   long queryRangeStart,
                                                   long queryRangeEnd) throws Exception {

        String sequenceStr = getEventCombinationConditionStr(eventCombinationCondition, queryRangeStart, queryRangeEnd);

        int count = EventUtil.sequenceStrMatchRegexCount(sequenceStr, eventCombinationCondition.getMatchPattern());
        log.debug("在state中查询事件组合条件，得到的事件序列字符串：{} ,正则表达式：{}, 匹配结果： {}",sequenceStr,eventCombinationCondition.getMatchPattern(),count);

        return count;
    }


}
