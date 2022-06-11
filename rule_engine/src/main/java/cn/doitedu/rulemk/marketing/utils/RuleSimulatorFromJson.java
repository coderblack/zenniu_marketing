package cn.doitedu.rulemk.marketing.utils;

import cn.doitedu.rulemk.marketing.beans.EventCombinationCondition;
import cn.doitedu.rulemk.marketing.beans.EventCondition;
import cn.doitedu.rulemk.marketing.beans.MarketingRule;
import cn.doitedu.rulemk.marketing.beans.TimerCondition;
import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;


import java.io.File;
import java.io.IOException;
import java.util.*;

public class RuleSimulatorFromJson {

    public static List<MarketingRule> getRuleList() throws IOException {
        String json1 = FileUtils.readFileToString(new File("rule_engine/rules/rule1.json"), "utf-8");
        String json2 = FileUtils.readFileToString(new File("rule_engine/rules/rule2.json"), "utf-8");

        MarketingRule rule1 = JSON.parseObject(json1, MarketingRule.class);
        MarketingRule rule2 = JSON.parseObject(json2, MarketingRule.class);
        return Arrays.asList(rule1,rule2);
    }


    public static void main(String[] args) throws IOException {
        List<MarketingRule> ruleList = getRuleList();
        MarketingRule marketingRule = ruleList.get(0);

        TimerCondition timerCondition = new TimerCondition();
        timerCondition.setTimeLate(30*60*1000);
        EventCombinationCondition eventCombinationCondition = new EventCombinationCondition();
        eventCombinationCondition.setCacheId("004");
        Map<String, String> props = new HashMap<>();
        EventCondition ec1 = new EventCondition("H", props, 1000000000000L, 20000000000000L, 0, 0);
        eventCombinationCondition.setEventConditionList(Arrays.asList(ec1));
        timerCondition.setEventCombinationConditionList(Arrays.asList(eventCombinationCondition));
        marketingRule.setTimerConditionList(Arrays.asList(timerCondition));


        System.out.println(JSON.toJSONString(marketingRule));

    }

}
