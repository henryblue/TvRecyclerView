package com.app.tvrecyclerview.reuglar;

import android.content.Context;

import app.com.tvrecyclerview.Presenter;
import app.com.tvrecyclerview.PresenterSelector;

public class RegularPresenterSelector extends PresenterSelector {

    private Context mContext;

    public RegularPresenterSelector(Context context) {
        mContext = context;
    }

    @Override
    public Presenter getPresenter(Object item) {
        return new RegularPresenter(mContext);
    }
}
