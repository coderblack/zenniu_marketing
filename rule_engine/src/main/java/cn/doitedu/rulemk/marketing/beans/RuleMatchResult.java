package cn.doitedu.rulemk.marketing.beans;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author 涛哥
 * @nick_name "deep as the sea"
 * @contact qq:657270652 wx:doit_edu
 * @site www.doitedu.cn
 * @date 2021-07-23
 * @desc 规则匹配计算结果
 */
@Data
@AllArgsConstructor
public class RuleMatchResult {
    String keyByValue;
    String ruleId;
    long trigEventTimestamp;
    long matchTimestamp;
}
