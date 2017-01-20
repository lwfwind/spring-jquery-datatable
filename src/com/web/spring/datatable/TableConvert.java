package com.web.spring.datatable;


import com.library.common.ReflectHelper;
import com.library.common.StringHelper;
import com.web.spring.datatable.util.Validate;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.toIntExact;


/**
 * The type Table convert.
 */
public class TableConvert {
    private List<?> entityList;
    private Class entityClass;
    private DatatablesCriterias criterias;
    private int displayRecordsLength = 0;
    private Long totalCount = 0L;
    private Long filteredCount = 0L;
    private HashMap<String, Class> fieldTypeMap = new HashMap<>();
    private DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Instantiates a new Table convert.
     *
     * @param entityList the entity list
     * @param criterias  the criterias
     */
    public <T> TableConvert(List<T> entityList, DatatablesCriterias criterias) {
        this.entityList = entityList;
        this.criterias = criterias;
        getActualClass(this.entityList);
        Field[] fields = this.entityClass.getDeclaredFields();
        for (Field field : fields) {
            fieldTypeMap.put(field.getName(), field.getType());
        }
    }

    /**
     * Gets filtered count.
     *
     * @return the filtered count
     */
    public Long getFilteredCount() {
        return filteredCount;
    }

    /**
     * Gets total count.
     *
     * @return the total count
     */
    public Long getTotalCount() {
        return totalCount;
    }


    /**
     * Gets result data set.
     *
     * @param <T> the type parameter
     * @return the result data set
     */
    public <T> DataSet<T> getResultDataSet() {
        List<T> rows = getRows();
        this.displayRecordsLength = rows.size();
        Long count = fetchTotalCount();
        Long countFiltered = fetchFilteredCount();
        return new DataSet<T>(rows, count, countFiltered);
    }

    private <T> void getActualClass(List<T> entityList) {
        Class genericClass = null;
        Iterator it = entityList.iterator();
        if (it.hasNext()) {
            genericClass = it.next().getClass();
        }
        if (genericClass != null) {
            this.entityClass = genericClass;
        }
    }

