package com.app.tvrecyclerview.reuglar;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.app.tvrecyclerview.R;
import com.app.tvrecyclerview.Utils;
import com.app.tvrecyclerview.bean.Row;
import com.app.tvrecyclerview.bean.itemPosition;
import com.google.gson.Gson;

import app.com.tvrecyclerview.FocusHighlightHelper;
import app.com.tvrecyclerview.GridObjectAdapter;
import app.com.tvrecyclerview.HorizontalGridView;
import app.com.tvrecyclerview.RowItem;


public class RegularActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regular);

        HorizontalGridView gridView = findViewById(R.id.id_grid_first);
        gridView.addItemDecoration(new SpaceItemDecoration());
        gridView.setNumRows(2);
        GridObjectAdapter adapter = new GridObjectAdapter(new RegularPresenter(this));
        gridView.setFocusZoomFactor(FocusHighlightHelper.ZOOM_FACTOR_SMALL);
        gridView.setAdapter(adapter);
        for (int i = 0; i < 10; i++) {
            adapter.add(new RowItem());
        }


        HorizontalGridView gridView1 = findViewById(R.id.id_grid_second);
        String json1 = Utils.inputStreamToString(getResources().openRawResource(R.raw.horizonal_regular));
        Row row1 = new Gson().fromJson(json1, Row.class);
        GridObjectAdapter adapter1 = new GridObjectAdapter(new RegularPresenter(this),
                row1.getRowSpacing(), row1.getColumnSpacing(), row1.getColumns(),
                row1.getAspectRatio());
        gridView1.setFocusZoomFactor(FocusHighlightHelper.ZOOM_FACTOR_SMALL);
        gridView1.setAdapter(adapter1);
        for (itemPosition position : row1.getItems()) {
            RowItem rowItem = new RowItem();
            rowItem.setX(position.getX());
            rowItem.setY(position.getY());
            rowItem.setWidth(position.getWidth());
            rowItem.setHeight(position.getHeight());
            adapter1.add(rowItem);
        }
    }

    private class SpaceItemDecoration extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.right = 30;
            outRect.bottom = 30;
        }
    }
}
