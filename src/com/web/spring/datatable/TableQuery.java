package com.web.spring.datatable;


import com.web.spring.datatable.annotations.SqlCondition;
import com.web.spring.datatable.annotations.SqlIndex;
import com.web.spring.datatable.annotations.SqlIndexOperator;
import com.web.spring.datatable.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class TableQuery {
    private EntityManager entityManager;
    private Class entiteClass;
    private DatatablesCriterias criterias;
    private Long totalCount = 0L;
    private int displayRecordsLength = 0;

    public <T> TableQuery(EntityManager entityManager, Class<T> entiteClass, DatatablesCriterias criterias) {
        this.entityManager = entityManager;
        this.entiteClass = entiteClass;
        this.criterias = criterias;
    }

    public <T> DataSet<T> getResultDataSet() {
        List<T> actions = getRecordsWithDatatablesCriterias();
        Long count = getTotalCount();
        Long countFiltered = getFilteredCount();
        return new DataSet<T>(actions, count, countFiltered);
    }

    public StringBuilder getFilterQuery() {
        StringBuilder queryBuilder = new StringBuilder();
        List<String> paramList = new ArrayList<String>();
        List<String> indexColumnList = new ArrayList<String>();
        List<String> unIndexColumnList = new ArrayList<String>();
        HashMap<String, String> conditionMap = new HashMap<>();
        HashMap<String, String> indexOperatorMap = new HashMap<>();
        Field[] fields = this.entiteClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(SqlIndex.class)) {
                if (field.isAnnotationPresent(Column.class)) {
                    Column column = field.getAnnotation(Column.class);
                    indexColumnList.add(column.name());
                    if (field.isAnnotationPresent(SqlIndexOperator.class)) {
                        SqlIndexOperator indexOperator = field.getAnnotation(SqlIndexOperator.class);
                        indexOperatorMap.put(column.name(), indexOperator.value());
                    }
                } else {
                    indexColumnList.add(field.getName());
                    if (field.isAnnotationPresent(SqlIndexOperator.class)) {
                        SqlIndexOperator indexOperator = field.getAnnotation(SqlIndexOperator.class);
                        indexOperatorMap.put(field.getName(), indexOperator.value());
                    }
                }

            } else {
                if (field.isAnnotationPresent(Column.class)) {
                    Column column = field.getAnnotation(Column.class);
                    unIndexColumnList.add(column.name());
                } else {
                    unIndexColumnList.add(field.getName());
                }
            }
            if (field.isAnnotationPresent(SqlCondition.class)) {
                SqlCondition sqlCondition = field.getAnnotation(SqlCondition.class);
                if (field.isAnnotationPresent(Column.class)) {
                    Column column = field.getAnnotation(Column.class);
                    conditionMap.put(column.name(),sqlCondition.value());
                } else {
                    conditionMap.put(field.getName(),sqlCondition.value());
                }
            }
        }

        /**
         * Step 1.1: custom condition
         */
        if(conditionMap.size()>0) {
            queryBuilder.append(" WHERE ");
            for (ColumnDef columnDef : criterias.getColumnDefs()) {
                if (indexColumnList.contains(columnDef.getName())) {
                    if (conditionMap.get(columnDef.getName()) != null) {
                        String replace = conditionMap.get(columnDef.getName()).replaceAll(columnDef.getName(), "p." + columnDef.getName());
                        paramList.add(" " + replace);
                    }
                }

                if (unIndexColumnList.contains(columnDef.getName())) {
                    if (conditionMap.get(columnDef.getName()) != null) {
                        String replace = conditionMap.get(columnDef.getName()).replaceAll(columnDef.getName(), "p." + columnDef.getName());
                        paramList.add(" " + replace);
                    }
                }
            }
            Iterator<String> tr = paramList.iterator();
            while (tr.hasNext()) {
                queryBuilder.append(tr.next());
                if (tr.hasNext()) {
                    queryBuilder.append(" AND ");
                }
            }
        }

        /**
         * Step 1.2: individual column filtering
         */
        if (criterias.hasOneSearchableColumn() && criterias.hasOneFilteredColumn()) {
            paramList = new ArrayList<String>();
            if (!queryBuilder.toString().contains("WHERE")) {
                queryBuilder.append(" WHERE ");
            } else {
                queryBuilder.append(" AND ");
            }
            for (ColumnDef columnDef : criterias.getColumnDefs()) {
                if (columnDef.isSearchable() && indexColumnList.contains(columnDef.getName())) {
                    if (StringUtils.isNotBlank(columnDef.getSearch())) {
                        if (indexOperatorMap.get(columnDef.getName()) != null) {
                            if (indexOperatorMap.get(columnDef.getName()).equalsIgnoreCase("like")) {
                                paramList.add(" p." + columnDef.getName()
                                        + " like '?%'".replace("?", columnDef.getSearch()));
                            } else {
                                paramList.add(" p." + columnDef.getName()
                                        + " = '?'".replace("?", columnDef.getSearch()));
                            }
                        } else {
                            paramList.add(" p." + columnDef.getName()
                                    + " = '?'".replace("?", columnDef.getSearch()));
                        }
                    }
                }
            }

            for (ColumnDef columnDef : criterias.getColumnDefs()) {
                if (columnDef.isSearchable() && indexColumnList.contains(columnDef.getName())) {
                    if (StringUtils.isNotBlank(columnDef.getSearchFrom())) {
                        paramList.add("p." + columnDef.getName() + " >= " + columnDef.getSearchFrom());
                    }
                    if (StringUtils.isNotBlank(columnDef.getSearchTo())) {
                        paramList.add("p." + columnDef.getName() + " < " + columnDef.getSearchTo());
                    }
                }
            }

            for (ColumnDef columnDef : criterias.getColumnDefs()) {
                if (columnDef.isSearchable() && unIndexColumnList.contains(columnDef.getName())) {
                    if (StringUtils.isNotBlank(columnDef.getSearchFrom())) {
                        paramList.add("p." + columnDef.getName() + " >= " + columnDef.getSearchFrom());
                    }
                    if (StringUtils.isNotBlank(columnDef.getSearchTo())) {
                        paramList.add("p." + columnDef.getName() + " < " + columnDef.getSearchTo());
                    }
                    if (StringUtils.isNotBlank(columnDef.getSearch())) {
                        paramList.add(" p." + columnDef.getName()
                                + " LIKE '%?%'".replace("?", columnDef.getSearch()));
                    }
                }
            }

            Iterator<String> itr = paramList.iterator();
            while (itr.hasNext()) {
                queryBuilder.append(itr.next());
                if (itr.hasNext()) {
                    queryBuilder.append(" AND ");
                }
            }
        }

        /**
         * Step 1.3: global filtering
         */
        if (StringUtils.isNotBlank(criterias.getSearch()) && criterias.hasOneSearchableColumn()) {
            paramList = new ArrayList<String>();
            if (!queryBuilder.toString().contains("WHERE")) {
                queryBuilder.append(" WHERE (");
            } else {
                queryBuilder.append(" AND (");
            }
            for (ColumnDef columnDef : criterias.getColumnDefs()) {
                if (columnDef.isSearchable() && StringUtils.isBlank(columnDef.getSearch()) && indexColumnList.contains(columnDef.getName())) {
                    if (indexOperatorMap.get(columnDef.getName()) != null) {
                        if (indexOperatorMap.get(columnDef.getName()).equalsIgnoreCase("like")) {
                            paramList.add(" p." + columnDef.getName()
                                    + " like '?%'".replace("?", criterias.getSearch()));
                        } else {
                            paramList.add(" p." + columnDef.getName()
                                    + " = '?'".replace("?", criterias.getSearch()));
                        }
                    } else {
                        paramList.add(" p." + columnDef.getName()
                                + " = '?'".replace("?", criterias.getSearch()));
                    }
                }
            }
            for (ColumnDef columnDef : criterias.getColumnDefs()) {
                if (columnDef.isSearchable() && StringUtils.isBlank(columnDef.getSearch()) && unIndexColumnList.contains(columnDef.getName())) {
                    paramList.add(" p." + columnDef.getName()
                            + " LIKE '%?%'".replace("?", criterias.getSearch()));
                }
            }

            Iterator<String> itr = paramList.iterator();
            while (itr.hasNext()) {
                queryBuilder.append(itr.next());
                if (itr.hasNext()) {
                    queryBuilder.append(" OR ");
                }
            }
            queryBuilder.append(" )");
        }

        return queryBuilder;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getRecordsWithDatatablesCriterias() {
        StringBuilder queryBuilder = new StringBuilder("SELECT p FROM " + entiteClass.getSimpleName() + " p");

        /**
         * Step 1: global and individual column filtering
         */
        queryBuilder.append(getFilterQuery());

        /**
         * Step 2: sorting
         */
        if (criterias.hasOneSortedColumn()) {
            List<String> orderParams = new ArrayList<String>();
            queryBuilder.append(" ORDER BY ");
            for (ColumnDef columnDef : criterias.getSortedColumnDefs()) {
                orderParams.add("p." + columnDef.getName() + " " + columnDef.getSortDirection());
            }

            Iterator<String> itr2 = orderParams.iterator();
            while (itr2.hasNext()) {
                queryBuilder.append(itr2.next());
                if (itr2.hasNext()) {
                    queryBuilder.append(" , ");
                }
            }
        }

        TypedQuery<T> query = this.entityManager.createQuery(queryBuilder.toString(), entiteClass);

        /**
         * Step 3: paging
         */
        query.setFirstResult(criterias.getStart());
        query.setMaxResults(criterias.getLength());

        List<T> result = query.getResultList();
        displayRecordsLength = result.size();

        return result;
    }

    public Long getFilteredCount() {
        if (StringUtils.isBlank(criterias.getSearch()) && (!criterias.hasOneFilteredColumn())) {
            return totalCount;
        }
        if (criterias.getStart() == 0) {
            if (criterias.getLength() > displayRecordsLength) {
                return (long) displayRecordsLength;
            }
        }
        javax.persistence.Query query = this.entityManager.createQuery("SELECT COUNT(*) FROM " + entiteClass.getSimpleName() + " p" + getFilterQuery());
        return (Long) query.getSingleResult();
    }

    public Long getTotalCount() {
        javax.persistence.Query query = this.entityManager.createQuery("SELECT COUNT(*) FROM " + entiteClass.getSimpleName() + " p");
        totalCount = (Long) query.getSingleResult();
        return totalCount;
    }
}