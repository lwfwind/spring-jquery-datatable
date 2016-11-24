package com.web.spring.datatable;


import com.library.common.ReflectHelper;
import com.library.common.StringHelper;
import com.web.spring.datatable.annotations.SqlCondition;
import com.web.spring.datatable.annotations.SqlIndex;
import com.web.spring.datatable.annotations.SqlIndexOperator;
import com.web.spring.datatable.util.Validate;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.*;


public class TableQuery {
    private static HashMap<String, Boolean> innodbMap = new HashMap<>();
    private static boolean isInnodbFlag = false;
    private EntityManager entityManager;
    private Class entiteClass;
    private DatatablesCriterias criterias;
    private int displayRecordsLength = 0;
    private String customSQL = "";
    private List<String> selectColumnList = new ArrayList<>();
    private HashMap<String, Class> fieldTypeMap = new HashMap<>();
    private String entiteTableName = "";
    private Long totalCount = 0L;
    private Long filteredCount = 0L;

    public <T> TableQuery(EntityManager entityManager, Class<T> entiteClass, DatatablesCriterias criterias) {
        this.entityManager = entityManager;
        this.entiteClass = entiteClass;
        this.criterias = criterias;
        init();
    }

    public <T> TableQuery(EntityManager entityManager, Class<T> entiteClass, DatatablesCriterias criterias, String customSQL) {
        this.entityManager = entityManager;
        this.entiteClass = entiteClass;
        this.criterias = criterias;
        this.customSQL = customSQL;
        String[] columnArray = StringHelper.getBetweenString(this.customSQL.toLowerCase(), "select", "from").split(",");
        for (String aColumnArray : columnArray) {
            if (aColumnArray.toLowerCase().contains("as")) {
                selectColumnList.add(aColumnArray.substring(aColumnArray.indexOf("as") + 2).trim());
            } else {
                if(aColumnArray.contains(".")){
                    selectColumnList.add(aColumnArray.substring(aColumnArray.indexOf(".") + 1).trim());
                }
                else {
                    selectColumnList.add(aColumnArray.trim());
                }
            }
        }
        init();
    }

