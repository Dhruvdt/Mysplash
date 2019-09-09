package com.wangdaye.search.ui;

import android.annotation.SuppressLint;

import com.wangdaye.base.i.PagerManageView;
import com.wangdaye.common.base.adapter.footerAdapter.FooterAdapter;
import com.wangdaye.common.utils.helper.RecyclerViewHelper;
import com.wangdaye.search.R;
import com.wangdaye.search.SearchActivity;

import androidx.recyclerview.widget.RecyclerView;

@SuppressLint("ViewConstructor")
public class CollectionSearchPageView extends AbstractSearchPageView {

    public CollectionSearchPageView(SearchActivity a, FooterAdapter adapter,
                                    boolean selected, int index, PagerManageView v) {
        super(a, adapter, selected, index, v);
    }

    @Override
    protected RecyclerView.LayoutManager getLayoutManager() {
        return RecyclerViewHelper.getDefaultStaggeredGridLayoutManager(getContext());
    }

    @Override
    protected String getInitFeedbackText() {
        return getContext().getString(R.string.feedback_search_collections_tv);
    }
}
