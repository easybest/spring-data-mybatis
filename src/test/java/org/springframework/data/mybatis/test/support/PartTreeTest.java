package org.springframework.data.mybatis.test.support;

import org.junit.Test;
import org.springframework.data.mybatis.test.domains.User;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

import java.util.Iterator;

/**
 * Created by songjiawei on 2016/11/16.
 */
public class PartTreeTest {

    @Test
    public void getByFirstnameAndLastname() {
        testMethodName("getByFirstnameAndLastname");
    }

    @Test
    public void findByLastnameOrderByFirstnameAsc() {
        testMethodName("findByLastnameOrderByFirstnameAsc");
    }

    @Test
    public void queryFirst10ByLastname() {
        testMethodName("queryFirst10ByLastname");
    }

    @Test
    public void findTop3ByLastname() {
        testMethodName("findTop3ByLastname");
    }

    @Test
    public void findDistinctPeopleByLastnameOrFirstname() {
        testMethodName("findDistinctPeopleByLastnameOrFirstname");
    }

    @Test
    public void findByLastnameAndFirstnameAllIgnoreCase() {
        testMethodName("findByLastnameAndFirstnameAllIgnoreCase");
    }

    @Test
    public void findByLastnameAndFirstnameLikeAllIgnoreCase() {
        testMethodName("findByLastnameAndFirstnameLikeAllIgnoreCase");
    }

    @Test
    public void findByLastnameAndAgeGreaterThan() {
        testMethodName("findByLastnameAndAgeGreaterThan");
    }

    @Test
    public void findByLastnameStartingWith() {
        testMethodName("findByLastnameStartingWith");
    }

    @Test
    public void findByLastnameIn() {
        testMethodName("findByLastnameIn");
    }

    @Test
    public void findByLastnameBetween() {
        testMethodName("findByLastnameBetween");
    }

    @Test
    public void findByLastnameAndDepartment() {
        testMethodName("findByLastnameAndDepartment");
    }

    @Test
    public void findByLastnameAndDepartmentName() {
        testMethodName("findByLastnameAndDepartmentName");
    }

    @Test
    public void findByLastnameAndDepartmentNameLike() {
        testMethodName("findByLastnameAndDepartmentNameLike");
    }

    @Test
    public void countByLastname() {
        testMethodName("countByLastname");
    }

    @Test
    public void deleteByLastname() {
        testMethodName("deleteByLastname");
    }

    @Test
    public void removeByLastname() {
        testMethodName("removeByLastname");
    }

    private void testMethodName(String methodName) {
        System.out.println("=============================" + methodName + "============================");
        PartTree tree = new PartTree(methodName, User.class);
        System.out.println("tree: " + tree);

        for (Iterator<PartTree.OrPart> iterator = tree.iterator(); iterator.hasNext(); ) {
            PartTree.OrPart orPart = iterator.next();
            System.out.println("orPart: " + orPart);

            for (Part part : orPart) {
                System.out.println(">>> part: " + part);

                System.out.println(">>>>>> NumberOfArguments:" + part.getNumberOfArguments());
                System.out.println(">>>>>> Type:" + part.getType());
                System.out.println(">>>>>> ParameterRequired:" + part.getParameterRequired());
                System.out.println(">>>>>> PropertyPath:" + part.getProperty());
                System.out.println(">>>>>>>>> Segment:" + part.getProperty().getSegment());
                System.out.println(">>>>>>>>> Type:" + part.getProperty().getType());
                System.out.println(">>>>>>>>> OwningType:" + part.getProperty().getOwningType());
                System.out.println(">>>>>>>>> LeafProperty:" + part.getProperty().getLeafProperty());
            }

        }

        System.out.println("sort: " + tree.getSort());
        System.out.println("isCountProjection: " + tree.isCountProjection());
        System.out.println("isDelete: " + tree.isDelete());
        System.out.println("isDistinct: " + tree.isDistinct());
        System.out.println("isLimiting: " + tree.isLimiting());
        System.out.println("MaxResults: " + tree.getMaxResults());


    }

}
