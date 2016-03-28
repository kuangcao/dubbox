/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.config.guice.impl;

import com.alibaba.dubbo.config.guice.api.Box;
import com.alibaba.dubbo.config.guice.api.DemoService;

/**
 * DemoServiceImpl
 * 
 * @author william.liangf
 */
public class DemoServiceImpl implements DemoService {
    
    private String prefix = "say:";

    public String sayName(String name) {
        return prefix + name;
    }

    public Box getBox() {
        return null;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

}