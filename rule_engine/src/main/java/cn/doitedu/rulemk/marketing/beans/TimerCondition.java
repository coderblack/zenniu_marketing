package cn.doitedu.rulemk.marketing.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author 涛哥
 * @nick_name "deep as the sea"
 * @contact qq:657270652 wx:doit_edu
 * @site www.doitedu.cn
 * @date 2021-07-28
 * @desc 规则定时条件封装对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimerCondition {

    private long timeLate;

    private List<EventCombinationCondition> eventCombinationConditionList;

}
