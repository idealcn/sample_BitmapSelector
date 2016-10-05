package com.idealcn.bitmapselector;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * author:idealgn
 * date:16-10-4 下午5:10
 */
public class ImageLoader {

    private static ImageLoader loader;

    /*
    默认开启的线程数
     */
    private static final int DEFAULT_THREAD_COUNT = 5;

    private LruCache<String, Bitmap> mLruCache;
    /*
    线程池
     */
    private ExecutorService mThreadPool;
    /*
    任务队列
     */
    private LinkedList<Runnable> mTaskQueue;
    /*
    后台轮询线程
     */
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;
    /*
    UI线程的handler
     */
    private Handler mUIHandler;
    /*
    队列调度方式
     */
    private Type mType = Type.LIFO;

    private Semaphore semaphore = new Semaphore(0);

    public enum Type {
        LIFO, FIFO;
    }

    private ImageLoader() {
    }

    private ImageLoader(int threadCount, Type type) {
        init(threadCount, type);
    }


    public synchronized static ImageLoader getLoader() {
        if (loader == null)
            loader = new ImageLoader(DEFAULT_THREAD_COUNT, Type.LIFO);
        return loader;
    }

    private void init(int threadCount, Type type) {
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        mLruCache = new LruCache<String, Bitmap>(maxMemory / 8) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getHeight() * value.getRowBytes();
            }
        };
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList<>();
        mType = type;

        //后台轮询线程
        mPoolThread = new Thread() {
            @Override
            public void run() {
                super.run();
                Looper.prepare();
                mPoolThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        mThreadPool.execute(getTask());
                    }
                };
                semaphore.release();
                Looper.loop();
            }
        };
        mPoolThread.start();
    }

    private Runnable getTask() {
        if (mType == Type.FIFO)
            return mTaskQueue.removeFirst();
        else if (mType == Type.LIFO)
            return mTaskQueue.removeLast();
        return null;
    }

    public void loadImage(final String path, final ImageView view) {
        view.setTag(path);
        if (mUIHandler == null) {
            mUIHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    ImageBeanHolder holder = (ImageBeanHolder) msg.obj;
                    Bitmap bitmap = holder.bitmap;
                    ImageView imageView = holder.imageView;
                    String p = holder.path;
                    if (imageView.getTag().toString().equals(p)) {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            };
        }

        Bitmap bitmap = getBitmapFromCache(path);
        if (bitmap != null) {
            refreshBitmap(path, view, bitmap);
        }else {
            addTasks(new Runnable() {
                @Override
                public void run() {
                    ImageSize imageSize = getImageViewSize(view);
                    //解析图片并把图片加入缓存
                    Bitmap bitmap = decodeBitmap(path, imageSize.width, imageSize.height);
                    mLruCache.put(path, bitmap);
                    refreshBitmap(path, view, bitmap);
                }

            });
        }
    }

    private void refreshBitmap(String path, ImageView view, Bitmap bitmap) {
        Message message = Message.obtain();
        ImageBeanHolder holder = new ImageBeanHolder();
        holder.bitmap = bitmap;
        holder.imageView = view;
        holder.path = path;
        message.obj = holder;
        mUIHandler.sendMessage(message);
    }

    /**
     * 解析图片
     *
     * @param path
     */
    private Bitmap decodeBitmap(String path, int reqWidth, int reqHeight) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opt);
        int sampleSize = 1;
        int height = opt.outHeight;
        int width = opt.outWidth;
        if (width > reqWidth || height > reqHeight) {
            int scaleH = Math.round(height * 0.1f / reqHeight);
            int scaleW = Math.round(width * 0.1f / reqWidth);
            sampleSize = Math.max(scaleH, scaleW);
        }
        opt.inSampleSize = sampleSize;
        opt.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, opt);
        return bitmap;
    }

    /**
     * 得到图片大小
     *
     * @param view
     * @return
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    protected ImageSize getImageViewSize(ImageView view) {
        DisplayMetrics metrics = view.getContext().getResources().getDisplayMetrics();
        ImageSize imageSize = new ImageSize();
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        int height = view.getHeight();
        if (height <= 0) {
            height = lp.height;
        }
        if (height <= 0) {
            height = view.getMaxHeight();
        }
        if (height <= 0) {
            height = metrics.heightPixels;
        }
        int width = view.getWidth();
        if (width <= 0)
            width = lp.width;
        if (width <= 0)
            width = view.getMaxWidth();
        if (width <= 0)
            width = metrics.widthPixels;
        imageSize.height = height;
        imageSize.width = width;
        return imageSize;
    }

    private synchronized void addTasks(Runnable task) {
        mTaskQueue.add(task);
        if (mPoolThreadHandler==null)
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        mPoolThreadHandler.sendEmptyMessage(0x110);
    }

    private Bitmap getBitmapFromCache(String path) {
        return mLruCache.get(path);
    }

    private class ImageSize {
        int width;
        int height;
    }

    private class ImageBeanHolder {
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }

}
