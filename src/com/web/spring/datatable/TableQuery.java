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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TableQuery {
    private static HashMap<String, Boolean> innodbMap = new HashMap<>();
    private static HashMap<EntityManager, Boolean> entityManagerInitMap = new HashMap<>();
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
        List<String> columnList;
        int from_min = 10000;
        String from_str = "";
        int from_1 = this.customSQL.indexOf(" from ");
        if(from_1 > 0 && from_min > from_1){
            from_min = from_1;
            from_str = " from ";
        }
        int from_2 = this.customSQL.indexOf("\nfrom ");
        if(from_2 > 0 && from_min > from_2){
            from_min = from_2;
            from_str = "\nfrom ";
        }
        int from_3 = this.customSQL.indexOf(" from\n");
        if(from_3 > 0 && from_min > from_3){
            from_str = " from\n";
        }
        String columnString = StringHelper.getBetweenString(this.customSQL.toLowerCase(), "select", from_str);
        String regex = "\\(.*?\\) *?as";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(columnString);
        while (matcher.find()) {
            System.out.println("matcher: " + matcher.group(0));
            columnString = columnString.replace(matcher.group(0).substring(0,matcher.group(0).length()-3),"");
        }
        columnList = Arrays.asList(columnString.split(","));
        for (String columnName : columnList) {
            if (columnName.toLowerCase().contains(" as ")) {
                selectColumnList.add(columnName.substring(columnName.lastIndexOf(" as ") + 4).trim());
            } else {
                if(columnName.contains("(") || columnName.contains(")")){
                    continue;
                }
                if(columnName.contains(".")){
                    selectColumnList.add(columnName.substring(columnName.lastIndexOf(".") + 1).trim());
                }
                else {
                    selectColumnList.add(columnName.trim());
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

        if (entityManagerInitMap.get(this.entityManager) == null) {
            Query query = this.entityManager.createNativeQuery("SELECT table_name FROM INFORMATION_SCHEMA.TABLES WHERE engine = 'InnoDB' and TABLE_SCHEMA != 'mysql'");
            List<Object> result = query.getResultList();
            for (Object object : result) {
                innodbMap.put(object.toString(), true);
            }
            entityManagerInitMap.put(this.entityManager,true);
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
                Query query = this.entityManager.createNativeQuery("SELECT TABLE_ROWS FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '" + this.entiteTableName + "' and TABLE_SCHEMA != 'mysql'");
                totalCount = ((BigInteger) query.getSingleResult()).longValue();
            }
        } else {
            javax.persistence.Query query = this.entityManager.createNativeQuery("SELECT COUNT(*) FROM (" + this.customSQL + ") customSQL");
            totalCount = ((BigInteger) query.getSingleResult()).longValue();
        }
        return totalCount;
    }
}