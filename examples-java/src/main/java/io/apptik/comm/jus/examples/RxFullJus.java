package io.apptik.comm.jus.examples;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.apptik.comm.jus.Jus;
import io.apptik.comm.jus.Request;
import io.apptik.comm.jus.RequestQueue;
import io.apptik.comm.jus.converter.Converters;
import io.apptik.comm.jus.http.HttpUrl;
import io.apptik.comm.jus.rx.event.JusEvent;
import io.apptik.comm.jus.rx.queue.RxRequestQueue;
import rx.Observer;

public class RxFullJus {
    public static void main(String[] args) {
        RequestQueue queue = Jus.newRequestQueue(new File("."));
        Set<String> opts = new HashSet<>();
        if (args != null) {
            Collections.addAll(opts, args);
        }

        if (opts.contains("beer")) {
            queue.add(new Request<String>(
                            Request.Method.GET,
                            HttpUrl.parse(BeerService.fullUrl),
                            new Converters.StringResponseConverter())
            );
        }

        RxRequestQueue.allEventsObservable(queue, null).subscribe(new Observer<JusEvent>() {

            @Override
            public void onCompleted() {
                System.out.println("RX: Completed");
            }

            @Override
            public void onError(Throwable e) {
                System.out.println("RX: Error: " + e);
            }

            @Override
            public void onNext(JusEvent jusEvent) {
                System.out.println("RX: Event: " + jusEvent);
            }
        });

        queue.stopWhenDone();
    }
}
