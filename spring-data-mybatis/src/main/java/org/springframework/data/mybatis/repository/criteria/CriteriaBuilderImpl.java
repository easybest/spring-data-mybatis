package org.springframework.data.mybatis.repository.criteria;

import javax.persistence.Tuple;
import javax.persistence.criteria.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class CriteriaBuilderImpl implements CriteriaBuilder {

	@Override
	public CriteriaQuery<Object> createQuery() {
		return new CriteriaQueryImpl<>(this, Object.class);
	}

	@Override
	public <T> CriteriaQuery<T> createQuery(Class<T> resultClass) {
		return new CriteriaQueryImpl<>(this, resultClass);
	}

	@Override
	public CriteriaQuery<Tuple> createTupleQuery() {
		return new CriteriaQueryImpl<>(this, Tuple.class);
	}

	@Override
	public <T> CriteriaUpdate<T> createCriteriaUpdate(Class<T> targetEntity) {
		return null;
	}

	@Override
	public <T> CriteriaDelete<T> createCriteriaDelete(Class<T> targetEntity) {
		return null;
	}

	@Override
	public <Y> CompoundSelection<Y> construct(Class<Y> resultClass, Selection<?>... selections) {
		return null;
	}

	@Override
	public CompoundSelection<Tuple> tuple(Selection<?>... selections) {
		return null;
	}

	@Override
	public CompoundSelection<Object[]> array(Selection<?>... selections) {
		return null;
	}

	@Override
	public Order asc(Expression<?> x) {
		return null;
	}

	@Override
	public Order desc(Expression<?> x) {
		return null;
	}

	@Override
	public <N extends Number> Expression<Double> avg(Expression<N> x) {
		return null;
	}

	@Override
	public <N extends Number> Expression<N> sum(Expression<N> x) {
		return null;
	}

	@Override
	public Expression<Long> sumAsLong(Expression<Integer> x) {
		return null;
	}

	@Override
	public Expression<Double> sumAsDouble(Expression<Float> x) {
		return null;
	}

	@Override
	public <N extends Number> Expression<N> max(Expression<N> x) {
		return null;
	}

	@Override
	public <N extends Number> Expression<N> min(Expression<N> x) {
		return null;
	}

	@Override
	public <X extends Comparable<? super X>> Expression<X> greatest(Expression<X> x) {
		return null;
	}

	@Override
	public <X extends Comparable<? super X>> Expression<X> least(Expression<X> x) {
		return null;
	}

	@Override
	public Expression<Long> count(Expression<?> x) {
		return null;
	}

	@Override
	public Expression<Long> countDistinct(Expression<?> x) {
		return null;
	}

	@Override
	public Predicate exists(Subquery<?> subquery) {
		return null;
	}

	@Override
	public <Y> Expression<Y> all(Subquery<Y> subquery) {
		return null;
	}

	@Override
	public <Y> Expression<Y> some(Subquery<Y> subquery) {
		return null;
	}

	@Override
	public <Y> Expression<Y> any(Subquery<Y> subquery) {
		return null;
	}

	@Override
	public Predicate and(Expression<Boolean> x, Expression<Boolean> y) {
		return null;
	}

	@Override
	public Predicate and(Predicate... restrictions) {
		return null;
	}

	@Override
	public Predicate or(Expression<Boolean> x, Expression<Boolean> y) {
		return null;
	}

	@Override
	public Predicate or(Predicate... restrictions) {
		return null;
	}

	@Override
	public Predicate not(Expression<Boolean> restriction) {
		return null;
	}

	@Override
	public Predicate conjunction() {
		return null;
	}

	@Override
	public Predicate disjunction() {
		return null;
	}

	@Override
	public Predicate isTrue(Expression<Boolean> x) {
		return null;
	}

	@Override
	public Predicate isFalse(Expression<Boolean> x) {
		return null;
	}

	@Override
	public Predicate isNull(Expression<?> x) {
		return null;
	}

	@Override
	public Predicate isNotNull(Expression<?> x) {
		return null;
	}

	@Override
	public Predicate equal(Expression<?> x, Expression<?> y) {
		return null;
	}

	@Override
	public Predicate equal(Expression<?> x, Object y) {
		return null;
	}

	@Override
	public Predicate notEqual(Expression<?> x, Expression<?> y) {
		return null;
	}

	@Override
	public Predicate notEqual(Expression<?> x, Object y) {
		return null;
	}

	@Override
	public <Y extends Comparable<? super Y>> Predicate greaterThan(Expression<? extends Y> x, Expression<? extends Y> y) {
		return null;
	}

	@Override
	public <Y extends Comparable<? super Y>> Predicate greaterThan(Expression<? extends Y> x, Y y) {
		return null;
	}

	@Override
	public <Y extends Comparable<? super Y>> Predicate greaterThanOrEqualTo(Expression<? extends Y> x,
			Expression<? extends Y> y) {
		return null;
	}

	@Override
	public <Y extends Comparable<? super Y>> Predicate greaterThanOrEqualTo(Expression<? extends Y> x, Y y) {
		return null;
	}

	@Override
	public <Y extends Comparable<? super Y>> Predicate lessThan(Expression<? extends Y> x, Expression<? extends Y> y) {
		return null;
	}

	@Override
	public <Y extends Comparable<? super Y>> Predicate lessThan(Expression<? extends Y> x, Y y) {
		return null;
	}

	@Override
	public <Y extends Comparable<? super Y>> Predicate lessThanOrEqualTo(Expression<? extends Y> x,
			Expression<? extends Y> y) {
		return null;
	}

	@Override
	public <Y extends Comparable<? super Y>> Predicate lessThanOrEqualTo(Expression<? extends Y> x, Y y) {
		return null;
	}

	@Override
	public <Y extends Comparable<? super Y>> Predicate between(Expression<? extends Y> v, Expression<? extends Y> x,
			Expression<? extends Y> y) {
		return null;
	}

	@Override
	public <Y extends Comparable<? super Y>> Predicate between(Expression<? extends Y> v, Y x, Y y) {
		return null;
	}

	@Override
	public Predicate gt(Expression<? extends Number> x, Expression<? extends Number> y) {
		return null;
	}

	@Override
	public Predicate gt(Expression<? extends Number> x, Number y) {
		return null;
	}

	@Override
	public Predicate ge(Expression<? extends Number> x, Expression<? extends Number> y) {
		return null;
	}

	@Override
	public Predicate ge(Expression<? extends Number> x, Number y) {
		return null;
	}

	@Override
	public Predicate lt(Expression<? extends Number> x, Expression<? extends Number> y) {
		return null;
	}

	@Override
	public Predicate lt(Expression<? extends Number> x, Number y) {
		return null;
	}

	@Override
	public Predicate le(Expression<? extends Number> x, Expression<? extends Number> y) {
		return null;
	}

	@Override
	public Predicate le(Expression<? extends Number> x, Number y) {
		return null;
	}

	@Override
	public <N extends Number> Expression<N> neg(Expression<N> x) {
		return null;
	}

	@Override
	public <N extends Number> Expression<N> abs(Expression<N> x) {
		return null;
	}

	@Override
	public <N extends Number> Expression<N> sum(Expression<? extends N> x, Expression<? extends N> y) {
		return null;
	}

	@Override
	public <N extends Number> Expression<N> sum(Expression<? extends N> x, N y) {
		return null;
	}

	@Override
	public <N extends Number> Expression<N> sum(N x, Expression<? extends N> y) {
		return null;
	}

	@Override
	public <N extends Number> Expression<N> prod(Expression<? extends N> x, Expression<? extends N> y) {
		return null;
	}

	@Override
	public <N extends Number> Expression<N> prod(Expression<? extends N> x, N y) {
		return null;
	}

	@Override
	public <N extends Number> Expression<N> prod(N x, Expression<? extends N> y) {
		return null;
	}

	@Override
	public <N extends Number> Expression<N> diff(Expression<? extends N> x, Expression<? extends N> y) {
		return null;
	}

	@Override
	public <N extends Number> Expression<N> diff(Expression<? extends N> x, N y) {
		return null;
	}

	@Override
	public <N extends Number> Expression<N> diff(N x, Expression<? extends N> y) {
		return null;
	}

	@Override
	public Expression<Number> quot(Expression<? extends Number> x, Expression<? extends Number> y) {
		return null;
	}

	@Override
	public Expression<Number> quot(Expression<? extends Number> x, Number y) {
		return null;
	}

	@Override
	public Expression<Number> quot(Number x, Expression<? extends Number> y) {
		return null;
	}

	@Override
	public Expression<Integer> mod(Expression<Integer> x, Expression<Integer> y) {
		return null;
	}

	@Override
	public Expression<Integer> mod(Expression<Integer> x, Integer y) {
		return null;
	}

	@Override
	public Expression<Integer> mod(Integer x, Expression<Integer> y) {
		return null;
	}

	@Override
	public Expression<Double> sqrt(Expression<? extends Number> x) {
		return null;
	}

	@Override
	public Expression<Long> toLong(Expression<? extends Number> number) {
		return null;
	}

	@Override
	public Expression<Integer> toInteger(Expression<? extends Number> number) {
		return null;
	}

	@Override
	public Expression<Float> toFloat(Expression<? extends Number> number) {
		return null;
	}

	@Override
	public Expression<Double> toDouble(Expression<? extends Number> number) {
		return null;
	}

	@Override
	public Expression<BigDecimal> toBigDecimal(Expression<? extends Number> number) {
		return null;
	}

	@Override
	public Expression<BigInteger> toBigInteger(Expression<? extends Number> number) {
		return null;
	}

	@Override
	public Expression<String> toString(Expression<Character> character) {
		return null;
	}

	@Override
	public <T> Expression<T> literal(T value) {
		return null;
	}

	@Override
	public <T> Expression<T> nullLiteral(Class<T> resultClass) {
		return null;
	}

	@Override
	public <T> ParameterExpression<T> parameter(Class<T> paramClass) {
		return null;
	}

	@Override
	public <T> ParameterExpression<T> parameter(Class<T> paramClass, String name) {
		return null;
	}

	@Override
	public <C extends Collection<?>> Predicate isEmpty(Expression<C> collection) {
		return null;
	}

	@Override
	public <C extends Collection<?>> Predicate isNotEmpty(Expression<C> collection) {
		return null;
	}

	@Override
	public <C extends Collection<?>> Expression<Integer> size(Expression<C> collection) {
		return null;
	}

	@Override
	public <C extends Collection<?>> Expression<Integer> size(C collection) {
		return null;
	}

	@Override
	public <E, C extends Collection<E>> Predicate isMember(Expression<E> elem, Expression<C> collection) {
		return null;
	}

	@Override
	public <E, C extends Collection<E>> Predicate isMember(E elem, Expression<C> collection) {
		return null;
	}

	@Override
	public <E, C extends Collection<E>> Predicate isNotMember(Expression<E> elem, Expression<C> collection) {
		return null;
	}

	@Override
	public <E, C extends Collection<E>> Predicate isNotMember(E elem, Expression<C> collection) {
		return null;
	}

	@Override
	public <V, M extends Map<?, V>> Expression<Collection<V>> values(M map) {
		return null;
	}

	@Override
	public <K, M extends Map<K, ?>> Expression<Set<K>> keys(M map) {
		return null;
	}

	@Override
	public Predicate like(Expression<String> x, Expression<String> pattern) {
		return null;
	}

	@Override
	public Predicate like(Expression<String> x, String pattern) {
		return null;
	}

	@Override
	public Predicate like(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar) {
		return null;
	}

	@Override
	public Predicate like(Expression<String> x, Expression<String> pattern, char escapeChar) {
		return null;
	}

	@Override
	public Predicate like(Expression<String> x, String pattern, Expression<Character> escapeChar) {
		return null;
	}

	@Override
	public Predicate like(Expression<String> x, String pattern, char escapeChar) {
		return null;
	}

	@Override
	public Predicate notLike(Expression<String> x, Expression<String> pattern) {
		return null;
	}

	@Override
	public Predicate notLike(Expression<String> x, String pattern) {
		return null;
	}

	@Override
	public Predicate notLike(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar) {
		return null;
	}

	@Override
	public Predicate notLike(Expression<String> x, Expression<String> pattern, char escapeChar) {
		return null;
	}

	@Override
	public Predicate notLike(Expression<String> x, String pattern, Expression<Character> escapeChar) {
		return null;
	}

	@Override
	public Predicate notLike(Expression<String> x, String pattern, char escapeChar) {
		return null;
	}

	@Override
	public Expression<String> concat(Expression<String> x, Expression<String> y) {
		return null;
	}

	@Override
	public Expression<String> concat(Expression<String> x, String y) {
		return null;
	}

	@Override
	public Expression<String> concat(String x, Expression<String> y) {
		return null;
	}

	@Override
	public Expression<String> substring(Expression<String> x, Expression<Integer> from) {
		return null;
	}

	@Override
	public Expression<String> substring(Expression<String> x, int from) {
		return null;
	}

	@Override
	public Expression<String> substring(Expression<String> x, Expression<Integer> from, Expression<Integer> len) {
		return null;
	}

	@Override
	public Expression<String> substring(Expression<String> x, int from, int len) {
		return null;
	}

	@Override
	public Expression<String> trim(Expression<String> x) {
		return null;
	}

	@Override
	public Expression<String> trim(Trimspec ts, Expression<String> x) {
		return null;
	}

	@Override
	public Expression<String> trim(Expression<Character> t, Expression<String> x) {
		return null;
	}

	@Override
	public Expression<String> trim(Trimspec ts, Expression<Character> t, Expression<String> x) {
		return null;
	}

	@Override
	public Expression<String> trim(char t, Expression<String> x) {
		return null;
	}

	@Override
	public Expression<String> trim(Trimspec ts, char t, Expression<String> x) {
		return null;
	}

	@Override
	public Expression<String> lower(Expression<String> x) {
		return null;
	}

	@Override
	public Expression<String> upper(Expression<String> x) {
		return null;
	}

	@Override
	public Expression<Integer> length(Expression<String> x) {
		return null;
	}

	@Override
	public Expression<Integer> locate(Expression<String> x, Expression<String> pattern) {
		return null;
	}

	@Override
	public Expression<Integer> locate(Expression<String> x, String pattern) {
		return null;
	}

	@Override
	public Expression<Integer> locate(Expression<String> x, Expression<String> pattern, Expression<Integer> from) {
		return null;
	}

	@Override
	public Expression<Integer> locate(Expression<String> x, String pattern, int from) {
		return null;
	}

	@Override
	public Expression<Date> currentDate() {
		return null;
	}

	@Override
	public Expression<Timestamp> currentTimestamp() {
		return null;
	}

	@Override
	public Expression<Time> currentTime() {
		return null;
	}

	@Override
	public <T> In<T> in(Expression<? extends T> expression) {
		return null;
	}

	@Override
	public <Y> Expression<Y> coalesce(Expression<? extends Y> x, Expression<? extends Y> y) {
		return null;
	}

	@Override
	public <Y> Expression<Y> coalesce(Expression<? extends Y> x, Y y) {
		return null;
	}

	@Override
	public <Y> Expression<Y> nullif(Expression<Y> x, Expression<?> y) {
		return null;
	}

	@Override
	public <Y> Expression<Y> nullif(Expression<Y> x, Y y) {
		return null;
	}

	@Override
	public <T> Coalesce<T> coalesce() {
		return null;
	}

	@Override
	public <C, R> SimpleCase<C, R> selectCase(Expression<? extends C> expression) {
		return null;
	}

	@Override
	public <R> Case<R> selectCase() {
		return null;
	}

	@Override
	public <T> Expression<T> function(String name, Class<T> type, Expression<?>... args) {
		return null;
	}

	@Override
	public <X, T, V extends T> Join<X, V> treat(Join<X, T> join, Class<V> type) {
		return null;
	}

	@Override
	public <X, T, E extends T> CollectionJoin<X, E> treat(CollectionJoin<X, T> join, Class<E> type) {
		return null;
	}

	@Override
	public <X, T, E extends T> SetJoin<X, E> treat(SetJoin<X, T> join, Class<E> type) {
		return null;
	}

	@Override
	public <X, T, E extends T> ListJoin<X, E> treat(ListJoin<X, T> join, Class<E> type) {
		return null;
	}

	@Override
	public <X, K, T, V extends T> MapJoin<X, K, V> treat(MapJoin<X, K, T> join, Class<V> type) {
		return null;
	}

	@Override
	public <X, T extends X> Path<T> treat(Path<X> path, Class<T> type) {
		return null;
	}

	@Override
	public <X, T extends X> Root<T> treat(Root<X> root, Class<T> type) {
		return null;
	}
}
