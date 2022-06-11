package cn.doitedu.rulemk.marketing.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CanalLogBean {

    private List<RuleTableRecord> data;

    private String type;
}
