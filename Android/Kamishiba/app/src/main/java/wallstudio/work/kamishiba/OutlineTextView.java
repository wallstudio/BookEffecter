package wallstudio.work.kamishiba;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

public class OutlineTextView extends AppCompatTextView {

    public OutlineTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        //canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        String text = (String) getText();
        int textColor = getCurrentTextColor();
        int textSize = (int) getTextSize();

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(getPaint().getTypeface());


        paint.setStrokeWidth(12.0f);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);

        Rect textBounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), textBounds);
        int posX = getWidth() / 2;
        int posY = getHeight() / 2 + textBounds.height() / 2 - textBounds.bottom;

        canvas.drawText(text, posX, posY, paint);

        paint.setStrokeWidth(0);
        paint.setColor(textColor);
        paint.setStyle(Paint.Style.FILL);

        canvas.drawText(text, posX, posY, paint);
    }
}