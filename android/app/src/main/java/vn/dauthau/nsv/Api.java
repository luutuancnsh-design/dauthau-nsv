package vn.dauthau.nsv;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/** Gọi API công khai của muasamcong (không cần đăng nhập). */
public class Api {
    static final String HOST = "https://muasamcong.mof.gov.vn";
    static String SEARCH = HOST + "/o/egp-portal-contractor-selection-v2/services/smart/search?token=";
    static final TimeZone VN = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");

    static String fmt(long t) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        f.setTimeZone(VN);
        return f.format(new Date(t));
    }

    static JSONObject post(String body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(SEARCH).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(20000);
        c.setReadTimeout(30000);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("Accept", "application/json, text/plain, */*");
        c.setRequestProperty("Origin", HOST);
        c.setRequestProperty("Referer", HOST + "/web/guest/contractor-selection");
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36");
        try (OutputStream o = c.getOutputStream()) { o.write(body.getBytes(StandardCharsets.UTF_8)); }
        int code = c.getResponseCode();
        InputStream in = code < 400 ? c.getInputStream() : c.getErrorStream();
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while (in != null && (n = in.read(buf)) > 0) b.write(buf, 0, n);
        String s = b.toString("UTF-8");
        if (code != 200 || !s.trim().startsWith("{")) throw new Exception("muasamcong trả về lỗi (" + code + ")");
        return new JSONObject(s);
    }

    static JSONObject f(String field, String type, Object... vals) throws JSONException {
        JSONObject o = new JSONObject().put("fieldName", field).put("searchType", type);
        if ("range".equals(type)) o.put("from", vals[0] == null ? JSONObject.NULL : vals[0]).put("to", vals.length > 1 && vals[1] != null ? vals[1] : JSONObject.NULL);
        else { JSONArray a = new JSONArray(); for (Object v : vals) a.put(v); o.put("fieldValues", a); }
        return o;
    }

    static List<JSONObject> query(String keyWord, String matchField, List<JSONObject> filters, int maxPages) throws Exception {
        List<JSONObject> out = new ArrayList<>();
        for (int p = 0; p < maxPages; p++) {
            JSONArray fl = new JSONArray();
            for (JSONObject x : filters) fl.put(x);
            JSONObject q = new JSONObject().put("index", "es-contractor-selection").put("keyWord", keyWord)
                    .put("matchType", "all-1").put("matchFields", new JSONArray().put(matchField)).put("filters", fl);
            JSONObject body = new JSONObject().put("pageSize", 50).put("pageNumber", p).put("query", new JSONArray().put(q));
            JSONObject r = post(new JSONArray().put(body).toString());
            JSONArray content = r.optJSONObject("page") == null ? new JSONArray() : r.getJSONObject("page").optJSONArray("content");
            if (content == null) break;
            for (int i = 0; i < content.length(); i++) out.add(content.getJSONObject(i));
            if (content.length() < 50) break;
        }
        return out;
    }

    /** Tìm bên mời thầu theo tên, gộp theo mã đơn vị. */
    static JSONArray searchInvestor(String q) throws Exception {
        List<JSONObject> rows = query(q, "investorName", new ArrayList<>(), 3);
        Map<String, JSONObject> m = new LinkedHashMap<>();
        for (JSONObject x : rows) {
            String code = x.optString("investorCode"), name = x.optString("investorName");
            if (code.isEmpty() || name.isEmpty()) continue;
            JSONObject o = m.get(code);
            if (o == null) { o = new JSONObject().put("code", code).put("name", name).put("n", 0).put("last", ""); m.put(code, o); }
            o.put("n", o.getInt("n") + 1);
            String d = x.optString("publicDate");
            if (d.compareTo(o.getString("last")) > 0) o.put("last", d);
            JSONArray loc = x.optJSONArray("locations");
            if (loc != null && loc.length() > 0 && !o.has("prov")) o.put("prov", loc.getJSONObject(0).optString("provName"));
        }
        List<JSONObject> l = new ArrayList<>(m.values());
        l.sort((a, b) -> b.optInt("n") - a.optInt("n"));
        JSONArray out = new JSONArray();
        for (JSONObject o : l) out.put(o);
        return out;
    }

    static String title(JSONObject x) {
        JSONArray bn = x.optJSONArray("bidName");
        String t = bn != null && bn.length() > 0 ? bn.optString(0) : x.optString("bidName");
        if (t.isEmpty()) t = x.optString("name");
        return t;
    }

    static String enc(String s) {
        try { return URLEncoder.encode(s == null ? "" : s, "UTF-8"); } catch (Exception e) { return ""; }
    }

    static String detailUrl(JSONObject x, boolean kq) {
        String P = "egpportalcontractorselectionv2_WAR_egpportalcontractorselectionv2";
        StringBuilder u = new StringBuilder(HOST + "/web/guest/contractor-selection?p_p_id=" + P
                + "&p_p_lifecycle=0&p_p_state=normal&p_p_mode=view&_" + P + "_render=detail-v2");
        String no = x.optString("notifyNo");
        if ("es-ycbg".equals(x.optString("type"))) {
            u.append("&type=es-ycbg&stepCode=request-quote-step-1&id=").append(enc(x.optString("id")))
             .append("&notifyNo=").append(enc(no)).append("&isInternet=1");
            return u.toString();
        }
        String id = x.optString("id", no), inp = x.optString("inputResultId", "");
        u.append("&type=es-notify-contractor&stepCode=").append(kq && !inp.isEmpty() ? "notify-contractor-step-4-kqlcnt" : "notify-contractor-step-1-tbmt")
         .append("&step=tbmt&id=").append(enc(id)).append("&notifyId=").append(enc(id))
         .append("&inputResultId=").append(inp.isEmpty() ? "undefined" : enc(inp))
         .append("&bidInviteId=undefined&notifyNo=").append(enc(no)).append("&planNo=undefined&isInternet=1&processApply=LDT&bidMode=1_MTHS&bidForm=").append(enc(x.optString("bidForm")));
        return u.toString();
    }

    /** Trường có thể là chuỗi hoặc mảng chuỗi (nhiều nhà thầu). */
    static String names(Object o) {
        if (o instanceof JSONArray) {
            JSONArray a = (JSONArray) o;
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < a.length(); i++) {
                String v = a.optString(i).trim();
                if (v.isEmpty() || "null".equals(v)) continue;
                if (b.length() > 0) b.append("; ");
                b.append(v);
            }
            return b.toString();
        }
        String v = o == null || o == JSONObject.NULL ? "" : String.valueOf(o).trim();
        return "null".equals(v) ? "" : v;
    }

    static double sum(Object o) {
        if (o instanceof JSONArray) { double t = 0; JSONArray a = (JSONArray) o; for (int i = 0; i < a.length(); i++) t += a.optDouble(i, 0); return t; }
        if (o instanceof Number) return ((Number) o).doubleValue();
        return 0;
    }

    static JSONObject ev(String kind, String key, JSONObject x, String date, boolean kq) throws JSONException {
        JSONObject e = new JSONObject().put("kind", kind).put("key", key)
                .put("code", x.optString("investorCode")).put("investor", x.optString("investorName"))
                .put("no", x.optString("notifyNo")).put("title", title(x)).put("date", date)
                .put("form", x.optString("bidForm")).put("url", detailUrl(x, kq));
        if ("es-ycbg".equals(x.optString("type"))) e.put("sub", x.optString("name"));
        if (!x.optString("bidCloseDate").isEmpty()) e.put("close", x.optString("bidCloseDate"));
        if (!x.optString("bidOpenDate").isEmpty()) e.put("open", x.optString("bidOpenDate"));
        double bp = sum(x.opt("bidPrice"));
        if (bp > 0) e.put("budget", bp);
        if (kq) {
            String w = names(x.opt("winningContractorName"));
            if (w.isEmpty()) w = names(x.opt("contractorName"));
            if (!w.isEmpty()) e.put("winner", w);
            double p = sum(x.opt("bidWinningPrice"));
            if (p > 0) e.put("price", p);
        }
        return e;
    }

    /** Lấy mọi hoạt động của các đơn vị trong khoảng [since, now]. */
    static List<JSONObject> events(List<String> codes, long since, long now) throws Exception {
        List<JSONObject> out = new ArrayList<>();
        String s = fmt(since), n = fmt(now + 60000);
        for (int i = 0; i < codes.size(); i += 25) {
            Object[] cs = codes.subList(i, Math.min(codes.size(), i + 25)).toArray();
            // 1) Đăng tải yêu cầu báo giá / TBMT
            List<JSONObject> fl = new ArrayList<>();
            fl.add(f("investorCode", "in", cs));
            fl.add(f("type", "in", "es-ycbg", "es-notify-contractor"));
            fl.add(f("publicDate", "range", s, n));
            for (JSONObject x : query("", "investorName", fl, 4)) {
                String no = x.optString("notifyNo"), v = x.optString("notifyVersion", "00");
                if ("es-ycbg".equals(x.optString("type"))) out.add(ev("ycbg", "ycbg:" + no + ":" + v, x, x.optString("publicDate"), false));
                else if (x.optString("publicDateKqlcnt").isEmpty() && !no.startsWith("DC"))
                    out.add(ev("tbmt", "tbmt:" + no + ":" + v, x, x.optString("publicDate"), false));
            }
            // 2) Mở thầu
            fl = new ArrayList<>();
            fl.add(f("investorCode", "in", cs));
            fl.add(f("type", "in", "es-notify-contractor"));
            fl.add(f("bidOpenDate", "range", s, n));
            for (JSONObject x : query("", "investorName", fl, 2))
                out.add(ev("open", "open:" + x.optString("notifyNo"), x, x.optString("bidOpenDate"), false));
            // 3) Kết quả lựa chọn nhà thầu
            fl = new ArrayList<>();
            fl.add(f("investorCode", "in", cs));
            fl.add(f("type", "in", "es-notify-contractor"));
            fl.add(f("publicDateKqlcnt", "range", s, n));
            for (JSONObject x : query("", "investorName", fl, 3))
                out.add(ev("kq", "kq:" + x.optString("notifyNo"), x, x.optString("publicDateKqlcnt"), true));
        }
        return out;
    }
}
