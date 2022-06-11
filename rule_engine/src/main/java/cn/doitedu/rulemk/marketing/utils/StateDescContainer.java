package cn.doitedu.rulemk.marketing.utils;

import cn.doitedu.rulemk.marketing.beans.EventBean;
import cn.doitedu.rulemk.marketing.beans.MarketingRule;
import cn.doitedu.rulemk.marketing.beans.RuleStateBean;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.StateTtlConfig;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;

import java.util.List;

public class StateDescContainer {

    /**
     * 近期行为事件存储状态描述器
     */
    public static ListStateDescriptor<EventBean> getEventBeansDesc(){
        ListStateDescriptor<EventBean> eventBeansDesc = new ListStateDescriptor<>("event_beans", EventBean.class);
        StateTtlConfig ttlConfig = StateTtlConfig.newBuilder(Time.hours(2)).build();
        eventBeansDesc.enableTimeToLive(ttlConfig);

        return eventBeansDesc;
    }


    public static ListStateDescriptor<Tuple2<RuleStateBean, Long>> ruleTimerStateDesc
            =  new ListStateDescriptor<Tuple2<RuleStateBean, Long>>("rule_timer", TypeInformation.of(new TypeHint<Tuple2<RuleStateBean, Long>>() {}));




    // <分组keyby字段名,null>
    public static MapStateDescriptor<String, List<RuleStateBean> > ruleStateDesc
            = new MapStateDescriptor<>("rule_broadcast_state"
            ,TypeInformation.of(String.class)
            ,TypeInformation.of(new TypeHint<List<RuleStateBean>>() {}));
}
