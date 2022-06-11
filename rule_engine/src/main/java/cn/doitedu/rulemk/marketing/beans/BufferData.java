package cn.doitedu.rulemk.marketing.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BufferData {

    // 规则条件分组计算中的分组标识（如deviceId,或ip等）
    private String keyByValue;

    // 缓存id，用于唯一确定一个“规则条件”（事件约束相同）
    private String cacheId;

    // 缓存数据值（时间范围->事件序列）
    private Map<String,String> valueMap;

    // 返回拼接好的缓存顶层key
    public String getCacheKey(){
        return this.keyByValue+":"+this.cacheId;
    }

}



