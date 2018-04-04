package app.com.tvrecyclerview;

import android.graphics.PointF;
import android.graphics.Rect;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;


public abstract class ModuleLayoutManager extends RecyclerView.LayoutManager implements
        RecyclerView.SmoothScroller.ScrollVectorProvider {

    private static final String TAG = "ModuleLayoutManager";

    private static final int BASE_ITEM_DEFAULT_SIZE = 380;

    private static final int HORIZONTAL = OrientationHelper.HORIZONTAL;

    private static final int VERTICAL = OrientationHelper.VERTICAL;

    private final static int LAYOUT_START = -1;

    private final static int LAYOUT_END = 1;

    private int mOrientation;
    private final SparseArray<Rect> mItemsRect;

    private int mHorizontalOffset;
    private int mVerticalOffset;

    private final int mNumRowOrColumn;

    private int mOriItemWidth;
    private int mOriItemHeight;
    private int mTotalSize;
    // re-used variable to acquire decor insets from RecyclerView
    private final Rect mDecorInsets = new Rect();

    public ModuleLayoutManager(int rowOrColumnCount, int orientation) {
        mOrientation = orientation;
        mOriItemWidth = BASE_ITEM_DEFAULT_SIZE;
        mOriItemHeight = BASE_ITEM_DEFAULT_SIZE;
        mNumRowOrColumn = rowOrColumnCount;
        mItemsRect = new SparseArray<>();
    }

    public ModuleLayoutManager(int rowOrColumnCount, int orientation, int baseItemWidth,
                               int baseItemHeight) {
        mOrientation = orientation;
        mOriItemWidth = baseItemWidth;
        mOriItemHeight = baseItemHeight;
        mNumRowOrColumn = rowOrColumnCount;
        mItemsRect = new SparseArray<>();
    }

    /**
     * reset default item row or column.
     * Avoid the width and height of all items in each row or column
     * more than the width and height of the parent view.
     */
    private void resetItemRowColumnSize() {
        if (mOrientation == HORIZONTAL) {
            mOriItemHeight = (getHeight() - (mNumRowOrColumn - 1) * getRowSpacing())
                             / mNumRowOrColumn;
        } else {
            mOriItemWidth = (getWidth() - (mNumRowOrColumn - 1) * getColumnSpacing())
                             / mNumRowOrColumn;
        }
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        layoutChildren(recycler, state);
    }

    private void layoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getItemCount() == 0) {
            detachAndScrapAttachedViews(recycler);
            return;
        }

        if (getChildCount() == 0 && state.isPreLayout()) {
            return;
        }

        detachAndScrapAttachedViews(recycler);

        if (mHorizontalOffset > 0 || mVerticalOffset > 0) {
            recycleAndFillItems(recycler, state);
            return;
        }
        mHorizontalOffset = 0;
        mVerticalOffset = 0;
        resetItemRowColumnSize();
        fill(recycler, state);
    }

    private void fill(RecyclerView.Recycler recycler, RecyclerView.State state) {
        int leftOffset;
        int topOffset;
        int itemCount = getItemCount();
        for (int i = 0; i < itemCount; i++) {
            View child = recycler.getViewForPosition(i);
            calculateItemDecorationsForChild(child, mDecorInsets);
            measureChild(child, getItemWidth(i), getItemHeight(i));
            addView(child);

            // change leftOffset
            int itemStartPos = getItemStartIndex(i);
            int childHorizontalSpace = getDecoratedMeasurementHorizontal(child);
            int childVerticalSpace = getDecoratedMeasurementVertical(child);
            int lastPos;
            int topPos;
            if (mOrientation == HORIZONTAL) {
                lastPos = itemStartPos / mNumRowOrColumn;
                topPos = itemStartPos % mNumRowOrColumn;
            } else {
                lastPos = itemStartPos % mNumRowOrColumn;
                topPos = itemStartPos / mNumRowOrColumn;
            }

            if (lastPos == 0) {
                leftOffset = -mDecorInsets.left;
            } else {
                leftOffset = mOriItemWidth * lastPos + getChildHorizontalPadding(child) * (lastPos - 1);
            }

            if (topPos == 0) {
                topOffset = -mDecorInsets.top;
            } else {
                topOffset = (mOriItemHeight + getChildVerticalPadding(child)) * topPos;
            }

            //calculate width includes margin
            layoutDecoratedWithMargins(
                    child,
                    leftOffset,
                    topOffset,
                    leftOffset + childHorizontalSpace,
                    topOffset + childVerticalSpace);
            if (mOrientation == HORIZONTAL) {
                mTotalSize = leftOffset + childHorizontalSpace;
            } else {
                mTotalSize = topOffset + childVerticalSpace;
            }

            // Save the current Bound field data for the item view
            Rect frame = mItemsRect.get(i);
            if (frame == null) {
                frame = new Rect();
            }
            frame.set(leftOffset + mHorizontalOffset,
                    topOffset,
                    leftOffset + childHorizontalSpace,
                    topOffset + childVerticalSpace);
            mItemsRect.put(i, frame);
        }
    }

    private void recycleAndFillItems(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.isPreLayout()) {
            return;
        }

        Rect displayFrame;
        if (mOrientation == HORIZONTAL) {
            displayFrame = new Rect(mHorizontalOffset - getColumnSpacing(), 0,
                    mHorizontalOffset + getHorizontalSpace() + getColumnSpacing(), getVerticalSpace());
        } else {
            displayFrame = new Rect(0, mVerticalOffset - getRowSpacing(),
                    getHorizontalSpace(), mVerticalOffset + getVerticalSpace() + getRowSpacing());
        }

        Rect childFrame = new Rect();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            childFrame.left = getDecoratedLeft(child);
            childFrame.top = getDecoratedTop(child);
            childFrame.right = getDecoratedRight(child);
            childFrame.bottom = getDecoratedBottom(child);
            if (!Rect.intersects(displayFrame, childFrame)) {
                removeAndRecycleView(child, recycler);
            }
        }

        //Re-display the subview that needs to appear on the screen
        for (int i = 0; i < getItemCount(); i++) {
            if (Rect.intersects(displayFrame, mItemsRect.get(i))) {
                View scrap = recycler.getViewForPosition(i);
                measureChild(scrap, getItemWidth(i), getItemHeight(i));
                addView(scrap);
                Rect frame = mItemsRect.get(i);
                if (mOrientation == HORIZONTAL) {
                    layoutDecorated(scrap,
                            frame.left - mHorizontalOffset,
                            frame.top,
                            frame.right - mHorizontalOffset,
                            frame.bottom);
                } else {
                    layoutDecorated(scrap,
                            frame.left,
                            frame.top - mVerticalOffset,
                            frame.right,
                            frame.bottom - mVerticalOffset);
                }
            } else {
                //item view that slide out of the screen will return Recycle cache
                View recyclerChild = recycler.getViewForPosition(i);
                removeAndRecycleView(recyclerChild, recycler);
            }
        }
    }

    public void measureChild(View child, int childWidth, int childHeight) {
        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
        int width = Math.max(0, childWidth - lp.leftMargin - lp.rightMargin);
        int height = Math.max(0, childHeight - lp.topMargin - lp.bottomMargin);
        int childWidthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int childHeightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
        child.measure(childWidthSpec, childHeightSpec);
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

        int realOffset = dx;
        int maxScrollSpace = mTotalSize - getHorizontalSpace();
        if (mHorizontalOffset + dx < 0) {
            if (Math.abs(dx) > mHorizontalOffset) {
                realOffset = -mHorizontalOffset;
            } else {
                realOffset -= mHorizontalOffset;
            }
        } else if (mHorizontalOffset + dx > maxScrollSpace) {
            realOffset = maxScrollSpace - mHorizontalOffset;
        }
        mHorizontalOffset += realOffset;

        offsetChildrenHorizontal(realOffset);
        detachAndScrapAttachedViews(recycler);
        recycleAndFillItems(recycler, state);
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

        int realOffset = dy;
        int maxScrollSpace = mTotalSize - getVerticalSpace();

        if (mVerticalOffset + dy < 0) {
            if (Math.abs(dy) > mVerticalOffset) {
                realOffset = -mVerticalOffset;
            } else {
                realOffset -= mVerticalOffset;
            }
        } else if (mVerticalOffset + dy > maxScrollSpace) {
            realOffset = maxScrollSpace - mVerticalOffset;
        }
        mVerticalOffset += realOffset;
        offsetChildrenVertical(-realOffset);
        detachAndScrapAttachedViews(recycler);
        recycleAndFillItems(recycler, state);
        return realOffset;
    }

    public int getOrientation() {
        return mOrientation;
    }

    private int getItemWidth(int position) {
        int itemColumnSize = getItemColumnSize(position);
        return itemColumnSize * mOriItemWidth
                + (itemColumnSize - 1) * (mDecorInsets.left + mDecorInsets.right);
    }

    private int getItemHeight(int position) {
        int itemRowSize = getItemRowSize(position);
        return itemRowSize * mOriItemHeight
                + (itemRowSize - 1) * (mDecorInsets.bottom + mDecorInsets.top);
    }

    private int getDecoratedMeasurementHorizontal(View child) {
        final RecyclerView.MarginLayoutParams params = (RecyclerView.LayoutParams)
                child.getLayoutParams();
        return getDecoratedMeasuredWidth(child) + params.leftMargin
                + params.rightMargin;
    }

    private int getChildHorizontalPadding(View child) {
        final RecyclerView.MarginLayoutParams params = (RecyclerView.LayoutParams)
                child.getLayoutParams();
        return getDecoratedMeasuredWidth(child) + params.leftMargin
                + params.rightMargin - child.getMeasuredWidth();
    }

    private int getChildVerticalPadding(View child) {
        final RecyclerView.MarginLayoutParams params = (RecyclerView.LayoutParams)
                child.getLayoutParams();
        return getDecoratedMeasuredHeight(child) + params.topMargin
                + params.bottomMargin - child.getMeasuredHeight();
    }

    private int getDecoratedMeasurementVertical(View view) {
        final RecyclerView.MarginLayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return getDecoratedMeasuredHeight(view) + params.topMargin
                + params.bottomMargin;
    }

    private int getVerticalSpace() {
        int space = getHeight() - getPaddingTop() - getPaddingBottom();
        return space <= 0 ? getMinimumHeight() : space;
    }

    private int getHorizontalSpace() {
        int space = getWidth() - getPaddingLeft() - getPaddingRight();
        return space <= 0 ? getMinimumWidth() : space;
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
        requestLayout();
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

    @Override
    public PointF computeScrollVectorForPosition(int targetPosition) {
        final int direction = calculateScrollDirectionForPosition(targetPosition);
        PointF outVector = new PointF();
        if (direction == 0) {
            return null;
        }
        if (mOrientation == HORIZONTAL) {
            outVector.x = direction;
            outVector.y = 0;
        } else {
            outVector.x = 0;
            outVector.y = direction;
        }
        return outVector;
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

    private int getDecoratedStart(View view) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return getDecoratedTop(view) - params.topMargin;
    }

    private int getDecoratedEnd(View view) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return getDecoratedBottom(view) + params.bottomMargin;
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

    protected abstract int getItemStartIndex(int position);

    protected abstract int getItemRowSize(int position);

    protected abstract int getItemColumnSize(int position);

    protected  abstract int getColumnSpacing();

    protected abstract int getRowSpacing();
}