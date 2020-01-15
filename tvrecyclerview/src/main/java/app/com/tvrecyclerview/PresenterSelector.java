package app.com.tvrecyclerview;

/**
 * A PresenterSelector is used to obtain a {@link Presenter} for a given Object.
 * Similar to {@link Presenter},  PresenterSelector is stateless.
 */
public abstract class PresenterSelector {
    /**
     * Returns a presenter for the given item.
     */
    public abstract Presenter getPresenter(Object item);

    /**
     * Returns an array of all possible presenters.  The returned array should
     * not be modified.
     */
    public Presenter[] getPresenters() {
        return null;
    }
}
