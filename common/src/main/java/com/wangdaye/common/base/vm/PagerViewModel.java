package com.wangdaye.common.base.vm;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.wangdaye.base.resource.ListResource;

/**
 * Pager view model.
 * */
public abstract class PagerViewModel<T> extends ViewModel {

    private MutableLiveData<ListResource<T>> listResource;

    public PagerViewModel() {
        this.listResource = null;
    }

    protected boolean init(@NonNull ListResource<T> resource) {
        if (listResource == null) {
            listResource = new MutableLiveData<>();
            listResource.setValue(resource);
            return true;
        }
        return false;
    }

    public abstract void refresh();

    public abstract void load();

    public MutableLiveData<ListResource<T>> getListResource() {
        return listResource;
    }

    public void setListResource(ListResource<T> resource) {
        listResource.setValue(resource);
    }
}
