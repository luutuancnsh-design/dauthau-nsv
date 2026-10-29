package vn.dauthau.nsv;

import android.webkit.JavascriptInterface;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Cầu nối giữa giao diện (app.html) và phần native: lưu follow, gọi muasamcong, thông báo. */
public class Bridge {
    final MainActivity a;
    final Store st;

    Bridge(MainActivity a) { this.a = a; this.st = new Store(a); }

    void cb(String id, String json) {
        a.runOnUiThread(() -> a.web.evaluateJavascript("window.DT_CB&&DT_CB(" + JSONObject.quote(id) + "," + json + ")", null));
    }

    String err(Exception e) {
        try { return new JSONObject().put("error", e.getMessage() == null ? "Lỗi kết nối" : e.getMessage()).toString(); }
        catch (Exception x) { return "{\"error\":\"Lỗi\"}"; }
    }

    @JavascriptInterface public String version() { return BuildConfigLite.VERSION; }

    @JavascriptInterface public String state() {
        try {
            JSONObject p = new JSONObject();
            for (String k : new String[]{"ycbg", "tbmt", "open", "kq"}) p.put(k, st.pref(k));
            return new JSONObject().put("follows", st.follows()).put("feed", st.feed()).put("prefs", p)
                    .put("lastCheck", st.lastCheck()).put("lastError", st.lastError()).put("notif", a.notifOk())
                    .put("version", BuildConfigLite.VERSION).toString();
        } catch (Exception e) { return err(e); }
    }

    @JavascriptInterface public void search(String q, String id) {
        new Thread(() -> {
            try {
                JSONArray r = Api.searchInvestor(q);
                for (int i = 0; i < r.length(); i++) r.getJSONObject(i).put("followed", st.isFollowed(r.getJSONObject(i).getString("code")));
                cb(id, new JSONObject().put("items", r).toString());
            } catch (Exception e) { cb(id, err(e)); }
        }).start();
    }

    @JavascriptInterface public void follow(String code, String name, String id) {
        new Thread(() -> {
            try {
                st.follow(code, name);
                List<String> one = new ArrayList<>();
                one.add(code);
                long now = System.currentTimeMillis();
                int n = st.addEvents(Api.events(one, now - 14 * 86400000L, now), true).size();
                if (st.lastCheck() == 0) st.setLastCheck(now, null);
                cb(id, new JSONObject().put("ok", true).put("history", n).toString());
            } catch (Exception e) { cb(id, err(e)); }
        }).start();
    }

    @JavascriptInterface public void unfollow(String code) { st.unfollow(code); }

    @JavascriptInterface public void check(String id) {
        new Thread(() -> {
            int n = CheckJob.run(a.getApplicationContext(), true);
            try { cb(id, new JSONObject().put("n", n).put("error", n < 0 ? st.lastError() : "").toString()); }
            catch (Exception e) { cb(id, err(e)); }
        }).start();
    }

    @JavascriptInterface public void markRead() { st.markAllRead(); }
    @JavascriptInterface public void setPref(String k, boolean v) { st.setPref(k, v); }
    @JavascriptInterface public void askNotif() { a.runOnUiThread(a::askNotif); }
    @JavascriptInterface public void openUrl(String url) { a.runOnUiThread(() -> a.openExternal(url)); }
    @JavascriptInterface public void download(String url, String name) { a.runOnUiThread(() -> a.download(url, name)); }
}
