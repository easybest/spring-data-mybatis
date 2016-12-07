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

package org.springframework.data.mybatis.domains;


import org.springframework.data.annotation.Transient;
import org.springframework.data.mybatis.annotations.MappedSuperclass;

import java.io.Serializable;
import java.util.Map;

/**
 * Extenable auditable.
 *
 * @param <PK> pk.
 * @author Jarvis Song
 */
@MappedSuperclass
public abstract class AbstractExtenableAuditable<PK extends Serializable> extends AbstractAuditable<PK> implements Extenable {
    protected Map<String, Object> extendedFields;

    @Override
    @Transient
    public Map<String, Object> getExtendedFields() {
        return extendedFields;
    }

    /**
     * 获取扩展字段值.
     *
     * @param extendedKey 扩展key
     * @return 值
     */
    @Transient
    public Object get(String extendedKey) {
        if (null == getExtendedFields()) {
            return null;
        }
        return getExtendedFields().get(extendedKey);
    }

    public void setExtendedFields(Map<String, Object> extendedFields) {
        this.extendedFields = extendedFields;
    }
}
