package org.springframework.data.mybatis.processor.visitor;


import org.springframework.data.mybatis.processor.domain.ColumnMetadata;

import javax.lang.model.type.*;

public class DomainTypeVisitor implements TypeVisitor<DomainTypeVisitor, ColumnMetadata> {

    @Override
    public DomainTypeVisitor visit(TypeMirror t, ColumnMetadata columnMetadata) {
        return this;
    }

    @Override
    public DomainTypeVisitor visit(TypeMirror t) {
        return this;
    }

    @Override
    public DomainTypeVisitor visitPrimitive(PrimitiveType t, ColumnMetadata columnMetadata) {
        columnMetadata.setJavaType(t.toString());
        return this;
    }

    @Override
    public DomainTypeVisitor visitNull(NullType t, ColumnMetadata columnMetadata) {
        columnMetadata.setJavaType(t.toString());
        return this;
    }

    @Override
    public DomainTypeVisitor visitArray(ArrayType t, ColumnMetadata columnMetadata) {
        columnMetadata.setJavaType(t.toString());
        return this;
    }

    @Override
    public DomainTypeVisitor visitDeclared(DeclaredType t, ColumnMetadata columnMetadata) {
        columnMetadata.setJavaType(t.toString());
        return this;
    }

    @Override
    public DomainTypeVisitor visitError(ErrorType t, ColumnMetadata columnMetadata) {
        return this;
    }

    @Override
    public DomainTypeVisitor visitTypeVariable(TypeVariable t, ColumnMetadata columnMetadata) {
        return this;
    }

    @Override
    public DomainTypeVisitor visitWildcard(WildcardType t, ColumnMetadata columnMetadata) {
        return this;
    }

    @Override
    public DomainTypeVisitor visitExecutable(ExecutableType t, ColumnMetadata columnMetadata) {
        return this;
    }

    @Override
    public DomainTypeVisitor visitNoType(NoType t, ColumnMetadata columnMetadata) {
        return this;
    }

    @Override
    public DomainTypeVisitor visitUnknown(TypeMirror t, ColumnMetadata columnMetadata) {
        return this;
    }

    @Override
    public DomainTypeVisitor visitUnion(UnionType t, ColumnMetadata columnMetadata) {
        return this;
    }

    @Override
    public DomainTypeVisitor visitIntersection(IntersectionType t, ColumnMetadata columnMetadata) {
        return this;
    }
}
