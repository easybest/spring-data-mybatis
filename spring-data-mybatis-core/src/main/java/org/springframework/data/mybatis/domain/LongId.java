package org.springframework.data.mybatis.domain;

import java.io.Serializable;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@MappedSuperclass
public class LongId implements Serializable {

	@GeneratedValue(strategy = GenerationType.AUTO)
	@Id
	protected Long id;

	public LongId(Long id) {
		this.id = id;
	}

}
