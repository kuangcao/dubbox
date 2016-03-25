package com.alibaba.dubbo.config.guice;

import com.alibaba.dubbo.config.guice.api.DemoService;
import com.alibaba.dubbo.config.guice.impl.DemoServiceImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;


public class DubboTestModule extends AbstractModule {

    @Override
    protected void configure() {

        DubboConfigs.addReferenceSubPackageScan("com.alibaba.dubbo.config.guice.api2");
        install(new DubboModule());
        bind(DemoService.class).to(DemoServiceImpl.class).asEagerSingleton();
        DubboConfigs.addServiceSubPackageScan("com.alibaba.dubbo.config.guice.api");
        bind(DubboConfigs.class).asEagerSingleton();
    }

    public static void main(String[] args) {

        Injector injector = Guice.createInjector(new DubboTestModule());
        DubboConfigs configs = injector.getInstance(DubboConfigs.class);
    }

}
