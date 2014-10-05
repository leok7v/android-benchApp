/*  Copyright (c) 2014, Leo Kuznetsov <Leo.Kuznetsov@gmail.com>
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

 * Neither the name of the {organization} nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.benchapp;

import android.annotation.*;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.webkit.*;

import java.io.*;

import static android.view.ViewGroup.LayoutParams.*;

public class Act extends Activity {

    private static final String TAG = Act.class.getSimpleName();

    private Handler handler = new Handler();
    private View cv;
    private WebView wv;
    private String ua;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cv = new Splash(this);
        if (!GCM.create(this)) {
            // TODO: prompt user to get valid Play Services APK.
        }
        setContentView(cv);
    }

    protected void onResume() {
        super.onResume();
        GCM.checkPlayServices(this);
    }

    @SuppressLint("SetJavaScriptEnabled")

    private void init() {
        wv = new WebView(this);
        wv.setLayoutParams(new ViewGroup.LayoutParams(FILL_PARENT, FILL_PARENT));
        WebSettings ws = wv.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setDatabaseEnabled(true);
        ws.setUseWideViewPort(true);
        ws.setBuiltInZoomControls(false);
        ws.setAppCacheEnabled(true);
        ws.setAppCacheMaxSize(100 * 1024 * 1024); // 100MB
        ws.setAllowContentAccess(true);
        ws.setAllowFileAccess(true);
        ws.setJavaScriptCanOpenWindowsAutomatically(true);
        wv.setWebViewClient(new WebViewClient() {

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                if (ua == null) {
                    ua = wv.getSettings().getUserAgentString();
                    Log.d(Act.class.getSimpleName(), "UserAgent: " + ua);
                }
                Log.d(Act.class.getSimpleName(), url);
                return true;
            }

            public void onLoadResource(WebView view, String url) {
                Log.d(Act.class.getSimpleName(), "onLoadResource " + url);
            }

            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                Log.d(Act.class.getSimpleName(), "shouldInterceptRequest " + url);
                return null;
            }


            public void onPageFinished(WebView view, final String url) {
                if (cv instanceof Splash) {
                    cv = wv;
                    handler.postDelayed(new Runnable() { public void run() { setContentView(cv); } }, 750);
                }
            }
        });
        wv.loadUrl("http:///www.benchapp.com");
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && wv != null && wv.canGoBack()) {
            wv.goBack();
            GCM.send(); // TODO: this is just for testing, shouldn't be here...
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private class Splash extends View {

        private boolean posted;
        private Bitmap bitmap;

        public Splash(Context context) {
            super(context);
            setBackgroundColor(Color.BLACK);
        }

        public void draw(Canvas c) {
            super.draw(c);
            if (!posted) {
                handler.post(new Runnable() {
                    public void run() {
                        init();
                    }
                });
                posted = true;
            }
            if (bitmap == null) {
                InputStream is = null;
                try {
                    is = getResources() != null ? getResources().openRawResource(R.drawable.icon) : null;
                    bitmap = is != null ? BitmapFactory.decodeStream(is) : null;
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) { /* ignore */ }
                    }
                }
            }
            if (bitmap != null) {
                int x = (getWidth() - bitmap.getWidth()) / 2;
                int y = (getHeight() - bitmap.getHeight()) / 2;
                c.drawBitmap(bitmap, x, y, null);
            }
        }

    }

}