    /**
     * Gets rows.
     *
     * @param <T> the type parameter
     * @return the rows
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getRows() {
        List<T> rows = (List<T>) this.entityList;

        /**
         * Step 1: individual column filtering
         */
        if (criterias.hasOneSearchableColumn() && criterias.hasOneFilteredColumn()) {
            for (ColumnDef columnDef : criterias.getColumnDefs()) {
                if (columnDef.isSearchable() && !columnDef.getName().equals("")) {
                    if (StringHelper.isNotEmpty(columnDef.getSearch())) {
                        rows = rows.stream().filter(entity -> {
                            Object result = ReflectHelper.getMethod(entity, columnDef.getName());
                            return result.toString().contains(columnDef.getSearch());
                        }).collect(Collectors.toList());
                        ;
                    }
                }
            }

            for (ColumnDef columnDef : criterias.getColumnDefs()) {
                if (columnDef.isSearchable() && !columnDef.getName().equals("")) {
                    if (StringHelper.isNotEmpty(columnDef.getSearchFrom())) {
                        if (Validate.isDate(columnDef.getSearchFrom())) {
                            try {
                                Long unixTime = df.parse(columnDef.getSearchFrom()).getTime() / 1000;
                                rows = rows.stream().filter(entity -> {
                                    Object result = ReflectHelper.getMethod(entity, columnDef.getName());
                                    return Long.parseLong(result.toString()) >= unixTime;
                                }).collect(Collectors.toList());
                                ;
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        } else {
                            rows = rows.stream().filter(entity -> {
                                Object result = ReflectHelper.getMethod(entity, columnDef.getName());
                                if (StringHelper.isInteger(columnDef.getSearchFrom())) {
                                    return Long.parseLong(result.toString()) >= Long.parseLong(columnDef.getSearchFrom());
                                } else {
                                    return Float.parseFloat(result.toString()) >= Float.parseFloat(columnDef.getSearchFrom());
                                }
                            }).collect(Collectors.toList());
                            ;
                        }
                    }
                    if (StringHelper.isNotEmpty(columnDef.getSearchTo())) {
                        if (Validate.isDate(columnDef.getSearchTo())) {
                            try {
                                Long unixTime = df.parse(columnDef.getSearchTo()).getTime() / 1000;
                                rows = rows.stream().filter(entity -> {
                                    Object result = ReflectHelper.getMethod(entity, columnDef.getName());
                                    return Long.parseLong(result.toString()) <= unixTime;
                                }).collect(Collectors.toList());
                                ;
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        } else {
                            rows = rows.stream().filter(entity -> {
                                Object result = ReflectHelper.getMethod(entity, columnDef.getName());
                                if (StringHelper.isInteger(columnDef.getSearchTo())) {
                                    return Long.parseLong(result.toString()) <= Long.parseLong(columnDef.getSearchTo());
                                } else {
                                    return Float.parseFloat(result.toString()) <= Float.parseFloat(columnDef.getSearchTo());
                                }
                            }).collect(Collectors.toList());
                            ;
                        }
                    }
                }
            }
        }

        /**
         * Step 2: global filtering
         */
        if (StringHelper.isNotEmpty(criterias.getSearch()) && criterias.hasOneSearchableColumn()) {
            rows = rows.stream().filter(entity -> {
                boolean condition = false;
                for (ColumnDef columnDef : criterias.getColumnDefs()) {
                    if (columnDef.isSearchable() && !columnDef.getName().equals("")) {
                        Object result = ReflectHelper.getMethod(entity, columnDef.getName());
                        condition = condition || result.toString().contains(criterias.getSearch());
                    }
                }
                return condition;
            }).collect(Collectors.toList());
        }

        this.filteredCount = (long) rows.size();

        /**
         * Step 3: sorting
         */
        if (criterias.hasOneSortedColumn()) {
            for (ColumnDef columnDef : criterias.getSortedColumnDefs()) {
                if (columnDef.getSortDirection().equals(ColumnDef.SortDirection.DESC)) {
                    rows.sort((entity1, entity2) -> {
                        Object result1 = ReflectHelper.getMethod(entity1, columnDef.getName());
                        Object result2 = ReflectHelper.getMethod(entity2, columnDef.getName());
                        Class fieldType = fieldTypeMap.get(columnDef.getName());
                        if (String.class.equals(fieldType)) {
                            return result2.toString().compareTo(result1.toString());
                        } else if (int.class.equals(fieldType)) {
                            return Integer.compare(Integer.parseInt(result2.toString()), Integer.parseInt(result1.toString()));
                        } else if (Integer.class.equals(fieldType)) {
                            return Integer.compare(Integer.parseInt(result2.toString()), Integer.parseInt(result1.toString()));
                        } else if (long.class.equals(fieldType)) {
                            return Long.compare(Long.parseLong(result2.toString()), Long.parseLong(result1.toString()));
                        } else if (Long.class.equals(fieldType)) {
                            return Long.compare(Long.parseLong(result2.toString()), Long.parseLong(result1.toString()));
                        } else if (float.class.equals(fieldType)) {
                            return Float.compare(Float.parseFloat(result2.toString()), Float.parseFloat(result1.toString()));
                        } else if (Float.class.equals(fieldType)) {
                            return Float.compare(Float.parseFloat(result2.toString()), Float.parseFloat(result1.toString()));
                        } else if (double.class.equals(fieldType)) {
                            return Double.compare(Double.parseDouble(result2.toString()), Double.parseDouble(result1.toString()));
                        } else if (Double.class.equals(fieldType)) {
                            return Double.compare(Double.parseDouble(result2.toString()), Double.parseDouble(result1.toString()));
                        }
                        return 0;
                    });
                } else {
                    rows.sort((entity1, entity2) -> {
                        Object result1 = ReflectHelper.getMethod(entity1, columnDef.getName());
                        Object result2 = ReflectHelper.getMethod(entity2, columnDef.getName());
                        Class fieldType = fieldTypeMap.get(columnDef.getName());
                        if (String.class.equals(fieldType)) {
                            return result1.toString().compareTo(result2.toString());
                        } else if (int.class.equals(fieldType)) {
                            return Integer.compare(Integer.parseInt(result1.toString()), Integer.parseInt(result2.toString()));
                        } else if (Integer.class.equals(fieldType)) {
                            return Integer.compare(Integer.parseInt(result1.toString()), Integer.parseInt(result2.toString()));
                        } else if (long.class.equals(fieldType)) {
                            return Long.compare(Long.parseLong(result1.toString()), Long.parseLong(result2.toString()));
                        } else if (Long.class.equals(fieldType)) {
                            return Long.compare(Long.parseLong(result1.toString()), Long.parseLong(result2.toString()));
                        } else if (float.class.equals(fieldType)) {
                            return Float.compare(Float.parseFloat(result1.toString()), Float.parseFloat(result2.toString()));
                        } else if (Float.class.equals(fieldType)) {
                            return Float.compare(Float.parseFloat(result1.toString()), Float.parseFloat(result2.toString()));
                        } else if (double.class.equals(fieldType)) {
                            return Double.compare(Double.parseDouble(result1.toString()), Double.parseDouble(result2.toString()));
                        } else if (Double.class.equals(fieldType)) {
                            return Double.compare(Double.parseDouble(result1.toString()), Double.parseDouble(result2.toString()));
                        }
                        return 0;
                    });
                }
            }
        }

        /**
         * Step 4: paging
         */
        if (criterias.getStart() + criterias.getLength() <= filteredCount) {
            return rows.subList(criterias.getStart(), criterias.getStart() + criterias.getLength());
        } else {
            return rows.subList(criterias.getStart(), toIntExact(filteredCount));
        }
    }

    /**
     * Fetch filtered count long.
     *
     * @return the long
     */
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
        return filteredCount;
    }

    /**
     * Fetch total count long.
     *
     * @return the long
     */
    public Long fetchTotalCount() {
        this.totalCount = (long) this.entityList.size();
        return this.totalCount;
    }
}