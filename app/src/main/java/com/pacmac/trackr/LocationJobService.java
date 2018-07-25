package com.pacmac.trackr;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Handler;
import android.util.Log;


/**
 * Created by pacmac on 2018-07-24.
 */
public final class LocationJobService extends JobService implements TrackLocationUpdateListener {

    private static final String TAG = "LocationJobService";

    private JobParameters parameters;
    private boolean pendingLocationUpdate;
    private boolean isPermissionEnabled = true;
    private Handler handler = null;
    private Runnable finishJOB = new Runnable() {
        @Override
        public void run() {
            jobFinished(parameters,true);
        }
    };

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "onStartJob");
        this.parameters = params;
        isPermissionEnabled = Utility.checkSelfPermission(getApplicationContext(), Constants.LOCATION_PERMISSION);

        if (!isPermissionEnabled) {
            jobFinished(params, false);
            return false;
        }

        this.pendingLocationUpdate = true;
        // if Location Update is pending for more than 5 minutes finishJOB.
        if (pendingLocationUpdate) {
            handler = new Handler(getMainLooper());
            handler.postDelayed(finishJOB, 5 * 60 * 1000L);
        }
        LocationUpdate locationUpdate = LocationUpdate.getLocationUpdateInstance(getApplicationContext(), this);
        locationUpdate.getLocation();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "onStopJob");
        pendingLocationUpdate = false;
        return true;
    }

    @Override
    public void newLocationUploadFinished() {
        pendingLocationUpdate = false;
        if(handler != null) {
            handler.removeCallbacks(finishJOB);
        }
        jobFinished(parameters,true);
        Log.d(TAG, "newLocationUploadFinished");
    }
}
