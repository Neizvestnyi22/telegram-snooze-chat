package org.telegram.lab;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;

/**
 * Presentation mode for demo videos: a square frame with the phone in a bezel,
 * an ambient glow made of the blurred screen behind it, and a camera that can zoom in.
 * Only used when an app is started with --ez stage true.
 */
public class StageView extends FrameLayout {

    public static final float PHONE_W_DP = 412, PHONE_H_DP = 892;

    private final FrameLayout phone;
    private final GlowView glow;
    private final Paint bezelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cameraPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private float scale = -1, tx, ty;
    private float zoom = 1;
    private ValueAnimator camera;

    public StageView(Context context, FrameLayout phone) {
        super(context);
        this.phone = phone;
        setWillNotDraw(false);
        setBackgroundColor(Color.BLACK);
        glow = new GlowView(context);
        addView(glow, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(phone, new LayoutParams(AndroidUtilities.dp(PHONE_W_DP), AndroidUtilities.dp(PHONE_H_DP)));
        phone.setPivotX(0);
        phone.setPivotY(0);
        phone.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), AndroidUtilities.dp(46));
            }
        });
        phone.setClipToOutline(true);
        bezelPaint.setColor(0xFF0B0B0C);
        edgePaint.setStyle(Paint.Style.STROKE);
        cameraPaint.setColor(0xFF050506);
    }

    public FrameLayout getPhone() {
        return phone;
    }

    /** Base camera: the whole phone fits the frame with a margin. */
    private float baseScale() {
        return getHeight() * 0.9f / AndroidUtilities.dp(PHONE_H_DP);
    }

    private float baseTop() {
        return (getHeight() - AndroidUtilities.dp(PHONE_H_DP) * baseScale()) / 2f;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (scale < 0 && getWidth() > 0) {
            scale = baseScale();
            tx = (getWidth() - AndroidUtilities.dp(PHONE_W_DP) * scale) / 2f;
            ty = baseTop();
            apply();
        }
    }

    private void apply() {
        phone.setScaleX(scale);
        phone.setScaleY(scale);
        phone.setTranslationX(tx);
        phone.setTranslationY(ty);
        invalidate();
    }

    /** Moves the camera so that (fx, fy) in phone coordinates comes to the middle of the frame. */
    public void zoomTo(float z, float fx, float fy, long duration) {
        float pw = AndroidUtilities.dp(PHONE_W_DP), ph = AndroidUtilities.dp(PHONE_H_DP);
        float s = baseScale() * z;
        float nx, ny;
        if (z <= 1.001f) {
            nx = (getWidth() - pw * s) / 2f;
            ny = baseTop();
        } else {
            nx = getWidth() / 2f - fx * s;
            ny = getHeight() / 2f - fy * s;
            float margin = getHeight() * 0.05f;
            if (pw * s > getWidth()) {
                nx = Math.min(0, Math.max(getWidth() - pw * s, nx));
            } else {
                nx = (getWidth() - pw * s) / 2f;
            }
            ny = Math.min(margin, Math.max(getHeight() - ph * s - margin, ny));
        }
        animateCamera(s, nx, ny, z, duration);
    }

    private void animateCamera(float s, float nx, float ny, float z, long duration) {
        if (camera != null) {
            camera.cancel();
        }
        final float s0 = scale, x0 = tx, y0 = ty;
        zoom = z;
        camera = ValueAnimator.ofFloat(0, 1);
        camera.setDuration(duration);
        camera.setInterpolator(new PathInterpolator(0.45f, 0, 0.2f, 1));
        camera.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            scale = AndroidUtilities.lerp(s0, s, f);
            tx = AndroidUtilities.lerp(x0, nx, f);
            ty = AndroidUtilities.lerp(y0, ny, f);
            apply();
        });
        camera.start();
    }

    public float getZoom() {
        return zoom;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (child == phone && scale > 0) {
            float pw = AndroidUtilities.dp(PHONE_W_DP) * scale, ph = AndroidUtilities.dp(PHONE_H_DP) * scale;
            float b = AndroidUtilities.dp(11) * scale;
            rect.set(tx - b, ty - b, tx + pw + b, ty + ph + b);
            float r = AndroidUtilities.dp(56) * scale;
            canvas.drawRoundRect(rect, r, r, bezelPaint);
            edgePaint.setStrokeWidth(AndroidUtilities.dp(2.5f) * scale);
            edgePaint.setColor(0xFF4A4B50);
            rect.inset(-edgePaint.getStrokeWidth() / 2, -edgePaint.getStrokeWidth() / 2);
            canvas.drawRoundRect(rect, r, r, edgePaint);
            boolean result = super.drawChild(canvas, child, drawingTime);
            float cr = AndroidUtilities.dp(6.5f) * scale;
            canvas.drawCircle(tx + pw / 2f, ty + AndroidUtilities.dp(16) * scale, cr, cameraPaint);
            return result;
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        glow.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        glow.stop();
    }

    /** The screen, scaled down, blurred and darkened; refreshed a few times per second. */
    private class GlowView extends View {

        private static final int DIV = 14;
        private static final int GLOW_ALPHA = 120;
        private final Paint base = new Paint();
        private int lastH;
        // A fresh snapshot is blended into `accum` a little at a time, so the glow drifts
        // smoothly between screens instead of jumping (which read as flicker).
        private Bitmap accum, snap;
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final Paint blend = new Paint(Paint.FILTER_BITMAP_FLAG);
        private boolean running;

        private final Runnable tick = new Runnable() {
            @Override
            public void run() {
                if (!running) {
                    return;
                }
                snapshot();
                postDelayed(this, 90);
            }
        };

        GlowView(Context context) {
            super(context);
            ColorMatrix cm = new ColorMatrix();
            cm.setSaturation(1.5f);
            ColorMatrix dark = new ColorMatrix();
            dark.setScale(0.8f, 0.8f, 0.8f, 1);
            cm.postConcat(dark);
            paint.setColorFilter(new ColorMatrixColorFilter(cm));
            paint.setAlpha(GLOW_ALPHA);
            blend.setAlpha(36);
            setRenderEffect(RenderEffect.createBlurEffect(AndroidUtilities.dp(38), AndroidUtilities.dp(38), Shader.TileMode.CLAMP));
        }

        void start() {
            running = true;
            post(tick);
        }

        void stop() {
            running = false;
            removeCallbacks(tick);
        }

        private void snapshot() {
            int w = phone.getWidth() / DIV, h = phone.getHeight() / DIV;
            if (w <= 0 || h <= 0) {
                return;
            }
            if (snap == null || snap.getWidth() != w || snap.getHeight() != h) {
                snap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                accum = null;
            }
            snap.eraseColor(Color.WHITE);
            Canvas c = new Canvas(snap);
            c.scale(1f / DIV, 1f / DIV);
            try {
                phone.draw(c);
            } catch (Throwable e) {
                android.util.Log.w("LabStage", "snapshot failed", e);
                return;
            }
            if (accum == null) {
                accum = snap.copy(Bitmap.Config.ARGB_8888, true);
            } else {
                new Canvas(accum).drawBitmap(snap, 0, 0, blend);
            }
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // Blue backdrop as in Telegram's own demo videos; the blurred screen tints it.
            if (base == null || base.getShader() == null || lastH != getHeight()) {
                lastH = getHeight();
                base.setShader(new android.graphics.RadialGradient(getWidth() * 0.3f, getHeight() * 0.2f, getHeight() * 1.1f,
                        new int[]{0xFF4F9BFF, 0xFF1F5FE0, 0xFF0C2C8C}, new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
            }
            canvas.drawRect(0, 0, getWidth(), getHeight(), base);
            if (accum == null) {
                return;
            }
            // The glow fills the frame: the screen stretched around the phone's center.
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float s = Math.max(getWidth() * 1.25f / accum.getWidth(), getHeight() * 1.25f / accum.getHeight());
            canvas.save();
            canvas.translate(cx, cy);
            canvas.scale(s, s);
            canvas.translate(-accum.getWidth() / 2f, -accum.getHeight() / 2f);
            canvas.drawBitmap(accum, 0, 0, paint);
            canvas.restore();
        }
    }
}
