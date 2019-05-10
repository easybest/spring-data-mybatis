package org.springframework.data.mybatis.processor;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.cursor.Cursor;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface MybatisExampleRepository<T, ID, Example> {

    List<T> selectAll();

    Cursor<T> cursorAll();

    Cursor<T> cursorByExample(Example query);

    List<T> selectByExample(Example query);

    long countByExample(Example query);

    int updateByExampleSelective(@Param("record") T t, @Param("example") Example query);

    int updateByExample(@Param("record") T t, @Param("example") Example query);

    boolean existsByExample(Example query);

}
