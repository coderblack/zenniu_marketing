package cn.doitedu.rulemk.demos;

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.util.Arrays;

/**
 * @author 涛哥
 * @nick_name "deep as the sea"
 * @contact qq:657270652 wx:doit_edu
 * @site www.doitedu.cn
 * @date 2021-07-31
 * @desc drools上手demo
 */
public class DroolsDemo {

    public static void main(String[] args) {
        KieServices kieServices = KieServices.Factory.get();
        //默认自动加载 META-INF/kmodule.xml
        KieContainer kieContainer = kieServices.getKieClasspathContainer();
        //kmodule.xml 中定义的 ksession name
        KieSession kieSession = kieContainer.newKieSession("s1");

        // 构造你的fact对象

        Person person = new Person("zhangsan", "manager", 38, "male", Arrays.asList("jack", "tom"), new Cat("kitty", "英短"));

        // 插入fact对象到规则引擎
        kieSession.insert(person);
        kieSession.fireAllRules();

        // 销毁会话
        kieSession.dispose();



    }
}
