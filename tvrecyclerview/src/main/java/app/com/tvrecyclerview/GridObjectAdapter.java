package app.com.tvrecyclerview;

import android.database.Observable;

import java.util.ArrayList;

public class GridObjectAdapter {

    /**
     * A DataObserver can be notified when an ObjectAdapter's underlying data
     * changes. Separate methods provide notifications about different types of
     * changes.
     */
    public static abstract class DataObserver {
        /**
         * Called whenever the ObjectAdapter's data has changed in some manner
         * outside of the set of changes covered by the other range-based change
         * notification methods.
         */
        public void onChanged() {
        }

        /**
         * Called when a range of items in the ObjectAdapter has changed. The
         * basic ordering and structure of the ObjectAdapter has not changed.
         *
         * @param positionStart The position of the first item that changed.
         * @param itemCount The number of items changed.
         */
        public void onItemRangeChanged(int positionStart, int itemCount) {
            onChanged();
        }

        /**
         * Called when a range of items is inserted into the ObjectAdapter.
         *
         * @param positionStart The position of the first inserted item.
         * @param itemCount The number of items inserted.
         */
        public void onItemRangeInserted(int positionStart, int itemCount) {
            onChanged();
        }

        /**
         * Called when a range of items is removed from the ObjectAdapter.
         *
         * @param positionStart The position of the first removed item.
         * @param itemCount The number of items removed.
         */
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            onChanged();
        }
    }

    private static final class DataObservable extends Observable<DataObserver> {

        public void notifyChanged() {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onChanged();
            }
        }

        public void notifyItemRangeChanged(int positionStart, int itemCount) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onItemRangeChanged(positionStart, itemCount);
            }
        }

        public void notifyItemRangeInserted(int positionStart, int itemCount) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onItemRangeInserted(positionStart, itemCount);
            }
        }

        public void notifyItemRangeRemoved(int positionStart, int itemCount) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onItemRangeRemoved(positionStart, itemCount);
            }
        }
    }

    private ArrayList<RowItem> mItems = new ArrayList<>();

    private int mRowSpacing = 0;

    private int mColumnSpacing = 0;

    private int mColumns = 0;

    private float mAspectRatio = 1.0f;

    private final DataObservable mObservable = new DataObservable();

    private PresenterSelector mPresenterSelector;

    public GridObjectAdapter() {
    }

    /**
     * Constructs an adapter with the given {@link PresenterSelector}.
     */
    public GridObjectAdapter(PresenterSelector presenterSelector) {
        setPresenterSelector(presenterSelector);
    }

    /**
     * Constructs an adapter that uses the given {@link Presenter} for all items.
     */
    public GridObjectAdapter(Presenter presenter) {
        setPresenterSelector(new SinglePresenterSelector(presenter));
    }

    public GridObjectAdapter(Presenter presenter,
                             int rowSpacing, int columnSpacing, int columns,
                             float aspectRatio) {
        mRowSpacing = rowSpacing;
        mColumnSpacing = columnSpacing;
        mColumns = columns;
        mAspectRatio = aspectRatio;
        setPresenterSelector(new SinglePresenterSelector(presenter));
    }

    public GridObjectAdapter(Presenter presenter, int columns,
                             float aspectRatio) {
        mColumns = columns;
        mAspectRatio = aspectRatio;
        setPresenterSelector(new SinglePresenterSelector(presenter));
    }

    public GridObjectAdapter(PresenterSelector presenterSelector, int columns,
                             float aspectRatio) {
        mColumns = columns;
        mAspectRatio = aspectRatio;
        setPresenterSelector(presenterSelector);
    }

    /**
     * Constructs an adapter.
     */
    public GridObjectAdapter(PresenterSelector presenterSelector,
                             int rowSpacing, int columnSpacing, int columns,
                             float aspectRatio) {
        mRowSpacing = rowSpacing;
        mColumnSpacing = columnSpacing;
        mColumns = columns;
        mAspectRatio = aspectRatio;
        setPresenterSelector(presenterSelector);
    }

    public void setGridStyle(int rowSpacing, int columnSpacing, int numRowOrColumns,
                             float aspectRatio) {
        mRowSpacing = rowSpacing;
        mColumnSpacing = columnSpacing;
        mColumns = numRowOrColumns;
        mAspectRatio = aspectRatio;
    }

    /**
     * Sets the presenter selector.  May not be null.
     */
    public final void setPresenterSelector(PresenterSelector presenterSelector) {
        if (presenterSelector == null) {
            throw new IllegalArgumentException("Presenter selector must not be null");
        }
        final boolean update = (mPresenterSelector != null);
        mPresenterSelector = presenterSelector;

        if (update) {
            notifyChanged();
        }
    }

    /**
     * Returns the presenter selector for this ObjectAdapter.
     */
    public final PresenterSelector getPresenterSelector() {
        return mPresenterSelector;
    }

    public int size() {
        return mItems.size();
    }

    public RowItem get(int position) {
        return mItems.get(position);
    }

    /**
     * Returns the index for the first occurrence of item in the adapter, or -1 if
     * not found.
     *
     * @param item  The item to find in the list.
     * @return Index of the first occurrence of the item in the adapter, or -1
     *         if not found.
     */
    public int indexOf(RowItem item) {
        return mItems.indexOf(item);
    }

    /**
     * Adds an item to the end of the adapter.
     *
     * @param item The item to add to the end of the adapter.
     */
    public void add(RowItem item) {
        add(mItems.size(), item);
    }

    /**
     * Inserts an item into this adapter at the specified index.
     * If the index is > {@link #size} an exception will be thrown.
     *
     * @param index The index at which the item should be inserted.
     * @param item The item to insert into the adapter.
     */
    public void add(int index, RowItem item) {
        mItems.add(index, item);
        notifyItemRangeInserted(index, 1);
    }

    /**
     * Replaces item at position with a new item and calls notifyItemRangeChanged()
     * at the given position.  Note that this method does not compare new item to
     * existing item.
     * @param position  The index of item to replace.
     * @param item      The new item to be placed at given position.
     */
    public void replace(int position, RowItem item) {
        mItems.set(position, item);
        notifyItemRangeChanged(position, 1);
    }

    /**
     * Removes all items from this adapter, leaving it empty.
     */
    public void clear() {
        int itemCount = mItems.size();
        if (itemCount == 0) {
            return;
        }
        mItems.clear();
        notifyItemRangeRemoved(0, itemCount);
    }

    public long getId(int position) {
        return position;
    }

    /**
     * Registers a DataObserver for data change notifications.
     */
    public final void registerObserver(DataObserver observer) {
        mObservable.registerObserver(observer);
    }

    /**
     * Unregisters a DataObserver for data change notifications.
     */
    public final void unregisterObserver(DataObserver observer) {
        mObservable.unregisterObserver(observer);
    }

    /**
     * Unregisters all DataObservers for this ObjectAdapter.
     */
    public final void unregisterAllObservers() {
        mObservable.unregisterAll();
    }

    final protected void notifyItemRangeChanged(int positionStart, int itemCount) {
        mObservable.notifyItemRangeChanged(positionStart, itemCount);
    }

    final protected void notifyItemRangeInserted(int positionStart, int itemCount) {
        mObservable.notifyItemRangeInserted(positionStart, itemCount);
    }

    final protected void notifyItemRangeRemoved(int positionStart, int itemCount) {
        mObservable.notifyItemRangeRemoved(positionStart, itemCount);
    }

    final protected void notifyChanged() {
        mObservable.notifyChanged();
    }

    public int getItemLeftIndex(int position) {
        if (mItems == null || position < 0) {
            return 0;
        }
        if (position < mItems.size()) {
            return mItems.get(position).getX();
        } else {
            return 0;
        }
    }

    public int getItemTopIndex(int position) {
        if (mItems == null || position < 0) {
            return 0;
        }
        if (position < mItems.size()) {
            return mItems.get(position).getY();
        } else {
            return 0;
        }
    }

    public int getItemRowSize(int position) {
        if (mItems == null || position < 0) {
            return 0;
        }
        if (position < mItems.size()) {
            return mItems.get(position).getHeight();
        } else {
            return 1;
        }
    }

    public int getItemColumnSize(int position) {
        if (mItems == null || position < 0) {
            return 0;
        }
        if (position < mItems.size()) {
            return mItems.get(position).getWidth();
        } else {
            return 1;
        }
    }

    public int getColumnSpacing() {
        return mColumnSpacing;
    }

    public int getRowSpacing() {
        return mRowSpacing;
    }

    public int getColumns() {
        return mColumns;
    }

    public float getAspectRatio() {
        return mAspectRatio;
    }
}
