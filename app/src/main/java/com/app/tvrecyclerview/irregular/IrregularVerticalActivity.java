package com.app.tvrecyclerview.irregular;

import android.app.Activity;
import android.os.Bundle;

import com.app.tvrecyclerview.R;
import com.app.tvrecyclerview.Utils;
import com.app.tvrecyclerview.bean.Row;
import com.app.tvrecyclerview.bean.itemPosition;
import com.app.tvrecyclerview.reuglar.RegularVerticalPresenter;
import com.google.gson.Gson;

import app.com.tvrecyclerview.FocusHighlightHelper;
import app.com.tvrecyclerview.GridObjectAdapter;
import app.com.tvrecyclerview.RowItem;
import app.com.tvrecyclerview.VerticalGridView;


public class IrregularVerticalActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_irregular_vertical);
        VerticalGridView gridView = findViewById(R.id.id_grid_vertical_ir);
        String json = Utils.inputStreamToString(getResources().openRawResource(R.raw.vertical_irregular));
        Row row = new Gson().fromJson(json, Row.class);

        GridObjectAdapter adapter = new GridObjectAdapter(new RegularVerticalPresenter(this),
                row.getRowSpacing(), row.getColumnSpacing(), row.getColumns(),
                row.getAspectRatio());
        gridView.setFocusZoomFactor(FocusHighlightHelper.ZOOM_FACTOR_SMALL);
        gridView.setAdapter(adapter);

        for (itemPosition position : row.getItems()) {
            RowItem rowItem = new RowItem();
            rowItem.setX(position.getX());
            rowItem.setY(position.getY());
            rowItem.setWidth(position.getWidth());
            rowItem.setHeight(position.getHeight());
            adapter.add(rowItem);
        }
    }
}
