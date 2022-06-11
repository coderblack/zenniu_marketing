package cn.doitedu.rulemk.marketing.utils;

import cn.doitedu.rulemk.marketing.beans.EventBean;
import cn.doitedu.rulemk.marketing.beans.EventCondition;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 * @author hunter.d
 * @qq 657270652
 * @wx haitao-duan
 * @date 2021/7/25
 **/

@Slf4j
public class EventUtil {

    public static String eventSeq2Str(Iterator<EventBean> eventSeq, List<EventCondition> eventConditionList){
        StringBuilder sb = new StringBuilder();
        while(eventSeq.hasNext()){
            EventBean next = eventSeq.next();
            for(int i=1;i<=eventConditionList.size();i++)
            if(eventMatchCondition(next,eventConditionList.get(i-1))) sb.append(i);
        }
        return sb.toString();
    }

    public static int sequenceStrMatchRegexCount(String eventStr, String pattern){
        Pattern r = Pattern.compile(pattern);
        Matcher matcher = r.matcher(eventStr);
        int count = 0;
        while(matcher.find()) count ++;
        //log.debug("字符串正则匹配,正则表达式：{}, 匹配结果为：{} ,字符串为：{} ",pattern,count,eventStr);
        return count;
    }

    public static boolean eventMatchCondition(EventBean bean,EventCondition eventCondition){
        if (bean.getEventId().equals(eventCondition.getEventId())) {
            Set<String> keys = eventCondition.getEventProps().keySet();
            for (String key : keys) {
                String conditionValue = eventCondition.getEventProps().get(key);
                if (!conditionValue.equals(bean.getProperties().get(key))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

}
