package app.com.tvrecyclerview;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;


abstract class BaseGridView extends RecyclerView {

    public static final String TAG = "BaseGridView";

    /**
     * Always keep focused item at a aligned position.  Developer can use
     * WINDOW_ALIGN_XXX and ITEM_ALIGN_XXX to define how focused item is aligned.
     * In this mode, the last focused position will be remembered and restored when focus
     * is back to the view.
     */
    public final static int FOCUS_SCROLL_ALIGNED = 0;

    /**
     * Scroll to make the focused item inside client area.
     */
    public final static int FOCUS_SCROLL_ITEM = 1;

    /**
     * Scroll a page of items when focusing to item outside the client area.
     * The page size matches the client area size of RecyclerView.
     */
    public final static int FOCUS_SCROLL_PAGE = 2;

    private int mFocusZoomFactor = FocusHighlightHelper.ZOOM_FACTOR_NONE;

    /**
     * Listener for intercepting touch dispatch events.
     */
    public interface OnTouchInterceptListener {
        /**
         * Returns true if the touch dispatch event should be consumed.
         */
        boolean onInterceptTouchEvent(MotionEvent event);
    }

    /**
     * Listener for intercepting generic motion dispatch events.
     */
    public interface OnMotionInterceptListener {
        /**
         * Returns true if the touch dispatch event should be consumed.
         */
        boolean onInterceptMotionEvent(MotionEvent event);
    }

    /**
     * Listener for intercepting key dispatch events.
     */
    public interface OnKeyInterceptListener {
        /**
         * Returns true if the key dispatch event should be consumed.
         */
        boolean onInterceptKeyEvent(KeyEvent event);
    }

    public interface OnUnhandledKeyListener {
        /**
         * Returns true if the key event should be consumed.
         */
        boolean onUnhandledKey(KeyEvent event);
    }

    final GridLayoutManager mLayoutManager;

    /**
     * Animate layout changes from a child resizing or adding/removing a child.
     */
    private boolean mAnimateChildLayout = true;

    private boolean mHasOverlappingRendering = true;

    private RecyclerView.ItemAnimator mSavedItemAnimator;

    private OnTouchInterceptListener mOnTouchInterceptListener;
    private OnMotionInterceptListener mOnMotionInterceptListener;
    private OnKeyInterceptListener mOnKeyInterceptListener;
    private RecyclerView.RecyclerListener mChainedRecyclerListener;
    private OnUnhandledKeyListener mOnUnhandledKeyListener;

    public BaseGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mLayoutManager = getLayoutManager();
        setLayoutManager(mLayoutManager);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setHasFixedSize(true);
        setChildrenDrawingOrderEnabled(true);
        setWillNotDraw(true);
        setOverScrollMode(View.OVER_SCROLL_NEVER);

