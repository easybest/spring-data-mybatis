package org.springframework.data.mybatis.repository.config;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.data.mybatis.repository.support.MyBatisRepositoryFactoryBean;
import org.springframework.data.repository.config.DefaultRepositoryBaseClass;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable MyBatis repositories. Will scan the package of the annotated configuration class for Spring Data
 * repositories by default.
 * 
 * @author Jarvis Song
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(MyBatisRepositoriesRegistrar.class)
public @interface EnableMyBatisRepositories {

	/**
	 * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation declarations e.g.:
	 * {@code @EnableMyBatisRepositories("org.my.pkg")} instead of
	 * {@code @EnableMyBatisRepositories(basePackages="org.my.pkg")}.
	 */
	String[] value() default {};

	/**
	 * Base packages to scan for annotated components. {@link #value()} is an alias for (and mutually exclusive with) this
	 * attribute. Use {@link #basePackageClasses()} for a type-safe alternative to String-based package names.
	 */
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages()} for specifying the packages to scan for annotated components. The
	 * package of each class specified will be scanned. Consider creating a special no-op marker class or interface in
	 * each package that serves no purpose other than being referenced by this attribute.
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * Specifies which types are eligible for component scanning. Further narrows the set of candidate components from
	 * everything in {@link #basePackages()} to everything in the base packages that matches the given filter or filters.
	 */
	Filter[] includeFilters() default {};

	/**
	 * Specifies which types are not eligible for component scanning.
	 */
	Filter[] excludeFilters() default {};

	/**
	 * Returns the postfix to be used when looking up custom repository implementations. Defaults to {@literal Impl}. So
	 * for a repository named {@code PersonRepository} the corresponding implementation class will be looked up scanning
	 * for {@code PersonRepositoryImpl}.
	 *
	 * @return
	 */
	String repositoryImplementationPostfix() default "Impl";

	/**
	 * Configures the location of where to find the Spring Data named queries properties file. Will default to
	 * {@code META-INF/mybatis-named-queries.properties}.
	 *
	 * @return
	 */
	String namedQueriesLocation() default "";

	/**
	 * Returns the key of the {@link QueryLookupStrategy} to be used for lookup queries for query methods. Defaults to
	 * {@link QueryLookupStrategy.Key#CREATE_IF_NOT_FOUND}.
	 *
	 * @return
	 */
	QueryLookupStrategy.Key queryLookupStrategy() default QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND;

	/**
	 * Returns the {@link FactoryBean} class to be used for each repository instance. Defaults to
	 * {@link MyBatisRepositoryFactoryBean}.
	 *
	 * @return
	 */
	Class<?> repositoryFactoryBeanClass() default MyBatisRepositoryFactoryBean.class;

	/**
	 * Configure the repository base class to be used to create repository proxies for this particular configuration.
	 *
	 * @return
	 * @since 1.9
	 */
	Class<?> repositoryBaseClass() default DefaultRepositoryBaseClass.class;

	/**
	 * Configures the name of the {@link org.apache.ibatis.session.SqlSessionFactory} bean definition to be used to create
	 * repositories discovered through this annotation. Defaults to {@code sqlSessionFactory}.
	 * 
	 * @return
	 */
	String sqlSessionTemplateRef() default "sqlSessionTemplate";

	/**
	 * Configures the name of the {@link PlatformTransactionManager} bean definition to be used to create repositories
	 * discovered through this annotation. Defaults to {@code transactionManager}.
	 *
	 * @return
	 */
	String transactionManagerRef() default "transactionManager";

	String auditorAwareRef() default "auditorAware";

	/**
	 * Configures whether nested repository-interfaces (e.g. defined as inner classes) should be discovered by the
	 * repositories infrastructure.
	 */
	boolean considerNestedRepositories() default false;

	/**
	 * Configures whether to enable default transactions for Spring Data MyBatis repositories. Defaults to
	 * {@literal true}. If disabled, repositories must be used behind a facade that's configuring transactions (e.g. using
	 * Spring's annotation driven transaction facilities) or repository methods have to be used to demarcate transactions.
	 *
	 * @return whether to enable default transactions, defaults to {@literal true}.
	 */
	boolean enableDefaultTransactions() default true;
}
