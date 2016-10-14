package com.web.spring.datatable;


import com.web.spring.datatable.annotations.SqlCondition;
import com.web.spring.datatable.annotations.SqlIndex;
import com.web.spring.datatable.annotations.SqlIndexOperator;
import com.web.spring.datatable.util.ReflectHelper;
import com.web.spring.datatable.util.StringHelper;

import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.*;


public class TableQuery {
    private EntityManager entityManager;
    private Class entiteClass;
    private DatatablesCriterias criterias;
    private Long totalCount = 0L;
    private int displayRecordsLength = 0;
    private String customSQL = "";
    private List<String> selectColumnList = new ArrayList<>();
    private HashMap<String, Class> fieldTypeMap = new HashMap<>();

    public <T> TableQuery(EntityManager entityManager, Class<T> entiteClass, DatatablesCriterias criterias) {
        this.entityManager = entityManager;
        this.entiteClass = entiteClass;
        this.criterias = criterias;
    }

    public <T> TableQuery(EntityManager entityManager, Class<T> entiteClass, DatatablesCriterias criterias, String customSQL) {
        this.entityManager = entityManager;
        this.entiteClass = entiteClass;
        this.criterias = criterias;
        this.customSQL = customSQL;
        String[] columnArray = StringHelper.getBetweenString(this.customSQL.toLowerCase(),"select","from").split(",");
        for (String aColumnArray : columnArray) {
            if (aColumnArray.toLowerCase().contains("as")) {
                selectColumnList.add(aColumnArray.substring(aColumnArray.indexOf("as") + 2).trim());
            } else {
                selectColumnList.add(aColumnArray.trim());
            }
        }
    }

