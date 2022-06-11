package cn.doitedu.rulemk.marketing.dao;

import cn.doitedu.rulemk.marketing.beans.BufferData;
import cn.doitedu.rulemk.marketing.beans.EventCombinationCondition;
import cn.doitedu.rulemk.marketing.beans.EventCondition;
import cn.doitedu.rulemk.marketing.buffer.BufferManager;
import cn.doitedu.rulemk.marketing.buffer.BufferManagerImpl;
import cn.doitedu.rulemk.marketing.utils.ConfigNames;
import cn.doitedu.rulemk.marketing.utils.EventUtil;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 涛哥
 * @nick_name "deep as the sea"
 * @contact qq:657270652 wx:doit_edu
 * @site www.doitedu.cn
 * @date 2021-07-27
 * @desc  clickhouse 数据查询 dao 类
 */
@Slf4j
public class ClickHouseQuerierOld {
    Connection conn;
    BufferManager bufferManager;
    long bufferTtl;

    public ClickHouseQuerierOld(Connection conn){
        this.conn = conn;
        bufferManager = new BufferManagerImpl();
        Config config = ConfigFactory.load();
        bufferTtl = config.getLong(ConfigNames.REDIS_BUFFER_TTL);
    }


    /**
     * 在clickhouse中，根据组合条件及查询的时间范围，得到返回结果的1213形式字符串序列
     * @param eventCombinationCondition 行为组合条件
     * @param queryRangeStart  查询时间范围其实
     * @param queryRangeEnd 查询时间范围结束
     * @return 用户做过的组合条件中的事件的字符串序列
     */
    public String getEventCombinationConditionStr(String deviceId,
                                                   EventCombinationCondition eventCombinationCondition,
                                                   long queryRangeStart,
                                                   long queryRangeEnd) throws SQLException {

        String querySql = eventCombinationCondition.getQuerySql();
        PreparedStatement preparedStatement = conn.prepareStatement(querySql);
        preparedStatement.setString(1,deviceId);
        preparedStatement.setLong(2,queryRangeStart);
        preparedStatement.setLong(3,queryRangeEnd);

        // 从组合条件中取出该组合所关心的事件列表 [A C F]
        List<EventCondition> eventConditionList = eventCombinationCondition.getEventConditionList();
        List<String> ids = eventConditionList.stream().map(e -> e.getEventId()).collect(Collectors.toList());


        // 遍历ck返回的结果
        ResultSet resultSet = preparedStatement.executeQuery();
        StringBuilder sb = new StringBuilder();
        while(resultSet.next()){
            String eventId = resultSet.getString(1);
            // 根据eventId到组合条件的事件列表中找对应的索引号，来作为最终结果拼接
            sb.append((ids.indexOf(eventId)+1));
        }

        return sb.toString();
    }



