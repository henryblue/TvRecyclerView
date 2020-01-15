package app.com.tvrecyclerview;


import android.view.View;

/**
 * Interface for search next focus fail listener.
 */
public interface OnFocusSearchFailedListener {
    /**
     * Listener for focus search failed events
     * @param focused focused view
     * @param direction next search direct
     * @param position The position of the current focused view in the adapter
     * @param itemCount the ViewGroup child count
     */
    void onFocusSearchFailed(View focused, int direction, int position, int itemCount);
}
