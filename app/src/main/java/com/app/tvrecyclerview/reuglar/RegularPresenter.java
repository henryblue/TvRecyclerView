package com.app.tvrecyclerview.reuglar;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.app.tvrecyclerview.Utils;

import app.com.tvrecyclerview.Presenter;


public class RegularPresenter extends Presenter {

    RegularPresenter(Context context) {
        super(context);
    }

    @Override
    public View onCreateView() {
        ImageView view = new ImageView(getContext());
        view.setSelected(true);
        view.setFocusable(true);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(423, 171);
        view.setLayoutParams(params);
        return view;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        viewHolder.view.setBackgroundColor(Utils.getRandColor());
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }
}
