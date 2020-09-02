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
package org.springframework.data.mybatis.querydsl;

/**
 * Pair.
 *
 * @param <F> first
 * @param <S> second
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class Pair<F, S> {

	private final F first;

	private final S second;

	public Pair(F first, S second) {
		this.first = first;
		this.second = second;
	}

	public static <F, S> Pair<F, S> of(F f, S s) {
		return new Pair<F, S>(f, s);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		else if (o instanceof Pair) {
			Pair p = (Pair) o;
			return equals(this.first, p.first) && equals(this.second, p.second);
		}
		else {
			return false;
		}
	}

	private static boolean equals(Object a, Object b) {
		return a == b || ((a != null) && a.equals(b));
	}

	public F getFirst() {
		return this.first;
	}

	public S getSecond() {
		return this.second;
	}

	@Override
	public int hashCode() {
		return 31 * ((this.first != null) ? this.first.hashCode() : 0)
				+ ((this.second != null) ? this.second.hashCode() : 0);
	}

}
