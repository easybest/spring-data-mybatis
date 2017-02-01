package org.springframework.data.mybatis.mapping;

/**
 * @author Jarvis Song
 */
public class MybatisManyToManyAssociation extends MybatisOneToManyAssociation {

    public MybatisManyToManyAssociation(MybatisPersistentProperty inverse, MybatisPersistentProperty obverse) {
        super(inverse, obverse);
    }
}
