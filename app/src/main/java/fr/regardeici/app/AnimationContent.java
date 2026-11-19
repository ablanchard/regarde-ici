package fr.regardeici.app;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.VideoView;

/** Shared content for standard Android and WindowManager rear-display presentations. */
public final class AnimationContent extends FrameLayout {
    private CartoonView cartoon;
    private VideoView video;
    private final Runnable clipError;

    public AnimationContent(Context context, Runnable onClipError) {
        super(context); clipError=onClipError; setBackgroundColor(Color.BLACK); setKeepScreenOn(true);
    }
    public void update(int character, int position, Uri clip) {
        release(); removeAllViews();
        if(clip==null) {
            cartoon=new CartoonView(getContext()); cartoon.setCharacter(character); cartoon.setPosition(position);
            addView(cartoon,new LayoutParams(-1,-1));
        } else {
            VideoView player=new VideoView(getContext()); video=player;
            addView(player,new LayoutParams(-1,-1,Gravity.CENTER));
            player.setAudioFocusRequest(android.media.AudioManager.AUDIOFOCUS_NONE);
            player.setOnPreparedListener(mp->{
                if(video!=player) return;
                mp.setLooping(true); mp.setVolume(0,0); player.start();
            });
            player.setOnErrorListener((mp,what,extra)->{
                if(video==player) post(clipError);
                return true;
            });
            try { player.setVideoURI(clip); }
            catch (SecurityException | IllegalArgumentException e) { post(clipError); }
        }
    }
    public void release() {
        if(cartoon!=null) { cartoon.setRunning(false); cartoon=null; }
        if(video!=null) { VideoView previous=video; video=null; previous.stopPlayback(); }
    }
    @Override protected void onDetachedFromWindow() { release(); super.onDetachedFromWindow(); }
}
