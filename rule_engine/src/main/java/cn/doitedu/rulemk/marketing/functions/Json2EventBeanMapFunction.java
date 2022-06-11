package cn.doitedu.rulemk.marketing.functions;

import cn.doitedu.rulemk.marketing.beans.EventBean;
import com.alibaba.fastjson.JSON;
import org.apache.flink.api.common.functions.MapFunction;

/**
 * 原始行为日志json解析成bean对象：LogBean
 *
 */
public class Json2EventBeanMapFunction implements MapFunction<String, EventBean> {


    @Override
    public EventBean map(String value) throws Exception {
        EventBean eventBean = null;
        try {
            eventBean = JSON.parseObject(value, EventBean.class);
        }catch (Exception e){

        }
        return eventBean;
    }
}
