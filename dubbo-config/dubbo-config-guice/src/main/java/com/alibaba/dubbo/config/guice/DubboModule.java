package com.alibaba.dubbo.config.guice;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.google.inject.AbstractModule;
import com.google.inject.util.Providers;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DubboModule extends AbstractModule {

    @Override
    protected void configure() {
        String applicationName = ConfigUtils.getProperty("dubbo.application.name");

        // 当前应用配置
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName(applicationName);
        String applicationVersion = ConfigUtils.getProperty("dubbo.application.version");
        applicationConfig.setVersion(applicationVersion);

        // 连接注册中心配置
        String registryAddress = ConfigUtils.getProperty("dubbo.registry.address");

        String isRegister = ConfigUtils.getProperty("dubbo.registry.register");
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress(registryAddress);
        if (isRegister!=null) {
            registry.setRegister(Boolean.valueOf(isRegister));
        }
        applicationConfig.setRegistry(registry);
        bind(RegistryConfig.class).toInstance(registry);

        // 服务提供者协议配置
        ProtocolConfig protocolConfig = new ProtocolConfig();

        String port = ConfigUtils.getProperty("dubbo.protocol.dubbo.port");
        if (port != null) {
            protocolConfig.setName("dubbo");
            protocolConfig.setPort(Integer.getInteger(port));
            String threads = ConfigUtils.getProperty("dubbo.protocol.dubbo.threads");
            if (threads != null) {
                protocolConfig.setThreads(Integer.getInteger(threads));
            }
        }

        bind(ApplicationConfig.class).toInstance(applicationConfig);
        bind(ProtocolConfig.class).toInstance(protocolConfig);

        try {
            importReferences(registry);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        bind(DubboConfigs.class).asEagerSingleton();
    }

    private void importReferences(RegistryConfig registry) throws ClassNotFoundException {
        Set<String> referenceSubPackages = DubboConfigs.getReferenceSubPackages();
        if (CollectionUtils.isEmpty(referenceSubPackages)) {
            return;
        }
        for (String subPackage : referenceSubPackages) {
            Set<String> classNames = null;
            try {
                classNames = getClassNamesFromPackage(subPackage);
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (String className : classNames) {
                Class claz = Class.forName(className);
                if (claz.isInterface()) {
                    ReferenceConfig referenceConfig = new ReferenceConfig();
                    referenceConfig.setInterface(claz.getCanonicalName());
                    referenceConfig.setId(claz.getSimpleName());
                    referenceConfig.setRegistry(registry);
                    bind(claz).toProvider(Providers.of(referenceConfig.get()));
                }
            }
        }
    }

    private static Set<String> getClassNamesFromPackage(String packName) throws IOException, URISyntaxException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL packageURL;
        Set<String> names = new HashSet<String>();
        String packageName = packName.replace(".", "/");
        packageURL = classLoader.getResource(packageName);

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

                    if (entryName.lastIndexOf('.') != -1) {
                        entryName = entryName.substring(0, entryName.lastIndexOf('.'));
                    }
                    names.add(entryName);
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
                entryName = actual.getName();
                if (entryName.lastIndexOf(".class") != -1) {
                    entryName = packName + "." + entryName.substring(0, entryName.lastIndexOf(".class"));
                }
                names.add(entryName);
            }
        }
        return names;
    }

}
