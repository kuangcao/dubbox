package com.alibaba.dubbo.config.guice;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.config.*;
import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;

import java.util.*;

public class DubboConfigs {

    private ApplicationConfig applicationConfig;
    private Set<ProtocolConfig> protocolConfigs;
    private final Injector injector;

    private static Set<ServiceConfig>  serviceConfigs = new HashSet<ServiceConfig>();

    @Inject
    protected DubboConfigs(Injector injector, ApplicationConfig applicationConfig, Set<ProtocolConfig> protocolConfigs) {
        this.injector = injector;
        this.applicationConfig = applicationConfig;
        this.protocolConfigs = protocolConfigs;
        exportServices();
    }

    private void exportServices() {
        Set<String> serviceSubPackages = DubboModule.getServiceSubPackages();
        Set<Class> excludeServiceClasses = DubboModule.getExcludeServiceClasses();
        if (CollectionUtils.isEmpty(serviceSubPackages)) {
            return;
        }
        for (Map.Entry<Key<?>, Binding<?>> binding : injector.getBindings()
                .entrySet()) {
            if (binding.getKey().getTypeLiteral().getRawType().isInterface()) {
                if (serviceSubPackages.contains(binding.getKey().getTypeLiteral().getRawType().getPackage().getName())
                        && !excludeServiceClasses.contains(binding.getKey().getTypeLiteral().getRawType())) {

                    ServiceConfig serviceConfig = new ServiceConfig();
                    serviceConfig.setApplication(applicationConfig);
                    serviceConfig.setRegistry(applicationConfig.getRegistry());
                    serviceConfig.setRegistries(applicationConfig.getRegistries());
                    serviceConfig.setMonitor(applicationConfig.getMonitor());
                    serviceConfig.setProtocols(new ArrayList<ProtocolConfig>(protocolConfigs));

                    serviceConfig.setInterface(binding.getKey().getTypeLiteral().getRawType().getCanonicalName());
                    serviceConfig.setRef(injector.getInstance(binding.getKey().getTypeLiteral().getRawType()));
                    serviceConfig.setVersion(applicationConfig.getVersion());
                    serviceConfig.export();

                    serviceConfigs.add(serviceConfig);
                }
            }
        }
    }

}
