package com.alibaba.dubbo.config.guice;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.config.*;
import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;

import java.util.*;

public class DubboConfigs {


    private static Set<String> serviceSubPackages = new HashSet<String>();
    private static Map<String, String> referenceSubPackageMap = new HashMap<String, String>(1);
    private static Set<Class> referenceExcludeClass = new HashSet<Class>(1);

    private ApplicationConfig applicationConfig;
    private RegistryConfig registryConfig;
    private ProtocolConfig protocolConfig;
    private final Injector injector;

    private static Set<ServiceConfig>  serviceConfigs = new HashSet<ServiceConfig>();

    public static void addServiceSubPackageScan(final String subPackage) {
        if (serviceSubPackages == null) {
            serviceSubPackages = new HashSet<String>();
        }
        serviceSubPackages.add(subPackage);
    }

    public static void addReferenceSubPackageScan(final String subPackage, final String version) {
        referenceSubPackageMap.put(subPackage, version);
    }

    public static void addReferenceExcludeClass(final Class excludeClass) {
        referenceExcludeClass.add(excludeClass);
    }

    public static Set<Class> getReferenceExcludeClasses() {
        return Collections.unmodifiableSet(referenceExcludeClass);
    }

    /**
     * key is class, value is version
     * @return
     */
    public static Map<String, String> getReferenceSubPackages() {
        return Collections.unmodifiableMap(referenceSubPackageMap);
    }

    @Inject
    public DubboConfigs(Injector injector, ApplicationConfig applicationConfig,
                        RegistryConfig registryConfig, ProtocolConfig protocolConfig) {
        this.injector = injector;
        this.applicationConfig = applicationConfig;
        this.registryConfig = registryConfig;
        this.protocolConfig = protocolConfig;
        exportServices();
    }

    private void exportServices() {
        if (CollectionUtils.isEmpty(serviceSubPackages)) {
            return;
        }
        for (Map.Entry<Key<?>, Binding<?>> binding : injector.getBindings()
                .entrySet()) {
            if (binding.getKey().getTypeLiteral().getRawType().isInterface()) {
                if (serviceSubPackages.contains(binding.getKey().getTypeLiteral().getRawType().getPackage().getName())) {
                    ServiceConfig serviceConfig = new ServiceConfig();
                    serviceConfig.setApplication(applicationConfig);
                    if (registryConfig.isRegister()) {
                        serviceConfig.setRegistry(registryConfig); // 多个注册中心可以用setRegistries()
                    }
                    serviceConfig.setProtocol(protocolConfig);
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
