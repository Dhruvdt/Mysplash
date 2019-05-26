package com.wangdaye.mysplash.me.ui.view;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;

import com.wangdaye.mysplash.R;
import com.wangdaye.mysplash.common.basic.model.PagerView;
import com.wangdaye.mysplash.common.ui.widget.SwipeBackCoordinatorLayout;
import com.wangdaye.mysplash.common.presenter.pager.PagerScrollablePresenter;
import com.wangdaye.mysplash.common.basic.model.PagerManageView;
import com.wangdaye.mysplash.common.ui.adapter.multipleState.LargeErrorStateAdapter;
import com.wangdaye.mysplash.common.ui.adapter.multipleState.LargeLoadingStateAdapter;
import com.wangdaye.mysplash.common.ui.widget.MultipleStateRecyclerView;
import com.wangdaye.mysplash.common.ui.widget.swipeRefreshView.BothWaySwipeRefreshLayout;
import com.wangdaye.mysplash.common.utils.BackToTopUtils;
import com.wangdaye.mysplash.common.utils.helper.RecyclerViewHelper;
import com.wangdaye.mysplash.common.utils.manager.ThemeManager;
import com.wangdaye.mysplash.common.presenter.pager.PagerStateManagePresenter;
import com.wangdaye.mysplash.me.ui.activity.MyFollowActivity;
import com.wangdaye.mysplash.me.ui.adapter.MyFollowAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * My follow user view.
 *
 * This view is used to show followers fo application user.
 *
 * */

@SuppressLint("ViewConstructor")
public class MyFollowUserView extends BothWaySwipeRefreshLayout
        implements PagerView, BothWaySwipeRefreshLayout.OnRefreshAndLoadListener,
        LargeErrorStateAdapter.OnRetryListener {

    @BindView(R.id.container_photo_list_recyclerView) MultipleStateRecyclerView recyclerView;

    private PagerStateManagePresenter stateManagePresenter;

    private boolean selected;
    private int index;
    private int userDeltaCount;
    private PagerManageView pagerManageView;

    public MyFollowUserView(MyFollowActivity a, int id, MyFollowAdapter adapter,
                            boolean selected, int index, PagerManageView v) {
        super(a);
        this.setId(id);
        this.init(adapter, selected, index, v);
    }

    // init.

    @SuppressLint("InflateParams")
    private void init(MyFollowAdapter adapter, boolean selected, int index, PagerManageView v) {
        View contentView = LayoutInflater.from(getContext())
                .inflate(R.layout.container_photo_list_2, null);
        addView(contentView);
        ButterKnife.bind(this, this);
        initData(selected, index, v);
        initView(adapter);
    }

    private void initData(boolean selected, int page, PagerManageView v) {
        this.selected = selected;
        this.index = page;
        this.userDeltaCount = 0;
        this.pagerManageView = v;
    }

    private void initView(MyFollowAdapter adapter) {
        setColorSchemeColors(ThemeManager.getContentColor(getContext()));
        setProgressBackgroundColorSchemeColor(ThemeManager.getRootColor(getContext()));
        setOnRefreshAndLoadListener(this);
        setRefreshEnabled(false);
        setLoadEnabled(false);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(
                new GridLayoutManager(
                        getContext(),
                        RecyclerViewHelper.getGirdColumnCount(getContext())
                )
        );
        recyclerView.setAdapter(
                new LargeLoadingStateAdapter(getContext(), 56),
                MultipleStateRecyclerView.STATE_LOADING
        );
        recyclerView.setAdapter(
                new LargeErrorStateAdapter(
                        getContext(), 56,
                        R.drawable.feedback_search,
                        getContext().getString(R.string.feedback_load_failed_tv),
                        getContext().getString(R.string.feedback_click_retry),
                        this
                ), MultipleStateRecyclerView.STATE_ERROR);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                PagerScrollablePresenter.onScrolled(
                        MyFollowUserView.this,
                        recyclerView,
                        adapter.getItemCount(),
                        pagerManageView,
                        index,
                        dy
                );
            }
        });

        recyclerView.setState(MultipleStateRecyclerView.STATE_LOADING);

        stateManagePresenter = new PagerStateManagePresenter(recyclerView);
    }

    public int getUserDeltaCount() {
        return userDeltaCount;
    }

    // interface.

    // pager view.

    @Override
    public State getState() {
        return stateManagePresenter.getState();
    }

    @Override
    public boolean setState(State state) {
        return stateManagePresenter.setState(state, selected);
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
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
        // do nothing.
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
        return stateManagePresenter.getState() != State.NORMAL
                || SwipeBackCoordinatorLayout.canSwipeBack(recyclerView, dir);
    }

    // on refresh an load listener.

    @Override
    public void onRefresh() {
        pagerManageView.onRefresh(index);
    }

    @Override
    public void onLoad() {
        pagerManageView.onLoad(index);
    }

    // on retry listener.

    @Override
    public void onRetry() {
        pagerManageView.onRefresh(index);
    }

    // item event callback.

    @Override
    public RecyclerView getRecyclerView() {
        return recyclerView;
    }
}
