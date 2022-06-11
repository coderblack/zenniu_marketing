package cn.doitedu.rulemk.marketing.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/***
 * @author hunter.d
 * @qq 657270652
 * @wx haitao-duan
 * @date 2021/7/26
 * 事件组合体条件封装  类似于： [C !W F G](>=2)  [A.*B.*C]
 *
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventCombinationCondition {

    /**
     * 组合条件的发生时间区间起始
     */
    private long timeRangeStart;

    /**
     * 组合条件的发生时间区间结束
     */
    private long timeRangeEnd;

    /**
     * 组合发生的最小次数
     */
    private int minLimit;

    /**
     * 组合发生的最大次数
     */
    private int maxLimit;




    /**
     * 组合条件中关心的事件的列表
     */
    // [{a,p1=v1}{y,p4=v2} !{c,p2=v1} {d,p3=p4,2=v8}]
    private List<EventCondition> eventConditionList;
    /**
     * 组合条件未来计算要用的正则匹配表达式
     */
    private String matchPattern;


    /**
     * 用于在数据库中过滤关心事件的sql
     */
    private String sqlType;
    private String querySql;


    /**
     * 条件缓存 ID
     */
    private String cacheId;
}
