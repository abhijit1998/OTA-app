package com.abhi.updater;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PeriodicJob extends JobService {
    private static final String TAG = "Updater_PeriodicJob";
    private static final int JOB_ID_PERIODIC = 1;
    private static final int JOB_ID_RETRY = 2;
    private static final long MIN_LATENCY_MILLIS = 4 * 60 * 1000;



    static void schedule(final Context context) {
        final JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (!Settings.getAutoUpdate(context)) {
            scheduler.cancel(JOB_ID_PERIODIC);
            scheduler.cancel(JOB_ID_RETRY);
            return;
        }
        final int networkType = Settings.getNetworkType(context);
        final String checktime1  = Settings.getCheckTime(context);
        long checktime = Long.valueOf(checktime1);
        final boolean batteryNotLow = Settings.getBatteryNotLow(context);
        final JobInfo jobInfo = scheduler.getPendingJob(JOB_ID_PERIODIC);
        if (jobInfo != null &&
                jobInfo.getNetworkType() == networkType &&
                jobInfo.isRequireBatteryNotLow() == batteryNotLow &&
                jobInfo.isPersisted() &&
                jobInfo.getIntervalMillis() ==  checktime) {
            Log.d(TAG, "Periodic job already registered");
            return;
        }
        final ComponentName serviceName = new ComponentName(context, PeriodicJob.class);
        final int result = scheduler.schedule(new JobInfo.Builder(JOB_ID_PERIODIC, serviceName)
            .setRequiredNetworkType(networkType)
            .setRequiresBatteryNotLow(batteryNotLow)
            .setPersisted(true)
            .setPeriodic(checktime)
            .build());
        if (result == JobScheduler.RESULT_FAILURE) {
            Log.d(TAG, "Periodic job schedule failed");
        }
    }

    static void scheduleRetry(final Context context) {
        final JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        final ComponentName serviceName = new ComponentName(context, PeriodicJob.class);
        final int result = scheduler.schedule(new JobInfo.Builder(JOB_ID_RETRY, serviceName)
            .setRequiredNetworkType(Settings.getNetworkType(context))
            .setRequiresBatteryNotLow(Settings.getBatteryNotLow(context))
            .setMinimumLatency(MIN_LATENCY_MILLIS)
            .build());
        if (result == JobScheduler.RESULT_FAILURE) {
            Log.d(TAG, "Retry job schedule failed");
        }
    }

    static void cancel(final Context context) {
        final JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        scheduler.cancel(JOB_ID_PERIODIC);
        scheduler.cancel(JOB_ID_RETRY);
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        Log.d(TAG, "onStartJob id: " + params.getJobId());
        sendBroadcast(new Intent(this, TriggerUpdateReceiver.class));
        return false;
    }

    @Override
    public boolean onStopJob(final JobParameters params) {
        return false;
    }
}
