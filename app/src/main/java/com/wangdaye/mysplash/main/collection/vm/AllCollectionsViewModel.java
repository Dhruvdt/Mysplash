package com.wangdaye.mysplash.main.collection.vm;

import com.wangdaye.mysplash.common.presenter.event.CollectionEventResponsePresenter;
import com.wangdaye.mysplash.main.collection.CollectionsViewRepository;

import javax.inject.Inject;

public class AllCollectionsViewModel extends AbstractCollectionsViewModel {

    @Inject
    public AllCollectionsViewModel(CollectionsViewRepository repository,
                                   CollectionEventResponsePresenter presenter) {
        super(repository, presenter);
    }

    @Override
    public void refresh() {
        getRepository().getAllCollections(getListResource(), true);
    }

    @Override
    public void load() {
        getRepository().getAllCollections(getListResource(), false);
    }
}
