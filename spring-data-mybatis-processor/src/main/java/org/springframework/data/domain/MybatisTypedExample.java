package org.springframework.data.domain;

public class MybatisTypedExample<T> implements Example<T> {

    public ExampleMatcher exampleMatcher;

    private T probe;


    @Override
    public T getProbe() {
        return probe;
    }

    @Override
    public ExampleMatcher getMatcher() {
        return this.exampleMatcher;
    }

}
