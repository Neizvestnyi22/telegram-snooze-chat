package org.telegram.lab;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.telegram.messenger.AndroidUtilities;

import java.util.ArrayDeque;
import java.util.function.Supplier;

/**
 * Plays a scripted demo by feeding real MotionEvents into the screen, so every gesture
 * goes through the same code paths as a finger would. Touches are drawn as circles.
 */
public class TouchDriver {

    public interface Step {
        void run(Runnable done);
    }

    private final ViewGroup screen;
    private final Overlay overlay;
    private final StageView stage;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayDeque<Step> steps = new ArrayDeque<>();
    private boolean busy;
    private long downTime;
    private final float[] px = new float[2], py = new float[2];
    private int pointers;

    public TouchDriver(ViewGroup screen, StageView stage) {
        this.screen = screen;
        this.stage = stage;
        overlay = new Overlay(screen.getContext());
        screen.addView(overlay, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public View getOverlay() {
        return overlay;
    }

    public TouchDriver step(Step step) {
        steps.add(step);
        if (!busy) {
            next();
        }
        return this;
    }

    private void next() {
        Step s = steps.poll();
        if (s == null) {
            busy = false;
            return;
        }
        busy = true;
        s.run(() -> handler.post(this::next));
    }

    public TouchDriver run(Runnable r) {
        return step(done -> {
            r.run();
            done.run();
        });
    }

    public TouchDriver wait(int ms) {
        return step(done -> handler.postDelayed(done, ms));
    }

    /** Waits until something is ready (a recognizer result, a download), up to timeoutMs. */
    public TouchDriver waitFor(java.util.function.BooleanSupplier ready, int timeoutMs) {
        return step(done -> {
            long until = SystemClock.uptimeMillis() + timeoutMs;
            Runnable check = new Runnable() {
                @Override
                public void run() {
                    if (ready.getAsBoolean() || SystemClock.uptimeMillis() > until) {
                        done.run();
                    } else {
                        handler.postDelayed(this, 100);
                    }
                }
            };
            check.run();
        });
    }

    // ---- coordinates ----

    public float[] center(View v) {
        return point(v, 0.5f, 0.5f);
    }

    /** Point inside v in screen coordinates, including translations of v and all its parents. */
    public float[] point(View v, float fx, float fy) {
        Rect r = new Rect(0, 0, v.getWidth(), v.getHeight());
        screen.offsetDescendantRectToMyCoords(v, r);
        float tx = 0, ty = 0;
        View p = v;
        while (p != null && p != screen) {
            tx += p.getTranslationX();
            ty += p.getTranslationY();
            p = p.getParent() instanceof View ? (View) p.getParent() : null;
        }
        return new float[]{r.left + r.width() * fx + tx, r.top + r.height() * fy + ty};
    }

    // ---- gestures ----

    public TouchDriver tap(View v) {
        return tap(() -> v, 0.5f, 0.5f);
    }

    public TouchDriver tap(Supplier<View> v) {
        return tap(v, 0.5f, 0.5f);
    }

    public TouchDriver tap(Supplier<View> v, float fx, float fy) {
        return step(done -> {
            float[] p = point(v.get(), fx, fy);
            tapAt(p[0], p[1], 90, done);
        });
    }

    public TouchDriver tapXY(float x, float y) {
        return step(done -> tapAt(x, y, 90, done));
    }

    /** Taps a point computed when the step runs (screen coordinates). */
    public TouchDriver tapPoint(Supplier<float[]> p) {
        return step(done -> {
            float[] xy = p.get();
            tapAt(xy[0], xy[1], 90, done);
        });
    }

    public TouchDriver longPressPoint(Supplier<float[]> p, int ms) {
        return step(done -> {
            float[] xy = p.get();
            tapAt(xy[0], xy[1], ms, done);
        });
    }

    /** Center of a message bubble in screen coordinates. */
    public float[] bubble(org.telegram.lab.ui.MessageCell cell) {
        float[] o = point(cell, 0, 0);
        return new float[]{o[0] + cell.bubble.centerX(), o[1] + cell.bubble.centerY()};
    }

    private void tapAt(float x, float y, int hold, Runnable done) {
        down(x, y);
        handler.postDelayed(() -> {
            up();
            handler.postDelayed(done, 120);
        }, hold);
    }

    public TouchDriver longPress(View v, int ms) {
        return longPress(() -> v, 0.5f, 0.5f, ms);
    }

    public TouchDriver longPress(Supplier<View> v, float fx, float fy, int ms) {
        return step(done -> {
            float[] p = point(v.get(), fx, fy);
            tapAt(p[0], p[1], ms, done);
        });
    }

    /** Presses and keeps the finger down; follow with moveTo/release. */
    public TouchDriver press(Supplier<View> v, float fx, float fy) {
        return step(done -> {
            float[] p = point(v.get(), fx, fy);
            down(p[0], p[1]);
            handler.postDelayed(done, 60);
        });
    }

    public TouchDriver hold(int ms) {
        return wait(ms);
    }

    public TouchDriver moveTo(Supplier<View> v, float fx, float fy, int ms) {
        return step(done -> {
            float[] p = point(v.get(), fx, fy);
            glide(px[0], py[0], p[0], p[1], ms, done);
        });
    }

    public TouchDriver moveBy(float dx, float dy, int ms) {
        return step(done -> glide(px[0], py[0], px[0] + dx, py[0] + dy, ms, done));
    }

    public TouchDriver release() {
        return step(done -> {
            up();
            handler.postDelayed(done, 100);
        });
    }

    public TouchDriver swipe(float x1, float y1, float x2, float y2, int ms) {
        return step(done -> {
            down(x1, y1);
            handler.postDelayed(() -> glide(x1, y1, x2, y2, ms, () -> {
                up();
                handler.postDelayed(done, 150);
            }), 40);
        });
    }

    /** Scrolls the content up (dy > 0) by dragging a finger in the middle of the screen. */
    public TouchDriver scroll(float dy, int ms) {
        return step(done -> {
            float x = screen.getWidth() * 0.55f, y0 = screen.getHeight() * 0.55f + dy / 2f;
            down(x, y0);
            handler.postDelayed(() -> glide(x, y0, x, y0 - dy, ms, () -> handler.postDelayed(() -> {
                up();
                handler.postDelayed(done, 250);
            }, 120)), 40);
        });
    }

    public TouchDriver pinch(Supplier<View> v, float fromSpan, float toSpan, int ms) {
        return step(done -> {
            float[] c = center(v.get());
            pinchAt(c[0], c[1], fromSpan, toSpan, ms, done);
        });
    }

    private void pinchAt(float cx, float cy, float s0, float s1, int ms, Runnable done) {
        float a = (float) Math.toRadians(62);
        float dx = (float) Math.cos(a) / 2f, dy = (float) Math.sin(a) / 2f;
        down(cx - dx * s0, cy + dy * s0);
        handler.postDelayed(() -> {
            pointerDown(cx + dx * s0, cy - dy * s0);
            ValueAnimator va = ValueAnimator.ofFloat(0, 1);
            va.setDuration(ms);
            va.addUpdateListener(an -> {
                float f = (float) an.getAnimatedValue();
                float s = AndroidUtilities.lerp(s0, s1, f);
                px[0] = cx - dx * s;
                py[0] = cy + dy * s;
                px[1] = cx + dx * s;
                py[1] = cy - dy * s;
                send(MotionEvent.ACTION_MOVE);
            });
            va.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    handler.postDelayed(() -> {
                        send(MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                        pointers = 1;
                        overlay.invalidate();
                        handler.postDelayed(() -> {
                            up();
                            handler.postDelayed(done, 200);
                        }, 40);
                    }, 160);
                }
            });
            va.start();
        }, 30);
    }

