package app.clickcounter;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;

import java.util.Locale;
import java.util.Random;

/** A single, self-contained mechanical counter. All artwork is drawn at device resolution. */
public final class MainActivity extends Activity {
    private SharedPreferences preferences;
    private CounterLayout counter;
    private int value;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences("counter", MODE_PRIVATE);
        value = Math.max(0, Math.min(9999, state == null
                ? preferences.getInt("value", 0) : state.getInt("value", 0)));

        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        if (Build.VERSION.SDK_INT >= 29) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        }

        counter = new CounterLayout(this);
        setContentView(counter);
        counter.requestApplyInsets();
    }

    private void changeValue(int next, View source) {
        next = Math.max(0, Math.min(9999, next));
        if (next == value) return;
        int previous = value;
        value = next;
        preferences.edit().putInt("value", value).apply();
        counter.artwork.changeValue(previous, value);
        source.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("value", value);
        super.onSaveInstanceState(outState);
    }

    private final class CounterLayout extends ViewGroup {
        final CounterArtwork artwork;
        final Button minus;
        final Button plus;
        final Button reset;
        private int insetLeft, insetTop, insetRight, insetBottom;

        CounterLayout(Context context) {
            super(context);
            setClipChildren(false);
            if (Build.VERSION.SDK_INT >= 29) setForceDarkAllowed(false);
            artwork = new CounterArtwork(context);
            addView(artwork);
            boolean russian = Locale.getDefault().getLanguage().equals("ru");
            minus = hitButton(russian ? "Уменьшить счётчик" : "Decrease counter", 0);
            plus = hitButton(russian ? "Увеличить счётчик" : "Increase counter", 1);
            reset = hitButton(russian ? "Сбросить счётчик" : "Reset counter", 2);
            minus.setOnClickListener(v -> changeValue(value - 1, v));
            plus.setOnClickListener(v -> changeValue(value + 1, v));
            reset.setOnClickListener(v -> changeValue(0, v));

            setOnApplyWindowInsetsListener((view, insets) -> {
                if (Build.VERSION.SDK_INT >= 30) {
                    android.graphics.Insets safe = insets.getInsets(
                            WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                    insetLeft = safe.left;
                    insetTop = safe.top;
                    insetRight = safe.right;
                    insetBottom = safe.bottom;
                } else {
                    insetLeft = insets.getSystemWindowInsetLeft();
                    insetTop = insets.getSystemWindowInsetTop();
                    insetRight = insets.getSystemWindowInsetRight();
                    insetBottom = insets.getSystemWindowInsetBottom();
                    if (Build.VERSION.SDK_INT >= 28 && insets.getDisplayCutout() != null) {
                        insetLeft = Math.max(insetLeft, insets.getDisplayCutout().getSafeInsetLeft());
                        insetTop = Math.max(insetTop, insets.getDisplayCutout().getSafeInsetTop());
                        insetRight = Math.max(insetRight, insets.getDisplayCutout().getSafeInsetRight());
                        insetBottom = Math.max(insetBottom, insets.getDisplayCutout().getSafeInsetBottom());
                    }
                }
                requestLayout();
                return insets;
            });
        }

        private Button hitButton(String label, int buttonIndex) {
            Button button = new Button(getContext());
            button.setBackgroundColor(Color.TRANSPARENT);
            button.setText("");
            button.setPadding(0, 0, 0, 0);
            button.setMinimumHeight(0);
            button.setMinimumWidth(0);
            button.setMinHeight(0);
            button.setMinWidth(0);
            button.setContentDescription(label);
            button.setSoundEffectsEnabled(false);
            button.setStateListAnimator(null);
            button.setOnTouchListener((view, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    artwork.pressed[buttonIndex] = true;
                    artwork.postInvalidateOnAnimation();
                } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                        || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    artwork.pressed[buttonIndex] = false;
                    artwork.postInvalidateOnAnimation();
                } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    artwork.pressed[buttonIndex] = event.getX() >= 0 && event.getY() >= 0
                            && event.getX() < view.getWidth() && event.getY() < view.getHeight();
                    artwork.postInvalidateOnAnimation();
                }
                return false;
            });
            button.setOnFocusChangeListener((view, focused) -> {
                artwork.focused[buttonIndex] = focused;
                artwork.invalidate();
            });
            addView(button);
            return button;
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            int width = MeasureSpec.getSize(widthSpec);
            int height = MeasureSpec.getSize(heightSpec);
            setMeasuredDimension(width, height);
            updateGeometry(width, height);
            artwork.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            measureHit(minus, 134, 134);
            measureHit(plus, 134, 134);
            measureHit(reset, 110, 48);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int width = right - left;
            int height = bottom - top;
            artwork.layout(0, 0, width, height);
            layoutHit(minus, 109, 285);
            layoutHit(plus, 281, 285);
            layoutHit(reset, 195, 423);
            artwork.invalidate();
        }

        private void updateGeometry(int width, int height) {
            float availableWidth = Math.max(1, width - insetLeft - insetRight);
            float availableHeight = Math.max(1, height - insetTop - insetBottom);
            artwork.scale = Math.min(availableWidth / 390f, availableHeight / 550f);
            artwork.originX = insetLeft + availableWidth / 2f - 195f * artwork.scale;
            artwork.originY = insetTop + availableHeight * .50f - 218f * artwork.scale;
        }

        private void measureHit(View button, float width, float height) {
            float minimum = 48 * getResources().getDisplayMetrics().density;
            int pixelWidth = Math.round(Math.max(minimum, width * artwork.scale));
            int pixelHeight = Math.round(Math.max(minimum, height * artwork.scale));
            button.measure(MeasureSpec.makeMeasureSpec(pixelWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(pixelHeight, MeasureSpec.EXACTLY));
        }

        private void layoutHit(View button, float x, float y) {
            int centerX = Math.round(artwork.originX + x * artwork.scale);
            int centerY = Math.round(artwork.originY + y * artwork.scale);
            int left = centerX - button.getMeasuredWidth() / 2;
            int top = centerY - button.getMeasuredHeight() / 2;
            button.layout(left, top, left + button.getMeasuredWidth(), top + button.getMeasuredHeight());
        }
    }

    private final class CounterArtwork extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        private final Paint grain = new Paint();
        private final RectF rect = new RectF();
        private final Typeface digitFace = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
        private final Typeface labelFace = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        final boolean[] pressed = new boolean[3];
        final boolean[] focused = new boolean[3];
        private final float[] depression = new float[3];
        float scale = 1, originX, originY;
        private String oldDigits = digits(value);
        private String newDigits = oldDigits;
        private int direction = 1;
        private long transitionStarted;
        private long lastFrame;
        private boolean animate;

        CounterArtwork(Context context) {
            super(context);
            setFocusable(true);
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            updateDescription();
            Bitmap noise = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
            Random random = new Random(4271);
            for (int y = 0; y < 96; y++) {
                for (int x = 0; x < 96; x++) {
                    int shade = random.nextBoolean() ? 255 : 0;
                    noise.setPixel(x, y, Color.argb(random.nextInt(12), shade, shade, shade));
                }
            }
            grain.setShader(new BitmapShader(noise, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
        }

        void changeValue(int previous, int next) {
            oldDigits = digits(previous);
            newDigits = digits(next);
            direction = next > previous ? 1 : -1;
            transitionStarted = SystemClock.uptimeMillis();
            updateDescription();
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED);
            postInvalidateOnAnimation();
        }

        private void updateDescription() {
            setContentDescription((Locale.getDefault().getLanguage().equals("ru")
                    ? "Счётчик: " : "Counter: ") + value);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            long now = SystemClock.uptimeMillis();
            float elapsed = lastFrame == 0 ? 16 : Math.min(48, now - lastFrame);
            lastFrame = now;
            animate = false;
            for (int i = 0; i < 3; i++) {
                float target = pressed[i] ? 1 : 0;
                depression[i] += (target - depression[i]) * (1 - (float) Math.exp(-elapsed / 38));
                if (Math.abs(target - depression[i]) > .005f) animate = true;
                else depression[i] = target;
            }

            paint.setColor(Color.WHITE);
            paint.setShader(new LinearGradient(0, 0, getWidth() * .6f, getHeight(),
                    new int[]{0xff171b1f, 0xff101418, 0xff0b0e11}, null, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            paint.setShader(new RadialGradient(getWidth() * .5f, getHeight() * .36f,
                    Math.max(getWidth(), getHeight()) * .65f,
                    new int[]{0x102e3942, 0x00000000}, null, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            canvas.drawRect(0, 0, getWidth(), getHeight(), grain);
            paint.setShader(null);

            canvas.save();
            canvas.translate(originX, originY);
            canvas.scale(scale, scale);
            drawCounter(canvas, now);
            drawButton(canvas, 109, 285, 0, false);
            drawButton(canvas, 281, 285, 1, true);
            drawReset(canvas);
            canvas.restore();
            if (animate) postInvalidateOnAnimation();
        }

        private void drawCounter(Canvas canvas, long now) {
            // A recessed machined frame with a fine, cold highlight along the upper/right edge.
            paint.setColor(Color.WHITE);
            paint.setShader(new RadialGradient(195, 161, 185,
                    new int[]{0x8a000000, 0x00000000}, null, Shader.TileMode.CLAMP));
            oval(canvas, 8, 105, 382, 196);
            paint.setShader(null);
            round(canvas, 22, 11, 368, 165, 20, 0xff080b0d);
            gradient(22, 14, 368, 159, new int[]{0xff424a51, 0xff171d22, 0xff515b63});
            round(canvas, 23, 10, 367, 159, 18);
            gradient(24, 11, 366, 158, new int[]{0xff252b30, 0xff151a1f, 0xff1b2228});
            round(canvas, 24.5f, 11.5f, 365.5f, 157.5f, 17);
            round(canvas, 32, 20, 358, 149, 12, 0xff080b0e);
            gradient(35, 25, 355, 147, new int[]{0xff020406, 0xff1f272d});
            round(canvas, 35, 24, 355, 147, 10);

            float progress = transitionStarted == 0 ? 1 : Math.min(1, (now - transitionStarted) / 175f);
            // Cubic ease-out makes every tap immediate while retaining a short mechanical movement.
            float eased = 1 - (float) Math.pow(1 - progress, 3);
            if (progress < 1) animate = true;
            for (int i = 0; i < 4; i++) {
                float x = 42 + i * 78;
                gradient(x, 28, x, 141, new int[]{0xff161b1f, 0xff22282d, 0xff11161a},
                        new float[]{0, .49f, 1});
                round(canvas, x, 28, x + 72, 141, 5);
                canvas.save();
                canvas.clipRect(x + 1, 29, x + 71, 140);
                paint.setShader(null);
                paint.setTypeface(digitFace);
                paint.setTextSize(89);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setColor(0xffe8e6dc);
                Paint.FontMetrics metrics = paint.getFontMetrics();
                float baseline = 85 - (metrics.ascent + metrics.descent) / 2;
                boolean changing = oldDigits.charAt(i) != newDigits.charAt(i) && progress < 1;
                if (changing) {
                    canvas.drawText(oldDigits.substring(i, i + 1), x + 36,
                            baseline - direction * eased * 112, paint);
                    canvas.drawText(newDigits.substring(i, i + 1), x + 36,
                            baseline + direction * (1 - eased) * 112, paint);
                } else {
                    canvas.drawText(newDigits.substring(i, i + 1), x + 36, baseline, paint);
                }
                // Shading belongs to the stationary window, including during a digit transition.
                gradient(x, 29, x, 141,
                        new int[]{0x9805090c, 0x00000000, 0x09000000, 0x88040608},
                        new float[]{0, .22f, .72f, 1});
                canvas.drawRect(x, 29, x + 72, 141, paint);
                paint.setShader(null);
                line(canvas, x, 84.4f, x + 72, 84.4f, 0x9a030507, 1.4f);
                line(canvas, x, 85.7f, x + 72, 85.7f, 0x1effffff, .65f);
                canvas.restore();
                line(canvas, x + 1, 33, x + 1, 137, 0x1effffff, .6f);
                line(canvas, x + 71, 33, x + 71, 137, 0x90000000, .9f);
            }
            line(canvas, 40, 23, 350, 23, 0x26000000, 1);
            line(canvas, 41, 148, 350, 148, 0x273f4b55, 1);
            // Delicate brushed-metal strokes; they add material without visual clutter.
            for (int i = 0; i < 5; i++) {
                line(canvas, 47, 14 + i * .65f, 342, 14 + i * .65f,
                        i % 2 == 0 ? 0x0bffffff : 0x14000000, .25f);
            }
        }

        private void drawButton(Canvas canvas, float x, float y, int index, boolean isPlus) {
            float pressedAmount = depression[index];
            paint.setColor(Color.WHITE);
            paint.setShader(new RadialGradient(x, y + 13, 84,
                    new int[]{0x99000000, 0x60000000, 0x00000000},
                    new float[]{0, .65f, 1}, Shader.TileMode.CLAMP));
            oval(canvas, x - 86, y - 57, x + 86, y + 97);
            paint.setShader(null);
            circle(canvas, x, y + 3, 66, 0xff080a0c);
            gradient(x - 50, y - 65, x + 50, y + 65,
                    new int[]{0xff485159, 0xff13191e, 0xff070a0c});
            canvas.drawCircle(x, y, 65, paint);
            gradient(x - 40, y - 66, x + 42, y + 62,
                    new int[]{0xff12171b, 0xff090d10, 0xff4d5962});
            canvas.drawCircle(x, y + 1, 62.4f, paint);
            canvas.save();
            canvas.translate(0, pressedAmount * 3.4f);
            paint.setShader(new RadialGradient(x - 23, y - 31, 115,
                    new int[]{0xff394047, 0xff282f35, 0xff171d22},
                    new float[]{0, .48f, 1}, Shader.TileMode.CLAMP));
            canvas.drawCircle(x, y - 1, 59.5f - pressedAmount * .4f, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(.65f);
            paint.setColor(isPlus ? 0x6777838d : 0x3a78848d);
            rect.set(x - 59, y - 60, x + 59, y + 58);
            canvas.drawArc(rect, 202, 150, false, paint);
            paint.setColor(0x38030608);
            canvas.drawArc(rect, 18, 153, false, paint);
            paint.setStyle(Paint.Style.FILL);
            if (pressedAmount > 0) circle(canvas, x, y - 1, 59, Color.argb(
                    Math.round(pressedAmount * 26), 0, 0, 0));

            // An engraved shadow under the clean ivory symbols.
            paint.setStrokeCap(Paint.Cap.ROUND);
            line(canvas, x - 12, y + 1, x + 12, y + 1, 0x95000000, 3.3f);
            if (isPlus) line(canvas, x, y - 11, x, y + 13, 0x95000000, 3.3f);
            line(canvas, x - 12, y, x + 12, y, 0xffe6e9e9, 2.8f);
            if (isPlus) line(canvas, x, y - 12, x, y + 12, 0xffe6e9e9, 2.8f);
            paint.setStrokeCap(Paint.Cap.BUTT);
            canvas.restore();
            if (isPlus) {
                // The fine blue-white edge catches the light on the exposed metal rim.
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(.9f);
                paint.setColor(0x88c3d7e7);
                rect.set(x - 64, y - 64, x + 64, y + 64);
                canvas.drawArc(rect, 288, 113, false, paint);
                paint.setStyle(Paint.Style.FILL);
            }
            if (focused[index]) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1.4f);
                paint.setColor(0xffc1ccd5);
                canvas.drawCircle(x, y, 70, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }

        private void drawReset(Canvas canvas) {
            float amount = depression[2];
            float y = 423 + amount * 1.6f;
            round(canvas, 146, 404, 244, 444, 20, 0x34000000);
            gradient(147, y - 20, 243, y + 20, new int[]{0xff30383e, 0xff12171b});
            round(canvas, 147, y - 20, 243, y + 20, 20);
            gradient(148, y - 19, 242, y + 19, new int[]{0xff1e252a, 0xff171d22});
            round(canvas, 148, y - 19, 242, y + 19, 19);
            paint.setShader(null);
            paint.setTypeface(labelFace);
            paint.setTextSize(10);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setColor(0xff8f9ba5);
            String label = "RESET";
            float spacing = 1.8f;
            float labelWidth = paint.measureText(label) + spacing * (label.length() - 1);
            float x = 195 - labelWidth / 2;
            Paint.FontMetrics metrics = paint.getFontMetrics();
            float baseline = y - (metrics.ascent + metrics.descent) / 2;
            for (int i = 0; i < label.length(); i++) {
                String character = label.substring(i, i + 1);
                canvas.drawText(character, x, baseline, paint);
                x += paint.measureText(character) + spacing;
            }
            if (focused[2]) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1);
                paint.setColor(0xffc1ccd5);
                rect.set(143, y - 24, 247, y + 24);
                canvas.drawRoundRect(rect, 24, 24, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }

        private void gradient(float x1, float y1, float x2, float y2, int[] colors) {
            gradient(x1, y1, x2, y2, colors, null);
        }

        private void gradient(float x1, float y1, float x2, float y2, int[] colors, float[] stops) {
            paint.setColor(Color.WHITE);
            paint.setShader(new LinearGradient(x1, y1, x2, y2, colors, stops, Shader.TileMode.CLAMP));
        }

        private void round(Canvas canvas, float l, float t, float r, float b, float radius, int color) {
            paint.setShader(null);
            paint.setColor(color);
            round(canvas, l, t, r, b, radius);
        }

        private void round(Canvas canvas, float l, float t, float r, float b, float radius) {
            rect.set(l, t, r, b);
            canvas.drawRoundRect(rect, radius, radius, paint);
        }

        private void oval(Canvas canvas, float l, float t, float r, float b) {
            rect.set(l, t, r, b);
            canvas.drawOval(rect, paint);
        }

        private void circle(Canvas canvas, float x, float y, float radius, int color) {
            paint.setShader(null);
            paint.setColor(color);
            canvas.drawCircle(x, y, radius, paint);
        }

        private void line(Canvas canvas, float x1, float y1, float x2, float y2,
                          int color, float thickness) {
            paint.setShader(null);
            paint.setColor(color);
            paint.setStrokeWidth(thickness);
            canvas.drawLine(x1, y1, x2, y2, paint);
        }

        private String digits(int number) {
            return String.format(Locale.ROOT, "%04d", number);
        }
    }
}
