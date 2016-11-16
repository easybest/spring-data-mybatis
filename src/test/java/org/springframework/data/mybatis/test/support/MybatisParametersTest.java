package org.springframework.data.mybatis.test.support;

import org.junit.Test;
import org.springframework.data.mybatis.repository.query.MybatisParameters;
import org.springframework.data.mybatis.repository.query.MybatisParameters.MybatisParameter;
import org.springframework.data.mybatis.test.repositories.UserRepository;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Iterator;

/**
 * Created by songjiawei on 2016/11/16.
 */
public class MybatisParametersTest {


    @Test
    public void parameters() {


        ReflectionUtils.doWithLocalMethods(UserRepository.class, new ReflectionUtils.MethodCallback() {
            @Override
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {

                System.out.println("=============================" + method.getName() + "============================");

                MybatisParameters parameters = new MybatisParameters(method);

                System.out.println("NumberOfParameters: " + parameters.getNumberOfParameters());
                System.out.println("hasDynamicProjection: " + parameters.hasDynamicProjection());
                System.out.println("hasPageableParameter: " + parameters.hasPageableParameter());
                System.out.println("hasSpecialParameter: " + parameters.hasSpecialParameter());
                System.out.println("potentiallySortsDynamically: " + parameters.potentiallySortsDynamically());
                System.out.println("hasSortParameter: " + parameters.hasSortParameter());
                System.out.println("DynamicProjectionIndex: " + parameters.getDynamicProjectionIndex());
                System.out.println("PageableIndex: " + parameters.getPageableIndex());
                System.out.println("SortIndex: " + parameters.getSortIndex());


                for (Iterator<MybatisParameter> iterator = parameters.iterator(); iterator.hasNext(); ) {
                    MybatisParameter parameter = iterator.next();
                    System.out.println(">>> Index: " + parameter.getIndex());
                    System.out.println(">>> Name: " + parameter.getName());
                    System.out.println(">>> Type: " + parameter.getType());
                    System.out.println(">>> Placeholder: " + parameter.getPlaceholder());
                    System.out.println(">>> TemporalType: " + parameter.getTemporalType());
                }

            }
        });


    }

}