    /**
     * 在clickhouse中，根据组合条件及查询的时间范围，查询该组合出现的次数
     * @param eventCombinationCondition 行为组合条件
     * @param queryRangeStart  查询时间范围其实
     * @param queryRangeEnd 查询时间范围结束
     * @return 出现的次数
     */
    public int queryEventCombinationConditionCount(String deviceId,
                                                   EventCombinationCondition eventCombinationCondition,
                                                   long queryRangeStart,
                                                   long queryRangeEnd) throws SQLException {


        /**
         * 缓存在什么情况下有用？
         *   缓存数据的时间范围： [t3 -> t8]
         *   查询条件的时间范围：
         *             [t3 -> t8]  直接用缓存的结果直接作为方法的返回值
         *             [t3 -> t10] 判断缓存数据的count值是否 >= 条件的count阈值，如果成立，则直接返回缓存结果；否则，用  “缓存的结果+t8->t10”   作为整个返回结果
         *             [t1 -> t8]  判断缓存数据的count值是否 >= 条件的count阈值，如果成立，则直接返回缓存结果；否则，用 “t1->t3+缓存的结果 ”  作为整个返回结果
         *             [t1 -> t10] 判断缓存数据的count值是否 >= 条件的count阈值，如果成立，则直接返回缓存结果；否则，无用
         *
         *   如下逻辑实现，其实没有考虑一个问题：
         *      valueMap中，可能同时存在多个上述的区间范围可能性
         *      下面在遍历缓存数据的时候，可能遇到一个满足条件的就直接判断并往下走了
         *
         *   最好的实现应该是：
         *       比如，条件是  2->12
         *           而valuemap中可能存在的缓存数据：
         *                 2->10
         *                 4->12
         *                 4->10
         *                 1->13
         *      先遍历一遍valueMap，从中找到最优的 缓存区间数据
         *      然后再去判断，并往下走
         *
         *
         */
        String bufferKey = deviceId + ":" + eventCombinationCondition.getCacheId();
        BufferData bufferData = bufferManager.getDataFromBuffer(bufferKey);
        Map<String, String> valueMap = bufferData.getValueMap();
        Set<String> keySet = valueMap.keySet();
        log.debug("dao-ck查询,获取缓存,bufferKey:{},valueMap:{}",bufferKey,valueMap);

        /**
         * key
         * deviceId:cacheId
         *
         * valueMap
         * key      value
         * 2:6:20    AAABDCC
         * 2:8:10    ADDFSDG
         * 0:6:30    AAACCF
         */
        long current = System.currentTimeMillis();
        for (String key : keySet) {
            String[] split = key.split(":");
            long bufferStartTime = Long.parseLong(split[0]);
            long bufferEndTime = Long.parseLong(split[1]);
            long bufferInsertTime = Long.parseLong(split[2]);


            //  判断缓存是否已过期，做清除动作
            if(System.currentTimeMillis() - bufferInsertTime >= bufferTtl) {
                bufferManager.delBufferEntry(bufferKey,key);
                log.debug("dao-ck,清除过期缓存,bufferKey: {},key: {}",bufferKey,key);
            }


            String bufferSeqStr = valueMap.get(key);

            // 查询范围 和  缓存范围  完全相同，直接返回缓存中数据的结果
            // 缓存: |------|
            // 查询: |------|
            if(bufferStartTime == queryRangeStart && bufferEndTime == queryRangeEnd){

                // 将原buffer数据从redis中清除，纯粹为了更新原缓存数据的insert时间戳
                bufferManager.delBufferEntry(bufferKey,key);
                HashMap<String, String> toPutMap = new HashMap<>();
                toPutMap.put(bufferStartTime+":"+bufferEndTime+":"+current,bufferSeqStr);
                bufferManager.putDataToBuffer(bufferKey,toPutMap);
                log.debug("缓存时段=查询时段,原缓存bufferKey:{},key:{}, 原缓存value:{}, 写入value:{}",bufferKey,key,valueMap,toPutMap);

                return EventUtil.sequenceStrMatchRegexCount(bufferSeqStr, eventCombinationCondition.getMatchPattern());
            }

            // 左端点对其，且条件的时间范围包含缓存的时间范围
            // 缓存: |---origin---|
            // 查询: |---origin---|--right---|
            if(queryRangeStart == bufferStartTime && queryRangeEnd >= bufferEndTime){
                int bufferCount = EventUtil.sequenceStrMatchRegexCount(bufferSeqStr, eventCombinationCondition.getMatchPattern());
                int queryMinCount = eventCombinationCondition.getMinLimit();
                // 是否满足阈值条件
                if(bufferCount>=queryMinCount) {
                    return bufferCount;
                }else{
                    // 调整查询时间，去clickhouse中查一小段
                    String rightStr = getEventCombinationConditionStr(deviceId, eventCombinationCondition, bufferEndTime, queryRangeEnd);

                    // 将原buffer数据从redis中清除
                    bufferManager.delBufferEntry(bufferKey,key);

                    HashMap<String, String> toPutMap = new HashMap<>();

                    // 写缓存，包含3种区间： 原buffer区间，  右边分段区间 ，  原buffer区间+右半边
                    // |---origin---|
                    //              |--right---|
                    // |---origin------right---|
                    toPutMap.put(bufferStartTime+":"+bufferEndTime+":"+current,bufferSeqStr);
                    toPutMap.put(bufferEndTime+":"+queryRangeEnd+":"+current,rightStr);
                    toPutMap.put(bufferStartTime+":"+queryRangeEnd+":"+current,bufferSeqStr+rightStr);
                    bufferManager.putDataToBuffer(bufferKey,toPutMap);

                    log.debug("缓存时段 右<=查询时段,原缓存bufferKey:{},key:{}, 原缓存value:{}, 写入value:{}",bufferKey,key,valueMap,toPutMap);


                    return EventUtil.sequenceStrMatchRegexCount(bufferSeqStr+rightStr, eventCombinationCondition.getMatchPattern());
                }
            }

            // 右端点对其，且条件的时间范围包含缓存的时间范围
            // 缓存:             |------origin-----|
            // 查询: |---left----|------origin-----|
            if(queryRangeEnd == bufferEndTime && queryRangeStart < bufferStartTime){
                int bufferCount = EventUtil.sequenceStrMatchRegexCount(bufferSeqStr, eventCombinationCondition.getMatchPattern());
                int queryMinCount = eventCombinationCondition.getMinLimit();
                if(bufferCount>=queryMinCount) {
                    return bufferCount;
                }else{
                    // 调整查询时间，去clickhouse中查一小段
                    String leftStr = getEventCombinationConditionStr(deviceId, eventCombinationCondition, queryRangeStart, bufferStartTime);


                    // 将原buffer数据从redis中清除
                    bufferManager.delBufferEntry(bufferKey,key);

                    // 写缓存，包含3种区间： 原buffer区间，  左分段区间 ，  左分段+原buffer区间
                    //             |------origin-----|
                    // |---left----|
                    // |---left-----------origin-----|
                    HashMap<String, String> toPutMap = new HashMap<>();
                    toPutMap.put(bufferStartTime+":"+bufferEndTime+":"+current,bufferSeqStr);
                    toPutMap.put(queryRangeStart+":"+bufferStartTime+":"+current,leftStr);
                    toPutMap.put(queryRangeStart+":"+queryRangeEnd+":"+current,leftStr+bufferSeqStr);
                    bufferManager.putDataToBuffer(bufferKey,toPutMap);

                    log.debug("查询时段 左< 缓存时段,原缓存bufferKey:{},key:{}, 原缓存value:{}, 写入value:{}",bufferKey,key,valueMap,toPutMap);

                    return EventUtil.sequenceStrMatchRegexCount(leftStr+bufferSeqStr, eventCombinationCondition.getMatchPattern());
                }
            }

            // 缓存:       |-------|
            // 查询:  |----------------|
            if(queryRangeEnd >= bufferEndTime && queryRangeStart <= bufferStartTime){
                int bufferCount = EventUtil.sequenceStrMatchRegexCount(bufferSeqStr, eventCombinationCondition.getMatchPattern());
                int queryMinCount = eventCombinationCondition.getMinLimit();
                if(bufferCount>=queryMinCount) {

                    // 将原buffer数据从redis中清除
                    bufferManager.delBufferEntry(bufferKey,key);
                    HashMap<String, String> toPutMap = new HashMap<>();
                    toPutMap.put(bufferStartTime+":"+bufferEndTime+":"+current,bufferSeqStr);
                    bufferManager.putDataToBuffer(bufferKey,toPutMap);
                    log.debug("查询时段 包含 缓存时段,原缓存bufferKey:{},key:{}, 原缓存value:{}, 写入value:{}",bufferKey,key,valueMap,toPutMap);
                    return bufferCount;
                }
            }
        }


        // 先查询到用户在组合条件中做过的事件的字符串序列 [AABAAAAAFFFCCCAAABBBCCCFFAAA]=> [11211111444333111222333441111]
        String eventSequenceStr = getEventCombinationConditionStr(deviceId, eventCombinationCondition, queryRangeStart, queryRangeEnd);
        // 将查询结果写入缓存
        HashMap<String, String> toPutMap = new HashMap<>();
        toPutMap.put(queryRangeStart+":"+queryRangeEnd+":"+current,eventSequenceStr);
        bufferManager.putDataToBuffer(bufferKey,toPutMap);
        log.debug("缓存穿透,查询clickhouse,写入bufferKey:{},写入value:{}",bufferKey,toPutMap);

        // 调用工具，来获取事件序列与正则表达式的匹配次数（既：组合条件发生的次数）
        int count = EventUtil.sequenceStrMatchRegexCount(eventSequenceStr, eventCombinationCondition.getMatchPattern());

        log.debug("在clickhouse中查询事件组合条件，得到的事件序列字符串：{} ,正则表达式：{}, 匹配结果： {}",eventSequenceStr,eventCombinationCondition.getMatchPattern(),count);

        return  count;
    }


}
