package app.com.tvrecyclerview;

import android.animation.TimeAnimator;
import android.content.res.Resources;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;


/**
 * Sets up the highlighting behavior when an item gains focus.
 */
public class FocusHighlightHelper {

    public static final int ZOOM_FACTOR_NONE = 0;

    /**
     * A small zoom factor, recommended for large item views.
     */
    public static final int ZOOM_FACTOR_SMALL = 1;

    /**
     * A medium zoom factor, recommended for medium sized item views.
     */
    public static final int ZOOM_FACTOR_MEDIUM = 2;

    /**
     * A large zoom factor, recommended for small item views.
     */
    public static final int ZOOM_FACTOR_LARGE = 3;

    /**
     * An extra small zoom factor.
     */
    public static final int ZOOM_FACTOR_XSMALL = 4;


    private static boolean isValidZoomIndex(int zoomIndex) {
        return zoomIndex == ZOOM_FACTOR_NONE || getResId(zoomIndex) > 0;
    }

    private static int getResId(int zoomIndex) {
        switch (zoomIndex) {
            case ZOOM_FACTOR_SMALL:
                return R.fraction.tr_focus_zoom_factor_small;
            case ZOOM_FACTOR_XSMALL:
                return R.fraction.tr_focus_zoom_factor_xsmall;
            case ZOOM_FACTOR_MEDIUM:
                return R.fraction.tr_focus_zoom_factor_medium;
            case ZOOM_FACTOR_LARGE:
                return R.fraction.tr_focus_zoom_factor_large;
            default:
                return 0;
        }
    }


    private static class FocusAnimator implements TimeAnimator.TimeListener {
        private final View mView;
        private final int mDuration;
        private final float mScaleDiff;
        private float mFocusLevel = 0f;
        private float mFocusLevelStart;
        private float mFocusLevelDelta;
        private final TimeAnimator mAnimator = new TimeAnimator();
        private final Interpolator mInterpolator = new AccelerateDecelerateInterpolator();

        void animateFocus(boolean select, boolean immediate) {
            endAnimation();
            final float end = select ? 1 : 0;
            if (immediate) {
                setFocusLevel(end);
            } else if (mFocusLevel != end) {
                mFocusLevelStart = mFocusLevel;
                mFocusLevelDelta = end - mFocusLevelStart;
                mAnimator.start();
            }
        }

        FocusAnimator(View view, float scale, int duration) {
            mView = view;
            mDuration = duration;
            mScaleDiff = scale - 1f;
            mAnimator.setTimeListener(this);
        }

        void setFocusLevel(float level) {
            mFocusLevel = level;
            float scale = 1f + mScaleDiff * level;
            mView.setScaleX(scale);
            mView.setScaleY(scale);
        }

        float getFocusLevel() {
            return mFocusLevel;
        }

        void endAnimation() {
            mAnimator.end();
        }

        @Override
        public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
            float fraction;
            if (totalTime >= mDuration) {
                fraction = 1;
                mAnimator.end();
            } else {
                fraction = (float) (totalTime / (double) mDuration);
            }
            if (mInterpolator != null) {
                fraction = mInterpolator.getInterpolation(fraction);
            }
            setFocusLevel(mFocusLevelStart + fraction * mFocusLevelDelta);
        }
    }

    private static class ItemBridgeFocusHighlight implements FocusHighlightHandler {
        private static final int DURATION_MS = 150;

        private int mScaleIndex;

        public ItemBridgeFocusHighlight(int zoomIndex) {
            if (!isValidZoomIndex(zoomIndex)) {
                throw new IllegalArgumentException("Unhandled zoom index");
            }
            mScaleIndex = zoomIndex;
        }

        private float getScale(Resources res) {
            return mScaleIndex == ZOOM_FACTOR_NONE ? 1f :
                    res.getFraction(getResId(mScaleIndex), 1, 1);
        }

        @Override
        public void onItemFocused(View view, boolean hasFocus) {
            view.setSelected(hasFocus);
            getOrCreateAnimator(view).animateFocus(hasFocus, false);
        }

        @Override
        public void onInitializeView(View view) {
            getOrCreateAnimator(view).animateFocus(false, true);
        }

        private FocusAnimator getOrCreateAnimator(View view) {
            FocusAnimator animator = (FocusAnimator) view.getTag(R.id.tr_focus_animator);
            if (animator == null) {
                animator = new FocusAnimator(
                        view, getScale(view.getResources()), DURATION_MS);
                view.setTag(R.id.tr_focus_animator, animator);
            }
            return animator;
        }

    }

    /**
     * Sets up the focus highlight behavior of a focused item in browse list row.
     * @param zoomIndex scale type
     * @param adapter  adapter of the list row.
     */
    public static void setupItemBridgeFocusHighlight(ItemBridgeAdapter adapter, int zoomIndex) {
        adapter.setFocusHighlight(new ItemBridgeFocusHighlight(zoomIndex));
    }

}
