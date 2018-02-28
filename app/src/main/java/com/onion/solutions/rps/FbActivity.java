package com.onion.solutions.rps;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.onion.solutions.rps.data.information;

public class FbActivity extends Activity {

    WebView mWebView;

    String URL = information.FBurl;

    ProgressBar loadingProgressBar, loadingTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview);
        TextView txttitle = (TextView) findViewById(R.id.activityTitle);
        txttitle.setText("Solutions Onion");


        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl(URL);
        mWebView.setWebViewClient(new MyWebViewClient());

        loadingProgressBar = (ProgressBar) findViewById(R.id.progressbar_Horizontal);

        mWebView.setWebChromeClient(new WebChromeClient() {

            // isso será chamado no progresso de carregamento da página

            @Override

            public void onProgressChanged(WebView view, int newProgress) {

                super.onProgressChanged(view, newProgress);


                loadingProgressBar.setProgress(newProgress);
                //loadingTitle.setProgress(newProgress);
                // Esconda a barra de progresso se o carregamento estiver completo

                if (newProgress == 100) {
                    loadingProgressBar.setVisibility(View.VISIBLE);

                } else {
                    loadingProgressBar.setVisibility(View.VISIBLE);

                }

            }

        });

    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    private class MyWebViewClient extends WebViewClient {


        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            view.loadUrl(url);
            return true;
        }
    }
}
