package app.com.tvrecyclerview;


import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;


public class ItemBridgeAdapter extends RecyclerView.Adapter {

    /**
     * Interface for listening to ViewHolder operations.
     */
    public static class AdapterListener {
        public void onAddPresenter(Presenter presenter, int type) {
        }
        public void onCreate(ViewHolder viewHolder) {
        }
        public void onBind(ViewHolder viewHolder) {
        }
        public void onUnbind(ViewHolder viewHolder) {
        }
        public void onAttachedToWindow(ViewHolder viewHolder) {
        }
        public void onDetachedFromWindow(ViewHolder viewHolder) {
        }
    }

    private GridObjectAdapter mAdapter;
    private ArrayList<Presenter> mPresenters = new ArrayList<>();
    private PresenterSelector mPresenterSelector;
    private FocusHighlightHandler mFocusHighlight;
    private AdapterListener mAdapterListener;


    private GridObjectAdapter.DataObserver mDataObserver = new GridObjectAdapter.DataObserver() {
        @Override
        public void onChanged() {
            ItemBridgeAdapter.this.notifyDataSetChanged();
        }
        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            ItemBridgeAdapter.this.notifyItemRangeChanged(positionStart, itemCount);
        }
        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            ItemBridgeAdapter.this.notifyItemRangeInserted(positionStart, itemCount);
        }
        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            ItemBridgeAdapter.this.notifyItemRangeRemoved(positionStart, itemCount);
        }
    };

    public ItemBridgeAdapter() {
    }

    public ItemBridgeAdapter(GridObjectAdapter adapter) {
        this(adapter, null);
    }

    public ItemBridgeAdapter(GridObjectAdapter adapter, PresenterSelector presenterSelector) {
        setAdapter(adapter);
        mPresenterSelector = presenterSelector;
    }

    /**
     * Sets the {@link GridObjectAdapter}.
     */
    public void setAdapter(GridObjectAdapter adapter) {
        if (mAdapter != null) {
            mAdapter.unregisterObserver(mDataObserver);
        }
        mAdapter = adapter;
        if (mAdapter == null) {
            return;
        }

        mAdapter.registerObserver(mDataObserver);
    }

    /**
     * Returns the {@link GridObjectAdapter}.
     */
    public GridObjectAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * Sets the AdapterListener.
     */
    public void setAdapterListener(AdapterListener listener) {
        mAdapterListener = listener;
    }

    void setFocusHighlight(FocusHighlightHandler listener) {
        mFocusHighlight = listener;
    }

    /**
     * Clears the adapter.
     */
    public void clear() {
        setAdapter(null);
    }

    @Override
    public int getItemViewType(int position) {
        PresenterSelector presenterSelector = mPresenterSelector != null ?
                mPresenterSelector : mAdapter.getPresenterSelector();
        Object item = mAdapter.get(position);
        Presenter presenter = presenterSelector.getPresenter(item);
        int type = mPresenters.indexOf(presenter);
        if (type < 0) {
            mPresenters.add(presenter);
            type = mPresenters.indexOf(presenter);
            if (mAdapterListener != null) {
                mAdapterListener.onAddPresenter(presenter, type);
            }
        }
        return type;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Presenter presenter = mPresenters.get(viewType);
        Presenter.ViewHolder presenterVh = presenter.onCreateViewHolder(parent);
        View view = presenterVh.view;
        ViewHolder viewHolder = new ViewHolder(presenter, view, presenterVh);
        if (mAdapterListener != null) {
            mAdapterListener.onCreate(viewHolder);
        }
        View presenterView = viewHolder.mHolder.view;
        if (presenterView != null) {
            viewHolder.mFocusChangeListener.mChainedListener = presenterView.getOnFocusChangeListener();
            presenterView.setOnFocusChangeListener(viewHolder.mFocusChangeListener);
        }
        if (mFocusHighlight != null) {
            mFocusHighlight.onInitializeView(view);
        }
        return viewHolder;
    }

    @Override
    public int getItemCount() {
        return mAdapter.size();
    }

    @Override
    public final void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ViewHolder viewHolder = (ViewHolder) holder;
        viewHolder.mItem = mAdapter.get(position);
        viewHolder.mPresenter.onBindViewHolder(viewHolder.mHolder, viewHolder.mItem);
        if (mAdapterListener != null) {
            mAdapterListener.onBind(viewHolder);
        }
    }

    @Override
    public final void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        ViewHolder viewHolder = (ViewHolder) holder;
        viewHolder.mPresenter.onUnbindViewHolder(viewHolder.mHolder);
        if (mAdapterListener != null) {
            mAdapterListener.onUnbind(viewHolder);
        }
        viewHolder.mItem = null;
    }

    @Override
    public final void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        ViewHolder viewHolder = (ViewHolder) holder;
        if (mAdapterListener != null) {
            mAdapterListener.onAttachedToWindow(viewHolder);
        }
        viewHolder.mPresenter.onViewAttachedToWindow(viewHolder.mHolder);
    }

    @Override
    public final void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        ViewHolder viewHolder = (ViewHolder) holder;
        viewHolder.mPresenter.onViewDetachedFromWindow(viewHolder.mHolder);
        if (mAdapterListener != null) {
            mAdapterListener.onDetachedFromWindow(viewHolder);
        }
    }

    @Override
    public long getItemId(int position) {
        return mAdapter.getId(position);
    }

    private final class OnFocusChangeListener implements View.OnFocusChangeListener {
        View.OnFocusChangeListener mChainedListener;

        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            if (mFocusHighlight != null) {
                mFocusHighlight.onItemFocused(view, hasFocus);
            }
            if (mChainedListener != null) {
                mChainedListener.onFocusChange(view, hasFocus);
            }
        }
    }

    /**
     * ViewHolder for the ItemBridgeAdapter.
     */
    public class ViewHolder extends RecyclerView.ViewHolder{
        final Presenter mPresenter;
        final Presenter.ViewHolder mHolder;
        final OnFocusChangeListener mFocusChangeListener = new OnFocusChangeListener();
        Object mItem;

        /**
         * Get {@link Presenter}.
         */
        public final Presenter getPresenter() {
            return mPresenter;
        }

        /**
         * Get {@link Presenter.ViewHolder}.
         */
        public final Presenter.ViewHolder getViewHolder() {
            return mHolder;
        }

        /**
         * Get currently bound object.
         */
        public final Object getItem() {
            return mItem;
        }

        ViewHolder(Presenter presenter, View view, Presenter.ViewHolder holder) {
            super(view);
            mPresenter = presenter;
            mHolder = holder;
        }
    }
}
