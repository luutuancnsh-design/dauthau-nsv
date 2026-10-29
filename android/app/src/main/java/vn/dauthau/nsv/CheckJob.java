package vn.dauthau.nsv;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Chạy nền khoảng 15 phút/lần: kiểm tra hoạt động mới của các bệnh viện đang follow và gửi thông báo. */
public class CheckJob extends JobService {
    static final int JOB_ID = 2601;
    static final String CH = "hoatdong";

    static void schedule(Context c) {
        JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        long period = 15 * 60 * 1000L; // mức tối thiểu Android cho phép
        for (JobInfo j : js.getAllPendingJobs()) if (j.getId() == JOB_ID) { if (j.getIntervalMillis() == period) return; js.cancel(JOB_ID); }
        js.schedule(new JobInfo.Builder(JOB_ID, new ComponentName(c, CheckJob.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(period)
                .setPersisted(true)
                .build());
    }

    @Override public boolean onStartJob(JobParameters p) {
        new Thread(() -> {
            run(getApplicationContext(), true);
            jobFinished(p, false);
        }).start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters p) { return true; }

    static final Object LOCK = new Object();

    /** Trả về số sự kiện mới. */
    static int run(Context c, boolean notify) {
        synchronized (LOCK) {
            Store st = new Store(c);
            JSONArray f = st.follows();
            if (f.length() == 0) return 0;
            List<String> codes = new ArrayList<>();
            for (int i = 0; i < f.length(); i++) codes.add(f.optJSONObject(i).optString("code"));
            long now = System.currentTimeMillis(), last = st.lastCheck();
            long since = last == 0 ? now - 3 * 86400000L : last - 36 * 3600000L;
            try {
                List<JSONObject> fresh = st.addEvents(Api.events(codes, since, now), false);
                st.setLastCheck(now, null);
                if (notify) notifyAll(c, st, fresh);
                return fresh.size();
            } catch (Exception e) {
                st.setLastCheck(last, e.getMessage());
                return -1;
            }
        }
    }

    static String label(String k) {
        switch (k) {
            case "ycbg": return "Đăng tải báo giá";
            case "tbmt": return "Đăng tải TBMT/HSMT";
            case "open": return "Mở thầu";
            case "kq": return "Kết quả LCNT";
        }
        return k;
    }

    static void channel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CH, "Hoạt động bệnh viện đang theo dõi", NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Báo giá, TBMT/HSMT, mở thầu, kết quả lựa chọn nhà thầu");
            c.getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    static void notifyAll(Context c, Store st, List<JSONObject> evs) {
        channel(c);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        int shown = 0;
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        for (JSONObject e : evs) {
            String kind = e.optString("kind");
            if (!st.pref(kind)) continue;
            if (shown >= 12) break;
            StringBuilder body = new StringBuilder(e.optString("title"));
            if (e.has("winner")) body.append("\nTrúng: ").append(e.optString("winner"));
            if (e.has("price")) body.append("\nGiá trúng: ").append(nf.format((long) e.optDouble("price"))).append(" đ");
            if ("ycbg".equals(kind) || "tbmt".equals(kind)) {
                String cl = e.optString("close");
                if (cl.length() >= 16) body.append("\nHạn: ").append(cl.substring(11, 16)).append(" ").append(cl.substring(8, 10)).append("/").append(cl.substring(5, 7));
            }
            body.append("\n").append(e.optString("no"));
            Intent open = new Intent(c, MainActivity.class).setAction("open." + e.optString("key"))
                    .putExtra("url", e.optString("url")).putExtra("no", e.optString("no")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(c, e.optString("key").hashCode(), open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CH) : new Notification.Builder(c);
            b.setSmallIcon(R.drawable.ic_stat)
             .setColor(0xFF0F766E)
             .setContentTitle(label(kind) + " · " + e.optString("investor"))
             .setContentText(e.optString("title"))
             .setStyle(new Notification.BigTextStyle().bigText(body.toString()))
             .setContentIntent(pi)
             .setAutoCancel(true)
             .setGroup("dt");
            try { nm.notify(e.optString("key").hashCode(), b.build()); shown++; } catch (SecurityException ignored) {}
        }
        if (shown > 1) {
            Notification.Builder s = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CH) : new Notification.Builder(c);
            s.setSmallIcon(R.drawable.ic_stat).setColor(0xFF0F766E).setGroup("dt").setGroupSummary(true)
             .setContentTitle(shown + " hoạt động mới").setAutoCancel(true)
             .setContentIntent(PendingIntent.getActivity(c, 1, new Intent(c, MainActivity.class).setAction("open.bell").putExtra("bell", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            try { nm.notify(1, s.build()); } catch (SecurityException ignored) {}
        }
    }
}
