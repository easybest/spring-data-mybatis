package org.springframework.data.mybatis.repository.criteria;

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.EntityType;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

public class CriteriaQueryImpl<T> implements CriteriaQuery<T>, Serializable {

	private final CriteriaBuilderImpl criteriaBuilder;
	private final Class<T> returnType;
	private final QueryStructure<T> queryStructure;

	public CriteriaQueryImpl(CriteriaBuilderImpl criteriaBuilder, Class<T> returnType) {
		this.criteriaBuilder = criteriaBuilder;
		this.returnType = returnType;
		this.queryStructure = new QueryStructure<T>(this, criteriaBuilder);
	}

	@Override
	public CriteriaQuery<T> select(Selection<? extends T> selection) {
		return null;
	}

	@Override
	public CriteriaQuery<T> multiselect(Selection<?>... selections) {
		return null;
	}

	@Override
	public CriteriaQuery<T> multiselect(List<Selection<?>> selectionList) {
		return null;
	}

	@Override
	public <X> Root<X> from(Class<X> entityClass) {
		return null;
	}

	@Override
	public <X> Root<X> from(EntityType<X> entity) {
		return null;
	}

	@Override
	public CriteriaQuery<T> where(Expression<Boolean> restriction) {
		return null;
	}

	@Override
	public CriteriaQuery<T> where(Predicate... restrictions) {
		return null;
	}

	@Override
	public CriteriaQuery<T> groupBy(Expression<?>... grouping) {
		return null;
	}

	@Override
	public CriteriaQuery<T> groupBy(List<Expression<?>> grouping) {
		return null;
	}

	@Override
	public CriteriaQuery<T> having(Expression<Boolean> restriction) {
		return null;
	}

	@Override
	public CriteriaQuery<T> having(Predicate... restrictions) {
		return null;
	}

	@Override
	public CriteriaQuery<T> orderBy(Order... o) {
		return null;
	}

	@Override
	public CriteriaQuery<T> orderBy(List<Order> o) {
		return null;
	}

	@Override
	public CriteriaQuery<T> distinct(boolean distinct) {
		return null;
	}

	@Override
	public Set<Root<?>> getRoots() {
		return null;
	}

	@Override
	public Selection<T> getSelection() {
		return null;
	}

	@Override
	public List<Expression<?>> getGroupList() {
		return null;
	}

	@Override
	public Predicate getGroupRestriction() {
		return null;
	}

	@Override
	public boolean isDistinct() {
		return false;
	}

	@Override
	public Class<T> getResultType() {
		return null;
	}

	@Override
	public List<Order> getOrderList() {
		return null;
	}

	@Override
	public Set<ParameterExpression<?>> getParameters() {
		return null;
	}

	@Override
	public <U> Subquery<U> subquery(Class<U> type) {
		return null;
	}

	@Override
	public Predicate getRestriction() {
		return null;
	}
}
