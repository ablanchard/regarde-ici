package fr.regardeici.app;

import android.app.Presentation;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.VideoView;

/** Uses only the public Presentation API. Cannot turn on a Samsung cover display. */
public final class CoverPresentation extends Presentation {
    private CartoonView cartoon;
    private VideoView video;
    private FrameLayout root;
    private final int initialCharacter, initialPosition;
    private final Uri initialClip;
    private final Runnable clipError;

    public CoverPresentation(Context context, Display display, int character, int position,
                             Uri clip, Runnable onClipError) {
        super(context,display);
        initialCharacter=character; initialPosition=position; initialClip=clip;
        clipError=onClipError;
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        root=new FrameLayout(getContext()); root.setBackgroundColor(Color.BLACK);
        setContentView(root);
        update(initialCharacter,initialPosition,initialClip);
    }
    public void update(int character, int position, Uri clip) {
        if(root==null) return;
        releaseContent(); root.removeAllViews();
        if(clip==null) {
            cartoon=new CartoonView(getContext()); cartoon.setCharacter(character); cartoon.setPosition(position);
            root.addView(cartoon,new FrameLayout.LayoutParams(-1,-1));
        } else {
            VideoView player=new VideoView(getContext()); video=player;
            root.addView(player,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));
            player.setAudioFocusRequest(android.media.AudioManager.AUDIOFOCUS_NONE);
            player.setOnPreparedListener(mp->{
                if(video!=player) return;
                mp.setLooping(true); mp.setVolume(0,0); player.start();
            });
            player.setOnErrorListener((mp,what,extra)->{
                if(video!=player) return true;
                Toast.makeText(getContext(),"Vidéo illisible : retour au personnage.",Toast.LENGTH_LONG).show();
                clipError.run(); return true;
            });
            try { player.setVideoURI(clip); }
            catch (SecurityException | IllegalArgumentException e) { clipError.run(); }
        }
    }
    private void releaseContent() {
        if(cartoon!=null) { cartoon.setRunning(false); cartoon=null; }
        if(video!=null) { video.stopPlayback(); video=null; }
    }
    @Override protected void onStop() { releaseContent(); super.onStop(); }
}
