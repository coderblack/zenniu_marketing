package cn.doitedu.rulemk.marketing.dao;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class HbaseQuerier {
    Table table ;
    String familyName;

    public HbaseQuerier(Connection conn,String profileTableName,String familyName) throws IOException {
        table = conn.getTable(TableName.valueOf(profileTableName));
        this.familyName = familyName;
    }

    /**
     * 从hbase中查询画像条件是否满足
     * @param profileConditions  画像条件  {标签名->标签值，标签名->标签值，.....}
     * @param deviceId 要查询的用户标识
     * @return 是否匹配
     * @throws IOException 异常
     */
    public boolean queryProfileConditionIsMatch(Map<String,String> profileConditions,String deviceId) throws IOException {

        // 构造封装get条件
        Get get = new Get(deviceId.getBytes());
        Set<String> tags = profileConditions.keySet();
        for (String tag : tags) {
            get.addColumn(familyName.getBytes(),tag.getBytes());
        }

        // 执行get查询
        Result result = table.get(get);
        for (String tag : tags) {
            byte[] v = result.getValue(familyName.getBytes(), tag.getBytes());
            String value = new String(v);
            if(StringUtils.isBlank(value) || !profileConditions.get(tag).equals(value)) return false;
        }

        return true;
    }
}
