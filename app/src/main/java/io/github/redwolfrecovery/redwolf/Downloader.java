package io.github.redwolfrecovery.redwolf;

import android.support.v7.app.AlertDialog;

import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.FetchListener;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Devil7DK on 4/3/18
 **/
public class Downloader extends AlertDialog.Builder implements FetchListener{
    @Override
    public void onQueued(@NotNull Download download) {
    }

    @Override
    public void onCompleted(@NotNull Download download) {
    }

    @Override
    public void onError(@NotNull Download download) {
    }

    @Override
    public void onProgress(@NotNull Download download, long etaInMilliseconds, long downloadedBytesPerSecond) {
    }

    @Override
    public void onPaused(@NotNull Download download) {
    }

    @Override
    public void onResumed(@NotNull Download download) {
    }

    @Override
    public void onCancelled(@NotNull Download download) {
    }

    @Override
    public void onRemoved(@NotNull Download download) {
    }

    @Override
    public void onDeleted(@NotNull Download download) {
    }
}
