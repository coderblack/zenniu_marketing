package cn.doitedu.rulemk.marketing.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/***
 * @author hunter.d
 * @qq 657270652
 * @wx haitao-duan
 * @date 2021/7/26
 *
 * 规则条件中，最原子的一个封装，封装“1个”事件条件
 *   要素：
 *   事件id
 *   事件属性约束
 *   事件时间约束
 *   事件次数约束
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventCondition {

    /**
     * 规则条件中的一个事件的id
     */
    private String eventId;

    /**
     * 规则条件中的一个事件的属性约束
     */
    private Map<String,String> eventProps;

    /**
     * 规则条件中的一个事件要求的发生时间段起始
     */
    private long timeRangeStart;

    /**
     * 规则条件中的一个事件要求的发生时间段终点
     */
    private long timeRangeEnd;


    /**
     * 规则条件中的一个事件要求的发生次数最小值
     */
    private int minLimit;


    /**
     * 规则条件中的一个事件要求的发生次数最大值
     */
    private int maxLimit;


}