    public Long getFilteredCount() {
        return filteredCount;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    @SuppressWarnings("unchecked")
    public void init() {
        if (this.entiteClass.isAnnotationPresent(Table.class)) {
            Table table = (Table) this.entiteClass.getAnnotation(Table.class);
            this.entiteTableName = table.name();
        } else {
            this.entiteTableName = this.entiteClass.getSimpleName();
        }

        if (!isInnodbFlag) {
            Query query = this.entityManager.createNativeQuery("SELECT table_name FROM INFORMATION_SCHEMA.TABLES WHERE engine = 'InnoDB'");
            List<Object> result = query.getResultList();
            for (Object object : result) {
                innodbMap.put(object.toString(), true);
            }
            isInnodbFlag = true;
        }
    }

    public <T> DataSet<T> getResultDataSet() {
        List<T> rows = getRows();
        Long count = fetchTotalCount();
        Long countFiltered = fetchFilteredCount();
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
            fieldTypeMap.put(field.getName(), field.getType());
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
                    conditionMap.put(column.name(), sqlCondition.value());
                } else {
                    conditionMap.put(field.getName(), sqlCondition.value());
                }
            }
        }

        /**
         * Step 1.1: custom condition
         */
        if (conditionMap.size() > 0) {
            queryBuilder.append(" WHERE ");
            for (ColumnDef columnDef : criterias.getColumnDefs()) {
                if (indexColumnList.contains(columnDef.getName())) {
                    if (conditionMap.get(columnDef.getName()) != null) {
                        String condition = conditionMap.get(columnDef.getName());
                        if (!condition.contains("?")) {
                            paramList.add(" " + condition);
                        }
                    }
                }

                if (unIndexColumnList.contains(columnDef.getName())) {
                    if (conditionMap.get(columnDef.getName()) != null) {
                        String condition = conditionMap.get(columnDef.getName());
                        if (!condition.contains("?")) {
                            paramList.add(" " + condition);
                        }
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
                        if (conditionMap.get(columnDef.getName()) == null) {
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
                        } else {
                            String condition = conditionMap.get(columnDef.getName());
                            if (condition.contains("?")) {
                                paramList.add(" " + condition.replaceAll("\\?", columnDef.getSearch()));
                            }
                        }
                    }
                }
            }

            for (ColumnDef columnDef : criterias.getColumnDefs()) {
                if (columnDef.isSearchable() && indexColumnList.contains(columnDef.getName())) {
                    if (StringHelper.isNotEmpty(columnDef.getSearchFrom())) {
                        if (Validate.isDate(columnDef.getSearchFrom())) {
                            paramList.add("" + columnDef.getName() + " >= '" + columnDef.getSearchFrom() + "'");
                        } else {
                            paramList.add("" + columnDef.getName() + " >= " + columnDef.getSearchFrom());
                        }
                    }
                    if (StringHelper.isNotEmpty(columnDef.getSearchTo())) {
                        if (Validate.isDate(columnDef.getSearchTo())) {
                            paramList.add("" + columnDef.getName() + " < '" + columnDef.getSearchTo() + "'");
                        } else {
                            paramList.add("" + columnDef.getName() + " < " + columnDef.getSearchTo());
                        }
                    }
                }
            }

            for (ColumnDef columnDef : criterias.getColumnDefs()) {
                if (columnDef.isSearchable() && unIndexColumnList.contains(columnDef.getName())) {
                    if (StringHelper.isNotEmpty(columnDef.getSearchFrom())) {
                        if (Validate.isDate(columnDef.getSearchFrom())) {
                            paramList.add("" + columnDef.getName() + " >= '" + columnDef.getSearchFrom() + "'");
                        } else {
                            paramList.add("" + columnDef.getName() + " >= " + columnDef.getSearchFrom());
                        }
                    }
                    if (StringHelper.isNotEmpty(columnDef.getSearchTo())) {
                        if (Validate.isDate(columnDef.getSearchTo())) {
                            paramList.add("" + columnDef.getName() + " < '" + columnDef.getSearchTo() + "'");
                        } else {
                            paramList.add("" + columnDef.getName() + " < " + columnDef.getSearchTo());
                        }
                    }
                    if (StringHelper.isNotEmpty(columnDef.getSearch())) {
                        if (conditionMap.get(columnDef.getName()) == null) {
                            paramList.add(" " + columnDef.getName()
                                    + " LIKE '%?%'".replace("?", columnDef.getSearch()));
                        } else {
                            String condition = conditionMap.get(columnDef.getName());
                            if (condition.contains("?")) {
                                paramList.add(" " + condition.replaceAll("\\?", columnDef.getSearch()));
                            }
                        }
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
        if (this.customSQL.equals("")) {
            queryBuilder.append("SELECT p FROM ").append(entiteClass.getSimpleName()).append(" p");
        } else {
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

        if (this.customSQL.equals("")) {
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
        } else {
            Query query = this.entityManager.createNativeQuery(queryBuilder.toString());

            /**
             * Step 3: paging
             */
            query.setFirstResult(criterias.getStart());
            if (criterias.getLength() == -1) {
                query.setMaxResults(query.getResultList().size());
            } else {
                query.setMaxResults(criterias.getLength());
            }
            List<Object[]> result = query.getResultList();
            List<HashMap<String, Object>> resultMap = new ArrayList<>();
            for (Object[] object : result) {
                int i = 0;
                HashMap<String, Object> map = new HashMap<String, Object>();
                for (String columnName : selectColumnList) {
                    map.put(columnName, object[i]);
                    i++;
                }
                resultMap.add(map);
            }
            List<T> result2 = new ArrayList<>();
            for (HashMap<String, Object> map : resultMap) {
                T obj = ReflectHelper.newInstance(entiteClass);
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    Class fieldType = fieldTypeMap.get(entry.getKey());
                    ReflectHelper.setMethod(obj, entry.getKey(), entry.getValue(), fieldType);
                }
                result2.add((T) obj);
            }
            displayRecordsLength = result2.size();
            return result2;
        }
    }

    public Long fetchFilteredCount() {
        if (StringHelper.isEmpty(criterias.getSearch()) && (!criterias.hasOneFilteredColumn())) {
            filteredCount = totalCount;
            return totalCount;
        }
        if (criterias.getStart() == 0) {
            if (criterias.getLength() > displayRecordsLength) {
                filteredCount = (long) displayRecordsLength;
                return filteredCount;
            }
        }
        if (this.customSQL.equals("")) {
            javax.persistence.Query query = this.entityManager.createQuery("SELECT COUNT(*) FROM " + entiteClass.getSimpleName() + " p" + getFilterQuery());
            filteredCount = (Long) query.getSingleResult();
        } else {
            javax.persistence.Query query = this.entityManager.createNativeQuery("SELECT COUNT(*) FROM (" + this.customSQL + ") customSQL" + getFilterQuery());
            filteredCount = ((BigInteger) query.getSingleResult()).longValue();
        }
        return filteredCount;
    }

    public Long fetchTotalCount() {
        if (this.customSQL.equals("")) {
            if (innodbMap.get(this.entiteTableName) == null) {
                javax.persistence.Query query = this.entityManager.createQuery("SELECT COUNT(*) FROM " + entiteClass.getSimpleName() + " p");
                totalCount = (Long) query.getSingleResult();
            } else {
                Query query = this.entityManager.createNativeQuery("SELECT TABLE_ROWS FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '" + this.entiteTableName + "'");
                totalCount = ((BigInteger) query.getSingleResult()).longValue();
            }
        } else {
            javax.persistence.Query query = this.entityManager.createNativeQuery("SELECT COUNT(*) FROM (" + this.customSQL + ") customSQL");
            totalCount = ((BigInteger) query.getSingleResult()).longValue();
        }
        return totalCount;
    }
}