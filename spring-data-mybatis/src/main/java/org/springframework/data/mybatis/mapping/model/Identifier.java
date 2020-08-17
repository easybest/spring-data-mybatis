/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mybatis.mapping.model;

import java.util.Locale;

import lombok.Getter;
import lombok.Setter;

import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.util.StringUtils;

/**
 * Models an identifier (name), which may or may not be quoted.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class Identifier implements Comparable<Identifier> {

	@Getter
	private final String text;

	@Getter
	private final boolean quoted;

	@Getter
	@Setter
	private Dialect dialect;

	public static Identifier toIdentifier(String text) {
		return toIdentifier(text, false);
	}

	public static Identifier toIdentifier(String text, boolean quote) {
		if (StringUtils.isEmpty(text)) {
			return null;
		}

		final String trimmedText = text.trim();
		if (isQuoted(trimmedText)) {
			final String bareName = trimmedText.substring(1, trimmedText.length() - 1);
			return new Identifier(bareName, true);
		}
		return new Identifier(trimmedText, quote);
	}

	public static boolean isQuoted(String name) {
		return (name.startsWith("`") && name.endsWith("`")) || (name.startsWith("[") && name.endsWith("]"))
				|| (name.startsWith("\"") && name.endsWith("\""));
	}

	public static boolean areEqual(Identifier id1, Identifier id2) {
		if (null == id1) {
			return null == id2;
		}
		return id1.equals(id2);
	}

	public static Identifier quote(Identifier identifier) {
		return identifier.isQuoted() ? identifier : Identifier.toIdentifier(identifier.getText(), true);
	}

	public Identifier(String text, boolean quoted) {
		if (StringUtils.isEmpty(text)) {
			throw new IllegalIdentifierException("Identifier text cannot be null.");
		}
		if (isQuoted(text)) {
			throw new IllegalIdentifierException("Identifier text should not contain quote makers (` or \")");
		}
		this.text = text;
		this.quoted = quoted;
	}

	protected Identifier(String text) {
		this.text = text;
		this.quoted = false;
	}

	public String render(Dialect dialect) {
		return this.isQuoted() ? (dialect.openQuote() + this.getText() + dialect.closeQuote()) : this.getText();
	}

	public String render() {
		if (null != this.dialect) {
			return this.render(this.dialect);
		}
		return this.isQuoted() ? ('`' + this.getText() + '`') : this.getText();
	}

	public String getCanonicalName() {
		return this.isQuoted() ? this.getText() : this.getText().toLowerCase(Locale.ENGLISH);
	}

	@Override
	public int compareTo(Identifier o) {
		return this.getCanonicalName().compareTo(o.getCanonicalName());
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Identifier)) {
			return false;
		}

		final Identifier that = (Identifier) o;
		return this.getCanonicalName().equals(that.getCanonicalName());
	}

	@Override
	public int hashCode() {
		return this.isQuoted() ? this.text.hashCode() : this.text.toLowerCase(Locale.ENGLISH).hashCode();
	}

	@Override
	public String toString() {
		return this.render();
	}

}
