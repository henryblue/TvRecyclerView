package app.com.tvrecyclerview;

/**
 * A {@link PresenterSelector} that always returns the same {@link Presenter}.
 * Useful for rows of items of the same type that are all rendered the same way.
 */
public final class SinglePresenterSelector extends PresenterSelector {

    private final Presenter mPresenter;

    /**
     * @param presenter The Presenter to return for every item.
     */
    public SinglePresenterSelector(Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public Presenter getPresenter(Object item) {
        return mPresenter;
    }

    @Override
    public Presenter[] getPresenters() {
        return new Presenter[]{mPresenter};
    }
}
