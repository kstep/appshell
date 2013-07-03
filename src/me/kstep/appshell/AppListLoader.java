package me.kstep.appshell;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import java.util.ArrayList;
import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class AppListLoader extends AsyncTaskLoader<List<AppEntry>> {
    final PackageManager mPm;
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

        if (takeContentChanged() || mApps == null) {
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

