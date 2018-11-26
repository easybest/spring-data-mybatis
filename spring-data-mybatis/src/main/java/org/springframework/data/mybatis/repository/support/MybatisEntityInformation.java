package org.springframework.data.mybatis.repository.support;

import org.springframework.data.mybatis.repository.query.MybatisEntityMetadata;
import org.springframework.data.repository.core.EntityInformation;

public interface MybatisEntityInformation<T, ID>
		extends EntityInformation<T, ID>, MybatisEntityMetadata<T> {


}
