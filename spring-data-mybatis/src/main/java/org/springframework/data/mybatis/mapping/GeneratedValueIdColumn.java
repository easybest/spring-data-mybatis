package org.springframework.data.mybatis.mapping;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;

/**
 * @author Jarvis Song
 */
@Getter
@Setter
public class GeneratedValueIdColumn extends IdColumn {

	private GenerationType strategy;
	private String generator;
	private SequenceGenerator sequenceGenerator;

	public GeneratedValueIdColumn(Table table) {
		super(table);
	}

	@Override
	public GeneratedValueIdColumn clone() {
		GeneratedValueIdColumn copy = new GeneratedValueIdColumn(this.table);
		BeanUtils.copyProperties(this, copy);
		return copy;
	}

	@Override
	public String toString() {
		return "GeneratedValueIdColumn{" + "strategy=" + strategy + ", generator='" + generator + '\'' + ", name='" + name
				+ '\'' + ", prefix='" + prefix + '\'' + ", alias='" + alias + '\'' + '}';
	}
}
