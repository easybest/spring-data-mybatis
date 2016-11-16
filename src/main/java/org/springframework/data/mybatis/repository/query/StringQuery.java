package org.springframework.data.mybatis.repository.query;

import org.springframework.util.Assert;

/**
 * Created by songjiawei on 2016/11/10.
 */
class StringQuery {

    private final String query;

    public StringQuery(String query) {
        Assert.hasText(query, "Query must not be null or empty!");

        this.query = query;
    }
}