    public TouchDriver type(EditText field, String text, int msPerChar) {
        return step(done -> {
            final int[] i = {0};
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    if (i[0] >= text.length()) {
                        done.run();
                        return;
                    }
                    field.getText().insert(field.getSelectionStart() < 0 ? field.length() : field.getSelectionStart(), String.valueOf(text.charAt(i[0])));
                    i[0]++;
                    handler.postDelayed(this, text.charAt(i[0] - 1) == ' ' ? msPerChar + 30 : msPerChar);
                }
            };
            r.run();
        });
    }

    public TouchDriver zoom(float z, Supplier<View> focus, int ms) {
        return step(done -> {
            if (stage != null) {
                float[] c = center(focus.get());
                stage.zoomTo(z, c[0], c[1], ms);
            }
            handler.postDelayed(done, ms);
        });
    }

    public TouchDriver zoomAt(float z, float fx, float fy, int ms) {
        return step(done -> {
            if (stage != null) {
                stage.zoomTo(z, screen.getWidth() * fx, screen.getHeight() * fy, ms);
            }
            handler.postDelayed(done, ms);
        });
    }

    public TouchDriver resetZoom(int ms) {
        return zoomAt(1, 0.5f, 0.5f, ms);
    }

    // ---- raw events ----

    private void glide(float x0, float y0, float x1, float y1, int ms, Runnable done) {
        ValueAnimator va = ValueAnimator.ofFloat(0, 1);
        va.setDuration(ms);
        va.setInterpolator(new android.view.animation.PathInterpolator(0.4f, 0, 0.3f, 1));
        va.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            px[0] = AndroidUtilities.lerp(x0, x1, f);
            py[0] = AndroidUtilities.lerp(y0, y1, f);
            send(MotionEvent.ACTION_MOVE);
        });
        va.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                done.run();
            }
        });
        va.start();
    }

    private void down(float x, float y) {
        downTime = SystemClock.uptimeMillis();
        px[0] = x;
        py[0] = y;
        pointers = 1;
        overlay.press();
        send(MotionEvent.ACTION_DOWN);
    }

    private void pointerDown(float x, float y) {
        px[1] = x;
        py[1] = y;
        pointers = 2;
        send(MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
    }

    private void up() {
        send(MotionEvent.ACTION_UP);
        pointers = 0;
        overlay.release();
    }

    private void send(int action) {
        int count = Math.max(1, pointers);
        MotionEvent.PointerProperties[] props = new MotionEvent.PointerProperties[count];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[count];
        for (int i = 0; i < count; i++) {
            props[i] = new MotionEvent.PointerProperties();
            props[i].id = i;
            props[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
            coords[i] = new MotionEvent.PointerCoords();
            coords[i].x = px[i];
            coords[i].y = py[i];
            coords[i].pressure = 1;
            coords[i].size = 1;
        }
        MotionEvent ev = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, count, props, coords, 0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        screen.dispatchTouchEvent(ev);
        ev.recycle();
        overlay.invalidate();
    }

    /** Finger circles, drawn above the whole screen. */
    private class Overlay extends View {

        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float pressProgress;
        private ValueAnimator anim;

        Overlay(Context context) {
            super(context);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(AndroidUtilities.dp(2));
        }

        void press() {
            animateTo(1, 120);
        }

        void release() {
            animateTo(0, 260);
        }

        private void animateTo(float v, long ms) {
            if (anim != null) {
                anim.cancel();
            }
            anim = ValueAnimator.ofFloat(pressProgress, v);
            anim.setDuration(ms);
            anim.addUpdateListener(a -> {
                pressProgress = (float) a.getAnimatedValue();
                invalidate();
            });
            anim.start();
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            return false;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (pressProgress <= 0) {
                return;
            }
            float r = AndroidUtilities.dp(21) * (0.8f + 0.2f * pressProgress);
            fill.setColor(Tg.alpha(0xFFFFFFFF, 0.38f * pressProgress));
            stroke.setColor(Tg.alpha(0xFFFFFFFF, 0.9f * pressProgress));
            int n = Math.max(1, pointers);
            for (int i = 0; i < n; i++) {
                canvas.drawCircle(px[i], py[i], r, fill);
                canvas.drawCircle(px[i], py[i], r, stroke);
            }
        }
    }
}
