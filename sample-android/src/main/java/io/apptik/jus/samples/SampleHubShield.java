package io.apptik.jus.samples;


import io.apptik.comm.jus.RequestQueue;
import io.apptik.comm.jus.rx.RxQueueHub;
import io.apptik.comm.jus.rx.event.ResultEvent;
import io.apptik.json.JsonArray;
import rx.Observable;

import static io.apptik.jus.samples.api.Instructables.REQ_LIST;

public class SampleHubShield {
    private RxQueueHub hub;

    public SampleHubShield(RequestQueue q) {
        this.hub = new RxQueueHub(q);
    }

    Observable<JsonArray> getList() {
        return hub.getResults(REQ_LIST).map(o -> ((ResultEvent<JsonArray>)o).response);
    }
 }
