package cn.doitedu.rulemk.marketing.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kie.api.runtime.KieSession;

/**
 * @author 涛哥
 * @nick_name "deep as the sea"
 * @contact qq:657270652 wx:doit_edu
 * @site www.doitedu.cn
 * @date 2021-08-02
 * @desc 在广播状态中存储规则元信息的封装对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleStateBean {

    private MarketingRule marketingRule;

    private KieSession kieSession;


}
