package cn.doitedu.rulemk.marketing.buffer;

import cn.doitedu.rulemk.marketing.beans.BufferData;
import cn.doitedu.rulemk.marketing.utils.ConnectionUtils;
import redis.clients.jedis.Jedis;

import java.util.Map;

/**
 * @author 涛哥
 * @nick_name "deep as the sea"
 * @contact qq:657270652 wx:doit_edu
 * @site www.doitedu.cn
 * @date 2021-07-30
 * @desc 缓存管理工具
 */
public class BufferManagerImpl implements BufferManager {

    Jedis jedis;
    long period;
    public BufferManagerImpl(){
        jedis = ConnectionUtils.getRedisConnection();
    }


    @Override
    public BufferData getDataFromBuffer(String bufferKey) {
        Map<String, String> valueMap = jedis.hgetAll(bufferKey);

        String[] split = bufferKey.split(":");

        return new BufferData(split[0],split[1],valueMap);
    }

    @Override
    public boolean putDataToBuffer(BufferData bufferData) {

        String hmset = jedis.hmset(bufferData.getKeyByValue() + ":" + bufferData.getCacheId(), bufferData.getValueMap());

        return "OK".equals(hmset);
    }

    @Override
    public boolean putDataToBuffer(String bufferKey, Map<String,String> valueMap) {

        String hmset = jedis.hmset(bufferKey, valueMap);

        return "OK".equals(hmset);

    }

    @Override
    public void delBufferEntry(String bufferKey, String key) {
        jedis.hdel(bufferKey,key);

    }
}
