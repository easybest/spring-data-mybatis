package org.springframework.data.mybatis.repository.criteria;

import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Subquery;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class QueryStructure<T> implements Serializable {

	private final AbstractQuery<T> owner;
	private final CriteriaBuilderImpl criteriaBuilder;
	private final boolean isSubQuery;

	private boolean distinct;
	private Selection<? extends T> selection;
	private Set<Root<?>> roots = new LinkedHashSet<Root<?>>();
	private Predicate restriction;
	private List<Expression<?>> groupings = Collections.emptyList();
	private Predicate having;
	private List<Subquery<?>> subqueries;

	public QueryStructure(AbstractQuery<T> owner, CriteriaBuilderImpl criteriaBuilder) {
		this.owner = owner;
		this.criteriaBuilder = criteriaBuilder;
		this.isSubQuery = Subquery.class.isInstance(owner);
	}
}
