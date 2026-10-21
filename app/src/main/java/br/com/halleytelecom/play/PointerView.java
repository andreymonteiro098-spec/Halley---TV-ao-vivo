package br.com.halleytelecom.play;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/**
 * Cursor (seta) desenhado por cima do site, movido com as setas do controle remoto.
 */
public class PointerView extends View {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrow = new Path();
    private float px = -1f;
    private float py = -1f;
    private boolean pressedLook = false;

    public PointerView(Context context) {
        super(context);
        float density = context.getResources().getDisplayMetrics().density;
        float s = 1.1f * density;

        setFocusable(false);
        setClickable(false);

        fill.setColor(Color.WHITE);
        fill.setStyle(Paint.Style.FILL);

        stroke.setColor(Color.BLACK);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2f * density);
        stroke.setStrokeJoin(Paint.Join.ROUND);

        arrow.moveTo(0f, 0f);
        arrow.lineTo(0f, 19f * s);
        arrow.lineTo(4.5f * s, 15f * s);
        arrow.lineTo(7.5f * s, 22f * s);
        arrow.lineTo(10.5f * s, 20.5f * s);
        arrow.lineTo(7.5f * s, 14f * s);
        arrow.lineTo(13.5f * s, 14f * s);
        arrow.close();
    }

    public float getPx() {
        return px;
    }

    public float getPy() {
        return py;
    }

    public void moveTo(float x, float y) {
        px = x;
        py = y;
        invalidate();
    }

    /** Pequeno efeito visual quando o botão OK é pressionado. */
    public void pulse() {
        pressedLook = true;
        invalidate();
        postDelayed(new Runnable() {
            @Override
            public void run() {
                pressedLook = false;
                invalidate();
            }
        }, 120);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (px < 0f || py < 0f) {
            px = w / 2f;
            py = h / 2f;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (px < 0f || py < 0f) {
            return;
        }
        canvas.save();
        canvas.translate(px, py);
        if (pressedLook) {
            canvas.scale(0.85f, 0.85f);
        }
        canvas.drawPath(arrow, fill);
        canvas.drawPath(arrow, stroke);
        canvas.restore();
    }
}
