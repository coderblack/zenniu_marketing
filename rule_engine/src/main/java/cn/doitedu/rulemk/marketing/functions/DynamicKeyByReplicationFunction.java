package cn.doitedu.rulemk.marketing.functions;

import cn.doitedu.rulemk.marketing.beans.*;
import cn.doitedu.rulemk.marketing.utils.StateDescContainer;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;
import org.roaringbitmap.RoaringBitmap;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
public class DynamicKeyByReplicationFunction extends BroadcastProcessFunction<EventBean, CanalLogBean, DynamicKeyedBean> {

    BroadcastState<String, List<RuleStateBean>> broadcastState;



    /*
      动态keyby数据复制
     */
    @Override
    public void processElement(EventBean eventBean, ReadOnlyContext ctx, Collector<DynamicKeyedBean> out) throws Exception {

        ReadOnlyBroadcastState<String, List<RuleStateBean>> broadcastState = ctx.getBroadcastState(StateDescContainer.ruleStateDesc);
        if(broadcastState == null) return;
        for (Map.Entry<String, List<RuleStateBean>> entry : broadcastState.immutableEntries()) {
            String keyByFields = entry.getKey();
            StringBuilder sb = new StringBuilder();
            String[] fieldNames = keyByFields.split(",");
            // 拼装keyByFields中指定的每一个字段的值
            for (String fieldName : fieldNames) {
                Class<?> beanClass = Class.forName("cn.doitedu.rulemk.marketing.beans.EventBean");
                Field declaredField = beanClass.getDeclaredField(fieldName);
                declaredField.setAccessible(true);
                // 从eventbean中取分组字段的值
                String fieldValue = (String)declaredField.get(eventBean);
                sb.append(fieldValue).append(",");
            }
            String keyByValue = sb.toString().substring(0, sb.length() - 1);

            eventBean.setKeyByValue(keyByValue);

            DynamicKeyedBean dynamicKeyedBean = new DynamicKeyedBean(keyByValue, keyByFields, eventBean);
            out.collect(dynamicKeyedBean);
        }

    }

    @Override
    public void processBroadcastElement(CanalLogBean canalLogBean, Context ctx, Collector<DynamicKeyedBean> out) throws Exception {

        broadcastState = ctx.getBroadcastState(StateDescContainer.ruleStateDesc);

        // 规则平台上操作的规则的数据
        List<RuleTableRecord> ruleTableRecords = canalLogBean.getData();
        log.debug("规则平台，操作了规则数据，被广播方法处理，接收到的数据为：");
        if(ruleTableRecords == null ) return;


        // 规则平台上的操作的类型
        String operationType = canalLogBean.getType();

        // 根据不同的操作类型，去操作state中的规则数据
        for (RuleTableRecord ruleTableRecord : ruleTableRecords) {
            String rule_condition_json = ruleTableRecord.getRule_condition_json();
            String rule_controller_drl = ruleTableRecord.getRule_controller_drl();
            log.debug("接收到一条规则binlog，drl为： {}" ,rule_controller_drl);
            String rule_name = ruleTableRecord.getRule_name();

            MarketingRule marketingRule = JSON.parseObject(rule_condition_json, MarketingRule.class);

            String keyByFields = marketingRule.getKeyByFields();

            // 如果是插入操作，或者是 （设置状态为1的更新操作）
            // <ip,list(ruleStateBean1,ruleStateBean2,ruleStateBean3)>
            // <deviceId,list(ruleStateBean1,ruleStateBean2,ruleStateBean3)>
            List<RuleStateBean> ruleStateBeanList = broadcastState.get(keyByFields);

            if ("INSERT".equals(operationType) || ("UPDATE".equals(operationType) && "1".equals(ruleTableRecord.getRule_status()))) {

                if(ruleStateBeanList == null){
                    ruleStateBeanList = new ArrayList<>();
                }
                // 往该list中放入一个新的  规则bean对象
                KieSession kieSession = new KieHelper().addContent(rule_controller_drl, ResourceType.DRL).build().newKieSession();
                RuleStateBean ruleStateBean = new RuleStateBean(marketingRule, kieSession);
                ruleStateBeanList.add(ruleStateBean);
                broadcastState.put(keyByFields,ruleStateBeanList);
                log.debug("在动态keyby函数中，放入了一条规则，分组key为：{},规则list大小为：{}",keyByFields,broadcastState.get(keyByFields));
            }

            if ("DELETE".equals(operationType) || ("UPDATE".equals(operationType) && "0".equals(ruleTableRecord.getRule_status()))) {

                Iterator<RuleStateBean> iterator = ruleStateBeanList.iterator();
                while(iterator.hasNext()){

                    RuleStateBean stateBean = iterator.next();
                    if(stateBean.getMarketingRule().getRuleId().equals(rule_name)){
                        iterator.remove();
                    }
                }

                // 如果该keybyfields对应的ruleList已经被删空，则直接从广播state中将这个keybyFields条目都删除
                if(ruleStateBeanList.size()<1){
                    broadcastState.remove(keyByFields);
                }

            }

        }
    }
}
