package com.wangdaye.main.multiFilter.ui;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import com.wangdaye.common.base.application.MysplashApplication;
import com.wangdaye.common.utils.BackToTopUtils;
import com.wangdaye.main.R;
import com.wangdaye.main.R2;
import com.wangdaye.base.i.PagerView;
import com.wangdaye.common.presenter.pager.PagerScrollablePresenter;
import com.wangdaye.base.i.PagerManageView;
import com.wangdaye.common.ui.adapter.photo.PhotoAdapter;
import com.wangdaye.common.ui.adapter.multipleState.LargeErrorStateAdapter;
import com.wangdaye.common.ui.adapter.multipleState.LargeLoadingStateAdapter;
import com.wangdaye.common.base.adapter.footerAdapter.GridMarginsItemDecoration;
import com.wangdaye.common.ui.widget.MultipleStateRecyclerView;
import com.wangdaye.common.ui.widget.swipeRefreshView.BothWaySwipeRefreshLayout;
import com.wangdaye.common.utils.helper.RecyclerViewHelper;
import com.wangdaye.common.utils.manager.ThemeManager;
import com.wangdaye.common.presenter.pager.PagerStateManagePresenter;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Multi-filter photos view.
 *
 * This view is used to search photos by multiple parameters for
 * {@link MultiFilterFragment}.
 *
 * */

public class MultiFilterPhotosView extends BothWaySwipeRefreshLayout
        implements PagerView, BothWaySwipeRefreshLayout.OnRefreshAndLoadListener,
        LargeErrorStateAdapter.OnRetryListener {

    @BindView(R2.id.container_photo_list_recyclerView) MultipleStateRecyclerView recyclerView;

    private View.OnClickListener hideKeyboardListener;
    private PagerStateManagePresenter stateManagePresenter;

    private PagerManageView pagerManageView;

    public MultiFilterPhotosView(Context context) {
        super(context);
        this.init();
    }

    public MultiFilterPhotosView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    // init.

    @SuppressLint("InflateParams")
    private void init() {
        View contentView = LayoutInflater.from(getContext())
                .inflate(R.layout.container_photo_list_2, null);
        addView(contentView);
        ButterKnife.bind(this, this);
        initView();
    }

    private void initView() {
        setColorSchemeColors(ThemeManager.getContentColor(getContext()));
        setProgressBackgroundColorSchemeColor(ThemeManager.getRootColor(getContext()));
        setOnRefreshAndLoadListener(this);
        setRefreshEnabled(false);
        setLoadEnabled(false);

        post(() -> setDragTriggerDistance(
                BothWaySwipeRefreshLayout.DIRECTION_BOTTOM,
                MysplashApplication.getInstance().getWindowInsets().bottom
                        + getResources().getDimensionPixelSize(R.dimen.normal_margin)
        ));

        recyclerView.setLayoutManager(
                RecyclerViewHelper.getDefaultStaggeredGridLayoutManager(getContext())
        );
        recyclerView.setAdapter(
                new LargeLoadingStateAdapter(getContext(), 160, v -> {
                    if (hideKeyboardListener != null) {
                        hideKeyboardListener.onClick(v);
                    }
                }), MultipleStateRecyclerView.STATE_LOADING
        );
        recyclerView.setAdapter(
                new LargeErrorStateAdapter(
                        getContext(), 160,
                        R.drawable.feedback_search,
                        getContext().getString(R.string.feedback_search_photos_tv),
                        getContext().getString(R.string.search),
                        false,
                        true,
                        v -> {
                            if (hideKeyboardListener != null) {
                                hideKeyboardListener.onClick(v);
                            }
                        }, this
                ), MultipleStateRecyclerView.STATE_ERROR
        );
        recyclerView.setState(MultipleStateRecyclerView.STATE_ERROR);

        stateManagePresenter = new PagerStateManagePresenter(recyclerView);
    }

    // control.

    public void setAdapter(PhotoAdapter adapter) {
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                PagerScrollablePresenter.onScrolled(
                        MultiFilterPhotosView.this,
                        recyclerView,
                        adapter.getRealItemCount(),
                        pagerManageView,
                        0,
                        dy
                );
            }
        });
        recyclerView.addItemDecoration(new GridMarginsItemDecoration(getContext()));
    }

    public void setPagerManageView(PagerManageView view) {
        pagerManageView = view;
    }

    public void setClickListenerForFeedbackView(View.OnClickListener l) {
        hideKeyboardListener = l;
    }

    // interface.

    // pager view.

    @Override
    public State getState() {
        return stateManagePresenter.getState();
    }

    @Override
    public boolean setState(State state) {
        boolean stateChanged = stateManagePresenter.setState(state, true);
        if (stateChanged && state == State.ERROR) {
            recyclerView.setAdapter(
                    new LargeErrorStateAdapter(
                            getContext(), 160,
                            R.drawable.feedback_search,
                            getContext().getString(R.string.feedback_search_failed_tv),
                            getContext().getString(R.string.feedback_click_retry),
                            true,
                            true,
                            v -> {
                                if (hideKeyboardListener != null) {
                                    hideKeyboardListener.onClick(v);
                                }
                            }, this
                    ), MultipleStateRecyclerView.STATE_ERROR
            );
            return true;
        }
        return stateChanged;
    }

    @Override
    public void setSelected(boolean selected) {
        // do nothing.
    }

    @Override
    public void setSwipeRefreshing(boolean refreshing) {
        setRefreshing(refreshing);
    }

    @Override
    public void setSwipeLoading(boolean loading) {
        setLoading(loading);
    }

    @Override
    public void setPermitSwipeRefreshing(boolean permit) {
        setRefreshEnabled(permit);
    }

    @Override
    public void setPermitSwipeLoading(boolean permit) {
        setLoadEnabled(permit);
    }

    @Override
    public boolean checkNeedBackToTop() {
        return recyclerView.canScrollVertically(-1)
                && stateManagePresenter.getState() == State.NORMAL;
    }

    @Override
    public void scrollToPageTop() {
        BackToTopUtils.scrollToTop(recyclerView);
    }

    @Override
    public boolean canSwipeBack(int dir) {
        return false;
    }

    @Override
    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    // on refresh and load listener.

    @Override
    public void onRefresh() {
        pagerManageView.onRefresh(0);
    }

    @Override
    public void onLoad() {
        pagerManageView.onLoad(0);
    }

    // on retry listener.

    @Override
    public void onRetry() {
        pagerManageView.onRefresh(0);
    }
}
