package com.alibaba.dubbo.config.guice;

import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.config.*;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Providers;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DubboModule extends AbstractModule {

    public static final String DUBBO_SERVICES_FILE = "dubbo.services.file";
    public static final String DUBBO_REFERENCES_FILE = "dubbo.references.file";
    public static final String PACKAGE = "package.";
    public static final String EXCLUDE_CLASS = "excludeClass.";
    private static Set<String> serviceSubPackages = new HashSet<String>();
    private static Set<Class> excludeServiceClasses = new HashSet<Class>(1);
    private static Map<String, String> referenceSubPackageMap = new HashMap<String, String>(1);
    private static Set<Class> excludeReferenceClasses = new HashSet<Class>(1);

    public static void addServiceSubPackageScan(final String subPackage) {
        serviceSubPackages.add(subPackage);
    }

    public static void addExcludeServiceClass(Class clasz) {
        excludeServiceClasses.add(clasz);
    }

    public static Set<Class> getExcludeServiceClasses() {
        return Collections.unmodifiableSet(excludeServiceClasses);
    }

    public static Set<String> getServiceSubPackages() {
        return Collections.unmodifiableSet(serviceSubPackages);
    }

    public static void addReferenceSubPackageScan(final String subPackage, final String version) {
        referenceSubPackageMap.put(subPackage, version);
    }

    public static void addExcludeReferenceClass(final Class excludeClass) {
        excludeReferenceClasses.add(excludeClass);
    }

    public static Set<Class> getExcludeReferenceClasses() {
        return Collections.unmodifiableSet(excludeReferenceClasses);
    }

    /**
     * key is class, value is version
     * @return
     */
    public static Map<String, String> getReferenceSubPackages() {
        return Collections.unmodifiableMap(referenceSubPackageMap);
    }

    private static volatile Properties SERVICE_PROPERTIES;
    private void initServices() {
        if (SERVICE_PROPERTIES == null) {
            synchronized (DubboModule.class) {
                if (SERVICE_PROPERTIES == null) {
                    String services = System.getProperty(DUBBO_SERVICES_FILE);
                    if (services == null || services.length() == 0) {
                        services = "dubbo-services.properties";
                    }
                    SERVICE_PROPERTIES = ConfigUtils.loadProperties(services, false, true);
                }
            }
        }
        if (SERVICE_PROPERTIES == null) {
            return;
        }
        for (String key : SERVICE_PROPERTIES.stringPropertyNames()) {
            if (key.startsWith(PACKAGE)) {
                serviceSubPackages.add(SERVICE_PROPERTIES.getProperty(key));
            } else if (key.startsWith(EXCLUDE_CLASS)) {
                try {
                    excludeServiceClasses.add(Class.forName(SERVICE_PROPERTIES.getProperty(key)));
                } catch (ClassNotFoundException e) {
                }
            }
        }
    }


    private static volatile Properties REFERENCE_PROPERTIES;
    private void initReferences() {
        if (REFERENCE_PROPERTIES == null) {
            synchronized (DubboModule.class) {
                if (REFERENCE_PROPERTIES == null) {
                    String services = System.getProperty(DUBBO_REFERENCES_FILE);
                    if (services == null || services.length() == 0) {
                        services = "dubbo-references.properties";
                    }
                    REFERENCE_PROPERTIES = ConfigUtils.loadProperties(services, false, true);
                }
            }
        }
        if (REFERENCE_PROPERTIES == null) {
            return;
        }
        for (String key : REFERENCE_PROPERTIES.stringPropertyNames()) {
            if (key.startsWith(EXCLUDE_CLASS)) {
                try {
                    excludeReferenceClasses.add(Class.forName(REFERENCE_PROPERTIES.getProperty(key)));
                } catch (ClassNotFoundException e) {
                }
            } else {
                referenceSubPackageMap.put(key, REFERENCE_PROPERTIES.getProperty(key));
            }
        }
    }

    @Override
    protected void configure() {
        initServices();
        initReferences();
        String applicationName = ConfigUtils.getProperty("dubbo.application.name");

        // 设置应用配置
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName(applicationName);
        String applicationVersion = ConfigUtils.getProperty("dubbo.application.version");
        applicationConfig.setVersion(applicationVersion);

        // 设置监控
        String monitorProtocol = ConfigUtils.getProperty("dubbo.monitor.protocol");
        if (monitorProtocol != null) {
            MonitorConfig monitorConfig = new MonitorConfig();
            monitorConfig.setProtocol(monitorProtocol);
            monitorConfig.setAddress(ConfigUtils.getProperty("dubbo.monitor.address"));
            applicationConfig.setMonitor(monitorConfig);
        }

        bind(ApplicationConfig.class).toInstance(applicationConfig);

        bindingRegistries(applicationConfig);

        bindingProtocols(applicationConfig);

        bind(DubboConfigs.class).asEagerSingleton();
    }


    private void bindingRegistries(ApplicationConfig applicationConfig) {
        String registryAddress = ConfigUtils.getProperty("dubbo.registry.address");
        String isRegister = ConfigUtils.getProperty("dubbo.registry.register");
        if (RegistryConfig.NO_AVAILABLE.equalsIgnoreCase(registryAddress)) {
            RegistryConfig registry = getRegistryConfig(isRegister, RegistryConfig.NO_AVAILABLE);
            applicationConfig.setRegistry(registry);
        } else if (registryAddress.indexOf(',') != -1) {
            String[] values = registryAddress.split("\\s*[,]+\\s*");
            List<RegistryConfig> registryConfigs = new ArrayList<RegistryConfig>();
            for (int i = 0; i < values.length; i++) {
                RegistryConfig registry = getRegistryConfig(isRegister, values[i]);
                registryConfigs.add(registry);
            }
            applicationConfig.setRegistries(registryConfigs);
        } else {
            RegistryConfig registry = getRegistryConfig(isRegister, registryAddress);
            applicationConfig.setRegistry(registry);
        }
    }

    private void bindingProtocols(ApplicationConfig applicationConfig) {
        // 服务提供者协议配置
        Multibinder<ProtocolConfig> pcBinder = Multibinder.newSetBinder(binder(), ProtocolConfig.class);
        String protocol = ConfigUtils.getProperty("dubbo.protocol.name");

        if (protocol == null) {
            // 默认采用dubbo协议
            protocol = "dubbo";
            pcBinder.addBinding().toInstance(getProtocolConfig(protocol));
        } else if (protocol.indexOf(',') != -1) {
            String[] protos = protocol.split("\\s*[,]+\\s*");
            for (String proto : protos) {
                pcBinder.addBinding().toInstance(getProtocolConfig(proto));
            }
        } else {
            pcBinder.addBinding().toInstance(getProtocolConfig(protocol));
        }


        try {
            importReferences(applicationConfig);
        } catch (ClassNotFoundException e) {
        }
    }

    private ProtocolConfig getProtocolConfig(String protocol) {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setName(protocol);
        String port = ConfigUtils.getProperty("dubbo.protocol." + protocol + ".port");
        if (port != null) {
            protocolConfig.setPort(Integer.getInteger(port));
        }
        String threads = ConfigUtils.getProperty("dubbo.protocol." + protocol + ".threads");
        if (threads != null) {
            protocolConfig.setThreads(Integer.getInteger(threads));
        }
        return protocolConfig;
    }

    private RegistryConfig getRegistryConfig(String isRegister, String value) {
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress(value);
        registry.setRegister(isRegister != null ? Boolean.valueOf(isRegister) : true);
        return registry;
    }

    private void importReferences(ApplicationConfig applicationConfig) throws ClassNotFoundException {
        Map<String, String> referenceSubPackages = getReferenceSubPackages();
        if (referenceSubPackages.size() == 0) {
            return;
        }
        for (Map.Entry<String, String> entry : referenceSubPackages.entrySet()) {
            Set<String> classNames = null;
            try {
                classNames = getClassNamesFromPackage(entry.getKey());
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (String className : classNames) {
                Class claz = Class.forName(className);
                if (claz.isInterface() && ! getExcludeReferenceClasses().contains(claz)) {
                    ReferenceConfig referenceConfig = new ReferenceConfig();
                    referenceConfig.setInterface(claz.getCanonicalName());
                    referenceConfig.setId(claz.getSimpleName());
                    referenceConfig.setVersion(entry.getValue());
                    referenceConfig.setMonitor(applicationConfig.getMonitor());
                    referenceConfig.setRegistry(applicationConfig.getRegistry());
                    referenceConfig.setRegistries(applicationConfig.getRegistries());
                    try {
                        bind(claz).toProvider(Providers.of(referenceConfig.get()));
                    } catch (Exception e) {
                        //TODO: log 引用不存在
                        throw e;
                    }
                }
            }
        }
    }

    private static Set<String> getClassNamesFromPackage(String packName) throws IOException, URISyntaxException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL packageURL;
        Set<String> names = new HashSet<String>();
        String packageName = packName.replace(".", "/");

        Enumeration<URL> packageUrls = classLoader.getResources(packageName);
        while (packageUrls.hasMoreElements()) {
            packageURL = packageUrls.nextElement();
            if (packageURL.getProtocol().equals("jar")) {
                String jarFileName;
                JarFile jf;
                Enumeration<JarEntry> jarEntries;
                String entryName;

                // build jar file name, then loop through zipped entries
                jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
                jarFileName = jarFileName.substring(5, jarFileName.indexOf("!"));
                System.out.println(">" + jarFileName);
                jf = new JarFile(jarFileName);
                jarEntries = jf.entries();
                while (jarEntries.hasMoreElements()) {
                    entryName = jarEntries.nextElement().getName();
                    if (entryName.startsWith(packageName) && entryName.length() > packageName.length() + 5) {
                        if (entryName.lastIndexOf(".class") != -1) {
                            entryName = entryName.substring(0, entryName.lastIndexOf('.')).replace('/', '.');
                            // 不递归
                            if (entryName.replace(packName,"").lastIndexOf('.') <= 0) {
                                names.add(entryName);
                            }
                        }
                    }
                }

                // loop through files in classpath
            } else {
                URI uri = new URI(packageURL.toString());
                File folder = new File(uri.getPath());
                // won't work with path which contains blank (%20)
                // File folder = new File(packageURL.getFile());
                File[] contenuti = folder.listFiles();
                String entryName;
                for (File actual : contenuti) {
                    if (actual.isDirectory()) {
                        continue;
                    }
                    entryName = actual.getName();
                    if (entryName.lastIndexOf(".class") != -1) {
                        entryName = packName + "." + entryName.substring(0, entryName.lastIndexOf(".class"));
                        names.add(entryName);
                    }
                }
            }
        }
        return names;
    }

}