        super.setRecyclerListener(new RecyclerView.RecyclerListener() {
            @Override
            public void onViewRecycled(RecyclerView.ViewHolder holder) {
                if (mChainedRecyclerListener != null) {
                    mChainedRecyclerListener.onViewRecycled(holder);
                }
            }
        });
    }

    public GridLayoutManager getLayoutManager() {
        return new GridLayoutManager(this);
    }

    protected void initBaseGridViewAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BaseGridView);
        if (a.hasValue(R.styleable.BaseGridView_android_gravity)) {
            setGravity(a.getInt(R.styleable.BaseGridView_android_gravity, Gravity.NO_GRAVITY));
        }
        a.recycle();
    }

    public void setAdapter(GridObjectAdapter adapter) {
        setAdapter(adapter, null);
    }

    public void setAdapter(GridObjectAdapter adapter, PresenterSelector presenterSelector) {
        if (adapter != null) {
            mLayoutManager.setAdapter(adapter);
            ItemBridgeAdapter bridgeAdapter = new ItemBridgeAdapter(adapter, presenterSelector);
            FocusHighlightHelper.setupItemBridgeFocusHighlight(bridgeAdapter, mFocusZoomFactor);
            setAdapter(bridgeAdapter);
        }
    }

    /**
     * set zoom factor
     * @param factor factor
     * <ul>
     * <li>{@link FocusHighlightHelper #ZOOM_FACTOR_NONE} (default) </li>
     * <li>{@link FocusHighlightHelper #ZOOM_FACTOR_SMALL}</li>
     * <li>{@link FocusHighlightHelper #ZOOM_FACTOR_MEDIUM}</li>
     * <li>{@link FocusHighlightHelper #ZOOM_FACTOR_LARGE}</li>
     * <li>{@link FocusHighlightHelper #ZOOM_FACTOR_XSMALL}</li>
     * </ul>
     */
    public void setFocusZoomFactor(int factor) {
        mFocusZoomFactor = factor;
        Adapter adapter = getAdapter();
        if (adapter != null && adapter instanceof ItemBridgeAdapter) {
            ItemBridgeAdapter bridgeAdapter = (ItemBridgeAdapter) adapter;
            FocusHighlightHelper.setupItemBridgeFocusHighlight(bridgeAdapter, mFocusZoomFactor);
        }
    }
    /**
     * Sets the strategy used to scroll in response to item focus changing:
     * <ul>
     * <li>{@link #FOCUS_SCROLL_ALIGNED} (default) </li>
     * <li>{@link #FOCUS_SCROLL_ITEM}</li>
     * <li>{@link #FOCUS_SCROLL_PAGE}</li>
     * </ul>
     */
    public void setFocusScrollStrategy(int scrollStrategy) {
        if (scrollStrategy != FOCUS_SCROLL_ALIGNED && scrollStrategy != FOCUS_SCROLL_ITEM
                && scrollStrategy != FOCUS_SCROLL_PAGE) {
            throw new IllegalArgumentException("Invalid scrollStrategy");
        }
        mLayoutManager.setFocusScrollStrategy(scrollStrategy);
        requestLayout();
    }

    /**
     * Returns the strategy used to scroll in response to item focus changing.
     * <ul>
     * <li>{@link #FOCUS_SCROLL_ALIGNED} (default) </li>
     * <li>{@link #FOCUS_SCROLL_ITEM}</li>
     * <li>{@link #FOCUS_SCROLL_PAGE}</li>
     * </ul>
     */
    public int getFocusScrollStrategy() {
        return mLayoutManager.getFocusScrollStrategy();
    }

