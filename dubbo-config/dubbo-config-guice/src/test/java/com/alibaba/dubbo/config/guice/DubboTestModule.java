package com.alibaba.dubbo.config.guice;

import com.alibaba.dubbo.config.guice.api.DemoService;
import com.alibaba.dubbo.config.guice.api.DemoServiceSon;
import com.alibaba.dubbo.config.guice.api.HelloService;
import com.alibaba.dubbo.config.guice.impl.DemoServiceImpl;
import com.alibaba.dubbo.config.guice.impl.DemoServiceSonImpl;
import com.alibaba.dubbo.config.guice.impl.HelloServiceImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;


public class DubboTestModule extends AbstractModule {

    @Override
    protected void configure() {

//        DubboConfigs.addReferenceSubPackageScan("com.alibaba.dubbo.config.guice.api2");
        DubboConfigs.addServiceSubPackageScan("com.alibaba.dubbo.config.guice.api");
        install(new DubboModule());
        bind(DemoService.class).to(DemoServiceImpl.class).asEagerSingleton();
        bind(DemoServiceSon.class).to(DemoServiceSonImpl.class).asEagerSingleton();
        bind(HelloService.class).to(HelloServiceImpl.class).asEagerSingleton();
    }

    public static void main(String[] args) {

        Injector injector = Guice.createInjector(new DubboTestModule());
        DubboConfigs configs = injector.getInstance(DubboConfigs.class);
        while (true) {
            try {
                Thread.sleep(1L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
