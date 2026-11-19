package fr.regardeici.app;

import android.app.Presentation;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;

/** Presentation for an already available display; activation is handled separately. */
public final class CoverPresentation extends Presentation {
    private AnimationContent content;
    private final int initialCharacter, initialPosition;
    private final Uri initialClip;
    private final Runnable clipError;

    public CoverPresentation(Context context, Display display, int character, int position,
                             Uri clip, Runnable onClipError) {
        super(context,display);
        initialCharacter=character; initialPosition=position; initialClip=clip; clipError=onClipError;
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        content=new AnimationContent(getContext(),clipError); setContentView(content);
        update(initialCharacter,initialPosition,initialClip);
    }
    public void update(int character, int position, Uri clip) {
        if(content!=null) content.update(character,position,clip);
    }
    @Override protected void onStop() { if(content!=null) content.release(); super.onStop(); }
}
