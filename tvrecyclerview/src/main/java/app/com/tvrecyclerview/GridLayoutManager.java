package app.com.tvrecyclerview;

import android.graphics.PointF;
import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Locale;


final class GridLayoutManager extends RecyclerView.LayoutManager {

    private static final String TAG = "GridLayoutManager";

    private static final boolean DEBUG = true;

    private static final int BASE_ITEM_DEFAULT_SIZE = 220;

    private static final int HORIZONTAL = OrientationHelper.HORIZONTAL;

    private static final int VERTICAL = OrientationHelper.VERTICAL;

    private final static int LAYOUT_START = -1;

    private final static int LAYOUT_END = 1;
    private static final int NO_POSITION = -1;
    private static final int NO_ID = -1;
    private static final int DEFAULT_DIRECTION = -1;

    private GridObjectAdapter mAdapter;

    private BaseGridView mBaseRecyclerView;

    /**
     * The orientation of a "row".
     */
    private int mOrientation;

    /**
     * save items layout param
     */
    private final SparseArray<Rect> mItemsRect;

    /**
     * offset in horizontal direction
     */
    private int mHorizontalOffset;

    /**
     * offset in vertical direction
     */
    private int mVerticalOffset;

    /**
     * The number of rows(or columns in Horizontal)
     */
    private int mNumRowOrColumn = 1;

    /**
     * Width of base item
     */
    private float mOriItemWidth;

    /**
     * Height of base item
     */
    private float mOriItemHeight;

    /**
     * size all items. In horizontal, it represents the total width.
     * otherwise, it represents the total height
     */
    private int mTotalSize;

    /**
     * Focus Scroll strategy.
     */
    private int mFocusScrollStrategy = BaseGridView.FOCUS_SCROLL_ALIGNED;

    /**
     * In Scroll item mode, the display area needs to be expanded.
     * When all child is displayed in the display area,
     * it can not slide down, but there are still data at this time.
     */
    private int mExpandSpace = 0;

    /**
     * re-used variable to acquire decor insets from RecyclerView
     */
    private final Rect mDecorInsets = new Rect();

    /**
     * Temporary variable: an int array of length=2.
     */
    private static int[] mTwoInts = new int[2];

    /**
     * True if focus search is disabled.
     */
    private boolean mFocusSearchDisabled = false;

    /**
     * override child visibility
     */
    private int mChildVisibility;

    private boolean mInLayout = true;

    private boolean mInScroll = false;

    private boolean mInSelection = false;

    private boolean mIsSlidingChildViews = false;

    /**
     * The focused position, it's not the currently visually aligned position
     * but it is the final position that we intend to focus on.
     */
    private int mFocusPosition = NO_POSITION;

    /**
     * A view can have mutliple alignment position,  this is the index of which
     * alignment is used,  by default is 0.
     */
    private int mSubFocusPosition = 0;

    /**
     * Extra pixels applied on primary direction.
     */
    private int mPrimaryScrollExtra;

    /**
     * A view extra height after measured
     */
    private int mExtraChildHeight = 0;

    /**
     *  Allow DPAD key to navigate out at the front of the View (where position = 0),
     *  default is false.
     */
    private boolean mFocusOutFront;

    /**
     * Allow DPAD key to navigate out at the end of the view, default is false.
     */
    private boolean mFocusOutEnd;

    /**
     * How to position child in secondary direction.
     */
    private int mGravity = Gravity.START | Gravity.TOP;

    private boolean mScrollEnabled = true;

    private int mDirection = DEFAULT_DIRECTION;

    private OnChildSelectedListener mChildSelectedListener = null;

    private ArrayList<OnChildViewHolderSelectedListener> mChildViewHolderSelectedListeners = null;

    private OnFocusSearchFailedListener mFocusSearchFailedListener;

    GridLayoutManager(BaseGridView recyclerView) {
        this(recyclerView, HORIZONTAL);
    }

    GridLayoutManager(BaseGridView recyclerView, int orientation) {
        mBaseRecyclerView = recyclerView;
        mOrientation = orientation;
        mItemsRect = new SparseArray<>();
        mChildVisibility = -1;
        mAdapter = new GridObjectAdapter();
    }

    /**
     * reset default item row or column.
     * Avoid the width and height of all items in each row or column
     * more than the width and height of the parent view.
     */
    private void initItemRowColumnSize(int width) {
        int maxRowOrColumn = mAdapter.getColumns();
        if (maxRowOrColumn > 0) {
            mOriItemWidth = (width - (maxRowOrColumn - 1) * mAdapter.getColumnSpacing()
                    - getPaddingLeft() - getPaddingRight()) / maxRowOrColumn;
            mOriItemHeight = mOriItemWidth / mAdapter.getAspectRatio();
            if (DEBUG) {
                Log.i(TAG, "initItemRowColumnSize: mOriItemWidth= " + mOriItemWidth +
                        "=mOriItemHeight=" + mOriItemHeight);
            }
        }
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private boolean hasDoneFirstLayout() {
        return mItemsRect.size() > 0;
    }

    @Override
    public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state, int widthSpec,
                          int heightSpec) {
        int width = chooseMeasureSize(widthSpec, getPaddingLeft() + getPaddingRight(),
                ViewCompat.getMinimumWidth(mBaseRecyclerView), true);
        initItemRowColumnSize(width);
        int height = chooseMeasureSize(heightSpec, getPaddingTop() + getPaddingBottom(),
                ViewCompat.getMinimumHeight(mBaseRecyclerView), false);
        if (DEBUG) {
            Log.i(TAG, "onMeasure: width=" + width + "=height=" + height);
        }
        setMeasuredDimension(width, height);
    }

    private int chooseMeasureSize(int spec, int desired, int min, boolean isWidthSpec) {
        final int mode = View.MeasureSpec.getMode(spec);
        final int size = View.MeasureSpec.getSize(spec);
        int space = Math.max(desired, min);
        switch (mode) {
            case View.MeasureSpec.EXACTLY:
                return size;
            case View.MeasureSpec.AT_MOST:
                if ((isWidthSpec && mOrientation == HORIZONTAL) ||
                        (!isWidthSpec && mOrientation == VERTICAL)){
                    return size;
                } else if (!isWidthSpec && mOrientation == HORIZONTAL) {
                    return calculateMeasureHeight(getChildAt(0), space);
                } else if (isWidthSpec && mOrientation == VERTICAL) {
                    return calculateMeasureWidth(getChildAt(0), size, space);
                }
                return Math.min(size, space);
            case View.MeasureSpec.UNSPECIFIED:
            default:
                if (!isWidthSpec) {
                    return calculateMeasureHeight(getChildAt(0), space);
                } else {
                    return space;
                }
        }
    }

    private int calculateMeasureWidth(View view, int size, int space) {
        int width = size;
        if (mAdapter.getColumns() <= 0) {
            if (view != null) {
                width = mNumRowOrColumn * view.getMeasuredWidth() + space
                        + (mDecorInsets.left + mDecorInsets.right) * (mNumRowOrColumn - 1);
            }
        }
        if (DEBUG) {
            Log.i(TAG, "calculateMeasureWidth: width==" + width);
        }
        return width;
    }

    private int calculateMeasureHeight(View view, int space) {
        int height = space;
        if (mAdapter.getColumns() > 0) {
            int rowNum = mAdapter.getItemTopIndex(getItemCount() - 1) +
                    mAdapter.getItemRowSize(getItemCount() - 1);
            height = (int) (rowNum * mOriItemHeight +
                    (mAdapter.getRowSpacing() * (rowNum - 1)) +
                    space + rowNum * mExtraChildHeight);
        } else {
            if (view != null) {
                height = mNumRowOrColumn * view.getMeasuredHeight() + space
                        + (mDecorInsets.top + mDecorInsets.bottom) * (mNumRowOrColumn - 1);
            }
        }
        if (DEBUG) {
            Log.i(TAG, "calculateMeasureHeight: height==" + height);
        }
        return height;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        layoutChildren(recycler, state);
    }

