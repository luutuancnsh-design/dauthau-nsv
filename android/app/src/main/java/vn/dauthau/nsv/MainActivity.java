package vn.dauthau.nsv;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    static final String BASE = "https://dauthau-nsv.luutuan-cnsh.workers.dev/";
    static final String HOME = BASE + "app.html";
    WebView web;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#F6F8F8"));
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setSupportMultipleWindows(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setUserAgentString(s.getUserAgentString() + " DauThauApp/" + BuildConfigLite.VERSION);
        web.addJavascriptInterface(new Bridge(this), "DauThauApp");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                String url = u.toString();
                if (url.contains("/edocproxy/file/share/")) { download(url, null); return true; }
                if (url.startsWith(BASE)) return false;
                openExternal(url);
                return true;
            }
            @Override public void onPageFinished(WebView v, String url) {
                if (url != null && url.startsWith(BASE + "app")) {
                    pageReady = true;
                    if (pendingJs != null) { final String js = pendingJs; pendingJs = null; v.postDelayed(() -> v.evaluateJavascript(js, null), 400); }
                }
            }
            @Override public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) v.loadDataWithBaseURL(BASE, OFFLINE, "text/html", "utf-8", null);
            }
        });
        web.setDownloadListener((url, ua, cd, mime, len) -> download(url, URLUtil.guessFileName(url, cd, mime)));
        CheckJob.channel(this);
        CheckJob.schedule(this);
        askNotif();
        if (b != null) web.restoreState(b); else web.loadUrl(HOME);
        handle(getIntent());
    }

    static final String OFFLINE = "<html><head><meta name=viewport content='width=device-width,initial-scale=1'></head>"
            + "<body style='font-family:sans-serif;background:#f6f8f8;color:#133;display:flex;align-items:center;justify-content:center;height:90vh;text-align:center'>"
            + "<div><h3>Không có kết nối mạng</h3><p>Kiểm tra mạng rồi thử lại.</p>"
            + "<button onclick=\"location.href='" + HOME + "'\" style='background:#0f766e;color:#fff;border:0;border-radius:10px;padding:10px 18px;font-size:16px'>Thử lại</button></div></body></html>";

    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); handle(i); }

    String pendingJs = null;
    boolean pageReady = false;

    /** Bấm thông báo: mở trang chi tiết gói ngay trong app (chỉ mở muasamcong khi trang không hỗ trợ). */
    void handle(Intent i) {
        if (i == null) return;
        String no = i.getStringExtra("no"), url = i.getStringExtra("url");
        boolean bell = i.getBooleanExtra("bell", false);
        i.removeExtra("no"); i.removeExtra("url"); i.removeExtra("bell");
        String js = null;
        if (no != null && !no.isEmpty()) {
            js = "window.DT_OPEN?DT_OPEN(" + org.json.JSONObject.quote(no) + "):(window.DauThauApp&&DauThauApp.openUrl(" + org.json.JSONObject.quote(url == null ? "" : url) + "))";
        } else if (bell) {
            js = "window.DT_BELL&&DT_BELL()";
        } else if (url != null && !url.isEmpty()) { openExternal(url); return; }
        if (js == null) return;
        if (pageReady) web.evaluateJavascript(js, null); else pendingJs = js;
    }

    @Override protected void onResume() {
        super.onResume();
        if (web != null) web.evaluateJavascript("window.DT_RESUME&&DT_RESUME()", null);
    }

    @Override protected void onSaveInstanceState(Bundle o) { super.onSaveInstanceState(o); web.saveState(o); }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    void askNotif() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
    }

    boolean notifOk() {
        return Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    void openExternal(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (ActivityNotFoundException e) { Toast.makeText(this, "Không mở được liên kết", Toast.LENGTH_SHORT).show(); }
    }

    void download(String url, String name) {
        if (Build.VERSION.SDK_INT < 29 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
            Toast.makeText(this, "Cho phép lưu file rồi bấm tải lại", Toast.LENGTH_LONG).show();
            return;
        }
        if (name == null || name.trim().isEmpty()) name = "Chuong V";
        name = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        try {
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url))
                    .setTitle(name)
                    .setDescription("Tải từ muasamcong")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
            ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(r);
            Toast.makeText(this, "Đang tải " + name + " vào thư mục Download", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            openExternal(url);
        }
    }
}
