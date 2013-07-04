package me.kstep.appshell;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;
import java.io.File;

public class AppEntry {
    private AppListLoader mLoader;
    private ApplicationInfo mInfo;
    private File mApkFile;
    private String mLabel;
    private boolean mMounted;
    private Drawable mIcon;

    public AppEntry(AppListLoader loader, ApplicationInfo info) {
        mLoader = loader;
        mInfo = info;
        mApkFile = new File(info.sourceDir);
    }

    public ApplicationInfo getAppInfo() {
        return mInfo;
    }

    public String getLabel() {
        return mLabel;
    }

    public Drawable getIcon() {
        if (mIcon == null) {
            if (mApkFile.exists()) {
                mIcon = mInfo.loadIcon(mLoader.mPm);
                return mIcon;
            } else {
                mMounted = false;
            }
        } else if (!mMounted) {
            if (mApkFile.exists()) {
                mMounted = true;
                mIcon = mInfo.loadIcon(mLoader.mPm);
                return mIcon;
            }
        } else {
            return mIcon;
        }

        return mLoader.getContext().getResources().getDrawable(android.R.drawable.sym_def_app_icon);
    }

    @Override
    public String toString() {
        return getLabel();
    }

    void loadLabel(Context context) {
        if (mLabel != null && mMounted) return;

        if (!mApkFile.exists()) {
            mMounted = false;
            mLabel = mInfo.packageName;
        } else {
            mMounted = true;
            CharSequence label = mInfo.loadLabel(context.getPackageManager());
            mLabel = label == null? mInfo.packageName: label.toString();
        }
    }

    public boolean runApp() {
        Intent intent = mLoader.mPm.getLaunchIntentForPackage(mInfo.packageName);
        if (intent != null) {
            try {
                mLoader.getContext().startActivity(intent);
            } catch (ActivityNotFoundException e) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    public void showAppDetails() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", mInfo.packageName, null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mLoader.getContext().startActivity(intent);
    }
}

