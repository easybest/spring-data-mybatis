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

package org.springframework.data.mybatis.repository.support;

import org.springframework.data.domain.Sort;
import org.springframework.data.mybatis.repository.localism.Localism;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Jarvis Song
 */
public class MybatisMapperGenerator {

    private final MybatisEntityModel model; // entity's mapper model
    private final Localism           localism;

    public MybatisMapperGenerator(MybatisEntityModel model, Localism localism) {
        this.model = model;
        this.localism = localism;
    }


    public String buildSelectColumns(boolean basic) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, MybatisEntityModel> entry : model.getPrimaryKeys().entrySet()) {
            builder.append(quota(model.getName()) + "." + entry.getValue().getNameInDatabase()).append(" as ").append(quota(entry.getValue().getName())).append(",");
        }
        for (Map.Entry<String, MybatisEntityModel> entry : model.getColumns().entrySet()) {
            builder.append(quota(model.getName()) + "." + entry.getValue().getNameInDatabase()).append(" as ").append(quota(entry.getValue().getName())).append(",");
        }

        if (basic) {
            for (Map.Entry<String, MybatisEntityModel> entry : model.getJoinColumns().entrySet()) {
                builder.append(quota(model.getName()) + "." + entry.getValue().getNameInDatabase()).append(" as ").append(quota(entry.getValue().getName())).append(",");
            }
        } else {
            buildColumnsAssociations(builder, model.getOneToOnes());
            buildColumnsAssociations(builder, model.getManyToOnes());
        }

        if (builder.length() > 0 && builder.charAt(builder.length() - 1) == ',') {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.toString();
    }

    private void buildColumnsAssociations(StringBuilder builder, Map<String, MybatisEntityModel> associations) {
        for (Map.Entry<String, MybatisEntityModel> entry : associations.entrySet()) {
            for (Map.Entry<String, MybatisEntityModel> ent : entry.getValue().getPrimaryKeys().entrySet()) {
                builder.append(quota(model.getName() + "." + entry.getKey()) + "." + ent.getValue().getNameInDatabase()).append(" as ").append(quota(entry.getKey() + "." + ent.getValue().getName())).append(",");
            }
            for (Map.Entry<String, MybatisEntityModel> ent : entry.getValue().getColumns().entrySet()) {
                builder.append(quota(model.getName() + "." + entry.getKey()) + "." + ent.getValue().getNameInDatabase()).append(" as ").append(quota(entry.getKey() + "." + ent.getValue().getName())).append(",");
            }
            for (Map.Entry<String, MybatisEntityModel> ent : entry.getValue().getJoinColumns().entrySet()) {
                builder.append(quota(model.getName() + "." + entry.getKey()) + "." + ent.getValue().getNameInDatabase()).append(" as ").append(quota(entry.getKey() + "." + ent.getValue().getName())).append(",");
            }
        }
    }

    public String buildFrom(boolean basic) {
        StringBuilder builder = new StringBuilder();
        builder.append(model.getNameInDatabase()).append(" ").append(quota(model.getName()));
        if (!basic) {
            builder.append(buildLeftOuterJoin());
        }
        return builder.toString();
    }

    private String buildLeftOuterJoin() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, MybatisEntityModel> entry : model.getOneToOnes().entrySet()) {
            builder.append(" left outer join ").append(entry.getValue().getNameInDatabase()).append(" ").append(quota(model.getName() + "." + entry.getKey()))
                    .append(" on ").append(quota(model.getName())).append(".").append(entry.getValue().getJoinColumnName())
                    .append("=").append(quota(model.getName() + "." + entry.getKey())).append(".").append(entry.getValue().getJoinReferencedColumnName());
        }
        for (Map.Entry<String, MybatisEntityModel> entry : model.getManyToOnes().entrySet()) {
            builder.append(" left outer join ").append(entry.getValue().getNameInDatabase()).append(" ").append(quota(model.getName() + "." + entry.getKey()))
                    .append(" on ").append(quota(model.getName())).append(".").append(entry.getValue().getJoinColumnName())
                    .append("=").append(quota(model.getName() + "." + entry.getKey())).append(".").append(entry.getValue().getJoinReferencedColumnName());
        }
        return builder.toString();
    }

    private String quota(String alias) {
        return localism.openQuote() + alias + localism.closeQuote();
    }

    public String buildSorts(boolean basic, Sort sort) {
        StringBuilder builder = new StringBuilder();

        if (null != sort) {
            Map<String, String> map = new HashMap<String, String>();
            String[] arr = buildSelectColumns(basic).split(",");
            for (String s : arr) {
                if (StringUtils.isEmpty(s)) {
                    continue;
                }
                String[] ss = s.split(" as ");
                String key = ss[ss.length - 1];
                String val = ss[0];
                key = key.replace(String.valueOf(localism.openQuote()), "").replace(String.valueOf(localism.closeQuote()), "");
                map.put(key, val);
            }

            builder.append(" order by ");
            for (Iterator<Sort.Order> iterator = sort.iterator(); iterator.hasNext(); ) {
                Sort.Order order = iterator.next();
                String p = map.get(order.getProperty());
                builder.append((StringUtils.isEmpty(p) ? order.getProperty() : p) + " " + order.getDirection().name() + ",");
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
        } else {
            builder.append("<if test=\"_sorts != null\">");
            builder.append("<bind name=\"_columnsMap\" value='#{");
            String[] arr = buildSelectColumns(basic).split(",");
            for (String s : arr) {
                if (StringUtils.isEmpty(s)) {
                    continue;
                }
                String[] ss = s.split(" as ");
                String key = ss[ss.length - 1];
                String val = ss[0];
                key = key.replace(String.valueOf(localism.openQuote()), "").replace(String.valueOf(localism.closeQuote()), "");
                val = val.replace("\"", "\\\"");
                builder.append(String.format("\"%s\":\"%s\",", key, val));
            }
            if (builder.charAt(builder.length() - 1) == ',') {
                builder.deleteCharAt(builder.length() - 1);
            }
            builder.append("}' />");
            builder.append(" order by ");
            builder.append("<foreach item=\"item\" index=\"idx\" collection=\"_sorts\" open=\"\" separator=\",\" close=\"\">");
            builder.append("<if test=\"item.ignoreCase\">lower(</if>").append("${_columnsMap[item.property]}").append("<if test=\"item.ignoreCase\">)</if>").append(" ${item.direction}");
            builder.append("</foreach>");
            builder.append("</if>");
        }
        return builder.toString();
    }


}
