package vn.dauthau.nsv;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.LinkedHashSet;
import java.util.List;

/** Lưu danh sách follow, bảng tin và các sự kiện đã thấy ngay trong máy. */
public class Store {
    static final int FEED_MAX = 400, SEEN_MAX = 8000;
    final SharedPreferences sp;

    Store(Context c) { sp = c.getApplicationContext().getSharedPreferences("dt", Context.MODE_PRIVATE); }

    JSONArray arr(String k) {
        try { return new JSONArray(sp.getString(k, "[]")); } catch (JSONException e) { return new JSONArray(); }
    }
    void put(String k, JSONArray a) { sp.edit().putString(k, a.toString()).apply(); }

    JSONArray follows() { return arr("follows"); }
    JSONArray feed() { return arr("feed"); }

    synchronized boolean isFollowed(String code) {
        JSONArray f = follows();
        for (int i = 0; i < f.length(); i++) if (code.equals(f.optJSONObject(i).optString("code"))) return true;
        return false;
    }

    synchronized void follow(String code, String name) throws JSONException {
        if (isFollowed(code)) return;
        JSONArray f = follows();
        f.put(new JSONObject().put("code", code).put("name", name).put("at", System.currentTimeMillis()));
        put("follows", f);
    }

    synchronized void unfollow(String code) {
        JSONArray f = follows(), nf = new JSONArray();
        for (int i = 0; i < f.length(); i++) if (!code.equals(f.optJSONObject(i).optString("code"))) nf.put(f.opt(i));
        put("follows", nf);
        JSONArray fd = feed(), nfd = new JSONArray();
        for (int i = 0; i < fd.length(); i++) if (!code.equals(fd.optJSONObject(i).optString("code"))) nfd.put(fd.opt(i));
        put("feed", nfd);
    }

    LinkedHashSet<String> seen() {
        LinkedHashSet<String> s = new LinkedHashSet<>();
        JSONArray a = arr("seen");
        for (int i = 0; i < a.length(); i++) s.add(a.optString(i));
        return s;
    }

    /** Thêm sự kiện mới vào bảng tin (mới nhất lên đầu). Trả về các sự kiện chưa từng thấy. */
    synchronized java.util.ArrayList<JSONObject> addEvents(List<JSONObject> evs, boolean read) {
        LinkedHashSet<String> seen = seen();
        java.util.ArrayList<JSONObject> fresh = new java.util.ArrayList<>();
        for (JSONObject e : evs) {
            String k = e.optString("key");
            if (k.isEmpty() || seen.contains(k)) continue;
            seen.add(k);
            try { e.put("read", read).put("at", System.currentTimeMillis()); if (read) e.put("hist", true); } catch (JSONException ignored) {}
            fresh.add(e);
        }
        if (fresh.isEmpty()) return fresh;
        JSONArray old = feed();
        java.util.ArrayList<JSONObject> all = new java.util.ArrayList<>(fresh);
        for (int i = 0; i < old.length(); i++) all.add(old.optJSONObject(i));
        all.sort((a, b) -> b.optString("date").compareTo(a.optString("date")));
        JSONArray out = new JSONArray();
        for (int i = 0; i < all.size() && i < FEED_MAX; i++) out.put(all.get(i));
        put("feed", out);
        JSONArray sa = new JSONArray();
        int skip = Math.max(0, seen.size() - SEEN_MAX), i = 0;
        for (String s : seen) { if (i++ >= skip) sa.put(s); }
        put("seen", sa);
        return fresh;
    }

    synchronized void markAllRead() {
        JSONArray fd = feed();
        for (int i = 0; i < fd.length(); i++) try { fd.getJSONObject(i).put("read", true); } catch (JSONException ignored) {}
        put("feed", fd);
    }

    boolean pref(String k) { return sp.getBoolean("p_" + k, true); }
    void setPref(String k, boolean v) { sp.edit().putBoolean("p_" + k, v).apply(); }
    long lastCheck() { return sp.getLong("lastCheck", 0); }
    void setLastCheck(long t, String err) { sp.edit().putLong("lastCheck", t).putString("lastError", err == null ? "" : err).apply(); }
    String lastError() { return sp.getString("lastError", ""); }
}
