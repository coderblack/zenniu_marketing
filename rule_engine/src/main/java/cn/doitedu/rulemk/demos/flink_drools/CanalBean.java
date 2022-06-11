package cn.doitedu.rulemk.demos.flink_drools;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CanalBean {

    private List<DbRecord> data;
    private String type;

}