    private void layoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (DEBUG) {
            Log.d(TAG, "layoutChildren: state.isPreLayout=" + state.isPreLayout());
        }

        final int newItemCount = state.getItemCount();

        if (newItemCount == 0) {
            mFocusPosition = NO_POSITION;
            removeAndRecycleAllViews(recycler);
            return;
        } else if (mFocusPosition >= newItemCount) {
            mFocusPosition = newItemCount - 1;
        } else if (mFocusPosition == NO_POSITION && newItemCount > 0) {
            // if focus position is never set before,  initialize it to 0
            mFocusPosition = 0;
        }

        if (getChildCount() <= 0 && state.isPreLayout()) {
            return;
        }

        mInLayout = true;
        mExtraChildHeight = getChildExtraHeight();
        if (DEBUG) {
            Log.d(TAG, "layoutChildren: extra child height=" + mExtraChildHeight);
        }
        detachAndScrapAttachedViews(recycler);
        mItemsRect.clear();
        fill(recycler, state);
        // appends items till focus position.
        if (mFocusPosition != NO_POSITION) {
            View focusView = findViewByPosition(mFocusPosition);
            if (focusView != null) {
                if (mBaseRecyclerView.hasFocus()) {
                    if (!focusView.hasFocus()) {
                        focusView.requestFocus();
                    }
                    dispatchChildSelected();
                }
            }
        }
        mInLayout = false;
    }

    /**
     * child view may be adjust own height when measure.
     * exc: Child add information area below.
     * @return extra height
     */
    private int getChildExtraHeight() {
        if (mAdapter.getColumns() > 0) {
            View view = getChildAt(0);
            if (view != null) {
                int itemHeight = getItemHeight(0);
                measureChild(view, getItemWidth(0), itemHeight);
                int viewMeasuredHeight = view.getMeasuredHeight();
                if (viewMeasuredHeight > itemHeight) {
                    return viewMeasuredHeight - itemHeight;
                }
            }
        }
        return 0;
    }

    /**
     * In layout, if there is a view that needs to get focus,
     * make sure the view is displayed.
     */
    private void scrollToFocusViewInLayout() {
        if (hasFocus() && !isSmoothScrolling() && mFocusPosition != NO_POSITION) {
            View focusView = findViewByPosition(mFocusPosition);
            if (focusView != null) {
                if (getScrollPosition(focusView, mTwoInts)) {
                    int maxScrollDistance = getMaxScrollDistance();
                    int scrollDistance = mTwoInts[0];
                    if (scrollDistance > 0) {
                        if (mHorizontalOffset + scrollDistance > maxScrollDistance) {
                            scrollDistance = maxScrollDistance - mHorizontalOffset;
                        }
                        offsetChildrenPrimary(-scrollDistance);
                    }
                    if (!focusView.hasFocus()) {
                        focusView.requestFocus();
                        dispatchChildSelected();
                    }
                }
            } else if (mFocusPosition != NO_POSITION){
                scrollToSelection(mBaseRecyclerView, mFocusPosition, mSubFocusPosition, true,
                        mPrimaryScrollExtra);
            }
        }
    }

    /**
     * Fills the given layout
     *
     * @param recycler   Current recycler that is attached to HorizontalModuleGridView
     * @param state      Context passed by the RecyclerView to control scroll steps.
     */
    private void fill(RecyclerView.Recycler recycler, RecyclerView.State state) {
        int itemsCount = state.getItemCount();
        Rect displayRect = getDisplayRect();

        // fill towards start, stacking from bottom
        for (int i = mFocusPosition; i >= 0; i--) {
            if (!layoutChild(recycler, displayRect, i)) {
                break;
            }
        }
        // fill towards end, stacking from top
        for (int i = mFocusPosition + 1; i < itemsCount; i++) {
            if (!layoutChild(recycler, displayRect, i)) {
                break;
            }
        }

        if (DEBUG) {
            Log.d(TAG, String.format(Locale.getDefault(), "fill=displayRect=%s " +
                            "=itemTotalSize=%d, HorizontalOffset=%d, " +
                            "VerticalOffset=%d", displayRect.toString(), mTotalSize,
                    mHorizontalOffset, mVerticalOffset));
        }
    }

    private boolean layoutChild(RecyclerView.Recycler recycler, Rect displayRect, int position) {
        if (position < 0) {
            return false;
        }
        View child = recycler.getViewForPosition(position);
        Rect itemRect = calculateViewSizeByPosition(child, position);
        if (!Rect.intersects(displayRect, itemRect)) {
            recycler.recycleView(child);
            return false;
        }
        addView(child);
        if (mOrientation == HORIZONTAL) {
            layoutDecoratedWithMargins(child,
                    itemRect.left - mHorizontalOffset,
                    itemRect.top,
                    itemRect.right - mHorizontalOffset,
                    itemRect.bottom);
        } else {
            layoutDecoratedWithMargins(child,
                    itemRect.left,
                    itemRect.top - mVerticalOffset,
                    itemRect.right,
                    itemRect.bottom - mVerticalOffset);
        }

        if (mOrientation == HORIZONTAL) {
            if (itemRect.right > mTotalSize) {
                mTotalSize = itemRect.right;
            }
        } else {
            if (itemRect.bottom > mTotalSize) {
                mTotalSize = itemRect.bottom;
            }
        }

        if (DEBUG) {
            Log.d(TAG, "layoutChild: position=" + position
                    + "=childRect=" + itemRect.toString());
        }
        // Save the current Bound field data for the item view
        Rect frame = mItemsRect.get(position);
        if (frame == null) {
            frame = new Rect();
        }
        frame.set(itemRect);
        mItemsRect.put(position, frame);
        return true;
    }

    @Override
    public void layoutDecoratedWithMargins(View child, int left, int top, int right, int bottom) {
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams)child.getLayoutParams();
        Rect insets = new Rect();
        calculateItemDecorationsForChild(child, mDecorInsets);
        child.layout(left + insets.left + lp.leftMargin, top + insets.top + lp.topMargin,
                right - insets.right - lp.rightMargin, bottom - insets.bottom - lp.bottomMargin);
    }

    /**
     * Calculates the item layout size
     * @param child The child view should be calculated
     * @param position child position
     * @return The Rect to hold result values
     */
    private Rect calculateViewSizeByPosition(View child, int position) {
        if (position >= getItemCount()) {
            throw new IllegalArgumentException("position outside of itemCount position is "
                    + position + " itemCount is " + getItemCount());
        }

        int leftOffset;
        int topOffset;
        Rect childFrame = new Rect();
        calculateItemDecorationsForChild(child, mDecorInsets);
        if (mAdapter.getColumns() > 0) {
            measureChild(child, getItemWidth(position), getItemHeight(position));

            int lastPos = mAdapter.getItemLeftIndex(position);
            int topPos = mAdapter.getItemTopIndex(position);

            if (lastPos == 0) {
                leftOffset = -mDecorInsets.left + getPaddingLeft();
            } else {
                leftOffset = (int) ((mOriItemWidth + getChildHorizontalPadding(child)) * lastPos
                        - mDecorInsets.left + getPaddingLeft());
            }

            if (topPos == 0) {
                topOffset = -mDecorInsets.top + getPaddingTop();
            } else {
                topOffset = (int) ((mOriItemHeight + getChildVerticalPadding(child)) * topPos
                        - mDecorInsets.top + getPaddingTop()
                        + mExtraChildHeight * mAdapter.getItemTopIndex(position));
            }

            int childHorizontalSpace = getDecoratedMeasurementHorizontal(child);
            int childVerticalSpace = getDecoratedMeasurementVertical(child);

            childFrame.left = leftOffset;
            childFrame.top = topOffset;
            childFrame.right = leftOffset + childHorizontalSpace;
            childFrame.bottom = topOffset + childVerticalSpace;
        } else {
            measureChild(child);
            int leftIndex;
            int topIndex;

            if (mOrientation == HORIZONTAL) {
                leftIndex = position / mNumRowOrColumn;
                topIndex = position % mNumRowOrColumn;
            } else {
                leftIndex = position % mNumRowOrColumn;
                topIndex = position / mNumRowOrColumn;
            }
            leftOffset = leftIndex * (child.getMeasuredWidth() + mDecorInsets.left + mDecorInsets.right)
                    + getPaddingLeft();
            topOffset = topIndex * (child.getMeasuredHeight() + mDecorInsets.top + mDecorInsets.bottom)
                    + getPaddingTop();

            final int verticalGravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
            final int horizontalGravity = mGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
            if (mOrientation == HORIZONTAL && verticalGravity == Gravity.TOP
                    || mOrientation == VERTICAL && horizontalGravity == Gravity.LEFT) {
                // do nothing
            } else if (mOrientation == HORIZONTAL && verticalGravity == Gravity.BOTTOM) {
                topOffset +=  getHeight() - calculateMeasureHeight(child, 0);
            } else if (mOrientation == VERTICAL && horizontalGravity == Gravity.RIGHT) {
                leftOffset += getWidth() - calculateMeasureWidth(child, 0, 0);
            } else if (mOrientation == HORIZONTAL && verticalGravity == Gravity.CENTER_VERTICAL) {
                topOffset += (getHeight() - calculateMeasureHeight(child, 0)) / 2;
            } else if (mOrientation == VERTICAL && horizontalGravity == Gravity.CENTER_HORIZONTAL) {
                leftOffset += (getWidth() - calculateMeasureWidth(child, 0, 0)) / 2;
            }

            childFrame.left = leftOffset;
            childFrame.top = topOffset;
            childFrame.right = leftOffset + getMeasurementHorizontal(child);
            childFrame.bottom = topOffset + getMeasurementVertical(child);
        }

        return childFrame;
    }

    private Rect getDisplayRect() {
        Rect displayFrame;
        if (mOrientation == HORIZONTAL) {
            displayFrame = new Rect(mHorizontalOffset - getPaddingLeft(),
                    getPaddingTop(),
                    mHorizontalOffset + getPaddingLeft() + getHorizontalSpace() + mExpandSpace,
                    getVerticalSpace() + getPaddingTop());
        } else {
            displayFrame = new Rect(getPaddingLeft(),
                    mVerticalOffset - getPaddingTop(),
                    getPaddingLeft() + getHorizontalSpace(),
                    mVerticalOffset + getPaddingTop() + getVerticalSpace() + mExpandSpace);
        }
        return displayFrame;
    }

    /**
     *  call at scroll mode, Add the moved view to recycle the removed view
     * @param recycler   Current recycler that is attached to HorizontalModuleGridView
     * @param state      Context passed by the RecyclerView to control scroll steps.
     */
    private void recycleAndFillViews(RecyclerView.Recycler recycler, RecyclerView.State state, int dt) {
        if (state.isPreLayout()) {
            return;
        }
        recycleByScrollState(recycler, dt);

        if (dt >= 0) {
            fillEnd(recycler);
        } else {
            fillStart(recycler);
        }
    }

    private void fillEnd(RecyclerView.Recycler recycler) {
        int beginPos = findLastViewLayoutPosition() + 1;
        if (DEBUG) {
            Log.d(TAG, "fillEnd: beginPos=" + beginPos);
        }
        int itemCount = getItemCount();
        Rect displayRect = getDisplayRect();
        int rectCount;
        // Re-display the subview that needs to appear on the screen
        for (int i = beginPos; i < itemCount; i++) {
            rectCount = mItemsRect.size();
            Rect frame = mItemsRect.get(i);
            if (frame != null && i < rectCount) {
                if (Rect.intersects(displayRect, frame)) {
                    View scrap = recycler.getViewForPosition(i);
                    if (mAdapter.getColumns() > 0) {
                        measureChild(scrap, getItemWidth(i), getItemHeight(i));
                    } else {
                        measureChild(scrap);
                    }
                    addView(scrap);
                    if (mOrientation == HORIZONTAL) {
                        layoutDecoratedWithMargins(scrap,
                                frame.left - mHorizontalOffset,
                                frame.top,
                                frame.right - mHorizontalOffset,
                                frame.bottom);
                    } else {
                        layoutDecoratedWithMargins(scrap,
                                frame.left,
                                frame.top - mVerticalOffset,
                                frame.right,
                                frame.bottom - mVerticalOffset);
                    }
                    if (DEBUG) {
                        Log.d(TAG, "fillEnd: scroll down/right recycle=i=" + i
                                + "=rectCount=" + rectCount);
                    }
                }
            } else if (rectCount < itemCount) {
                View child = recycler.getViewForPosition(i);
                Rect itemRect = calculateViewSizeByPosition(child, i);
                if (!Rect.intersects(displayRect, itemRect)) {
                    recycler.recycleView(child);
                    return;
                }
                addView(child);
                if (mOrientation == HORIZONTAL) {
                    layoutDecoratedWithMargins(child,
                            itemRect.left - mHorizontalOffset,
                            itemRect.top,
                            itemRect.right - mHorizontalOffset,
                            itemRect.bottom);
                    mTotalSize = itemRect.right;
                } else {
                    layoutDecoratedWithMargins(child,
                            itemRect.left,
                            itemRect.top - mVerticalOffset,
                            itemRect.right,
                            itemRect.bottom - mVerticalOffset);
                    mTotalSize = itemRect.bottom;
                }

                mItemsRect.put(i, itemRect);
                if (DEBUG) {
                    Log.d(TAG, "fillEnd: scroll down/right new item=i=" + i
                            + "=rectCount=" + rectCount);
                }
            }
        }
    }

    private void fillStart(RecyclerView.Recycler recycler) {
        int endPos = getPositionByView(getChildAt(1));
        Rect displayRect = getDisplayRect();
        for (int i = endPos; i >= 0; i--) {
            Rect frame = mItemsRect.get(i);
            if (frame == null) {
                layoutChild(recycler, displayRect, i);
            } else if (Rect.intersects(displayRect, frame)) {
                if (findViewByPosition(i) != null) {
                    if (DEBUG) {
                        Log.d(TAG, "fillStart: view is exist at i=" + i);
                    }
                    continue;
                }
                View scrap = recycler.getViewForPosition(i);
                if (mAdapter.getColumns() > 0) {
                    measureChild(scrap, getItemWidth(i), getItemHeight(i));
                } else {
                    measureChild(scrap);
                }
                addView(scrap, 0);
                if (mOrientation == HORIZONTAL) {
                    layoutDecoratedWithMargins(scrap,
                            frame.left - mHorizontalOffset,
                            frame.top,
                            frame.right - mHorizontalOffset,
                            frame.bottom);
                } else {
                    layoutDecoratedWithMargins(scrap,
                            frame.left,
                            frame.top - mVerticalOffset,
                            frame.right,
                            frame.bottom - mVerticalOffset);
                }
                if (DEBUG) {
                    Log.d(TAG, "fillStart: scroll up/left recycle=i=" + i
                            + "=frame=" + frame.toString() +
                            "=mVerticalOffset=" + mVerticalOffset);
                }
            }
        }
        if (DEBUG) {
            Log.d(TAG, "fillStart: endPos=" + endPos);
        }
    }

    private boolean recycleByScrollState(RecyclerView.Recycler recycler, int dt) {
        final int childCount = getChildCount();
        ArrayList<Integer> recycleIndexList = new ArrayList<>();
        Rect displayRect = getDisplayRect();
        if (dt >= 0) {
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                int pos = getPositionByView(child);
                Rect frame = mItemsRect.get(pos);
                if (!Rect.intersects(displayRect, frame)) {
                    recycleIndexList.add(i);
                }
            }
        } else {
            for (int i = childCount - 1; i >= 0; i--) {
                View child = getChildAt(i);
                int pos = getPositionByView(child);
                Rect frame = mItemsRect.get(pos);
                if (!Rect.intersects(displayRect, frame)) {
                    recycleIndexList.add(i);
                }
            }
        }
        if (recycleIndexList.size() > 0) {
            recycleChildren(recycler, dt, recycleIndexList);
            return true;
        }
        return false;
    }

    /**
     * Recycles children between given index.
     *
     * @param dt direction
     * @param recycleIndexList   save need recycle index
     */
    private void recycleChildren(RecyclerView.Recycler recycler, int dt,
                                 ArrayList<Integer> recycleIndexList) {
        int size = recycleIndexList.size();
        if (DEBUG) {
            Log.d(TAG, "recycleChildren: ===recycle " + size + " items.");
        }
        if (dt < 0) {
            for (int i = 0; i < size; i++) {
                int pos = recycleIndexList.get(i);
                removeAndRecycleViewAt(pos, recycler);
            }
        } else {
            for (int i = size - 1; i >= 0; i--) {
                int pos = recycleIndexList.get(i);
                removeAndRecycleViewAt(pos, recycler);
            }
        }
    }


    private int findLastViewLayoutPosition() {
        int lastPos = getChildCount();
        if (lastPos > 0) {
            lastPos = getPositionByView(getChildAt(lastPos - 1));
        }
        return lastPos;
    }

    private void measureChild(View child) {
        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
        calculateItemDecorationsForChild(child, mDecorInsets);
        int widthUsed = lp.leftMargin + lp.rightMargin + mDecorInsets.left + mDecorInsets.right;
        int heightUsed = lp.topMargin + lp.bottomMargin + mDecorInsets.top + mDecorInsets.bottom;

        final int secondarySpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int widthSpec, heightSpec;

        if (mOrientation == HORIZONTAL) {
            widthSpec = ViewGroup.getChildMeasureSpec(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), widthUsed, lp.width);
            heightSpec = ViewGroup.getChildMeasureSpec(secondarySpec, heightUsed, lp.height);
        } else {
            heightSpec = ViewGroup.getChildMeasureSpec(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), heightUsed, lp.height);
            widthSpec = ViewGroup.getChildMeasureSpec(secondarySpec, widthUsed, lp.width);
        }
        child.measure(widthSpec, heightSpec);
        if (DEBUG) {
            Log.v(TAG, "measureChild secondarySpec " + Integer.toHexString(secondarySpec)
                    + " widthSpec " + Integer.toHexString(widthSpec)
                    + " heightSpec " + Integer.toHexString(heightSpec)
                    + " measuredWidth " + child.getMeasuredWidth()
                    + " measuredHeight " + child.getMeasuredHeight());
        }
        if (DEBUG) Log.v(TAG, "child lp width " + lp.width + " height " + lp.height);
    }

    /**
     * Measure a child view using standard measurement policy, taking the padding
     * of the parent HorizontalModuleGridView, any added item decorations and the child margins
     * into account.
     *
     * @param child Child view to measure
     * @param childWidth Width with child need
     * @param childHeight Height with child need
     */
    public void measureChild(View child, int childWidth, int childHeight) {
        final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
        calculateItemDecorationsForChild(child, mDecorInsets);

        final int widthSpec = getChildMeasureSpec(getWidth(),
                getPaddingLeft() + getPaddingRight() +
                        lp.leftMargin + lp.rightMargin, childWidth,
                canScrollHorizontally());
        final int heightSpec = getChildMeasureSpec(getHeight(),
                getPaddingTop() + getPaddingBottom() +
                        lp.topMargin + lp.bottomMargin, childHeight,
                canScrollVertically());

        child.measure(widthSpec, heightSpec);
    }

    @Override
    public boolean canScrollHorizontally() {
        return mOrientation == HORIZONTAL;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (dx == 0 || getChildCount() == 0) {
            return 0;
        }

        mInScroll = true;
        int realOffset = dx;
        int maxScrollSpace = getMaxScrollDistance();
        if (mHorizontalOffset + dx < 0) {
            if (Math.abs(dx) > mHorizontalOffset) {
                realOffset = -mHorizontalOffset;
            } else {
                realOffset -= mHorizontalOffset;
            }
        } else if (mItemsRect.size() >= getItemCount() && mHorizontalOffset + dx > maxScrollSpace) {
            realOffset = maxScrollSpace - mHorizontalOffset;
        }

        if (DEBUG) {
            Log.d(TAG, "scrollHorizontallyBy: dx=" + dx + "=realOffset=" + realOffset
                    + "=HorizontalOffset=" + mHorizontalOffset +
                    "=maxScrollSpace=" + maxScrollSpace);
        }
        if (mHorizontalOffset != 0) {
            recycleAndFillViews(recycler, state, dx);
        }
        mHorizontalOffset += realOffset;
        offsetChildrenHorizontal(-realOffset);
        mInScroll = false;
        return realOffset;
    }

    @Override
    public boolean canScrollVertically() {
        return mOrientation == VERTICAL;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (dy == 0 || getChildCount() == 0) {
            return 0;
        }

        mInScroll = true;
        int realOffset = dy;
        int maxScrollSpace = getMaxScrollDistance();

        if (mVerticalOffset + dy < 0) {
            if (Math.abs(dy) > mVerticalOffset) {
                realOffset = -mVerticalOffset;
            } else {
                realOffset -= mVerticalOffset;
            }
        } else if (mItemsRect.size() >= getItemCount() && mVerticalOffset + dy > maxScrollSpace) {
            realOffset = maxScrollSpace - mVerticalOffset;
        }

        if (DEBUG) {
            Log.d(TAG, "scrollVerticallyBy: dy=" + dy + "=realOffset=" + realOffset
                    + "=VerticalOffset=" + mVerticalOffset +
                    "=maxScrollSpace=" + maxScrollSpace);
        }
        if (mVerticalOffset != 0) {
            recycleAndFillViews(recycler, state, dy);
        }
        mVerticalOffset += realOffset;
        offsetChildrenPrimary(-realOffset);
        mInScroll = false;
        return realOffset;
    }

    @Override
    public void scrollToPosition(int position) {
        setSelection(mBaseRecyclerView, position, mSubFocusPosition, false, mPrimaryScrollExtra);
    }

    @Override
    public View onFocusSearchFailed(View focused, int direction, RecyclerView.Recycler recycler,
                                    RecyclerView.State state) {
        if (DEBUG) Log.v(TAG, "onFocusSearchFailed direction " + direction);
        View view = null;
        int movement = getMovement(direction);
        final boolean isScroll = mBaseRecyclerView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE;
        if (movement == NEXT_ITEM) {
            if (isScroll || !mFocusOutEnd) {
                view = focused;
            }
        } else if (movement == PREV_ITEM) {
            if (isScroll || !mFocusOutFront) {
                view = focused;
            }
        }
        if (mFocusSearchFailedListener != null) {
            mFocusSearchFailedListener.onFocusSearchFailed(focused, direction, mFocusPosition,
                    state.getItemCount());
        }
        if (DEBUG) Log.v(TAG, "onFocusSearchFailed returning view " + view);
        return view;
    }

    private final static int PREV_ITEM = 0;
    private final static int NEXT_ITEM = 1;
    private final static int PREV_ROW = 2;
    private final static int NEXT_ROW = 3;

    private int getMovement(int direction) {
        int movement = View.FOCUS_LEFT;

        if (mOrientation == HORIZONTAL) {
            switch(direction) {
                case View.FOCUS_LEFT:
                    movement = PREV_ITEM;
                    break;
                case View.FOCUS_RIGHT:
                    movement = NEXT_ITEM;
                    break;
                case View.FOCUS_UP:
                    movement = PREV_ROW;
                    break;
                case View.FOCUS_DOWN:
                    movement = NEXT_ROW;
                    break;
            }
        } else if (mOrientation == VERTICAL) {
            switch(direction) {
                case View.FOCUS_LEFT:
                    movement = PREV_ROW;
                    break;
                case View.FOCUS_RIGHT:
                    movement = NEXT_ROW;
                    break;
                case View.FOCUS_UP:
                    movement = PREV_ITEM;
                    break;
                case View.FOCUS_DOWN:
                    movement = NEXT_ITEM;
                    break;
            }
        }

        return movement;
    }

    @Override
    public View onInterceptFocusSearch(View focused, int direction) {
        mDirection = direction;
        if (mFocusSearchDisabled) {
            return focused;
        }
        return null;
    }

    @Override
    public boolean onRequestChildFocus(RecyclerView parent, RecyclerView.State state, View child,
                                       View focused) {
        if (mFocusSearchDisabled) {
            return true;
        }

        if (getPosition(child) == NO_POSITION) {
            if (DEBUG) {
                Log.d(TAG, "onRequestChildFocus: getPosition error.");
            }
            return true;
        }
        if (!mInLayout && !mInSelection && !mInScroll) {
            if (DEBUG) {
                Log.d(TAG, "onRequestChildFocus: call scrollToView.");
            }
            scrollToView(child, true);
        }
        return true;
    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        if (DEBUG) Log.v(TAG, "onAdapterChanged to " + newAdapter);
        if (oldAdapter != null) {
            discardLayoutInfo();
            mFocusPosition = NO_POSITION;
        }
        super.onAdapterChanged(oldAdapter, newAdapter);
    }

    public void setFocusOutAllowed(boolean throughFront, boolean throughEnd) {
        mFocusOutFront = throughFront;
        mFocusOutEnd = throughEnd;
    }

    private void discardLayoutInfo() {
        mDirection = DEFAULT_DIRECTION;
        mExtraChildHeight = 0;
        mHorizontalOffset = 0;
        mVerticalOffset = 0;
        mItemsRect.clear();
        mOriItemWidth = 0;
        mOriItemHeight = 0;
        mTotalSize = 0;
    }

    @Override
    public boolean onAddFocusables(RecyclerView recyclerView, ArrayList<View> views,
                                   int direction, int focusableMode) {
        if (mFocusSearchDisabled) {
            return true;
        }

        if (!recyclerView.hasFocus() && mFocusScrollStrategy != BaseGridView.FOCUS_SCROLL_ALIGNED) {
            int focusableCount = views.size();
            for (int i = 0, count = getChildCount(); i < count; i++) {
                Rect displayRect = getDisplayRect();
                View child = getChildAt(i);
                if (child.getVisibility() == View.VISIBLE) {
                    if (getViewMin(child) >= displayRect.left && getViewMax(child) <= displayRect.right) {
                        child.addFocusables(views, direction, focusableMode);
                    }
                }
            }
            // if we cannot find any, then just add all children.
            if (views.size() == focusableCount) {
                for (int i = 0, count = getChildCount(); i < count; i++) {
                    View child = getChildAt(i);
                    if (child.getVisibility() == View.VISIBLE) {
                        child.addFocusables(views, direction, focusableMode);
                    }
                }
                if (views.size() != focusableCount) {
                    return true;
                }
            } else {
                return true;
            }
            if (recyclerView.isFocusable()) {
                views.add(recyclerView);
            }
            return true;
        }
        return false;
    }

    /**
     * Scroll to a given child view and change mFocusPosition.
     */
    private void scrollToView(View view, boolean smooth) {
        if (mIsSlidingChildViews || view == null) {
            return;
        }

        int newFocusPosition = getPositionByView(view);
        if (DEBUG) {
            Log.d(TAG, "scrollToView: newFocusPosition=" + newFocusPosition +
                    "=mFocusPosition=" + mFocusPosition);
        }

        if (newFocusPosition != mFocusPosition) {
            mFocusPosition = newFocusPosition;
            if (!mInLayout) {
                dispatchChildSelected();
            }
            if (mBaseRecyclerView.isChildrenDrawingOrderEnabledInternal()) {
                mBaseRecyclerView.invalidate();
            }
        }

        if (!view.hasFocus() && mBaseRecyclerView.hasFocus()) {
            // transfer focus to the child if it does not have focus yet (e.g. triggered
            // by setSelection())
            view.requestFocus();
        }
        if (!mScrollEnabled && smooth) {
            return;
        }

        if (getScrollPosition(view, mTwoInts)) {
            scrollModule(mTwoInts[0], smooth);
        }
    }

    private void dispatchChildSelected() {
        if (mChildSelectedListener == null) {
            return;
        }

        View view = mFocusPosition == NO_POSITION ? null : findViewByPosition(mFocusPosition);
        if (view != null) {
            RecyclerView.ViewHolder vh = mBaseRecyclerView.getChildViewHolder(view);
            if (mChildSelectedListener != null) {
                mChildSelectedListener.onChildSelected(mBaseRecyclerView, view, mFocusPosition,
                        vh == null? NO_ID: vh.getItemId());
            }
            fireOnChildViewHolderSelected(mBaseRecyclerView, vh, mFocusPosition, 0);
        } else {
            if (mChildSelectedListener != null) {
                mChildSelectedListener.onChildSelected(mBaseRecyclerView, null, NO_POSITION, NO_ID);
            }
            fireOnChildViewHolderSelected(mBaseRecyclerView, null, NO_POSITION, 0);
        }

        if (!mInLayout && !mBaseRecyclerView.isLayoutRequested()) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (getChildAt(i).isLayoutRequested()) {
                    forceRequestLayout();
                    break;
                }
            }
        }
    }

    private void dispatchChildSelectedAndPositioned() {
        if (!hasOnChildViewHolderSelectedListener()) {
            return;
        }

        View view = mFocusPosition == NO_POSITION ? null : findViewByPosition(mFocusPosition);
        if (view != null) {
            RecyclerView.ViewHolder vh = mBaseRecyclerView.getChildViewHolder(view);
            fireOnChildViewHolderSelectedAndPositioned(mBaseRecyclerView, vh, mFocusPosition,
                    0);
        } else {
            if (mChildSelectedListener != null) {
                mChildSelectedListener.onChildSelected(mBaseRecyclerView, null, NO_POSITION, NO_ID);
            }
            fireOnChildViewHolderSelectedAndPositioned(mBaseRecyclerView, null, NO_POSITION, 0);
        }

    }

    private void forceRequestLayout() {
        if (DEBUG) Log.v(TAG, "forceRequestLayout");
        ViewCompat.postOnAnimation(mBaseRecyclerView, mRequestLayoutRunnable);
    }

    private final Runnable mRequestLayoutRunnable = new Runnable() {
        @Override
        public void run() {
            requestLayout();
        }
    };

    private int getPositionByView(View view) {
        if (view == null) {
            return NO_POSITION;
        }
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();
        if (params == null || params.isItemRemoved()) {
            // when item is removed, the position value can be any value.
            return NO_POSITION;
        }
        return params.getViewLayoutPosition();
    }

    private boolean getScrollPosition(View view, int[] distanceInts) {
        switch (mFocusScrollStrategy) {
            case BaseGridView.FOCUS_SCROLL_ALIGNED:
            default:
                return getAlignedPosition(view, distanceInts);
            case BaseGridView.FOCUS_SCROLL_ITEM:
            case BaseGridView.FOCUS_SCROLL_PAGE: // page scroll not achieve
                return getNoneAlignedPosition(view, distanceInts);
        }
    }

    private boolean getAlignedPosition(View view, int[] distanceInts) {
        if (!isOverHalfScreen(view)) {
            if (DEBUG) {
                Log.d(TAG, "getAlignedPosition: is not over half screen.");
            }
            return false;
        }
        int scrollPrimary = getAlignedScrollPrimary(view);

        if (DEBUG) {
            Log.d(TAG, "getAlignedPosition: scrollPrimary=" + scrollPrimary);
        }
        if (scrollPrimary != 0) {
            distanceInts[0] = scrollPrimary;
            distanceInts[1] = scrollPrimary;
            return true;
        }
        return false;
    }

    private boolean getNoneAlignedPosition(View view, int[] distanceInts) {
        if (!isCoverChild(view)) {
            if (DEBUG) {
                Log.d(TAG, "getNoneAlignedPosition: child is visible.");
            }
            return false;
        }
        if (mFocusScrollStrategy == BaseGridView.FOCUS_SCROLL_ITEM) {
            int scrollPrimary = getNoneAlignedScrollPrimary(view);
            if (DEBUG) {
                Log.d(TAG, "getNoneAlignedPosition: scrollPrimary=" + scrollPrimary);
            }
            if (scrollPrimary != 0) {
                distanceInts[0] = scrollPrimary;
                distanceInts[1] = scrollPrimary;
                return true;
            }
        }
        return false;
    }

    private boolean isOverHalfScreen(View child) {
        Rect ret = new Rect();
        child.getGlobalVisibleRect(ret);
        int size = getClientSize();
        if (mOrientation == HORIZONTAL) {
            if (ret.right > size / 2 || ret.left < size / 2) {
                return true;
            }
        } else {
            if (ret.top < size / 2 || ret.bottom > size / 2) {
                return true;
            }
        }
        return false;
    }

    private boolean isCoverChild(View child) {
        if (child != null) {
            Rect ret = new Rect();
            boolean isVisible = child.getGlobalVisibleRect(ret);
            if (!isVisible) {
                return true;
            }
            if (mOrientation == HORIZONTAL) {
                return ret.width() < child.getWidth();
            } else {
                return ret.height() < child.getHeight();
            }
        }
        return false;
    }

    private int getAlignedScrollPrimary(View view) {
        int distance;
        if (mOrientation == HORIZONTAL) {
            if (mDirection != DEFAULT_DIRECTION) {
                if (mDirection == View.FOCUS_UP || mDirection == View.FOCUS_DOWN) {
                    return 0;
                }
            }
            distance = view.getLeft() +
                    view.getWidth() / 2 - getClientSize() / 2 - getPaddingLeft();
        } else {
            if (mDirection != DEFAULT_DIRECTION) {
                if (mDirection == View.FOCUS_LEFT || mDirection == View.FOCUS_RIGHT) {
                    return 0;
                }
            }
            distance = view.getTop() +
                    view.getHeight() / 2 - getClientSize() / 2 - getPaddingTop();
        }
        return distance;
    }

    private int getNoneAlignedScrollPrimary(View view) {
        int distance = 0;

        int viewMin = getDecoratedStart(view);
        int viewMax = getDecoratedEnd(view);

        View firstView = null;
        View lastView = null;
        int paddingLow = getPaddingLow();
        int clientSize = getClientSize();
        if (viewMin < paddingLow) {
            firstView = view;
        } else if (viewMax > clientSize + paddingLow) {
            lastView = view;
        }
        if (firstView != null) {
            distance = getDecoratedStart(firstView) - paddingLow;
        } else if (lastView != null) {
            distance = getDecoratedEnd(lastView) - (paddingLow + clientSize);
        }

        return distance;
    }

    private int getPaddingLow() {
        if (mOrientation == HORIZONTAL) {
            return getPaddingLeft();
        } else {
            return getPaddingTop();
        }
    }

    private int getMaxScrollDistance() {
        int scrollDistance;
        if (mOrientation == HORIZONTAL) {
            scrollDistance = mTotalSize - getClientSize();
        } else {
            scrollDistance = mTotalSize - getClientSize();
        }
        return scrollDistance > 0 ? scrollDistance : 0;
    }

    private int getClientSize() {
        if (mOrientation == HORIZONTAL) {
            return getWidth() - getPaddingLeft() - getPaddingRight();
        } else {
            return getHeight() - getPaddingTop() - getPaddingBottom();
        }
    }

    private void scrollModule(int scrollPrimary, boolean smooth) {
        if (mInLayout) {
            offsetChildrenPrimary(-scrollPrimary);
        } else {
            int scrollX = 0;
            int scrollY = 0;
            if (mOrientation == HORIZONTAL) {
                scrollX = scrollPrimary;
            } else {
                scrollY = scrollPrimary;
            }
            if (smooth) {
                mBaseRecyclerView.smoothScrollBy(scrollX, scrollY);
            } else {
                mBaseRecyclerView.scrollBy(scrollX, scrollY);
                dispatchChildSelected();
                dispatchChildSelectedAndPositioned();
            }
        }
    }

    private void offsetChildrenPrimary(int increment) {
        final int childCount = getChildCount();
        if (mOrientation == VERTICAL) {
            for (int i = 0; i < childCount; i++) {
                getChildAt(i).offsetTopAndBottom(increment);
            }
        } else {
            for (int i = 0; i < childCount; i++) {
                getChildAt(i).offsetLeftAndRight(increment);
            }
        }
    }

    public void setChildrenVisibility(int visibility) {
        mChildVisibility = visibility;
        if (mChildVisibility != -1) {
            int count = getChildCount();
            for (int i= 0; i < count; i++) {
                getChildAt(i).setVisibility(visibility);
            }
        }
    }

    public int getChildDrawingOrder(RecyclerView recyclerView, int childCount, int i) {
        View view = findViewByPosition(mFocusPosition);
        if (view == null) {
            return i;
        }
        int focusIndex = recyclerView.indexOfChild(view);
        // supposedly 0 1 2 3 4 5 6 7 8 9, 4 is the center item
        // drawing order is 0 1 2 3 9 8 7 6 5 4
        if (i < focusIndex) {
            return i;
        } else if (i < childCount - 1) {
            return focusIndex + childCount - 1 - i;
        } else {
            return focusIndex;
        }
    }

    public void setScrollEnabled(boolean scrollEnabled) {
        if (mScrollEnabled != scrollEnabled) {
            mScrollEnabled = scrollEnabled;
            if (mScrollEnabled && mFocusPosition != NO_POSITION) {
                scrollToSelection(mBaseRecyclerView, mFocusPosition, mSubFocusPosition, true,
                        mPrimaryScrollExtra);
            }
        }
    }

    public void setSelection(RecyclerView parent, int position,
                             int primaryScrollExtra) {
        setSelection(parent, position, 0, false, primaryScrollExtra);
    }

    public void setSelectionSmooth(RecyclerView parent, int position) {
        setSelection(parent, position, 0, true, 0);
    }

    public void setSelectionWithSub(RecyclerView parent, int position, int subPosition,
                                    int primaryScrollExtra) {
        setSelection(parent, position, subPosition, false, primaryScrollExtra);
    }

    public void setSelectionSmoothWithSub(RecyclerView parent, int position, int subPosition) {
        setSelection(parent, position, subPosition, true, 0);
    }

    private void setSelection(RecyclerView parent, int position, int subPosition,
                              boolean smooth, int primaryScrollExtra) {
        if (DEBUG) {
            Log.d(TAG, "setSelection: mFocusPosition=" + mFocusPosition
                    + "=position=" + position);
        }
        if (mFocusPosition != position && position != NO_POSITION) {
            scrollToSelection(parent, position, subPosition, smooth, primaryScrollExtra);
        }
    }

    private void scrollToSelection(RecyclerView parent, int position, int subPosition,
                                   boolean smooth, int primaryScrollExtra) {
        View view = findViewByPosition(position);
        if (view != null) {
            mInSelection = true;
            scrollToView(view, smooth);
            mInSelection = false;
        } else {
            mFocusPosition = position;
            if (mIsSlidingChildViews) {
                return;
            }
            if (smooth) {
                if (!hasDoneFirstLayout()) {
                    Log.w(TAG, "setSelectionSmooth should "
                            + "not be called before first layout pass");
                    return;
                }
                position = startPositionSmoothScroller(position);
                if (position != mFocusPosition) {
                    // gets cropped by adapter size
                    mFocusPosition = position;
                }
            } else {
                parent.requestLayout();
            }
        }
    }

    private int startPositionSmoothScroller(int position) {
        LinearSmoothScroller linearSmoothScroller = new GridLinearSmoothScroller() {
            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                if (getChildCount() == 0) {
                    return null;
                }
                final int firstChildPos = getPosition(getChildAt(0));
                final boolean isStart = targetPosition < firstChildPos;
                final int direction = isStart ? -1 : 1;
                if (mOrientation == HORIZONTAL) {
                    return new PointF(direction, 0);
                } else {
                    return new PointF(0, direction);
                }
            }

            @Override
            protected void onStop() {
                super.onStop();
            }
        };
        linearSmoothScroller.setTargetPosition(position);
        startSmoothScroll(linearSmoothScroller);
        return linearSmoothScroller.getTargetPosition();
    }

    public void setOnChildViewHolderSelectedListener(OnChildViewHolderSelectedListener listener) {
        if (listener == null) {
            mChildViewHolderSelectedListeners = null;
            return;
        }
        if (mChildViewHolderSelectedListeners == null) {
            mChildViewHolderSelectedListeners = new ArrayList<>();
        } else {
            mChildViewHolderSelectedListeners.clear();
        }
        mChildViewHolderSelectedListeners.add(listener);
    }

    public void addOnChildViewHolderSelectedListener(OnChildViewHolderSelectedListener listener) {
        if (mChildViewHolderSelectedListeners == null) {
            mChildViewHolderSelectedListeners = new ArrayList<>();
        }
        mChildViewHolderSelectedListeners.add(listener);
    }

    public void removeOnChildViewHolderSelectedListener(OnChildViewHolderSelectedListener
                                                                listener) {
        if (mChildViewHolderSelectedListeners != null) {
            mChildViewHolderSelectedListeners.remove(listener);
        }
    }

    private boolean hasOnChildViewHolderSelectedListener() {
        return mChildViewHolderSelectedListeners != null
                && mChildViewHolderSelectedListeners.size() > 0;
    }

    private void fireOnChildViewHolderSelected(RecyclerView parent, RecyclerView.ViewHolder child,
                                               int position, int subPosition) {
        if (mChildViewHolderSelectedListeners == null) {
            return;
        }
        for (int i = mChildViewHolderSelectedListeners.size() - 1; i >= 0 ; i--) {
            mChildViewHolderSelectedListeners.get(i).onChildViewHolderSelected(parent, child,
                    position, subPosition);
        }
    }

    private void fireOnChildViewHolderSelectedAndPositioned(RecyclerView parent, RecyclerView.ViewHolder
            child, int position, int subPosition) {
//        if (mChildViewHolderSelectedListeners == null) {
//            return;
//        }
//        for (int i = mChildViewHolderSelectedListeners.size() - 1; i >= 0 ; i--) {
//            mChildViewHolderSelectedListeners.get(i).onChildViewHolderSelectedAndPositioned(parent,
//                    child, position, subPosition);
//        }
    }

    public void setOnChildSelectedListener(OnChildSelectedListener listener) {
        mChildSelectedListener = listener;
    }

    public int getFocusScrollStrategy() {
        return mFocusScrollStrategy;
    }

    public void setFocusScrollStrategy(int focusScrollStrategy) {
        mFocusScrollStrategy = focusScrollStrategy;
        if (mFocusScrollStrategy == BaseGridView.FOCUS_SCROLL_ITEM) {
            mExpandSpace = BASE_ITEM_DEFAULT_SIZE / 2;
        } else {
            mExpandSpace = 0;
        }
    }

    public boolean isScrollEnabled() {
        return mScrollEnabled;
    }

    public void setFocusSearchDisabled(boolean disabled) {
        mFocusSearchDisabled = disabled;
    }

    public boolean isFocusSearchDisabled() {
        return mFocusSearchDisabled;
    }

    public int getSelection() {
        return mFocusPosition;
    }

    public int getSubSelection() {
        return mSubFocusPosition;
    }

    public void setOrientation(int orientation) {
        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            throw new IllegalArgumentException("invalid orientation.");
        }
        assertNotInLayoutOrScroll(null);
        if (orientation == mOrientation) {
            return;
        }
        mOrientation = orientation;
    }

    public int getOrientation() {
        return mOrientation;
    }

    void setNumRows(int numRowsOrColumns) {
        mNumRowOrColumn = numRowsOrColumns;
    }

    void setAdapter(GridObjectAdapter adapter) {
        if (adapter != null) {
            mAdapter = adapter;
        } else {
            throw new ClassCastException("invalid ObjectAdapter need GridObjectAdapter");
        }
    }

    boolean gridOnRequestFocusInDescendants(RecyclerView recyclerView, int direction,
                                            Rect previouslyFocusedRect) {
        switch (mFocusScrollStrategy) {
            case BaseGridView.FOCUS_SCROLL_ALIGNED:
            default:
                return gridOnRequestFocusInDescendantsAligned(recyclerView,
                        direction, previouslyFocusedRect);
            case BaseGridView.FOCUS_SCROLL_PAGE:
            case BaseGridView.FOCUS_SCROLL_ITEM:
                return gridOnRequestFocusInDescendantsUnaligned(recyclerView,
                        direction, previouslyFocusedRect);
        }
    }

    private boolean gridOnRequestFocusInDescendantsAligned(RecyclerView recyclerView,
                                                           int direction, Rect previouslyFocusedRect) {
        View view = findViewByPosition(mFocusPosition);
        if (view != null) {
            boolean result = view.requestFocus(direction, previouslyFocusedRect);
            if (!result && DEBUG) {
                Log.w(TAG, "failed to request focus on " + view);
            }
            return result;
        }
        return false;
    }

    private boolean gridOnRequestFocusInDescendantsUnaligned(RecyclerView recyclerView,
                                                             int direction, Rect previouslyFocusedRect) {
        // focus to view not overlapping padding area to avoid scrolling in gaining focus
        int index;
        int increment;
        int end;
        int count = getChildCount();
        if ((direction & View.FOCUS_FORWARD) != 0) {
            index = 0;
            increment = 1;
            end = count;
        } else {
            index = count - 1;
            increment = -1;
            end = -1;
        }
        int left = getPaddingLow();
        int right = getClientSize() + left;
        for (int i = index; i != end; i += increment) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                if (getViewMin(child) >= left && getViewMax(child) <= right) {
                    if (child.requestFocus(direction, previouslyFocusedRect)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    void setCellSize(int width, int height) {
        mOriItemWidth = width;
        mOriItemHeight = height;
    }

    public void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        if (DEBUG) {
            Log.d(TAG, "onFocusChanged: gainFocus=" + gainFocus);
        }
        if (gainFocus) {
            // if moduleRecyclerView.requestFocus() is called, select first focusable child.
            for (int i = mFocusPosition; ;i++) {
                View view = findViewByPosition(i);
                if (view == null) {
                    break;
                }
                if (view.getVisibility() == View.VISIBLE && view.hasFocusable()) {
                    view.requestFocus();
                    break;
                }
            }
        }
    }

    /**
     * Returns the adapter position of the first visible view. This position does not include
     * adapter changes that were dispatched after the last layout pass.
     * @return the first visible item position or -1
     */
    int findFirstVisibleItemPosition() {
        final View child = findOneVisibleChild(0, getChildCount(), false, true);
        return child == null ? -1 : getPosition(child);
    }

    /**
     * Returns the adapter position of the last visible view. This position does not include
     * adapter changes that were dispatched after the last layout pass.
     * @return the last visible item position or -1
     */
    int findLastVisibleItemPosition() {
        final View child = findOneVisibleChild(getChildCount() - 1, -1, false, true);
        return child == null ? -1 : getPosition(child);
    }

    private View findOneVisibleChild(int fromIndex, int toIndex, boolean completelyVisible,
                                     boolean acceptPartiallyVisible) {
        final int start = getPaddingLeft();
        final int end = getHeight() - getPaddingBottom();
        final int next = toIndex > fromIndex ? 1 : -1;
        View partiallyVisible = null;
        for (int i = fromIndex; i != toIndex; i+=next) {
            final View child = getChildAt(i);
            final int childStart = getDecoratedStart(child);
            final int childEnd = getDecoratedEnd(child);
            if (childStart < end && childEnd > start) {
                if (completelyVisible) {
                    if (childStart >= start && childEnd <= end) {
                        return child;
                    } else if (acceptPartiallyVisible && partiallyVisible == null) {
                        partiallyVisible = child;
                    }
                } else {
                    return child;
                }
            }
        }
        return partiallyVisible;
    }

    private int getViewMin(View v) {
        final RecyclerView.MarginLayoutParams lp = (RecyclerView.MarginLayoutParams)v.getLayoutParams();
        if (mOrientation == HORIZONTAL) {
            return getDecoratedLeft(v) - lp.leftMargin;
        } else {
            return getDecoratedTop(v) - lp.topMargin;
        }
    }

    private int getViewMax(View v) {
        final RecyclerView.MarginLayoutParams lp = (RecyclerView.MarginLayoutParams) v.getLayoutParams();
        if (mOrientation == HORIZONTAL) {
            return getDecoratedRight(v) + lp.rightMargin;
        } else {
            return getDecoratedBottom(v) + lp.bottomMargin;
        }
    }

    private int getItemWidth(int position) {
        int itemColumnSize = mAdapter.getItemColumnSize(position);
        return (int) (itemColumnSize * mOriItemWidth
                + (itemColumnSize - 1) * (mDecorInsets.left + mDecorInsets.right
                + mAdapter.getColumnSpacing()));
    }

    private int getItemHeight(int position) {
        int itemRowSize = mAdapter.getItemRowSize(position);
        return (int) (itemRowSize * mOriItemHeight
                + (itemRowSize - 1) * (mDecorInsets.bottom + mDecorInsets.top
                + mAdapter.getRowSpacing()));
    }

    private int getMeasurementHorizontal(View view) {
        final RecyclerView.MarginLayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return  view.getMeasuredWidth() + params.leftMargin
                + params.rightMargin;
    }

    private int getMeasurementVertical(View view) {
        final RecyclerView.MarginLayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return view.getMeasuredHeight() + params.topMargin
                + params.bottomMargin;
    }

    private int getDecoratedMeasurementHorizontal(View view) {
        final RecyclerView.MarginLayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return getDecoratedMeasuredWidth(view) + params.leftMargin
                + params.rightMargin;
    }

    private int getDecoratedMeasurementVertical(View view) {
        final RecyclerView.MarginLayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return getDecoratedMeasuredHeight(view) + params.topMargin
                + params.bottomMargin;
    }

    private int getChildHorizontalPadding(View child) {
        final RecyclerView.MarginLayoutParams params = (RecyclerView.LayoutParams)
                child.getLayoutParams();
        return getDecoratedMeasuredWidth(child) + params.leftMargin
                + params.rightMargin - child.getMeasuredWidth() + mAdapter.getColumnSpacing();
    }

    private int getChildVerticalPadding(View child) {
        final RecyclerView.MarginLayoutParams params = (RecyclerView.LayoutParams)
                child.getLayoutParams();
        return getDecoratedMeasuredHeight(child) + params.topMargin
                + params.bottomMargin - child.getMeasuredHeight() + mAdapter.getRowSpacing();
    }

    private int getVerticalSpace() {
        int space = getHeight();
        return space <= 0 ? getMinimumHeight() : space;
    }

    private int getHorizontalSpace() {
        int space = getWidth();
        return space <= 0 ? getMinimumWidth() : space;
    }

    private int getDecoratedStart(View view) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        if (mOrientation == VERTICAL) {
            return getDecoratedTop(view) - params.topMargin;
        } else {
            return getDecoratedLeft(view) - params.leftMargin;
        }
    }

    private int getDecoratedEnd(View view) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        if (mOrientation == VERTICAL) {
            return getDecoratedBottom(view) + params.bottomMargin;
        } else {
            return getDecoratedRight(view) + params.rightMargin;
        }
    }

    private int getFirstChildPosition() {
        final int childCount = getChildCount();
        return childCount == 0 ? 0 : getPosition(getChildAt(0));
    }

    private int calculateScrollDirectionForPosition(int position) {
        if (getChildCount() == 0) {
            return LAYOUT_START;
        }
        final int firstChildPos = getFirstChildPosition();
        return position < firstChildPos ? LAYOUT_START : LAYOUT_END;
    }

    void setOnFocusSearchFailedListener(OnFocusSearchFailedListener listener) {
        mFocusSearchFailedListener = listener;
    }

    void setGravity(int gravity) {
        mGravity = gravity;
    }

    int getOpticalLeft(View view) {
        return view.getLeft() + mDecorInsets.left;
    }

    int getOpticalTop(View view) {
        return view.getTop() + mDecorInsets.top;
    }

    int getOpticalRight(View view) {
        return view.getRight() - mDecorInsets.right;
    }

    int getOpticalBottom(View view) {
        return view.getBottom() - mDecorInsets.bottom;
    }

    /**
     * Base class which scrolls to selected view in onStop().
     */
    abstract class GridLinearSmoothScroller extends LinearSmoothScroller {
        GridLinearSmoothScroller() {
            super(mBaseRecyclerView.getContext());
        }

        @Override
        protected void onStop() {
            View targetView = findViewByPosition(getTargetPosition());
            if (targetView == null) {
                if (getTargetPosition() >= 0) {
                    // if smooth scroller is stopped without target, immediately jumps
                    // to the target position.
                    scrollToSelection(mBaseRecyclerView, getTargetPosition(), mSubFocusPosition,
                            false, mPrimaryScrollExtra);
                }
                super.onStop();
                return;
            }
            if (mFocusPosition != getTargetPosition()) {
                // This should not happen since we cropped value in startPositionSmoothScroller()
                mFocusPosition = getTargetPosition();
            }
            if (hasFocus()) {
                mInSelection = true;
                if (mFocusScrollStrategy != BaseGridView.FOCUS_SCROLL_ITEM) {
                    scrollToView(targetView, true);
                } else {
                    targetView.requestFocus();
                }
                mInSelection = false;
            }
            dispatchChildSelected();
            dispatchChildSelectedAndPositioned();
            super.onStop();
        }
    }
}