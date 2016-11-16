package org.springframework.data.mybatis.domains;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

/**
 * 审计实体基类.
 *
 * @param <PK> 主键类型
 * @author jarvis@caomeitu.com
 * @since 15/9/29
 */
@MappedSuperclass
public abstract class AbstractAuditable<PK extends Serializable> extends AbstractAuditableDateTime<PK> implements Auditable<PK> {

    private static final long serialVersionUID = 3732216348067110394L;

    @Column(name = "CREATOR")
    protected Long createdBy;

    @Column(name = "MODIFIER")
    protected Long lastModifiedBy;

    @Override
    public Long getCreatedBy() {
        return createdBy;
    }

    @Override
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public Long getLastModifiedBy() {
        return lastModifiedBy;
    }

    @Override
    public void setLastModifiedBy(Long lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

}
