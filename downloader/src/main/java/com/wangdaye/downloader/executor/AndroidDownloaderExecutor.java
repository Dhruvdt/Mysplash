package com.wangdaye.downloader.executor;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wangdaye.common.R;
import com.wangdaye.common.base.widget.FlagRunnable;
import com.wangdaye.base.DownloadTask;
import com.wangdaye.common.utils.helper.NotificationHelper;
import com.wangdaye.common.utils.FileUtils;
import com.wangdaye.common.utils.manager.ThreadManager;
import com.wangdaye.component.ComponentFactory;
import com.wangdaye.downloader.base.OnDownloadListener;

import java.util.List;

public class AndroidDownloaderExecutor extends AbstractDownloaderExecutor {

    private static AndroidDownloaderExecutor instance;

    public static AndroidDownloaderExecutor getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (AndroidDownloaderExecutor.class) {
                if (instance == null) {
                    instance = new AndroidDownloaderExecutor(context);
                }
            }
        }
        return instance;
    }

    @Nullable private DownloadManager downloadManager;
    private List<DownloadTask> downloadingList;

    private Handler handler;

    @Nullable private PollingRunnable runnable;
    private class PollingRunnable extends FlagRunnable {

        @Override
        public void run() {
            while (isRunning()) {
                synchronizedExecuteRunnable(listenerList -> {
                    for (int i = listenerList.size() - 1; i >= 0; i --) {
                        if (listenerList.get(i).result != DownloadTask.RESULT_DOWNLOADING) {
                            listenerList.remove(i);
                            if (listenerList.size() == 0) {
                                setRunning(false);
                                runnable = null;
                                return;
                            }
                        } else {
                            final PollingResult targetResult = getDownloadInformation(listenerList.get(i).taskId);
                            final OnDownloadListener targetListener = listenerList.get(i);
                            handler.post(() -> {
                                targetListener.result = targetResult.result;
                                if (targetResult.result == DownloadTask.RESULT_DOWNLOADING) {
                                    targetListener.onProcess(targetResult.process);
                                } else {
                                    targetListener.onComplete(targetResult.result);
                                }
                            });
                        }
                    }
                });
                SystemClock.sleep(300);
            }
        }
    }

    private class PollingResult {

        @DownloadTask.DownloadResultRule int result;
        float process;

        PollingResult(@DownloadTask.DownloadResultRule int result, float process) {
            this.result = result;
            this.process = process;
        }
    }

    private AndroidDownloaderExecutor(Context context) {
        super();
        this.downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        this.downloadingList = ComponentFactory.getDatabaseService()
                .readDownloadTaskList(DownloadTask.RESULT_DOWNLOADING);
        this.handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public long addTask(Context c, @NonNull DownloadTask task, boolean showSnackbar) {
        if (downloadManager == null) {
            showErrorNotification();
            return -1;
        }

        synchronizedExecuteRunnable(listenerList -> {
            FileUtils.deleteFile(task);

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(task.downloadUrl))
                    .setTitle(task.getNotificationTitle())
                    .setDescription(c.getString(R.string.feedback_downloading))
                    .setDestinationInExternalPublicDir(
                            DownloadTask.DOWNLOAD_PATH,
                            task.title + task.getFormat()
                    );
            request.allowScanningByMediaScanner();

            task.taskId = downloadManager.enqueue(request);
            task.result = DownloadTask.RESULT_DOWNLOADING;
            ComponentFactory.getDatabaseService().writeDownloadTask(task);
            registerDownloadingTask(task);
        });

        if (showSnackbar) {
            NotificationHelper.showSnackbar(c.getString(R.string.feedback_download_start));
        }

        return task.taskId;
    }

    @Override
    public long restartTask(Context c, @NonNull DownloadTask task) {
        if (downloadManager == null) {
            showErrorNotification();
            return -1;
        }

        synchronizedExecuteRunnable(listenerList -> {
            downloadManager.remove(task.taskId);
            ComponentFactory.getDatabaseService().deleteDownloadTask(task.taskId);
            unregisterDownloadingTask(task);
        });

        return addTask(c, task, true);
    }

    @Override
    public void completeTask(Context c, @NonNull DownloadTask task) {
        if (isMissionSuccess(task.taskId)) {
            if (task.downloadType != DownloadTask.COLLECTION_TYPE) {
                downloadPhotoSuccess(c, task);
            } else {
                downloadCollectionSuccess(c, task);
            }
            updateTaskResult(c, task, DownloadTask.RESULT_SUCCEED);
        } else {
            if (task.downloadType != DownloadTask.COLLECTION_TYPE) {
                downloadPhotoFailed(c, task);
            } else {
                downloadCollectionFailed(c, task);
            }
            updateTaskResult(c, task, DownloadTask.RESULT_FAILED);
        }
    }

    @Override
    public void removeTask(Context c, @NonNull DownloadTask task, boolean deleteEntity) {
        if (downloadManager == null) {
            showErrorNotification();
            return;
        }

        synchronizedExecuteRunnable(listenerList -> {
            if (task.result != DownloadTask.RESULT_SUCCEED) {
                downloadManager.remove(task.taskId);
            }
            if (deleteEntity) {
                ComponentFactory.getDatabaseService().deleteDownloadTask(task.taskId);
            }
            unregisterDownloadingTask(task);
        });
    }

    @Override
    public void clearTask(Context c, @NonNull List<DownloadTask> taskList) {
        if (downloadManager == null) {
            showErrorNotification();
            return;
        }

        synchronizedExecuteRunnable(listenerList -> {
            for (int i = 0; i < taskList.size(); i ++) {
                if (taskList.get(i).result != DownloadTask.RESULT_SUCCEED) {
                    downloadManager.remove(taskList.get(i).taskId);
                }
            }

            ComponentFactory.getDatabaseService().clearDownloadTask();
            clearDwonloadingTask();
        });
    }

    @Override
    public void updateTaskResult(Context c, @NonNull DownloadTask task, int result) {
        task.result = result;
        synchronizedExecuteRunnable(listenerList -> {
            ComponentFactory.getDatabaseService().updateDownloadTask(task);
            unregisterDownloadingTask(task);
        });
    }

    @Override
    public float getTaskProcess(Context c, @NonNull DownloadTask entity) {
        Cursor cursor = getMissionCursor(entity.taskId);
        if (cursor != null) {
            float process = getMissionProcess(cursor);
            cursor.close();
            return process;
        }
        return -1;
    }

    @Override
    public boolean isDownloading(String title) {
        final boolean[] result = new boolean[1];
        synchronizedExecuteRunnable(listenerList -> {
            if (downloadingList.size() == 0) {
                result[0] = false;
                return;
            }

            for (int i = 0; i < downloadingList.size(); i ++) {
                if (downloadingList.get(i).title.equals(title)) {
                    result[0] = true;
                    return;
                }
            }
            result[0] = false;
        });
        return result[0];
    }

    @Override
    public void addOnDownloadListener(@NonNull OnDownloadListener l) {
        super.addOnDownloadListener(l, listenerList -> {
            if (runnable == null || !runnable.isRunning()) {
                runnable = new PollingRunnable();
                ThreadManager.getInstance().execute(runnable);
            }
        });
    }

    @Override
    public void addOnDownloadListener(@NonNull List<? extends OnDownloadListener> list) {
        super.addOnDownloadListener(list, listenerList -> {
            if (runnable == null || !runnable.isRunning()) {
                runnable = new PollingRunnable();
                ThreadManager.getInstance().execute(runnable);
            }
        });
    }

    @Override
    public void removeOnDownloadListener(@NonNull OnDownloadListener l) {
        super.removeOnDownloadListener(l, listenerList -> {
            if (listenerList.size() == 0 && runnable != null && runnable.isRunning()) {
                runnable.setRunning(false);
                runnable = null;
            }
        });
    }

    @Override
    public void removeOnDownloadListener(@NonNull List<? extends OnDownloadListener> list) {
        super.removeOnDownloadListener(list, listenerList -> {
            if (listenerList.size() == 0 && runnable != null && runnable.isRunning()) {
                runnable.setRunning(false);
                runnable = null;
            }
        });
    }

    private void showErrorNotification() {
        NotificationHelper.showSnackbar("Cannot get DownloadManager.");
    }

    private boolean isMissionSuccess(long id) {
        Cursor cursor = getMissionCursor(id);
        if (cursor != null) {
            int result = getDownloadResult(cursor);
            cursor.close();
            return result == DownloadTask.RESULT_SUCCEED;
        } else {
            return true;
        }
    }

    @DownloadTask.DownloadResultRule
    private int getDownloadResult(@NonNull Cursor cursor) {
        switch (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
            case DownloadManager.STATUS_SUCCESSFUL:
                return DownloadTask.RESULT_SUCCEED;

            case DownloadManager.STATUS_FAILED:
            case DownloadManager.STATUS_PAUSED:
                return DownloadTask.RESULT_FAILED;

            default:
                return DownloadTask.RESULT_DOWNLOADING;
        }
    }

    @Nullable
    private Cursor getMissionCursor(long id) {
        if (downloadManager == null) {
            NotificationHelper.showSnackbar("Cannot get DownloadManager.");
            return null;
        }

        Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(id));
        if (cursor == null) {
            return null;
        } else if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            return cursor;
        } else {
            cursor.close();
            return null;
        }
    }

    private PollingResult getDownloadInformation(long missionId) {
        Cursor cursor = getMissionCursor(missionId);
        if (cursor != null) {
            PollingResult result = new PollingResult(
                    getDownloadResult(cursor),
                    getMissionProcess(cursor)
            );
            cursor.close();
            return result;
        }
        return new PollingResult(DownloadTask.RESULT_SUCCEED, 100);
    }

    private float getMissionProcess(@NonNull Cursor cursor) {
        long soFar = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
        long total = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
        int result = (int) (100.0 * soFar / total);
        result = Math.max(0, result);
        result = Math.min(100, result);
        return result;
    }

    private void registerDownloadingTask(@NonNull DownloadTask task) {
        downloadingList.add(task);
    }

    private void unregisterDownloadingTask(@NonNull DownloadTask task) {
        for (int i = 0; i < downloadingList.size(); i ++) {
            if (downloadingList.get(i).taskId == task.taskId) {
                downloadingList.remove(i);
                break;
            }
        }
    }

    private void clearDwonloadingTask() {
        downloadingList.clear();
    }
}
