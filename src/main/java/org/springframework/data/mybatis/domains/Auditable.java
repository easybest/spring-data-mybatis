package org.springframework.data.mybatis.domains;

import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.util.Date;

/**
 * 审计实体接口.
 *
 * @param <ID> 主键类型.
 * @author jarvis@caomeitu.com
 * @since 15/10/11
 */
public interface Auditable<ID extends Serializable> extends Persistable<ID> {

    /**
     * Returns the user who created this entity.
     *
     * @return the createdBy
     */
    Long getCreatedBy();

    /**
     * Sets the user who created this entity.
     *
     * @param createdBy the creating entity to set
     */
    void setCreatedBy(final Long createdBy);

    /**
     * Returns the creation date of the entity.
     *
     * @return the createdDate
     */
    Date getCreatedDate();

    /**
     * Sets the creation date of the entity.
     *
     * @param creationDate the creation date to set
     */
    void setCreatedDate(final Date creationDate);

    /**
     * Returns the user who modified the entity lastly.
     *
     * @return the lastModifiedBy
     */
    Long getLastModifiedBy();

    /**
     * Sets the user who modified the entity lastly.
     *
     * @param lastModifiedBy the last modifying entity to set
     */
    void setLastModifiedBy(final Long lastModifiedBy);

    /**
     * Returns the date of the last modification.
     *
     * @return the lastModifiedDate
     */
    Date getLastModifiedDate();

    /**
     * Sets the date of the last modification.
     *
     * @param lastModifiedDate the date of the last modification to set
     */
    void setLastModifiedDate(final Date lastModifiedDate);
}
