package com.pacmac.trackr;

import android.support.design.widget.AppBarLayout;


/**
 * Created by pacmac on 2017-09-16.
 */


public abstract class AppBarStateChangeListener implements AppBarLayout.OnOffsetChangedListener {

    public enum State {
        EXPANDED,
        COLLAPSED,
        IDLE
    }

    private int AlPHA_RANGE = 255;

    private State mCurrentState = State.IDLE;

    @Override
    public final void onOffsetChanged(AppBarLayout appBarLayout, int i) {


        if (i == 0) {
            if (mCurrentState != State.EXPANDED) {
                onStateChanged(appBarLayout, State.EXPANDED, 0);
            }
            mCurrentState = State.EXPANDED;
        } else if (Math.abs(i) >= appBarLayout.getTotalScrollRange()) {
            if (mCurrentState != State.COLLAPSED) {
                onStateChanged(appBarLayout, State.COLLAPSED, 0);
            }
            mCurrentState = State.COLLAPSED;
        } else {
            double x = (double) AlPHA_RANGE / appBarLayout.getTotalScrollRange();
            int alpha = AlPHA_RANGE - Math.abs((int) (x * i));

            if (alpha > 255) {
                alpha = 255;
            } else if (alpha < 0) {
                alpha = 0;
            }
            onStateChanged(appBarLayout, State.IDLE, alpha);
            mCurrentState = State.IDLE;
        }
    }

    public abstract void onStateChanged(AppBarLayout appBarLayout, State state, int alpha);
}
