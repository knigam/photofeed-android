package com.keonasoft.photofeed.app.helper;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.PersistentCookieStore;


/**
 * Created by kushal on 12/22/14.
 */
public class HttpHelper {

    private static HttpHelper ourInstance = new HttpHelper();
    public static HttpHelper getInstance() { return ourInstance; }

    private PersistentCookieStore mCookie;
    private AsyncHttpClient mAsyncHttpClient;
    private boolean initialized = false;
    private HttpHelper(){
    }

    /**
     * This method sets up ourInstance for use. Initialize must be run at least once before any other
     * methods are called on ourInstance
     * @param context
     */
    public void initialize(Context context) {
        this.mCookie = new PersistentCookieStore(context);
        this.mAsyncHttpClient = new AsyncHttpClient();
        this.mAsyncHttpClient.setCookieStore(mCookie);
        this.initialized = true;
    }

    /**
     * Determines if ourInstance has been initialized and then returns an error or AsyncHttpClient
     * @return
     */
    public AsyncHttpClient getClient() {
        if (initialized == false)
            throw new RuntimeException("Make sure ourInstance of HttpHelper has been Initialized");
        else
            return mAsyncHttpClient;
    }
}