    public <T> DataSet<T> getResultDataSet() {
        List<T> rows = getRows();
        Long count = getTotalCount();
        Long countFiltered = getFilteredCount();
        return new DataSet<T>(rows, count, countFiltered);
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
            fieldTypeMap.put(field.getName(),field.getType());
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
                        paramList.add(" " + conditionMap.get(columnDef.getName()));
                    }
                }

                if (unIndexColumnList.contains(columnDef.getName())) {
                    if (conditionMap.get(columnDef.getName()) != null) {
                        paramList.add(" " + conditionMap.get(columnDef.getName()));
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
                    if (StringHelper.isNotEmpty(columnDef.getSearch())) {
                        if (indexOperatorMap.get(columnDef.getName()) != null) {
                            if (indexOperatorMap.get(columnDef.getName()).equalsIgnoreCase("like")) {
                                paramList.add(" " + columnDef.getName()
                                        + " like '?%'".replace("?", columnDef.getSearch()));
                            } else {
                                paramList.add(" " + columnDef.getName()
                                        + " = '?'".replace("?", columnDef.getSearch()));
                            }
                        } else {
                            paramList.add(" " + columnDef.getName()
                                    + " = '?'".replace("?", columnDef.getSearch()));
                        }
                    }
                }
            }

            for (ColumnDef columnDef : criterias.getColumnDefs()) {
                if (columnDef.isSearchable() && indexColumnList.contains(columnDef.getName())) {
                    if (StringHelper.isNotEmpty(columnDef.getSearchFrom())) {
                        paramList.add("" + columnDef.getName() + " >= " + columnDef.getSearchFrom());
                    }
                    if (StringHelper.isNotEmpty(columnDef.getSearchTo())) {
                        paramList.add("" + columnDef.getName() + " < " + columnDef.getSearchTo());
                    }
                }
            }

            for (ColumnDef columnDef : criterias.getColumnDefs()) {
                if (columnDef.isSearchable() && unIndexColumnList.contains(columnDef.getName())) {
                    if (StringHelper.isNotEmpty(columnDef.getSearchFrom())) {
                        paramList.add("" + columnDef.getName() + " >= " + columnDef.getSearchFrom());
                    }
                    if (StringHelper.isNotEmpty(columnDef.getSearchTo())) {
                        paramList.add("" + columnDef.getName() + " < " + columnDef.getSearchTo());
                    }
                    if (StringHelper.isNotEmpty(columnDef.getSearch())) {
                        paramList.add(" " + columnDef.getName()
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
        if (StringHelper.isNotEmpty(criterias.getSearch()) && criterias.hasOneSearchableColumn()) {
            paramList = new ArrayList<String>();
            if (!queryBuilder.toString().contains("WHERE")) {
                queryBuilder.append(" WHERE (");
            } else {
                queryBuilder.append(" AND (");
            }
            for (ColumnDef columnDef : criterias.getColumnDefs()) {
                if (columnDef.isSearchable() && StringHelper.isEmpty(columnDef.getSearch()) && indexColumnList.contains(columnDef.getName())) {
                    if (indexOperatorMap.get(columnDef.getName()) != null) {
                        if (indexOperatorMap.get(columnDef.getName()).equalsIgnoreCase("like")) {
                            paramList.add(" " + columnDef.getName()
                                    + " like '?%'".replace("?", criterias.getSearch()));
                        } else {
                            paramList.add(" " + columnDef.getName()
                                    + " = '?'".replace("?", criterias.getSearch()));
                        }
                    } else {
                        paramList.add(" " + columnDef.getName()
                                + " = '?'".replace("?", criterias.getSearch()));
                    }
                }
            }
            for (ColumnDef columnDef : criterias.getColumnDefs()) {
                if (columnDef.isSearchable() && StringHelper.isEmpty(columnDef.getSearch()) && unIndexColumnList.contains(columnDef.getName())) {
                    paramList.add(" " + columnDef.getName()
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
    public <T> List<T> getRows() {
        StringBuilder queryBuilder = new StringBuilder();
        if(this.customSQL.equals("")){
            queryBuilder.append("SELECT p FROM ").append(entiteClass.getSimpleName()).append(" p");
        }
        else
        {
            queryBuilder.append("SELECT * FROM (").append(this.customSQL).append(") customSQL");
        }

        /**
         * Step 1: individual column and global filtering
         */
        queryBuilder.append(getFilterQuery());

        /**
         * Step 2: sorting
         */
        if (criterias.hasOneSortedColumn()) {
            List<String> orderParams = new ArrayList<String>();
            queryBuilder.append(" ORDER BY ");
            for (ColumnDef columnDef : criterias.getSortedColumnDefs()) {
                orderParams.add("" + columnDef.getName() + " " + columnDef.getSortDirection());
            }

            Iterator<String> itr2 = orderParams.iterator();
            while (itr2.hasNext()) {
                queryBuilder.append(itr2.next());
                if (itr2.hasNext()) {
                    queryBuilder.append(" , ");
                }
            }
        }

        if(this.customSQL.equals("")) {
            TypedQuery<T> query = this.entityManager.createQuery(queryBuilder.toString(), entiteClass);

            /**
             * Step 3: paging
             */
            query.setFirstResult(criterias.getStart());
            if (criterias.getLength() == -1) {
                query.setMaxResults(query.getResultList().size());
            } else {
                query.setMaxResults(criterias.getLength());
            }
            List<T> result = query.getResultList();
            displayRecordsLength = result.size();

            return result;
        }
        else {
            Query query = this.entityManager.createNativeQuery(queryBuilder.toString());

            /**
             * Step 3: paging
             */
            query.setFirstResult(criterias.getStart());
            if(criterias.getLength()==-1){
                query.setMaxResults(query.getResultList().size());
            }else{
                query.setMaxResults(criterias.getLength());
            }
            List<Object[]> result = query.getResultList();
            List<HashMap<String,Object>> resultMap = new ArrayList<>();
            for (Object[] object : result) {
                int i =0;
                HashMap<String,Object> map = new HashMap<String,Object>();
                for(String columnName : selectColumnList){
                    map.put(columnName,object[i]);
                    i++;
                }
                resultMap.add(map);
            }
            List<T> result2 = new ArrayList<>();
            for (HashMap<String,Object> map : resultMap) {
                T obj = ReflectHelper.newInstance(entiteClass);
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    Class fieldType = fieldTypeMap.get(entry.getKey());
                    ReflectHelper.setMethod(obj,entry.getKey(),entry.getValue(),fieldType);
                }
                result2.add((T) obj);
            }
            displayRecordsLength = result2.size();
            return result2;
        }
    }

    public Long getFilteredCount() {
        if (StringHelper.isEmpty(criterias.getSearch()) && (!criterias.hasOneFilteredColumn())) {
            return totalCount;
        }
        if (criterias.getStart() == 0) {
            if (criterias.getLength() > displayRecordsLength) {
                return (long) displayRecordsLength;
            }
        }
        if(this.customSQL.equals("")) {
            javax.persistence.Query query = this.entityManager.createQuery("SELECT COUNT(*) FROM " + entiteClass.getSimpleName() + " p" + getFilterQuery());
            return (Long) query.getSingleResult();
        }
        else {
            javax.persistence.Query query = this.entityManager.createNativeQuery("SELECT COUNT(*) FROM (" + this.customSQL + ") customSQL" + getFilterQuery());
            return ((BigInteger) query.getSingleResult()).longValue();
        }
    }

    public Long getTotalCount() {
        if(this.customSQL.equals("")) {
            javax.persistence.Query query = this.entityManager.createQuery("SELECT COUNT(*) FROM " + entiteClass.getSimpleName() + " p");
            totalCount = (Long) query.getSingleResult();
        }
        else
        {
            javax.persistence.Query query = this.entityManager.createNativeQuery("SELECT COUNT(*) FROM (" + this.customSQL + ") customSQL");
            totalCount = ((BigInteger) query.getSingleResult()).longValue();
        }
        return totalCount;
    }
}