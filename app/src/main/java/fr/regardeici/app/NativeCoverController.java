package fr.regardeici.app;

import android.app.Activity;
import android.net.Uri;
import androidx.core.util.Consumer;
import androidx.window.area.WindowAreaCapability;
import androidx.window.area.WindowAreaController;
import androidx.window.area.WindowAreaInfo;
import androidx.window.area.WindowAreaPresentationSessionCallback;
import androidx.window.area.WindowAreaSessionPresenter;
import androidx.window.java.area.WindowAreaControllerCallbackAdapter;
import java.util.ArrayList;
import java.util.List;

/** Activates the rear screen through the official Jetpack WindowManager extension. */
@androidx.annotation.OptIn(markerClass=androidx.window.core.ExperimentalWindowApi.class)
public final class NativeCoverController {
    private final Activity activity;
    private final Consumer<String> status;
    private final Runnable clipError;
    private WindowAreaController controller;
    private WindowAreaControllerCallbackAdapter adapter;
    private List<WindowAreaInfo> areas=new ArrayList<>();
    private Runnable onAreasChanged=()->{};
    private final Consumer<List<WindowAreaInfo>> listener=value->{areas=value;onAreasChanged.run();};
    private WindowAreaSessionPresenter session;
    private AnimationContent content;
    private int character,position;
    private Uri clip;
    private boolean requested;
    private int generation;
    private String lastResult="Pas de session demandée.";

    public NativeCoverController(Activity owner,Consumer<String> onStatus,Runnable onClipError) {
        activity=owner;status=onStatus;clipError=onClipError;
        try {
            controller=WindowAreaController.getOrCreate();
            adapter=new WindowAreaControllerCallbackAdapter(controller);
            adapter.addWindowAreaInfoListListener(activity.getMainExecutor(),listener);
        } catch(RuntimeException | LinkageError e) { lastResult=e.getClass().getSimpleName()+": "+e.getMessage(); }
    }
    public boolean start(int which,int where,Uri video) {
        if(requested) return true;
        if(controller==null) return false;
        for(WindowAreaInfo area:areas) {
            if(!area.getType().equals(WindowAreaInfo.Type.TYPE_REAR_FACING)) continue;
            if(!area.getCapability(WindowAreaCapability.Operation.OPERATION_PRESENT_ON_AREA).getStatus()
                    .equals(WindowAreaCapability.Status.WINDOW_AREA_STATUS_AVAILABLE)) continue;
            character=which;position=where;clip=video;requested=true;
            final int attempt=++generation;
            status.accept("Activation de l’écran externe…");
            try {
                controller.presentContentOnWindowArea(area.getToken(),activity,activity.getMainExecutor(),new WindowAreaPresentationSessionCallback() {
                    @Override public void onSessionStarted(WindowAreaSessionPresenter started) {
                        if(!requested || attempt!=generation || activity.isDestroyed() || activity.isFinishing()) {
                            try { started.close(); } catch(RuntimeException ignored) { }
                            return;
                        }
                        session=started;
                        try {
                            content=new AnimationContent(started.getContext(),clipError);
                            content.update(character,position,clip);started.setContentView(content);
                            lastResult="Session native démarrée.";
                            status.accept("Animation active sur l’écran externe");
                        } catch(RuntimeException e) {
                            lastResult=e.getClass().getSimpleName()+": "+e.getMessage();
                            stop();status.accept("Écran externe indisponible · réessayer");
                        }
                    }
                    @Override public void onSessionEnded(Throwable error) {
                        if(attempt!=generation) return;
                        requested=false;session=null;
                        if(content!=null) { content.release();content=null; }
                        lastResult=error==null?"Session terminée.":error.getClass().getSimpleName()+": "+error.getMessage();
                        status.accept(error==null?"Animation arrêtée":"Activation native refusée · voir Diagnostic");
                    }
                    @Override public void onContainerVisibilityChanged(boolean visible) {
                        if(attempt==generation && requested) status.accept(visible?"Animation visible sur l’écran externe":"Animation externe masquée");
                    }
                });
                return true;
            } catch(RuntimeException | LinkageError e) {
                requested=false;lastResult=e.getClass().getSimpleName()+": "+e.getMessage();return false;
            }
        }
        return false;
    }
    public void update(int which,int where,Uri video) {
        character=which;position=where;clip=video;
        if(content!=null) content.update(which,where,video);
    }
    public boolean isRequested() { return requested; }
    public void setOnAreasChanged(Runnable listener) { onAreasChanged=listener;listener.run(); }
    public void stop() {
        requested=false;generation++;
        if(content!=null) { content.release();content=null; }
        WindowAreaSessionPresenter previous=session;session=null;
        if(previous!=null) {
            try { previous.close(); }
            catch(RuntimeException e) { lastResult=e.getClass().getSimpleName()+": "+e.getMessage(); }
        }
    }
    public void dispose() { stop();if(adapter!=null) adapter.removeWindowAreaInfoListListener(listener); }
    public String diagnostics() {
        StringBuilder s=new StringBuilder("WindowManager : ").append(areas.size()).append(" zone(s)\n");
        for(WindowAreaInfo area:areas) s.append(area.getType()).append(" : ")
            .append(area.getCapability(WindowAreaCapability.Operation.OPERATION_PRESENT_ON_AREA).getStatus()).append('\n');
        return s.append("Activation native : ").append(lastResult).toString();
    }
}
