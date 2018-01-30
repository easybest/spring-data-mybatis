package org.springframework.data.mybatis.dialect;

/**
 * Represents the InnoDB storage engine.
 *
 * @author Vlad Mihalcea
 */
public class InnoDBStorageEngine implements MySQLStorageEngine {

	public static final MySQLStorageEngine INSTANCE = new InnoDBStorageEngine();

	@Override
	public boolean supportsCascadeDelete() {
		return true;
	}

	@Override
	public String getTableTypeString(String engineKeyword) {
		return String.format(" %s=InnoDB", engineKeyword);
	}

	@Override
	public boolean hasSelfReferentialForeignKeyBug() {
		return true;
	}

	@Override
	public boolean dropConstraints() {
		return true;
	}
}
