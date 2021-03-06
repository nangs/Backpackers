package com.backpackers.android.framework.base;

public interface MvpPresenter<V extends BaseMvpView> {
    void onViewAttached(V view);

    void onViewDetached();

    void onDestroyed();
}
