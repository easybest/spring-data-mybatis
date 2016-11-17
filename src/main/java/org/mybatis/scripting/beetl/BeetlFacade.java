/*
 *
 *   Copyright 2016 the original author or authors.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.mybatis.scripting.beetl;

import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * Beetl script facade.
 *
 * @author Jarvis Song
 */
public final class BeetlFacade {


    GroupTemplate gt = null;
    Properties    ps = null;

    private final static BeetlFacade instance = new BeetlFacade();

    public static BeetlFacade getInstance() {
        return instance;
    }


    private BeetlFacade() {
        try {
            ps = loadDefaultConfig();
            Properties ext = loadExtConfig();
            ps.putAll(ext);
            StringSqlTemplateLoader resourceLoader = new StringSqlTemplateLoader();
            Configuration cfg = new Configuration(ps);
            gt = new GroupTemplate(resourceLoader, cfg);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    public static String apply(String script, Map<String, Object> context) {
        Template template = getInstance().getGroupTemplate().getTemplate(script);
        template.fastBinding(context);
//        template.binding(context);
//        if (null != template.getCtx().globalVar) {
//            context.putAll(template.getCtx().globalVar);
//        }
        return template.render();
    }

    /**
     * load default configuration.
     *
     * @return Properties
     */
    public Properties loadDefaultConfig() {
        Properties ps = new Properties();
        InputStream ins = this.getClass().getResourceAsStream(
                "/mybatis-beetl.properties");
        if (ins == null) {
            return ps;
        }
        try {
            ps.load(ins);
        } catch (IOException e) {
            throw new RuntimeException("load default mybatis beetl configuration error: /mybatis-beetl.properties");
        }
        return ps;
    }

    /**
     * load ext configuration.
     *
     * @return Properties
     */
    public Properties loadExtConfig() {
        Properties ps = new Properties();
        InputStream ins = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "mybatis-beetl-ext.properties");
        if (ins == null) {
            return ps;
        }
        try {
            ps.load(ins);
            ins.close();
        } catch (IOException e) {
            throw new RuntimeException("load ext mybatis beetl configuration error: /mybatis-beetl-ext.properties");
        }
        return ps;
    }

    public GroupTemplate getGroupTemplate() {
        return gt;
    }

}
