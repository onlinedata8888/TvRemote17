package com.example.tvremote;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

/** Floating remote: a small draggable bubble; tap it to open a mini pad (Up/Down/Left/Right/OK/Back). */
public final class FloatService extends Service {
    public static volatile boolean running = false;
    static final int UP = 19, DOWN = 20, LEFT = 21, RIGHT = 22, OK = 23, BACK = 4;
    private WindowManager wm;
    private BubbleView bubble;
    private PadView pad;
    private WindowManager.LayoutParams lp;
    private float dp;
    private final Handler h = new Handler(Looper.getMainLooper());

    static void send(Context c, int code, int dir) {
        TvBridge b = TvBridge.getInstance();
        if (b == null) { Toast.makeText(c, "Open the Remote app first", Toast.LENGTH_SHORT).show(); return; }
        b.key(code, dir);
    }

    public static void start(Context c) { c.startService(new Intent(c, FloatService.class)); }
    public static void stop(Context c) { c.stopService(new Intent(c, FloatService.class)); }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public int onStartCommand(Intent i, int f, int id) {
        if (bubble == null && pad == null) build();
        return START_NOT_STICKY;
    }

    private void build() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        dp = getResources().getDisplayMetrics().density;
        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        lp = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        DisplayMetrics m = getResources().getDisplayMetrics();
        lp.x = m.widthPixels - (int) (46 * dp);
        lp.y = m.heightPixels / 2;
        bubble = new BubbleView(this);
        try { wm.addView(bubble, lp); running = true; }
        catch (Throwable t) { bubble = null; running = false; stopSelf(); }
    }

    private void showPad() {
        if (pad != null || wm == null) return;
        DisplayMetrics m = getResources().getDisplayMetrics();
        int sz = (int) (176 * dp), bs = (int) (46 * dp);
        int cx = lp.x + bs / 2, cy = lp.y + bs / 2;
        lp.x = Math.max(0, Math.min(m.widthPixels - sz, cx - sz / 2));
        lp.y = Math.max(0, Math.min(m.heightPixels - sz, cy - sz / 2));
        try { wm.removeView(bubble); } catch (Throwable ignored) {}
        pad = new PadView(this);
        wm.addView(pad, lp);
    }

    private void hidePad() {
        if (pad == null) return;
        int sz = (int) (176 * dp), bs = (int) (46 * dp);
        DisplayMetrics m = getResources().getDisplayMetrics();
        int cx = lp.x + sz / 2, cy = lp.y + sz / 2;
        try { wm.removeView(pad); } catch (Throwable ignored) {}
        pad = null;
        lp.x = cx < m.widthPixels / 2 ? 0 : m.widthPixels - bs;
        lp.y = Math.max(0, Math.min(m.heightPixels - bs, cy - bs / 2));
        wm.addView(bubble, lp);
    }

    @Override public void onDestroy() {
        running = false;
        try { if (bubble != null && bubble.isAttachedToWindow()) wm.removeView(bubble); } catch (Throwable ignored) {}
        try { if (pad != null) wm.removeView(pad); } catch (Throwable ignored) {}
        bubble = null; pad = null;
        super.onDestroy();
    }

    // ---------- shared drawing ----------
    private static void tri(Canvas c, Paint p, float cx, float cy, float r, int dir) {
        Path t = new Path();
        if (dir == 0) { t.moveTo(cx, cy - r); t.lineTo(cx + r, cy + r * .7f); t.lineTo(cx - r, cy + r * .7f); }
        else if (dir == 1) { t.moveTo(cx, cy + r); t.lineTo(cx + r, cy - r * .7f); t.lineTo(cx - r, cy - r * .7f); }
        else if (dir == 2) { t.moveTo(cx - r, cy); t.lineTo(cx + r * .7f, cy - r); t.lineTo(cx + r * .7f, cy + r); }
        else { t.moveTo(cx + r, cy); t.lineTo(cx - r * .7f, cy - r); t.lineTo(cx - r * .7f, cy + r); }
        t.close();
        c.drawPath(t, p);
    }

    private final class BubbleView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float dx, dy; private int sx, sy; private boolean moved;
        BubbleView(Context c) { super(c); setAlpha(.88f); }
        @Override protected void onMeasure(int w, int hh) { int s = (int) (46 * dp); setMeasuredDimension(s, s); }
        @Override protected void onDraw(Canvas c) {
            float s = getWidth(), r = s / 2f;
            p.setStyle(Paint.Style.FILL); p.setColor(0xF0191C24); c.drawCircle(r, r, r - 2 * dp, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2 * dp); p.setColor(0xFF3EE6A8); c.drawCircle(r, r, r - 2 * dp, p);
            p.setStyle(Paint.Style.FILL); p.setColor(0xFFFFFFFF);
            float o = s * .24f, k = s * .075f;
            tri(c, p, r, r - o, k, 0); tri(c, p, r, r + o, k, 1); tri(c, p, r - o, r, k, 2); tri(c, p, r + o, r, k, 3);
            p.setColor(0xFF3EE6A8); c.drawCircle(r, r, s * .07f, p);
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: dx = e.getRawX(); dy = e.getRawY(); sx = lp.x; sy = lp.y; moved = false; return true;
                case MotionEvent.ACTION_MOVE: {
                    float mx = e.getRawX() - dx, my = e.getRawY() - dy;
                    if (!moved && Math.abs(mx) + Math.abs(my) > 8 * dp) moved = true;
                    if (moved) { lp.x = sx + (int) mx; lp.y = sy + (int) my; try { wm.updateViewLayout(this, lp); } catch (Throwable ignored) {} }
                    return true; }
                case MotionEvent.ACTION_UP:
                    if (!moved) { performHaptic(); showPad(); }
                    else { // snap to nearest side edge
                        DisplayMetrics m = getResources().getDisplayMetrics();
                        lp.x = lp.x + getWidth() / 2 < m.widthPixels / 2 ? 0 : m.widthPixels - getWidth();
                        lp.y = Math.max(0, Math.min(m.heightPixels - getHeight(), lp.y));
                        try { wm.updateViewLayout(this, lp); } catch (Throwable ignored) {}
                    }
                    return true;
            }
            return true;
        }
        private void performHaptic() { performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY); }
    }

    private final class PadView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        // cells: 0 back, 1 up, 2 close, 3 left, 4 ok, 5 right, 6 (empty), 7 down, 8 (empty)
        private int active = -1;
        private final int[] codes = {BACK, UP, -1, LEFT, OK, RIGHT, -1, DOWN, -1};
        PadView(Context c) { super(c); setAlpha(.92f); }
        @Override protected void onMeasure(int w, int hh) { int s = (int) (176 * dp); setMeasuredDimension(s, s); }
        private int cellAt(float x, float y) {
            float cs = getWidth() / 3f; int cx = (int) (x / cs), cy = (int) (y / cs);
            if (cx < 0 || cx > 2 || cy < 0 || cy > 2) return -1;
            return cy * 3 + cx;
        }
        @Override protected void onDraw(Canvas c) {
            float s = getWidth(), cs = s / 3f, g = 3 * dp;
            p.setStyle(Paint.Style.FILL); p.setColor(0xF0151821);
            c.drawRoundRect(new RectF(0, 0, s, s), 22 * dp, 22 * dp, p);
            for (int i = 0; i < 9; i++) {
                if (i == 6 || i == 8) continue;
                float l = (i % 3) * cs + g, t = (i / 3) * cs + g;
                RectF r = new RectF(l, t, l + cs - 2 * g, t + cs - 2 * g);
                p.setStyle(Paint.Style.FILL);
                p.setColor(i == active ? 0xFF1FA588 : (i == 4 ? 0xFF2A2F3B : 0xFF222733));
                c.drawRoundRect(r, 14 * dp, 14 * dp, p);
                float mx = r.centerX(), my = r.centerY(), k = cs * .13f;
                p.setColor(0xFFFFFFFF);
                if (i == 1) tri(c, p, mx, my, k, 0);
                else if (i == 7) tri(c, p, mx, my, k, 1);
                else if (i == 3) tri(c, p, mx, my, k, 2);
                else if (i == 5) tri(c, p, mx, my, k, 3);
                else if (i == 4) { p.setTextAlign(Paint.Align.CENTER); p.setTextSize(cs * .3f); p.setFakeBoldText(true); c.drawText("OK", mx, my + cs * .1f, p); p.setFakeBoldText(false); }
                else if (i == 0) { p.setTextAlign(Paint.Align.CENTER); p.setTextSize(cs * .22f); c.drawText("Back", mx, my + cs * .08f, p); }
                else if (i == 2) { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2.4f * dp); p.setStrokeCap(Paint.Cap.ROUND); float q = cs * .12f;
                    c.drawLine(mx - q, my - q, mx + q, my + q, p); c.drawLine(mx - q, my + q, mx + q, my - q, p); }
            }
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) {
                int cell = cellAt(e.getX(), e.getY());
                if (cell == 2) { performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY); hidePad(); return true; }
                if (cell >= 0 && codes[cell] > 0) { active = cell; send(getContext(), codes[cell], 1); performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP); invalidate(); }
                return true;
            }
            if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                if (active >= 0) { send(getContext(), codes[active], 2); active = -1; invalidate(); }
                return true;
            }
            return true;
        }
    }
}
