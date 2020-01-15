package com.app.tvrecyclerview.reuglar;

import android.app.Activity;
import android.os.Bundle;

import com.app.tvrecyclerview.R;
import com.app.tvrecyclerview.Utils;
import com.app.tvrecyclerview.bean.Row;
import com.app.tvrecyclerview.bean.itemPosition;
import com.google.gson.Gson;

import app.com.tvrecyclerview.GridObjectAdapter;
import app.com.tvrecyclerview.HorizontalGridView;
import app.com.tvrecyclerview.RowItem;


public class RegularActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regular);
        HorizontalGridView gridView = findViewById(R.id.id_grid_single);
        GridObjectAdapter adapter = new GridObjectAdapter(new RegularPresenter(this));

        String json = Utils.inputStreamToString(getResources().openRawResource(R.raw.horizonal_regular));
        Row row = new Gson().fromJson(json, Row.class);
        for (itemPosition position : row.getItems()) {
            RowItem rowItem = new RowItem();
            rowItem.setX(position.getX());
            rowItem.setY(position.getY());
            rowItem.setWidth(position.getWidth());
            rowItem.setHeight(position.getHeight());
            adapter.add(rowItem);
        }
        adapter.setGridStyle(row.getRowSpacing(), row.getColumnSpacing(), row.getColumns(),
                row.getAspectRatio());
        gridView.setObjectAdapter(adapter);
    }
}
