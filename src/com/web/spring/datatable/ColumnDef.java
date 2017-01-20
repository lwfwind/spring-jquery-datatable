package com.web.spring.datatable;

import java.io.Serializable;

/**
 * A column definition, containing the different information used when
 * server-side processing is enabled.
 */
public class ColumnDef implements Serializable {

    private static final long serialVersionUID = 6349611254914115218L;
    private String name;
    private boolean sortable;
    private boolean sorted = false;
    private boolean searchable;
    private boolean filtered;
    private String regex;
    private String search;
    private String searchFrom;
    private String searchTo;
    private SortDirection sortDirection;

    /**
     * Gets sort direction.
     *
     * @return the sort direction
     */
    public SortDirection getSortDirection() {
        return sortDirection;
    }

    /**
     * Sets sort direction.
     *
     * @param sortDirection the sort direction
     */
    public void setSortDirection(SortDirection sortDirection) {
        this.sortDirection = sortDirection;
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Is sortable boolean.
     *
     * @return the boolean
     */
    public boolean isSortable() {
        return sortable;
    }

    /**
     * Sets sortable.
     *
     * @param sortable the sortable
     */
    public void setSortable(boolean sortable) {
        this.sortable = sortable;
    }

    /**
     * Is filterable boolean.
     *
     * @return {@code true} if the column is searchable, {@code false} otherwise.
     * @deprecated Use {@link #isSearchable()} instead.
     */
    public boolean isFilterable() {
        return searchable;
    }

    /**
     * Is searchable boolean.
     *
     * @return {@code true} if the column is searchable, {@code false} otherwise.
     */
    public boolean isSearchable() {
        return searchable;
    }

    /**
     * Sets searchable.
     *
     * @param searchable the searchable
     */
    public void setSearchable(boolean searchable) {
        this.searchable = searchable;
    }

    /**
     * Gets regex.
     *
     * @return the regex
     */
    public String getRegex() {
        return regex;
    }

    /**
     * Sets regex.
     *
     * @param regex the regex
     */
    public void setRegex(String regex) {
        this.regex = regex;
    }

    /**
     * Gets search.
     *
     * @return the search
     */
    public String getSearch() {
        return search;
    }

    /**
     * Sets search.
     *
     * @param search the search
     */
    public void setSearch(String search) {
        this.search = search;
    }

    /**
     * Gets search from.
     *
     * @return the search from
     */
    public String getSearchFrom() {
        return searchFrom;
    }

    /**
     * Sets search from.
     *
     * @param searchFrom the search from
     */
    public void setSearchFrom(String searchFrom) {
        this.searchFrom = searchFrom;
    }

    /**
     * Gets search to.
     *
     * @return the search to
     */
    public String getSearchTo() {
        return searchTo;
    }

    /**
     * Sets search to.
     *
     * @param searchTo the search to
     */
    public void setSearchTo(String searchTo) {
        this.searchTo = searchTo;
    }

    /**
     * Is sorted boolean.
     *
     * @return the boolean
     */
    public boolean isSorted() {
        return sorted;
    }

    /**
     * Sets sorted.
     *
     * @param sorted the sorted
     */
    public void setSorted(boolean sorted) {
        this.sorted = sorted;
    }

    /**
     * Is filtered boolean.
     *
     * @return the boolean
     */
    public boolean isFiltered() {
        return filtered;
    }

    /**
     * Sets filtered.
     *
     * @param filtered the filtered
     */
    public void setFiltered(boolean filtered) {
        this.filtered = filtered;
    }

    @Override
    public String toString() {
        return "ColumnDef [name=" + name + ", sortable=" + sortable + ", sorted=" + sorted + ", searchable=" + searchable
                + ", filtered=" + filtered + ", regex=" + regex + ", search=" + search + ", searchFrom=" + searchFrom
                + ", searchTo=" + searchTo + ", sortDirection=" + sortDirection + "]";
    }

    /**
     * The enum Sort direction.
     */
    public enum SortDirection {
        /**
         * Asc sort direction.
         */
        ASC, /**
         * Desc sort direction.
         */
        DESC;
    }
}