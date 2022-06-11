package cn.doitedu.rulemk.demos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Person {

    private String name;
    private String job;
    private int age;
    private String gender;

    private List<String> nickNames;

    private Cat cat;

}
