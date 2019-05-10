package org.springframework.data.mybatis.processor;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.springframework.data.mybatis.processor.domain.ColumnMetadata;
import org.springframework.data.mybatis.processor.domain.JoinMetadata;
import org.springframework.data.mybatis.processor.domain.TableMetadata;
import org.springframework.data.mybatis.processor.util.CamelUtils;
import org.springframework.data.mybatis.processor.visitor.DomainTypeVisitor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.persistence.*;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes(value = "org.springframework.data.mybatis.processor.Example")
public class MybatisDomainProcessor extends AbstractProcessor {

    private final DomainTypeVisitor domainTypeVisitor = new DomainTypeVisitor();


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            Filer filer = processingEnv.getFiler();
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Example.class);
            MustacheFactory mf = new DefaultMustacheFactory() {
                @Override
                public void encode(String value, Writer writer) {
                    try {
                        writer.write(value);
                    } catch (IOException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }
            };
            ClassLoader classLoader = MybatisDomainProcessor.class.getClassLoader();

            for (Element element : elements) {
                PackageElement packageOf = processingEnv.getElementUtils().getPackageOf(element);

                TableMetadata tableMetadata = read(element);
                String exampleName = tableMetadata.getExampleClazzName();

                JavaFileObject javaFileObject = filer.createSourceFile(exampleName);
                HashMap<String, Object> scopes = new HashMap<>();
                scopes.put("metadata", tableMetadata);


                InputStream exampleInputStream = classLoader.getResourceAsStream("templates/Example.java");
                try (InputStreamReader in = new InputStreamReader(exampleInputStream, StandardCharsets.UTF_8); Writer writer = javaFileObject.openWriter()) {
                    Mustache mustache = mf.compile(in, exampleName);
                    mustache.execute(writer, scopes);
                }


                String xml = tableMetadata.getDomainClazzSimpleName() + "ExampleMapper.xml";
                FileObject xmlOut = filer.createResource(StandardLocation.CLASS_OUTPUT, packageOf.toString(), xml);
                InputStream xmlInputStream = classLoader.getResourceAsStream("templates/Example.xml");
                try (InputStreamReader in = new InputStreamReader(xmlInputStream, StandardCharsets.UTF_8);
                     Writer writer = xmlOut.openWriter()) {
                    Mustache mustache = mf.compile(in, tableMetadata.getDomainClazzName() + ".xml");
                    mustache.execute(writer, scopes);
                }

            }

        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, Arrays.toString(e.getStackTrace()));
        }
        return true;
    }


    private TableMetadata read(Element element) {
        Example example = element.getAnnotation(Example.class);
        Table table = element.getAnnotation(Table.class);
        PackageElement packageOf = processingEnv.getElementUtils().getPackageOf(element);

        String clazzName = element.toString();

        String exampleName = (clazzName + "Example");


        TableMetadata tableMetadata = new TableMetadata()
                .setDomainClazzName(clazzName)
                .setExampleClazzName(exampleName)
                .setPackageName(packageOf.toString());

        String repositoryName = example.value();
        tableMetadata.setRepositoryClazzName(repositoryName)
                .setTableName(table != null ? table.name() : String.join("_",
                        CamelUtils.split(tableMetadata.getDomainClazzSimpleName(), true)));

        for (Element member : element.getEnclosedElements()) {
            if (member.getModifiers().contains(Modifier.STATIC) || !member.getKind().isField() ||
                    member.getAnnotation(Transient.class) != null ||
                    member.getAnnotation(ManyToMany.class) != null ||
                    member.getAnnotation(ManyToOne.class) != null) {
                continue;
            }

            String name = member.toString();
            JoinColumn joinColumn = member.getAnnotation(JoinColumn.class);
            OneToOne oneToOne = member.getAnnotation(OneToOne.class);
            OneToMany oneToMany = member.getAnnotation(OneToMany.class);

            if (joinColumn != null) {
                if (!"".equals(joinColumn.name())) {
                    JoinMetadata joinMetadata = new JoinMetadata()
                            .setColumnName(joinColumn.name())
                            .setFieldName(name);
                    if (oneToOne != null) {
                        joinMetadata.setMappedBy(oneToOne.mappedBy())
                                .setFetchType(oneToOne.fetch().name().toLowerCase());
                        tableMetadata.getOneToOne().add(joinMetadata);
                    }

                    if (oneToMany != null) {
                        joinMetadata.setMappedBy(oneToMany.mappedBy())
                                .setFetchType(oneToMany.fetch().name().toLowerCase());
                        tableMetadata.getOneToMany().add(joinMetadata);
                    }
                }
                continue;
            }


            Id id = member.getAnnotation(Id.class);
            Column column = member.getAnnotation(Column.class);
            GeneratedValue generatedValue = member.getAnnotation(GeneratedValue.class);
            ColumnMetadata columnMetadata = new ColumnMetadata();
            member.asType().accept(domainTypeVisitor, columnMetadata);
            columnMetadata.setFieldName(name)
                    .setUseGeneratedKeys(generatedValue != null)
                    .setPrimary(id != null);


            if (column == null || "".equals(column.name())) {
                columnMetadata.setColumnName(String.join("_", CamelUtils.split(name, true)));
            }

            if (column != null && !"".equals(column.columnDefinition())) {
                columnMetadata.setJdbcType(column.columnDefinition());
            }

            if (column != null && !"".equals(column.name())) {
                columnMetadata.setColumnName(column.name());
            }

            if (id != null) {
                tableMetadata.setPrimaryMetadata(columnMetadata);
            }

            tableMetadata.getColumnMetadataList().add(columnMetadata);

        }

        String columns = tableMetadata.getColumnMetadataList()
                .stream()
                .map(ColumnMetadata::getColumnName)
                .collect(Collectors.joining(", "));

        return tableMetadata.setColumns(columns);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
