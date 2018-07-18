package org.springframework.data.mybatis.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

@Data
@NoArgsConstructor
@MappedSuperclass
public class LongId implements Serializable {

	@GeneratedValue(strategy = GenerationType.AUTO) @Id protected Long id;

	public LongId(Long id) {
		this.id = id;
	}
}
