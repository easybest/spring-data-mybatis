package org.springframework.data.mybatis.mapping;

/**
 * @author Jarvis Song
 */
public class MybatisOneToManyAssociation extends MybatisAssociation {

    public MybatisOneToManyAssociation(MybatisPersistentProperty inverse, MybatisPersistentProperty obverse) {
        super(inverse, obverse);
    }
}
