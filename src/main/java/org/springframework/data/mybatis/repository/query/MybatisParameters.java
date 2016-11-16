package org.springframework.data.mybatis.repository.query;

import org.springframework.core.MethodParameter;
import org.springframework.data.mybatis.repository.query.MybatisParameters.MybatisParameter;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

/**
 * Created by songjiawei on 2016/11/13.
 */
public class MybatisParameters extends Parameters<MybatisParameters, MybatisParameter> {

    public MybatisParameters(Method method) {
        super(method);
    }

    protected MybatisParameters(List<MybatisParameter> parameters) {
        super(parameters);
    }

    @Override
    protected MybatisParameter createParameter(MethodParameter parameter) {
        return new MybatisParameter(parameter);
    }

    @Override
    protected MybatisParameters createFrom(List<MybatisParameter> parameters) {
        return new MybatisParameters(parameters);
    }

    public static class MybatisParameter extends Parameter {
        private final Temporal     annotation;
        private       TemporalType temporalType;

        /**
         * Creates a new {@link Parameter} for the given {@link MethodParameter}.
         *
         * @param parameter must not be {@literal null}.
         */
        protected MybatisParameter(MethodParameter parameter) {
            super(parameter);


            this.annotation = parameter.getParameterAnnotation(Temporal.class);
            this.temporalType = null;

            if (!isDateParameter() && hasTemporalParamAnnotation()) {
                throw new IllegalArgumentException(Temporal.class.getSimpleName()
                        + " annotation is only allowed on Date parameter!");
            }
        }

        @Override
        public boolean isBindable() {
            return super.isBindable() || isTemporalParameter();
        }

        /**
         * @return {@literal true} if this parameter is of type {@link Date} and has an {@link Temporal} annotation.
         */
        public boolean isTemporalParameter() {
            return isDateParameter() && hasTemporalParamAnnotation();
        }

        /**
         * @return the {@link TemporalType} on the {@link Temporal} annotation of the given {@link Parameter}.
         */
        public TemporalType getTemporalType() {

            if (temporalType == null) {
                this.temporalType = annotation == null ? null : annotation.value();
            }

            return this.temporalType;
        }

        private boolean hasTemporalParamAnnotation() {
            return annotation != null;
        }

        private boolean isDateParameter() {
            return getType().equals(Date.class);
        }
    }
}
