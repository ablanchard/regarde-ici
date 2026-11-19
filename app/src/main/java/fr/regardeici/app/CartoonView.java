package fr.regardeici.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.view.View;

/** Original vector cartoons, rendered locally at ~30 fps, with no media or network. */
public final class CartoonView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final long epoch = SystemClock.uptimeMillis();
    private int character;
    private int position;
    private boolean running = true;

    public CartoonView(Context context) { super(context); setContentDescription("Personnage animé"); }
    public void setCharacter(int value) { character = value; invalidate(); }
    public void setPosition(int value) { position = value; invalidate(); }
    public void setRunning(boolean value) { running = value; if (value) invalidate(); }

    private void oval(Canvas c, float x, float y, float rx, float ry, int color) {
        paint.setColor(color); paint.setStyle(Paint.Style.FILL);
        c.drawOval(x-rx, y-ry, x+rx, y+ry, paint);
    }
    private void line(Canvas c, float x, float y, float xx, float yy, float width, int color) {
        paint.setColor(color); paint.setStrokeWidth(width); paint.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(x,y,xx,yy,paint);
    }
    private void triangle(Canvas c, float x, float y, float xx, float yy, float xxx, float yyy, int color) {
        path.reset(); path.moveTo(x,y); path.lineTo(xx,yy); path.lineTo(xxx,yyy); path.close();
        paint.setStyle(Paint.Style.FILL); paint.setColor(color); c.drawPath(path,paint);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        c.drawColor(Color.rgb(15,36,40));
        float t = (SystemClock.uptimeMillis()-epoch)/1000f;
        float size = Math.min(getWidth(),getHeight());
        // Direction is selectable because lens position depends on how the phone is held.
        float anchor = position == 1 ? .27f : position == 2 ? .73f : .5f;
        c.save();
        c.translate(getWidth()*anchor, getHeight()*.48f);
        float scale = size / (position == 0 ? 310f : 420f);
        c.scale(scale,scale);
        int lime = Color.rgb(215,255,113), ink=Color.rgb(21,51,47);
        for (int i=0;i<6;i++) {
            float angle=(float)(i*Math.PI/3 + .10*Math.sin(t));
            float x=(float)Math.cos(angle)*133, y=(float)Math.sin(angle)*115;
            float r=3+(float)Math.sin(t*2+i)*1.2f;
            oval(c,x,y,r,r,Color.rgb(94,136,128));
        }
        oval(c,0,104,67,8,Color.rgb(8,22,24));
        c.translate(0,(float)Math.sin(t*2.6)*7);
        c.rotate((float)Math.sin(t*1.5)*4);
        int body = character == 1 ? Color.rgb(255,211,100) : character == 2 ? Color.rgb(192,173,255) : lime;
        oval(c,0,48,54,54,body);
        oval(c,-37,90,22,11,body); oval(c,37,90,22,11,body);
        c.save(); c.translate(-48,35); c.rotate(-25+(float)Math.sin(t*4)*18);
        oval(c,-15,-13,15,34,body); c.restore();
        c.save(); c.translate(48,35); c.rotate(25+(float)Math.sin(t*4)*25);
        oval(c,15,-15,15,34,body); c.restore();
        if(character==0) {
            triangle(c,-66,-40,-62,-101,-17,-64,body);
            triangle(c,66,-40,62,-101,17,-64,body);
            triangle(c,-53,-51,-51,-81,-31,-59,Color.rgb(255,160,167));
            triangle(c,53,-51,51,-81,31,-59,Color.rgb(255,160,167));
        } else if(character==2) {
            oval(c,-35,-87,15,47,body); oval(c,35,-87,15,47,body);
            oval(c,-35,-92,6,28,Color.rgb(255,183,207)); oval(c,35,-92,6,28,Color.rgb(255,183,207));
        } else {
            line(c,-8,-83,0,-100,6,body); line(c,0,-100,13,-97,6,body);
        }
        oval(c,0,-24,72,65,body);
        oval(c,-45,-7,13,8,Color.rgb(255,161,151)); oval(c,45,-7,13,8,Color.rgb(255,161,151));
        // Slow periodic blinking; no flashing background.
        boolean blink=(t%4.8f)>4.58f;
        for(int side=-1;side<=1;side+=2) {
            float x=side*26;
            if(blink) line(c,x-8,-32,x+8,-32,5,ink);
            else {
                oval(c,x,-33,11,16,ink);
                oval(c,x-3,-39,3.3f,4,Color.WHITE);
            }
        }
        if(character==1) oval(c,0,1,24,12,Color.rgb(255,132,61));
        else {
            triangle(c,-7,-9,7,-9,0,-2,ink);
            paint.setColor(ink); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(4);
            c.drawArc(-16,-4,16,19,15,150,false,paint); paint.setStyle(Paint.Style.FILL);
        }
        if(character==0) {
            line(c,-48,-1,-80,-7,3,ink); line(c,-48,8,-80,13,3,ink);
            line(c,48,-1,80,-7,3,ink); line(c,48,8,80,13,3,ink);
        }
        c.restore();
        if(running && isShown()) postInvalidateDelayed(33);
    }
}
