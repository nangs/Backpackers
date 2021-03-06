package com.backpackers.android.framework.base;

public interface BaseMvpDataView<M> extends BaseMvpView {

    /**
     * Load the data. Typically invokes the presenter method to list the desired data.
     * <p>
     * <b>Should not be called from presenter</b> to prevent infinity loops. The method is declared
     * in
     * the views interface to addForumComment support for view state easily.
     * </p>
     *
     * @param isPullToRefresh true, if triggered by a pull to refresh. Otherwise false.
     */
    void onLoadStarted(boolean isPullToRefresh, String cursor);

    /**
     * Display a loading view while loading data in background.
     * <b>The loading view must have the id = R.id.loadingView</b>
     *
     * @param isPullToRefresh true, if pull-to-refresh has been invoked loading.
     */
    void onLoading(boolean isPullToRefresh);

    /**
     * Show the error view.
     * <b>The error view must be a TextView with the id = R.id.errorView</b>
     *
     * @param e             The Throwable that has caused this error
     */
    void onError(Throwable e);

    /**
     * Show the empty view.
     * If no data is arrived then show the empty view.
     */
    void onEmpty();

    /**
     * The data that should be displayed with {@link #onLoadFinished()}
     */
    void onDataArrived(M data);

    /**
     * Show the content view.
     * <p>
     * <b>The content view must have the id = R.id.contentView</b>
     */
    void onLoadFinished();
}
