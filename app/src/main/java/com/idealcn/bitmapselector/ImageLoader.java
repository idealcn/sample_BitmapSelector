package com.idealcn.bitmapselector;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    public enum Type {
        LIFO, FIFO;
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
        mThreadPool = Executors.newFixedThreadPool(DEFAULT_THREAD_COUNT);

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
            }
        };
    }

    private Runnable getTask() {
        if (mType == Type.FIFO)
            return mTaskQueue.removeFirst();
        else if (mType == Type.LIFO)
            return mTaskQueue.removeLast();
        return null;
    }

    public void loadImage(String path, final ImageView view) {
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
                    if (imageView.getTag().equals(p)) {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            };
        } else {
            addTasks(new Runnable() {
                @Override
                public void run() {
                    getImageViewSize(view);
                }
            });
        }

        Bitmap bitmap = getBitmapFromCache(path);
        if (bitmap != null) {
            Message message = Message.obtain();
            ImageBeanHolder holder = new ImageBeanHolder();
            holder.bitmap = bitmap;
            holder.imageView = view;
            holder.path = path;
            message.obj = holder;
            mUIHandler.sendMessage(message);
        }
    }

    protected ImageSize getImageViewSize(ImageView view) {
        ImageSize imageSize = new ImageSize();
        ViewGroup.LayoutParams lp = view.getLayoutParams();

        return imageSize;
    }

    private void addTasks(Runnable task) {
        mTaskQueue.add(task);
        mPoolThreadHandler.sendEmptyMessage(0);
    }

    private Bitmap getBitmapFromCache(String path) {
        return mLruCache.get(path);
    }

    private class ImageSize{
        int width;
        int height;
    }
    private class ImageBeanHolder {
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }

}
