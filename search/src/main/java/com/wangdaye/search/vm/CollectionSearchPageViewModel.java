package com.wangdaye.search.vm;

import com.wangdaye.base.unsplash.Collection;
import com.wangdaye.common.bus.event.CollectionEvent;
import com.wangdaye.common.presenter.event.CollectionEventResponsePresenter;
import com.wangdaye.search.repository.CollectionSearchPageViewRepository;

import javax.inject.Inject;

public class CollectionSearchPageViewModel extends AbstractSearchPageViewModel<Collection, CollectionEvent> {
    
    private CollectionSearchPageViewRepository repository;
    private CollectionEventResponsePresenter presenter;

    @Inject
    public CollectionSearchPageViewModel(CollectionSearchPageViewRepository repository,
                                         CollectionEventResponsePresenter presenter) {
        super(CollectionEvent.class);
        this.repository = repository;
        this.presenter = presenter;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cancel();
        presenter.clearResponse();
    }

    @Override
    public void refresh() {
        repository.getCollections(getListResource(), getQuery(), true);
    }

    @Override
    public void load() {
        repository.getCollections(getListResource(), getQuery(), false);
    }

    // interface.

    @Override
    public void accept(CollectionEvent collectionEvent) {
        switch (collectionEvent.event) {
            case UPDATE:
                presenter.updateCollection(getListResource(), collectionEvent.collection);
                break;

            case DELETE:
                presenter.deleteCollection(getListResource(), collectionEvent.collection);
                break;
        }
    }
}
