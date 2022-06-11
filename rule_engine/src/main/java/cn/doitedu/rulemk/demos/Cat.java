package cn.doitedu.rulemk.demos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.ws.BindingType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cat {

    private String name;
    private String pinZhong;
}
