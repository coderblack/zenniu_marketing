package cn.doitedu.rulemk.marketing.service;

import cn.doitedu.rulemk.marketing.beans.EventBean;
import cn.doitedu.rulemk.marketing.beans.EventCombinationCondition;
import cn.doitedu.rulemk.marketing.dao.ClickHouseQuerier;
import cn.doitedu.rulemk.marketing.dao.HbaseQuerier;
import cn.doitedu.rulemk.marketing.dao.StateQuerier;
import cn.doitedu.rulemk.marketing.utils.ConfigNames;
import cn.doitedu.rulemk.marketing.utils.ConnectionUtils;
import cn.doitedu.rulemk.marketing.utils.CrossTimeQueryUtil;
import cn.doitedu.rulemk.marketing.utils.EventUtil;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.java.tuple.Tuple2;

import java.io.IOException;
import java.sql.Connection;
import java.util.Map;

@Slf4j
public class TriggerModeRulelMatchServiceImpl {

    ClickHouseQuerier clickHouseQuerier;
    HbaseQuerier hbaseQuerier;
    StateQuerier stateQuerier;
    /**
     * 构造函数
     * @param listState
     * @throws Exception
     */
    public TriggerModeRulelMatchServiceImpl(ListState<EventBean> listState) throws Exception {
        Config config = ConfigFactory.load();

        Connection ckConn = ConnectionUtils.getClickhouseConnection();
        clickHouseQuerier = new ClickHouseQuerier(ckConn);

        org.apache.hadoop.hbase.client.Connection hbaseConn = ConnectionUtils.getHbaseConnection();
        String profileTableName = config.getString(ConfigNames.HBASE_PROFILE_TABLE);
        String profileFamilyName = config.getString(ConfigNames.HBASE_PROFILE_FAMILY);
        hbaseQuerier = new HbaseQuerier(hbaseConn, profileTableName, profileFamilyName);

        stateQuerier = new StateQuerier(listState);
    }
    /**
     * 画像条件匹配
     * @param profileCondition 画像条件
     * @param deviceId 用户标识
     * @return 是否匹配
     * @throws IOException 异常
     */
    public boolean matchProfileCondition(Map<String,String> profileCondition,String deviceId) throws IOException {

        return hbaseQuerier.queryProfileConditionIsMatch(profileCondition,deviceId);
    }
    /**
     *  计算（一个）行为组合条件是否匹配
     */
    public boolean matchEventCombinationCondition(EventCombinationCondition combinationCondition, EventBean event) throws Exception {

        // 获取当前事件时间对应的查询分界点
        long segmentPoint = CrossTimeQueryUtil.getSegmentPoint(event.getTimeStamp());

        // 判断条件的时间区间是否跨分界点
        long conditionStart = combinationCondition.getTimeRangeStart();
        long conditionEnd = combinationCondition.getTimeRangeEnd();

        String keyByValue = event.getKeyByValue();

        // 查state状态
        if(conditionStart >= segmentPoint){
            int count = stateQuerier.queryEventCombinationConditionCount(combinationCondition, conditionStart, conditionEnd);
            log.debug("事件组合条件:{},区间落在state中,条件起始:{},分界点:{},结果:{}",combinationCondition.getCacheId(),conditionStart,segmentPoint,count);
            return count >= combinationCondition.getMinLimit() && count <= combinationCondition.getMaxLimit();
        }

        // 查clickhouse
        else if(conditionEnd<segmentPoint){
            Tuple2<String, Integer> resTuple = clickHouseQuerier.queryEventCombinationConditionCount(keyByValue, combinationCondition, conditionStart, conditionEnd);
            log.debug("事件组合条件:{},区间落在clickhouse中,条件结束点:{},分界点:{},结果:{}",combinationCondition.getCacheId(),conditionStart,segmentPoint,resTuple.f1);
            return resTuple.f1 >= combinationCondition.getMinLimit() && resTuple.f1 <= combinationCondition.getMaxLimit();
        }

        // 跨区段查询
        else {
            // 先查一次state，看是否能提前结束
            int count = stateQuerier.queryEventCombinationConditionCount(combinationCondition, segmentPoint, conditionEnd);
            log.debug("事件组合条件:{},区间跨界在state尝试,条件起始点:{},条件结束点:{},分界点:{},结果:{}",combinationCondition.getCacheId(),conditionStart,conditionEnd,segmentPoint,count);
            if(count >= combinationCondition.getMinLimit()) return true;

            /**
             * 再查一次clickhouse，看是否能提前结束
             * 以便在后续的分段组合查询时，一定是state或ck子段都无法独立满足条件的情况
             */
            Tuple2<String, Integer> resTupleCk = clickHouseQuerier.queryEventCombinationConditionCount(keyByValue, combinationCondition, conditionStart, segmentPoint,true);
            log.debug("事件组合条件:{},区间跨界,在ck中尝试,条件起始点:{},条件结束点:{},分界点:{},结果:{}",combinationCondition.getCacheId(),conditionStart,conditionEnd,segmentPoint,resTupleCk.f1);
            if(resTupleCk.f1 >= combinationCondition.getMinLimit()) return true;


            /**
             * TODO BUG 此处的调用，违反了buffer工作层级的原则，导致此处会跳过缓存处理
             * 补救思路1：在getEventCombinationConditionStr方法中添加缓存处理逻辑，那将是灾难性的
             * 补救思路2：改造queryEventCombinationConditionCount方法，不仅返回count值，同时携带str返回（此str不能是缓存处理中阶段后的部分结果，而应该是整个str结果）
             */
            // 分段组合查询,先从ck中查询序列字符串,再从state中查询序列字符串,拼接后作为整体匹配正则表达式
            //Tuple2<String, Integer> resTuple = clickHouseQuerier.queryEventCombinationConditionCount(event.getDeviceId(), combinationCondition, conditionStart, segmentPoint, true);
            String str2 = stateQuerier.getEventCombinationConditionStr(combinationCondition, segmentPoint, conditionEnd);

            count = EventUtil.sequenceStrMatchRegexCount(resTupleCk.f0 + str2, combinationCondition.getMatchPattern());
            log.debug("事件组合条件:{},区间跨界在分段组合查询,条件起始点:{},条件结束点:{},分界点:{},str1:{},str2{},总结果:{}",combinationCondition.getCacheId(),conditionStart,conditionEnd,segmentPoint,resTupleCk.f0,str2,count);

            // 判断是否匹配成功
            return count >= combinationCondition.getMinLimit() && count <= combinationCondition.getMaxLimit();
        }

    }
}
