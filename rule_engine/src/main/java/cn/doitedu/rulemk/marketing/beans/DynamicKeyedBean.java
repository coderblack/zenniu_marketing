package cn.doitedu.rulemk.marketing.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 涛哥
 * @nick_name "deep as the sea"
 * @contact qq:657270652 wx:doit_edu
 * @site www.doitedu.cn
 * @date 2021-07-31
 * @desc 用于动态分区的一个包装类
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DynamicKeyedBean {

    // 山东省:济南市
    private String keyValue;

    // province,city
    private String keyNames;

    // 携带的数据本身
    private EventBean eventBean;


}
