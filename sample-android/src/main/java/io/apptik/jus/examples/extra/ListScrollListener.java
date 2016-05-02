/*
 * Copyright (C) 2015 AppTik Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apptik.jus.examples.extra;

import android.support.v7.widget.RecyclerView;


public class ListScrollListener extends RecyclerView.OnScrollListener {
    private long previousEventTime = System.nanoTime();
    private double speed = 0;

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        if (dy > 0) {
            long currTime = System.nanoTime();
            long timeToScrollOneElement = currTime - previousEventTime;
            //calc px/ms
            speed = ((double) dy / timeToScrollOneElement) * 1000 * 1000;

            previousEventTime = currTime;
        }

    }

    public double getSpeed() {
        return speed;
    }
}
