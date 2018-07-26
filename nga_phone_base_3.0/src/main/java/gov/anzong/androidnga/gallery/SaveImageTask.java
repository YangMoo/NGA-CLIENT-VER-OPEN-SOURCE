package gov.anzong.androidnga.gallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;

import org.apache.commons.io.FileUtils;

import java.io.File;

import gov.anzong.androidnga.R;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import sp.phone.common.ApplicationContextHolder;
import sp.phone.rxjava.BaseSubscriber;
import sp.phone.util.ActivityUtils;

/**
 */
public class SaveImageTask {

    private final Context mContext;

    private int mDownloadCount;

    private static final String PATH_IMAGES = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures/nga_open_source/";

    private boolean mTaskRunning;

    public SaveImageTask() {
        mContext = ApplicationContextHolder.getContext();
    }

    private static class DownloadResult {

        File file;

        String url;

        public DownloadResult(File file, String url) {
            this.file = file;
            this.url = url;
        }
    }

    public void execute(String... urls) {

        if (mTaskRunning) {
            ActivityUtils.showToast("图片正在下载，防止风怒！！");
            return;
        }

        mTaskRunning = true;
        mDownloadCount = 0;
        Observable.fromArray(urls)
                .observeOn(Schedulers.io())
                .map(new Function<String, DownloadResult>() {
                    @Override
                    public DownloadResult apply(String url) throws Exception {
                        File file = Glide
                                .with(mContext)
                                .load(url)
                                .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                                .get();
                        return new DownloadResult(file, url);
                    }
                })
                .map(new Function<DownloadResult, File>() {
                    @Override
                    public File apply(DownloadResult result) throws Exception {

                        String url = result.url;
                        String suffix = url.substring(url.lastIndexOf('.'));
                        String path = PATH_IMAGES + System.currentTimeMillis() + suffix;
                        File target = new File(path);
                        FileUtils.copyFile(result.file, target);
                        return target;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new BaseSubscriber<File>() {
                    @Override
                    public void onNext(File file) {
                        Uri uri = Uri.fromFile(file);
                        ApplicationContextHolder.getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                        mDownloadCount++;
                        if (mDownloadCount == urls.length) {
                            if (urls.length > 1) {
                                ActivityUtils.showToast("所有图片已保存");
                            } else {
                                ActivityUtils.showToast(mContext.getString(R.string.file_saved) + file.getAbsolutePath());
                            }
                            mTaskRunning = false;
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        mTaskRunning = false;
                        ActivityUtils.showToast("下载失败");
                    }

                });
    }


}