//    /**
//     * Sets the offset in pixels for window alignment.
//     *
//     * @param offset The number of pixels to offset.  If the offset is positive,
//     *        it is distance from low edge (see {@link #WINDOW_ALIGN_LOW_EDGE});
//     *        if the offset is negative, the absolute value is distance from high
//     *        edge (see {@link #WINDOW_ALIGN_HIGH_EDGE}).
//     *        Default value is 0.
//     */
//    public void setWindowAlignmentOffset(int offset) {
//        mLayoutManager.setWindowAlignmentOffset(offset);
//        requestLayout();
//    }
//
//    /**
//     * Returns the offset in pixels for window alignment.
//     *
//     * @return The number of pixels to offset.  If the offset is positive,
//     *        it is distance from low edge (see {@link #WINDOW_ALIGN_LOW_EDGE});
//     *        if the offset is negative, the absolute value is distance from high
//     *        edge (see {@link #WINDOW_ALIGN_HIGH_EDGE}).
//     *        Default value is 0.
//     */
//    public int getWindowAlignmentOffset() {
//        return mLayoutManager.getWindowAlignmentOffset();
//    }
//
//    /**
//     * Sets the id of the view to align with. Use {@link android.view.View#NO_ID} (default)
//     * for the item view itself.
//     * Item alignment settings are ignored for the child if {@link ItemAlignmentFacet}
//     * is provided by {@link RecyclerView.ViewHolder} or {@link FacetProviderAdapter}.
//     */
//    public void setItemAlignmentViewId(int viewId) {
//        mLayoutManager.setItemAlignmentViewId(viewId);
//    }
//
//    /**
//     * Returns the id of the view to align with, or zero for the item view itself.
//     */
//    public int getItemAlignmentViewId() {
//        return mLayoutManager.getItemAlignmentViewId();
//    }

    /**
     * Registers a callback to be invoked when an item in BaseGridView has
     * been selected.  Note that the listener may be invoked when there is a
     * layout pending on the view, affording the listener an opportunity to
     * adjust the upcoming layout based on the selection state.
     *
     * @param listener The listener to be invoked.
     */
    public void setOnChildSelectedListener(OnChildSelectedListener listener) {
        mLayoutManager.setOnChildSelectedListener(listener);
    }

    /**
     * Registers a callback to be invoked when an item in BaseGridView has
     * been selected.  Note that the listener may be invoked when there is a
     * layout pending on the view, affording the listener an opportunity to
     * adjust the upcoming layout based on the selection state.
     *
     * @param listener The listener to be invoked.
     */
    public void setOnChildViewHolderSelectedListener(OnChildViewHolderSelectedListener listener) {
        mLayoutManager.setOnChildViewHolderSelectedListener(listener);
    }

    /**
     * Changes the selected item immediately without animation.
     */
    public void setSelectedPosition(int position) {
        mLayoutManager.setSelection(this, position, 0);
    }

    /**
     * Changes the selected item and/or subposition immediately without animation.
     */
    public void setSelectedPositionWithSub(int position, int subposition) {
        mLayoutManager.setSelectionWithSub(this, position, subposition, 0);
    }

    /**
     * Changes the selected item immediately without animation, scrollExtra is
     * applied in primary scroll direction.  The scrollExtra will be kept until
     * another {@link #setSelectedPosition} or {@link #setSelectedPositionSmooth} call.
     */
    public void setSelectedPosition(int position, int scrollExtra) {
        mLayoutManager.setSelection(this, position, scrollExtra);
    }

    /**
     * Changes the selected item and/or subposition immediately without animation, scrollExtra is
     * applied in primary scroll direction.  The scrollExtra will be kept until
     * another {@link #setSelectedPosition} or {@link #setSelectedPositionSmooth} call.
     */
    public void setSelectedPositionWithSub(int position, int subposition, int scrollExtra) {
        mLayoutManager.setSelectionWithSub(this, position, subposition, scrollExtra);
    }

    /**
     * Changes the selected item and run an animation to scroll to the target
     * position.
     */
    public void setSelectedPositionSmooth(int position) {
        mLayoutManager.setSelectionSmooth(this, position);
    }

    /**
     * Changes the selected item and/or subposition, runs an animation to scroll to the target
     * position.
     */
    public void setSelectedPositionSmoothWithSub(int position, int subposition) {
        mLayoutManager.setSelectionSmoothWithSub(this, position, subposition);
    }

    /**
     * Returns the selected item position.
     */
    public int getSelectedPosition() {
        return mLayoutManager.getSelection();
    }

    /**
     * Returns the sub selected item position started from zero.
     */
    public int getSelectedSubPosition() {
        return mLayoutManager.getSubSelection();
    }

    /**
     * Sets whether an animation should run when a child changes size or when adding
     * or removing a child.
     * <p><i>Unstable API, might change later.</i>
     */
    public void setAnimateChildLayout(boolean animateChildLayout) {
        if (mAnimateChildLayout != animateChildLayout) {
            mAnimateChildLayout = animateChildLayout;
            if (!mAnimateChildLayout) {
                mSavedItemAnimator = getItemAnimator();
                super.setItemAnimator(null);
            } else {
                super.setItemAnimator(mSavedItemAnimator);
            }
        }
    }

    /**
     * Returns true if an animation will run when a child changes size or when
     * adding or removing a child.
     * <p><i>Unstable API, might change later.</i>
     */
    public boolean isChildLayoutAnimated() {
        return mAnimateChildLayout;
    }

    /**
     * Sets the gravity used for child view positioning. Defaults to
     * GRAVITY_TOP|GRAVITY_START.
     *
     * @param gravity See {@link android.view.Gravity}
     */
    public void setGravity(int gravity) {
        mLayoutManager.setGravity(gravity);
        requestLayout();
    }

    @Override
    public boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        return mLayoutManager.gridOnRequestFocusInDescendants(this, direction,
                previouslyFocusedRect);
    }

    @Override
    public int getChildDrawingOrder(int childCount, int i) {
        return mLayoutManager.getChildDrawingOrder(this, childCount, i);
    }

    final boolean isChildrenDrawingOrderEnabledInternal() {
        return isChildrenDrawingOrderEnabled();
    }

    /**
     * Disables or enables focus search.
     */
    public final void setFocusSearchDisabled(boolean disabled) {
        // LayoutManager may detachView and attachView in fastRelayout, it causes RowsFragment
        // re-gain focus after a BACK key pressed, so block children focus during transition.
        setDescendantFocusability(disabled ? FOCUS_BLOCK_DESCENDANTS: FOCUS_AFTER_DESCENDANTS);
        mLayoutManager.setFocusSearchDisabled(disabled);
    }

    /**
     * Returns true if focus search is disabled.
     */
    public final boolean isFocusSearchDisabled() {
        return mLayoutManager.isFocusSearchDisabled();
    }

    /**
     * Changes and overrides children's visibility.
     */
    public void setChildrenVisibility(int visibility) {
        mLayoutManager.setChildrenVisibility(visibility);
    }

    /**
     * Enables or disables scrolling.  Disable is useful during transition.
     */
    public void setScrollEnabled(boolean scrollEnabled) {
        mLayoutManager.setScrollEnabled(scrollEnabled);
    }

    /**
     * Returns true if scrolling is enabled.
     */
    public boolean isScrollEnabled() {
        return mLayoutManager.isScrollEnabled();
    }

    /**
     * Enables or disables the default "focus draw at last" order rule.
     */
    public void setFocusDrawingOrderEnabled(boolean enabled) {
        super.setChildrenDrawingOrderEnabled(enabled);
    }

    /**
     * Returns true if default "focus draw at last" order rule is enabled.
     */
    public boolean isFocusDrawingOrderEnabled() {
        return super.isChildrenDrawingOrderEnabled();
    }

    /**
     * Sets the touch intercept listener.
     */
    public void setOnTouchInterceptListener(OnTouchInterceptListener listener) {
        mOnTouchInterceptListener = listener;
    }

    /**
     * Sets the generic motion intercept listener.
     */
    public void setOnMotionInterceptListener(OnMotionInterceptListener listener) {
        mOnMotionInterceptListener = listener;
    }

    /**
     * Sets the key intercept listener.
     */
    public void setOnKeyInterceptListener(OnKeyInterceptListener listener) {
        mOnKeyInterceptListener = listener;
    }

    /**
     * Sets the unhandled key listener.
     */
    public void setOnUnhandledKeyListener(OnUnhandledKeyListener listener) {
        mOnUnhandledKeyListener = listener;
    }

    /**
     * Returns the unhandled key listener.
     */
    public OnUnhandledKeyListener getOnUnhandledKeyListener() {
        return mOnUnhandledKeyListener;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mOnKeyInterceptListener != null && mOnKeyInterceptListener.onInterceptKeyEvent(event)) {
            return true;
        }
        if (super.dispatchKeyEvent(event)) {
            return true;
        }
        if (mOnUnhandledKeyListener != null && mOnUnhandledKeyListener.onUnhandledKey(event)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mOnTouchInterceptListener != null) {
            if (mOnTouchInterceptListener.onInterceptTouchEvent(event)) {
                return true;
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchGenericFocusedEvent(MotionEvent event) {
        if (mOnMotionInterceptListener != null) {
            if (mOnMotionInterceptListener.onInterceptMotionEvent(event)) {
                return true;
            }
        }
        return super.dispatchGenericFocusedEvent(event);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return mHasOverlappingRendering;
    }

    public void setHasOverlappingRendering(boolean hasOverlapping) {
        mHasOverlappingRendering = hasOverlapping;
    }

    @Override
    public void setRecyclerListener(RecyclerView.RecyclerListener listener) {
        mChainedRecyclerListener = listener;
    }


    public int getSelection() {
        return mLayoutManager.getSelection();
    }

    public void setOnFocusSearchFailedListener(OnFocusSearchFailedListener listener) {
        mLayoutManager.setOnFocusSearchFailedListener(listener);
    }
}
