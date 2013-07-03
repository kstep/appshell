package me.kstep.appshell;

import android.content.AsyncTaskLoader;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class AppListLoader extends AsyncTaskLoader<List<AppEntry>> {

    public class AppBroadcastReceiver extends BroadcastReceiver {
        public AppBroadcastReceiver() {
            AppListLoader loader = AppListLoader.this;

            IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addDataScheme("package");
            loader.getContext().registerReceiver(this, filter);

            // Register for events related to sdcard installation.
            IntentFilter sdFilter = new IntentFilter();
            sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
            sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
            loader.getContext().registerReceiver(this, sdFilter);
        }

        @Override public void onReceive(Context context, Intent intent) {
            // Tell the loader about the change.
            AppListLoader.this.onContentChanged();
        }
    }

    public static class InterestingConfigChanges {
        final Configuration mLastConfiguration = new Configuration();
        int mLastDensity;

        boolean applyNewConfig(Resources res) {
            int configChanges = mLastConfiguration.updateFrom(res.getConfiguration());
            boolean densityChanged = mLastDensity != res.getDisplayMetrics().densityDpi;
            if (densityChanged || (configChanges&(ActivityInfo.CONFIG_LOCALE
                            | ActivityInfo.CONFIG_UI_MODE | ActivityInfo.CONFIG_SCREEN_LAYOUT)) != 0) {
                mLastDensity = res.getDisplayMetrics().densityDpi;
                return true;
                            }
            return false;
        }
    }

    final PackageManager mPm;
    final InterestingConfigChanges mLastConfig = new InterestingConfigChanges();
    AppBroadcastReceiver mAppsObserver;
    List<AppEntry> mApps;

    public AppListLoader(Context context) {
        super(context);

        mPm = getContext().getPackageManager();
    }

    @Override
    public List<AppEntry> loadInBackground() {
        List<ApplicationInfo> apps = mPm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES | PackageManager.GET_DISABLED_COMPONENTS);

        if (apps == null) {
            apps = new ArrayList<ApplicationInfo>();
        }

        final Context context = getContext();

        List<AppEntry> entries = new ArrayList<AppEntry>(apps.size());

        for (ApplicationInfo app : apps) {
            AppEntry entry = new AppEntry(this, app);
            entry.loadLabel(context);
            entries.add(entry);
        }

        Collections.sort(entries, ALPHA_APP_COMPARATOR);

        return entries;
    }

    public static final Comparator<AppEntry> ALPHA_APP_COMPARATOR = new Comparator<AppEntry>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(AppEntry a, AppEntry b) {
            return sCollator.compare(a.getLabel(), b.getLabel());
        }
    };

    @Override
    public void deliverResult(List<AppEntry> apps) {
        if (isReset()) {
            if (apps != null) {
                onReleaseResources(apps);
            }
        }

        List<AppEntry> oldApps = apps;
        mApps = apps;

        if (isStarted()) {
            super.deliverResult(apps);
        }

        if (oldApps != null) {
            onReleaseResources(oldApps);
        }
    }

    @Override
    protected void onReset() {
        super.onReset();

        onStopLoading();

        if (mApps != null) {
            onReleaseResources(mApps);
            mApps = null;
        }
    }

    @Override
    protected void onStartLoading() {
        if (mApps != null) {
            deliverResult(mApps);
        }

        if (mAppsObserver == null) {
            mAppsObserver = this.new AppBroadcastReceiver();
        }

        boolean configChanged = mLastConfig.applyNewConfig(getContext().getResources());

        if (configChanged || mApps == null || takeContentChanged()) {
            forceLoad();
        }
    }

    @Override
    public void onCanceled(List<AppEntry> apps) {
        super.onCanceled(apps);
        onReleaseResources(apps);
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    protected void onReleaseResources(List<AppEntry> apps) {
        // nothing
    }
}

