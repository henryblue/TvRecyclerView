package com.app.tvrecyclerview;

import android.graphics.Rect;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import app.com.tvrecyclerview.ModuleLayoutManager;
import app.com.tvrecyclerview.TvRecyclerView;

public class ModuleFocusVerticalActivity extends AppCompatActivity {

    private TvRecyclerView mTvRecyclerView;
    public int[] mStartIndex = {0, 1, 3, 7, 8, 9, 11, 12, 13, 14, 20, 21, 22, 23, 25, 26, 27};
    public int[] mItemRowSizes = {2, 2, 1, 1, 1, 1, 1, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1};
    public int[] mItemColumnSizes = {1, 2, 1, 1, 1, 2, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module_vertical);
        mTvRecyclerView = (TvRecyclerView) findViewById(R.id.tv_recycler_view);
        init();
    }

    private void init() {
        ModuleLayoutManager manager = new MyModuleLayoutManager(4, LinearLayoutManager.VERTICAL,
                400, 260);
        mTvRecyclerView.setLayoutManager(manager);

        int itemSpace = getResources().
                getDimensionPixelSize(R.dimen.recyclerView_item_space);
        mTvRecyclerView.addItemDecoration(new SpaceItemDecoration(itemSpace));
        ModuleAdapter mAdapter = new ModuleAdapter(ModuleFocusVerticalActivity.this, mStartIndex.length);
        mTvRecyclerView.setAdapter(mAdapter);

        mTvRecyclerView.setOnItemStateListener(new TvRecyclerView.OnItemStateListener() {
            @Override
            public void onItemViewClick(View view, int position) {
                Toast.makeText(ModuleFocusVerticalActivity.this,
                        ContantUtil.TEST_DATAS[position], Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onItemViewFocusChanged(boolean gainFocus, View view, int position) {
            }
        });
        mTvRecyclerView.setSelectPadding(35, 34, 35, 38);
    }

    private class SpaceItemDecoration extends RecyclerView.ItemDecoration {

        private int space;

        SpaceItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                   RecyclerView.State state) {
            outRect.bottom = space;
            outRect.left = space;
        }
    }

    private class MyModuleLayoutManager extends ModuleLayoutManager {

        MyModuleLayoutManager(int rowCount, int orientation, int baseItemWidth, int baseItemHeight) {
            super(rowCount, orientation, baseItemWidth, baseItemHeight);
        }

        @Override
        protected int getItemStartIndex(int position) {
            if (position < mStartIndex.length) {
                return mStartIndex[position];
            } else {
                return 0;
            }
        }

        @Override
        protected int getItemRowSize(int position) {
            if (position < mItemRowSizes.length) {
                return mItemRowSizes[position];
            } else {
                return 1;
            }
        }

        @Override
        protected int getItemColumnSize(int position) {
            if (position < mItemColumnSizes.length) {
                return mItemColumnSizes[position];
            } else {
                return 1;
            }
        }

        @Override
        protected int getColumnSpacing() {
            return getResources().
                    getDimensionPixelSize(R.dimen.recyclerView_item_space);
        }

        @Override
        protected int getRowSpacing() {
            return getResources().
                    getDimensionPixelSize(R.dimen.recyclerView_item_space);
        }
    }
}
