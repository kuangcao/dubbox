package com.alibaba.dubbo.config.guice;

import com.alibaba.dubbo.config.guice.api.Box;
import com.alibaba.dubbo.config.guice.api.DemoService;
import com.alibaba.dubbo.config.guice.api.DemoServiceSon;
import com.alibaba.dubbo.config.guice.api.HelloService;
import com.alibaba.dubbo.config.guice.impl.DemoServiceImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;


public class DubboClientModule extends AbstractModule {

    @Override
    protected void configure() {

        DubboConfigs.addReferenceSubPackageScan("com.alibaba.dubbo.config.guice.api", "1.0.0");
        DubboConfigs.addReferenceExcludeClass(Box.class);
        DubboConfigs.addReferenceExcludeClass(DemoServiceSon.class);
//        DubboConfigs.addServiceSubPackageScan("com.alibaba.dubbo.config.guice.api");
        install(new DubboModule());

    }

    public static void main(String[] args) {

        Injector injector = Guice.createInjector(new DubboClientModule());
        DemoService demoService = injector.getInstance(DemoService.class);
        HelloService helloService = injector.getInstance(HelloService.class);
        while (true) {
            try {
                System.out.println(demoService.sayName("hello"));
                System.out.println(helloService.sayHello("xiaoyage"));
                Thread.sleep(1L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
