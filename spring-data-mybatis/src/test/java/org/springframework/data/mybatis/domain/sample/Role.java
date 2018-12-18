package org.springframework.data.mybatis.domain.sample;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.springframework.data.mybatis.annotation.Snowflake;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "ds_role")
@Data
@NoArgsConstructor
@RequiredArgsConstructor(staticName = "of")
public class Role {

	@Snowflake
	private Long id;

	@NonNull
	private String name;

}
