/**
    * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
    * @author tatianag
    */

'use strict';

import Enum from "enum";

export const Align = new Enum(['LEFT', 'RIGHT', 'CENTER']);

export class BaseTableColumn {

    /*
     private String title;
     private String name;
     private String type;
     private TableDataView.Align align;
     private int width;
     private int prefWidth = 0;
     private String units;
     private boolean isSortable = true;
     private boolean isHidden = false;
     private boolean isVisible = true;
     private String shortDesc;
     private String[] enumVals;
     private String[] sortByCols;
     private boolean requiresQuotes = false;
     */

    constructor(name, align = Align.LEFT, width = 10, sortable = true) {
        this.name = name;
        this.title = name;
        this.shortDesc = name;
        this.align = align;
        this.width = width;
        this.isSortable = sortable;
    }

    setShortDesc(shortDesc) {
        this.shortDesc = shortDesc;
    }

    getShortDesc() {
        return this.shortDesc;
    }

    getTitle() {
        return this.title;
    }

    getName() {
        return this.name;
    }

    setTitle(title) {
        this.title = title;
    }

    setName(name) {
        this.name = name;
    }

    getAlign() {
        return this.align;
    }

    setAlign(align) {
        this.align = align;
    }

    /**
     * returns recommnended width for this columns.
     */
    getWidth() {
        return this.width;
    }

    /**
     * set the recommnended width for this columns.
     * @param {Number} width
     */
    setWidth(width) {
        this.width = width;
    }

    getPrefWidth() {
        return this.prefWidth > 0 ? this.prefWidth : this.width;
    }

    setPrefWidth(prefWidth) {
        this.prefWidth = prefWidth;
    }

    getUnits() {
        return this.units;
    }

    setUnits(units) {
        this.units = units;
    }

    getType() {
        return this.type;
    }

    setType(type) {
        this.type = type;
    }

    isSortable() {
        return this.isSortable;
    }

    setSortable(sortable) {
        this.isSortable = sortable;
    }

    isHidden() {
        return this.isHidden;
    }

    setHidden(hidden) {
        this.isHidden = hidden;
        this.isVisible = hidden ? false : this.isVisible;
    }

    isVisible() {
        return this.isVisible;
    }

    setVisible(visible) {
        this.isVisible = visible;
    }

    // @param {Array} enumVals - an array of strings
    setEnums(enumVals) {
        this.enumVals = enumVals;
    }

    getEnums() {
        return this.enumVals;
    }

    // @return {Array} an array of strings
    getSortByCols() {
        return this.sortByCols;
    }

    // @param secondarySortCols - an array of strings
    setSortByCols(secondarySortCols) {
        this.sortByCols = secondarySortCols;
    }

    isRequiresQuotes() {
        return this.requiresQuotes;
    }

    setRequiresQuotes(requiresQuotes) {
        this.requiresQuotes = requiresQuotes;
    }
}
