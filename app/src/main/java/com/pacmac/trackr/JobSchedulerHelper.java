package com.pacmac.trackr;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

/**
 * Created by pacmac on 2018-07-24.
 */

public final class JobSchedulerHelper {

    private static final int LOCATION_JOB_ID = 4553;

    public static final void scheduleLocationUpdateJOB(Context context, long period) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(LOCATION_JOB_ID);
        ComponentName serviceComponent = new ComponentName(context, LocationJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(LOCATION_JOB_ID, serviceComponent)
                .setPeriodic(period)
                .setPersisted(true);
        JobInfo jobInfo = builder.build();
        jobScheduler.schedule(jobInfo);
    }

    public static final void cancelLocationUpdateJOB(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(LOCATION_JOB_ID);
    }

}
