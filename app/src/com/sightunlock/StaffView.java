package com.sightunlock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class StaffView extends View {

    public static final int INK = 0xFFE8DDC5;
    // Slightly darker than the lock-screen slate so the clef reads as an inlay.
    public static final int CLEF_INK = 0xFF20242F;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint notePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clefPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int notePosition = 4;
    private SrsState.Clef clef = SrsState.Clef.TREBLE;

    public StaffView(Context c) { super(c); init(); }
    public StaffView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        linePaint.setColor(INK);
        linePaint.setStyle(Paint.Style.STROKE);
        notePaint.setColor(INK);
        notePaint.setStyle(Paint.Style.FILL);
        clefPaint.setColor(CLEF_INK);
        clefPaint.setStyle(Paint.Style.FILL);
        clefPaint.setTypeface(Typeface.SERIF);
        clefPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setNotePosition(int p) {
        notePosition = p;
        invalidate();
    }

    public void setClef(SrsState.Clef c) {
        clef = c;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        float spacing = h * 0.08f;
        float staffHeight = spacing * 4f;
        float staffTop = (h - staffHeight) / 2f;
        float staffBottom = staffTop + staffHeight;
        float lineThickness = Math.max(2f, spacing * 0.06f);
        linePaint.setStrokeWidth(lineThickness);

        float staffLeft = w * 0.08f;
        float staffRight = w - w * 0.08f;

        drawClef(canvas, staffLeft + spacing * 0.4f, staffTop, staffBottom, spacing);

        for (int i = 0; i < 5; i++) {
            float y = staffBottom - i * spacing;
            canvas.drawLine(staffLeft, y, staffRight, y, linePaint);
        }

        float noteCenterX = staffLeft + (staffRight - staffLeft) * 0.62f;
        float noteY = staffBottom - notePosition * (spacing / 2f);
        float noteRadiusX = spacing * 0.6f;
        float noteRadiusY = spacing * 0.45f;

        if (notePosition < 0 || notePosition > 8) {
            int start = notePosition < 0 ? -2 : 10;
            if (notePosition < 0) {
                for (int p = start; p >= notePosition; p -= 2) {
                    float ly = staffBottom - p * (spacing / 2f);
                    canvas.drawLine(noteCenterX - spacing * 0.9f, ly,
                            noteCenterX + spacing * 0.9f, ly, linePaint);
                }
            } else {
                for (int p = start; p <= notePosition; p += 2) {
                    float ly = staffBottom - p * (spacing / 2f);
                    canvas.drawLine(noteCenterX - spacing * 0.9f, ly,
                            noteCenterX + spacing * 0.9f, ly, linePaint);
                }
            }
        }

        RectF oval = new RectF(
                noteCenterX - noteRadiusX, noteY - noteRadiusY,
                noteCenterX + noteRadiusX, noteY + noteRadiusY);
        canvas.save();
        canvas.rotate(-20f, noteCenterX, noteY);
        canvas.drawOval(oval, notePaint);
        canvas.restore();

        boolean stemDown = notePosition >= 4;
        float stemX, stemTopY, stemBotY;
        float stemLen = spacing * 3.5f;
        if (stemDown) {
            stemX = noteCenterX - noteRadiusX + lineThickness;
            stemTopY = noteY;
            stemBotY = noteY + stemLen;
        } else {
            stemX = noteCenterX + noteRadiusX - lineThickness;
            stemTopY = noteY - stemLen;
            stemBotY = noteY;
        }
        Paint stemPaint = new Paint(notePaint);
        stemPaint.setStrokeWidth(lineThickness * 1.4f);
        stemPaint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(stemX, stemTopY, stemX, stemBotY, stemPaint);
    }

    private void drawClef(Canvas canvas, float x, float top, float bottom, float spacing) {
        boolean isTreble = (clef == SrsState.Clef.TREBLE);
        String glyph = isTreble ? "𝄞" : "𝄢";
        boolean hasGlyph = clefPaint.hasGlyph(glyph);
        if (!hasGlyph) {
            // Bundled fonts on extremely stripped-down ROMs may lack the
            // music symbols block. Fall back to a stylised letter.
            glyph = isTreble ? "G" : "F";
            clefPaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC));
        } else {
            clefPaint.setTypeface(Typeface.SERIF);
        }
        // Treble glyph extends one space above the staff to one below; bass
        // is more compact, sitting just inside the staff with a small overhang.
        float clefHeight = (isTreble
                ? (hasGlyph ? 6.4f : 4.4f)
                : (hasGlyph ? 4.6f : 3.8f)) * spacing;
        clefPaint.setTextSize(clefHeight);

        Rect bounds = new Rect();
        clefPaint.getTextBounds(glyph, 0, glyph.length(), bounds);
        float staffMiddle = bottom - 2f * spacing;
        float baselineY = staffMiddle - bounds.exactCenterY();
        float xCenter = x + spacing * 0.6f + bounds.width() / 2f;
        canvas.drawText(glyph, xCenter, baselineY, clefPaint);
    }
}
