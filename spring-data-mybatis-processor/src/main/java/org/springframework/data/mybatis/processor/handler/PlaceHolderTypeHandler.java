package org.springframework.data.mybatis.processor.handler;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PlaceHolderTypeHandler implements TypeHandler<String> {

    public void setParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        throw new UnsupportedOperationException("SetParameter method Not supported yet.");
    }

    public String getResult(ResultSet rs, String columnName) throws SQLException {
        throw new UnsupportedOperationException("getResult method Not supported yet.");
    }

    public String getResult(ResultSet rs, int columnIndex) throws SQLException {
        throw new UnsupportedOperationException("getResult method Not supported yet.");
    }

    public String getResult(CallableStatement cs, int columnIndex) throws SQLException {
        throw new UnsupportedOperationException("getResult method Not supported yet.");
    }
}
