package com.alibaba.dubbo.config.guice;

import com.google.inject.Inject;
import com.google.inject.Provider;


public class ServiceProvider<T> implements Provider<T> {

    private final DubboConfigs dubboConfigs;

    @Inject
    public ServiceProvider(DubboConfigs dubboConfigs) {
        this.dubboConfigs = dubboConfigs;
    }

    @Override
    public T get() {
        return null;
    }
}
