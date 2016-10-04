package com.idealcn.bitmapselector;

import android.graphics.Bitmap;
import android.os.Handler;
import android.util.LruCache;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * author:idealgn
 * date:16-10-4 下午5:10
 */
public class ImageLoader {

    private static ImageLoader loader;


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

    public enum Type{
        LIFO,FIFO;
    }


    private ImageLoader(int threadCount,Type type) {
        init(threadCount,type);
    }



    public synchronized static ImageLoader getLoader() {
        if (loader == null)
            loader = new ImageLoader(DEFAULT_THREAD_COUNT,Type.LIFO);
        return loader;
    }

    private void init(int threadCount, Type type) {
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        mLruCache = new LruCache<String, Bitmap>(maxMemory/8){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getHeight() * value.getRowBytes();
            }
        };

        mThreadPool = Executors.newFixedThreadPool(DEFAULT_THREAD_COUNT);

    }

}
