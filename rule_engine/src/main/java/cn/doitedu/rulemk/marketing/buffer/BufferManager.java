package cn.doitedu.rulemk.marketing.buffer;

import cn.doitedu.rulemk.marketing.beans.BufferData;

import java.util.Map;

public interface BufferManager {

    public BufferData getDataFromBuffer(String bufferKey);

    public boolean putDataToBuffer(BufferData bufferData);

    public boolean putDataToBuffer(String bufferKey, Map<String,String> valueMap);

    public void delBufferEntry(String bufferKey, String key);
}
