package org.springframework.data.mybatis.domains;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotations.JdbcType;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;

/**
 * 审计实体仅时间基类.
 *
 * @param <PK> 主键类型.
 * @author jarvis@caomeitu.com
 * @since 15/9/29
 */
@MappedSuperclass
public abstract class AbstractAuditableDateTime<PK extends Serializable> extends AbstractPersistable<PK> {

    private static final long serialVersionUID = 3732216348067110393L;


    @Column(name = "CREATED_DATE")
    @JdbcType(org.apache.ibatis.type.JdbcType.BIGINT)
    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    protected Date createdDate;


    @Column(name = "LAST_MODIFIED_DATE")
    @JdbcType(org.apache.ibatis.type.JdbcType.BIGINT)
    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    protected Date lastModifiedDate;

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
