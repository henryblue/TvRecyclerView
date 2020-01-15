package com.app.tvrecyclerview.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Row {
    @SerializedName("rowSpacing") private int rowSpacing = 15;
    @SerializedName("columnSpacing") private int columnSpacing = 15;
    @SerializedName("columns") private int columns = 1;
    @SerializedName("aspectRatio") private float aspectRatio = 1.0f;
    @SerializedName("items") private List<itemPosition> items= null;

    public List<itemPosition> getItems() {
        return items;
    }

    public void setItems(List<itemPosition> items) {
        this.items = items;
    }

    public int getRowSpacing() {
        return rowSpacing;
    }

    public void setRowSpacing(int rowSpacing) {
        this.rowSpacing = rowSpacing;
    }

    public int getColumnSpacing() {
        return columnSpacing;
    }

    public void setColumnSpacing(int columnSpacing) {
        this.columnSpacing = columnSpacing;
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }
}
