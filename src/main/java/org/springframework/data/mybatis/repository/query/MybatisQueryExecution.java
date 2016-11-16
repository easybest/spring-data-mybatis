package org.springframework.data.mybatis.repository.query;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mybatis.repository.query.MybatisParameters.MybatisParameter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import javax.persistence.NoResultException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by songjiawei on 2016/11/10.
 */
public abstract class MybatisQueryExecution {

    private static final ConversionService CONVERSION_SERVICE;

    static {

        ConfigurableConversionService conversionService = new DefaultConversionService();

        conversionService.addConverter(MybatisResultConverters.BlobToByteArrayConverter.INSTANCE);
        conversionService.removeConvertible(Collection.class, Object.class);
        potentiallyRemoveOptionalConverter(conversionService);

        CONVERSION_SERVICE = conversionService;
    }

    protected abstract Object doExecute(AbstractMybatisQuery query, Object[] values);

    public Object execute(AbstractMybatisQuery query, Object[] values) {

        Assert.notNull(query);
        Assert.notNull(values);

        Object result;

        try {
            result = doExecute(query, values);
        } catch (NoResultException e) {
            return null;
        }

        if (result == null) {
            return null;
        }

        MybatisQueryMethod queryMethod = query.getQueryMethod();
        Class<?> requiredType = queryMethod.getReturnType();

        if (void.class.equals(requiredType) || requiredType.isAssignableFrom(result.getClass())) {
            return result;
        }

        return CONVERSION_SERVICE.canConvert(result.getClass(), requiredType)
                ? CONVERSION_SERVICE.convert(result, requiredType) : result;
    }

    static class CollectionExecution extends MybatisQueryExecution {

        @Override
        protected Object doExecute(AbstractMybatisQuery query, Object[] values) {

            if (null == values || values.length == 0) {
                return query.getSqlSessionTemplate().selectList(query.getStatementId());
            }

            MybatisParameters parameters = query.getQueryMethod().getParameters();
            Map<String, Object> parameter = new HashMap<String, Object>();

            int c = 0;
            for (MybatisParameter param : parameters.getBindableParameters()) {
                parameter.put("p" + (c++), values[param.getIndex()]);
            }

            if (parameters.hasSortParameter()) {
                parameter.put("sorts", values[parameters.getSortIndex()]);
            }

            return query.getSqlSessionTemplate().selectList(query.getStatementId(), parameter);

        }
    }

    static class SlicedExecution extends MybatisQueryExecution {

        /*
         * (non-Javadoc)
         * @see org.springframework.data.jpa.repository.query.JpaQueryExecution#doExecute(org.springframework.data.jpa.repository.query.AbstractJpaQuery, java.lang.Object[])
         */
        @Override
        protected Object doExecute(AbstractMybatisQuery query, Object[] values) {

//            ParametersParameterAccessor accessor = new ParametersParameterAccessor(parameters, values);
//            Pageable pageable = accessor.getPageable();
//
//            Query createQuery = query.createQuery(values);
//            int pageSize = pageable.getPageSize();
//            createQuery.setMaxResults(pageSize + 1);
//
//            List<Object> resultList = createQuery.getResultList();
//            boolean hasNext = resultList.size() > pageSize;
//
//            return new SliceImpl<Object>(hasNext ? resultList.subList(0, pageSize) : resultList, pageable, hasNext);
            return null;
        }
    }

    static class PagedExecution extends MybatisQueryExecution {

        @Override
        protected Object doExecute(AbstractMybatisQuery query, Object[] values) {
            MybatisParameters parameters = query.getQueryMethod().getParameters();
            Map<String, Object> parameter = new HashMap<String, Object>();
            int c = 0;
            for (MybatisParameter param : parameters.getBindableParameters()) {
                parameter.put("p" + (c++), values[param.getIndex()]);
            }

            if (parameters.hasSortParameter()) {
                parameter.put("sorts", values[parameters.getSortIndex()]);
            }

            Pageable pager = (Pageable) values[parameters.getPageableIndex()];
            parameter.put("offset", pager.getOffset());
            parameter.put("pageSize", pager.getPageSize());
            parameter.put("offsetEnd", pager.getOffset() + pager.getPageSize());
            List<?> result = query.getSqlSessionTemplate().selectList(query.getStatementId(), parameter);


            long total = calculateTotal(pager, result);
            if (total < 0) {
                total = query.getSqlSessionTemplate().selectOne(query.getCountStatementId(), parameter);
            }
            return new PageImpl(result, pager, total);
        }

        protected <X> long calculateTotal(Pageable pager, List<X> result) {
            if (pager.hasPrevious()) {
                if (CollectionUtils.isEmpty(result)) return -1;
                if (result.size() == pager.getPageSize()) return -1;
                return (pager.getPageNumber() - 1) * pager.getPageSize() + result.size();
            }
            if (result.size() < pager.getPageSize()) return result.size();
            return -1;
        }
    }

    static class SingleEntityExecution extends MybatisQueryExecution {

        @Override
        protected Object doExecute(final AbstractMybatisQuery repositoryQuery, final Object[] values) {


            return null;
        }
    }

    static class StreamExecution extends MybatisQueryExecution {

        /*
         * (non-Javadoc)
         * @see org.springframework.data.jpa.repository.query.JpaQueryExecution#doExecute(org.springframework.data.jpa.repository.query.AbstractJpaQuery, java.lang.Object[])
         */
        @Override
        protected Object doExecute(final AbstractMybatisQuery query, Object[] values) {

            return null;
        }
    }

    public static void potentiallyRemoveOptionalConverter(ConfigurableConversionService conversionService) {

        ClassLoader classLoader = MybatisQueryExecution.class.getClassLoader();

        if (ClassUtils.isPresent("java.util.Optional", classLoader)) {

            try {

                Class<?> optionalType = ClassUtils.forName("java.util.Optional", classLoader);
                conversionService.removeConvertible(Object.class, optionalType);

            } catch (ClassNotFoundException e) {
                return;
            } catch (LinkageError e) {
                return;
            }
        }
    }
}
