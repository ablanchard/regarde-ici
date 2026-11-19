package fr.regardeici.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraState;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class MainActivity extends ComponentActivity implements DisplayManager.DisplayListener {
    private static final int BG=0xff101815, PANEL=0xff1d2a24, INK=0xffeff5ed, LIME=0xffd7ff71;
    private PreviewView preview;
    private TextView cameraStatus, coverStatus, countdownLabel;
    private Button shutter, flashButton, timerButton, screenButton, animationButton;
    private DisplayManager displays;
    private CoverPresentation presentation;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private ImageCapture capture;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean foreground, cameraStarting, cameraReady, busy, counting, wantsCover;
    private int character, position, selectedDisplay=-1, remaining;
    private boolean timer, flash;
    private Uri clip;
    private String lastDisplayError="Aucun essai effectué.";
    private final ActivityResultLauncher<String> permission=registerForActivityResult(
        new ActivityResultContracts.RequestPermission(), granted->{
            if(granted) startCamera();
            else cameraStatus.setText("Caméra non autorisée · toucher ici pour réessayer");
        });
    private final ActivityResultLauncher<Intent> pickClip=registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), result->{
            if(result.getResultCode()!=RESULT_OK || result.getData()==null) return;
            Uri uri=result.getData().getData(); if(uri==null) return;
            try {
                getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
                clip=uri; saveChoices(); updateAnimation();
            } catch(SecurityException e) { toast("Impossible de conserver l’accès à cette vidéo. Choisir un fichier local."); }
        });
    private final Runnable tick=new Runnable() {
        @Override public void run() {
            if(!foreground) { cancelCountdown(); return; }
            if(remaining==0) { counting=false; countdownLabel.setText(""); shoot(); return; }
            countdownLabel.setText(String.valueOf(remaining--)); handler.postDelayed(this,1000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        displays=getSystemService(DisplayManager.class);
        var prefs=getPreferences(MODE_PRIVATE);
        character=prefs.getInt("character",0); position=prefs.getInt("position",0);
        timer=prefs.getBoolean("timer",false);
        String savedClip=prefs.getString("clip",null); if(savedClip!=null) clip=Uri.parse(savedClip);
        if(state!=null) { selectedDisplay=state.getInt("display",-1); wantsCover=state.getBoolean("cover",false); }
        buildUi();
        if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) startCamera();
        else permission.launch(Manifest.permission.CAMERA);
    }

    private void buildUi() {
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        root.setPadding(dp(16),0,dp(16),0); setContentView(root);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            var bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            v.setPadding(dp(16)+bars.left,bars.top,dp(16)+bars.right,bars.bottom); return insets;
        });
        root.requestApplyInsets();
        LinearLayout heading=row(); heading.setPadding(0,dp(10),0,dp(8));
        TextView title=text("Regarde ici !",26,LIME); title.setTypeface(null,Typeface.BOLD);
        heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button help=button("Aide",this::showHelp); heading.addView(help); root.addView(heading);
        cameraStatus=text("Ouverture de la caméra…",12,0xffb8c6bc);
        cameraStatus.setPadding(0,0,0,dp(8)); cameraStatus.setOnClickListener(v->requestCamera()); root.addView(cameraStatus);

        FrameLayout frame=new FrameLayout(this); frame.setBackground(rounded(0xff080d0b,20));
        frame.setClipToOutline(true); root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
        preview=new PreviewView(this); preview.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        preview.setScaleType(PreviewView.ScaleType.FIT_CENTER); frame.addView(preview,new FrameLayout.LayoutParams(-1,-1));
        preview.setOnTouchListener((v,event)->{
            if(event.getAction()==MotionEvent.ACTION_UP) {
                v.performClick();
                if(camera!=null && cameraReady) {
                    var point=preview.getMeteringPointFactory().createPoint(event.getX(),event.getY());
                    camera.getCameraControl().startFocusAndMetering(new FocusMeteringAction.Builder(point)
                        .setAutoCancelDuration(3,TimeUnit.SECONDS).build());
                }
            } return true;
        });
        countdownLabel=text("",76,Color.WHITE); countdownLabel.setGravity(Gravity.CENTER);
        frame.addView(countdownLabel,new FrameLayout.LayoutParams(-1,-1));
        countdownLabel.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(false);
        boolean landscape=getResources().getConfiguration().orientation==2;
        root.addView(scroll,new LinearLayout.LayoutParams(-1,dp(landscape?154:280)));
        LinearLayout controls=new LinearLayout(this); controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(0,dp(10),0,0); scroll.addView(controls);
        coverStatus=text("Écran externe : à tester sur ce téléphone",13,0xffcbd5cb);
        coverStatus.setPadding(dp(12),dp(9),dp(12),dp(9)); coverStatus.setBackground(rounded(PANEL,12)); controls.addView(coverStatus);
        LinearLayout screens=row();
        screenButton=button("Activer l’écran externe",this::chooseDisplay); weighted(screens,screenButton);
        screens.addView(button("Arrêter",()->{ wantsCover=false; dismissCover(); setCoverStatus("Animation arrêtée"); })); controls.addView(screens);
        LinearLayout animations=row(); animationButton=button(clip==null?characterName():"Vidéo personnelle",this::chooseAnimation);
        weighted(animations,animationButton); animations.addView(button("Position",()->{
            position=(position+1)%3; saveChoices(); updateAnimation();
            toast(new String[]{"Au centre","Vers la gauche","Vers la droite"}[position]);
        })); controls.addView(animations);
        LinearLayout options=row();
        timerButton=button(timer?"Retardateur 3 s":"Retardateur non",()->{
            timer=!timer; timerButton.setText(timer?"Retardateur 3 s":"Retardateur non"); saveChoices();
        }); weighted(options,timerButton);
        flashButton=button("Flash non",()->{ flash=!flash; if(capture!=null) capture.setFlashMode(flash?ImageCapture.FLASH_MODE_AUTO:ImageCapture.FLASH_MODE_OFF);
            flashButton.setText(flash?"Flash auto":"Flash non"); }); weighted(options,flashButton);
        options.addView(button("1× / 2×",()->{
            if(camera==null || !cameraReady || camera.getCameraInfo().getZoomState().getValue()==null) return;
            var zoom=camera.getCameraInfo().getZoomState().getValue();
            camera.getCameraControl().setZoomRatio(zoom.getZoomRatio()>1.1f?1f:Math.min(2f,zoom.getMaxZoomRatio()));
        })); controls.addView(options);
        shutter=button("●   PRENDRE LA PHOTO",this::takePhoto); shutter.setTextColor(BG); shutter.setTextSize(16);
        shutter.setBackground(rounded(LIME,30));
        shutter.setBackgroundTintList(android.content.res.ColorStateList.valueOf(LIME)); shutter.setEnabled(false);
        LinearLayout.LayoutParams shootParams=new LinearLayout.LayoutParams(-1,dp(58)); shootParams.setMargins(0,dp(8),0,dp(12)); root.addView(shutter,shootParams);
    }

    private void requestCamera() {
        if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) { startCamera(); return; }
        new AlertDialog.Builder(this).setTitle("Autoriser la caméra")
            .setMessage("L’accès à la caméra est nécessaire pour prendre une photo.")
            .setPositiveButton("Autoriser",(d,w)->permission.launch(Manifest.permission.CAMERA))
            .setNeutralButton("Paramètres",(d,w)->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))))
            .setNegativeButton("Fermer",null).show();
    }

    private void startCamera() {
        if(cameraStarting || camera!=null) return;
        cameraStarting=true;
        var future=ProcessCameraProvider.getInstance(this);
        future.addListener(()->{
            cameraStarting=false; if(isDestroyed()) return;
            try {
                cameraProvider=future.get();
                Preview usePreview=new Preview.Builder().build(); usePreview.setSurfaceProvider(preview.getSurfaceProvider());
                capture=new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setFlashMode(flash?ImageCapture.FLASH_MODE_AUTO:ImageCapture.FLASH_MODE_OFF).build();
                cameraProvider.unbindAll();
                camera=cameraProvider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,usePreview,capture);
                camera.getCameraInfo().getCameraState().observe(this,state->{
                    cameraReady=state.getType()==CameraState.Type.OPEN && state.getError()==null;
                    shutter.setEnabled(cameraReady && !busy && !counting);
                    if(state.getError()!=null) cameraStatus.setText("Caméra indisponible · fermer les autres applis photo puis relancer");
                    else cameraStatus.setText(cameraReady?"Caméra arrière · toucher l’image pour la netteté":"Connexion à la caméra…");
                });
                flashButton.setEnabled(camera.getCameraInfo().hasFlashUnit());
            } catch(Exception e) {
                camera=null; capture=null; cameraReady=false;
                cameraStatus.setText("Caméra indisponible · toucher ici pour réessayer");
            }
        },ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if(!foreground || !cameraReady || busy || counting || capture==null) return;
        if(timer) { counting=true; shutter.setEnabled(false); remaining=3; tick.run(); }
        else shoot();
    }
    private void shoot() {
        if(!foreground || !cameraReady || capture==null) { shutter.setEnabled(cameraReady); return; }
        busy=true; shutter.setEnabled(false); shutter.setText("Enregistrement…");
        capture.setTargetRotation(preview.getDisplay().getRotation());
        ContentValues values=new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME,"RegardeIci_"+new SimpleDateFormat("yyyyMMdd_HHmmss_SSS",Locale.ROOT).format(new Date())+".jpg");
        values.put(MediaStore.MediaColumns.MIME_TYPE,"image/jpeg");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,"Pictures/RegardeIci");
        var output=new ImageCapture.OutputFileOptions.Builder(getContentResolver(),MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values).build();
        try {
            capture.takePicture(output,ContextCompat.getMainExecutor(this),new ImageCapture.OnImageSavedCallback() {
                @Override public void onImageSaved(ImageCapture.OutputFileResults result) { finishCapture(); toast("Photo enregistrée dans Galerie → RegardeIci"); }
                @Override public void onError(ImageCaptureException e) { finishCapture(); toast("Photo non enregistrée : vérifier l’espace libre et réessayer."); }
            });
        } catch(RuntimeException e) { finishCapture(); toast("Prise de photo interrompue. Réessayer."); }
    }
    private void finishCapture() { busy=false; shutter.setEnabled(cameraReady && foreground); shutter.setText("●   PRENDRE LA PHOTO"); }
    private void cancelCountdown() { handler.removeCallbacks(tick); counting=false; countdownLabel.setText(""); shutter.setEnabled(cameraReady && !busy); }

    private void chooseDisplay() {
        Display[] all=displays.getDisplays(); ArrayList<Display> candidates=new ArrayList<>();
        for(Display d:all) if(d.getDisplayId()!=getDisplay().getDisplayId()) candidates.add(d);
        if(candidates.isEmpty()) {
            wantsCover=false;
            lastDisplayError="Aucun second écran exposé par DisplayManager.";
            setCoverStatus("Écran externe inaccessible à l’application");
            new AlertDialog.Builder(this).setTitle("Samsung ne propose aucun second écran")
                .setMessage("L’application ne peut pas allumer elle-même l’écran externe du Flip6. Aucun second écran n’est exposé ici. Le mode photo reste utilisable. Le diagnostic permet de vérifier la compatibilité de ce téléphone.")
                .setPositiveButton("Diagnostic",(d,w)->showDiagnostic()).setNegativeButton("Fermer",null).show(); return;
        }
        String[] labels=new String[candidates.size()];
        for(int i=0;i<labels.length;i++) { Display d=candidates.get(i); labels[i]=d.getName()+" · n° "+d.getDisplayId()+" · "+stateName(d); }
        new AlertDialog.Builder(this).setTitle("Choisir l’écran côté enfant")
            .setItems(labels,(dialog,which)->{
                dismissCover(); selectedDisplay=candidates.get(which).getDisplayId(); wantsCover=true; connectCover();
            }).setNegativeButton("Annuler",null).show();
    }
    private void connectCover() {
        if(!foreground || !wantsCover || selectedDisplay<0 || presentation!=null) return;
        Display d=displays.getDisplay(selectedDisplay);
        if(d==null) { setCoverStatus("Écran externe déconnecté"); return; }
        if(d.getState()!=Display.STATE_ON) { setCoverStatus("Écran externe éteint · activation bloquée par le système"); return; }
        try {
            CoverPresentation next=new CoverPresentation(this,d,character,position,clip,()->handler.post(()->{
                clip=null; saveChoices(); updateAnimation();
            }));
            next.setOnDismissListener(dialog->{
                if(presentation==next) { presentation=null; setCoverStatus("Affichage externe interrompu · réessayer"); }
            });
            next.show(); presentation=next;
            lastDisplayError="Fenêtre acceptée sur l’écran "+d.getDisplayId()+". Visibilité physique à confirmer.";
            setCoverStatus("Animation envoyée · vérifier l’écran côté enfant");
        } catch(WindowManager.InvalidDisplayException | SecurityException e) {
            lastDisplayError=e.getClass().getSimpleName()+": "+e.getMessage();
            setCoverStatus("Accès à l’écran externe refusé par Android / Samsung");
            toast("Cette version du système bloque l’affichage externe. Voir Aide → Diagnostic.");
        }
    }
    private void dismissCover() {
        CoverPresentation current=presentation; presentation=null;
        if(current!=null) current.dismiss();
    }
    private void updateAnimation() {
        animationButton.setText(clip==null?characterName():"Vidéo personnelle");
        if(presentation!=null) presentation.update(character,position,clip);
    }
    private String characterName() { return new String[]{"Animation : chat","Animation : canard","Animation : lapin"}[character]; }
    private void chooseAnimation() {
        new AlertDialog.Builder(this).setTitle("Attirer le regard")
            .setItems(new String[]{"Chat qui fait coucou","Petit canard","Lapin qui danse","Importer une vidéo (muette, en boucle)","Voir le personnage ici"},(d,which)->{
                if(which<3) { character=which; clip=null; saveChoices(); updateAnimation(); }
                else if(which==3) {
                    Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("video/*").addCategory(Intent.CATEGORY_OPENABLE);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    pickClip.launch(intent);
                } else {
                    CartoonView demo=new CartoonView(this); demo.setCharacter(character); demo.setPosition(position);
                    AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Aperçu du personnage").setView(demo)
                        .setPositiveButton("Fermer",null).create();
                    dialog.setOnDismissListener(v->demo.setRunning(false)); dialog.show();
                    demo.getLayoutParams().height=dp(300); demo.requestLayout();
                }
            }).setNegativeButton("Fermer",null).show();
    }
    private void saveChoices() {
        getPreferences(MODE_PRIVATE).edit().putInt("character",character).putInt("position",position)
            .putBoolean("timer",timer).putString("clip",clip==null?null:clip.toString()).apply();
    }
    private void showHelp() {
        new AlertDialog.Builder(this).setTitle("Une photo, un petit coucou")
            .setMessage("1. Ouvrir le Flip6, objectifs arrière vers l’enfant.\n\n2. Choisir un personnage ou importer une vidéo locale.\n\n3. Toucher « Activer l’écran externe ». Vérifier que l’animation est bien visible côté enfant.\n\n4. Cadrer puis prendre la photo. Le bouton de volume déclenche aussi. Les photos restent dans la galerie du téléphone.\n\nPrototype : l’accès simultané aux deux écrans dépend d’Android et de Samsung. L’application ne peut pas forcer l’allumage de l’écran externe. Good Lock seul ne garantit pas ce fonctionnement.")
            .setPositiveButton("Compris",null).setNeutralButton("Diagnostic",(d,w)->showDiagnostic()).show();
    }
    private String diagnostics() {
        StringBuilder s=new StringBuilder("Regarde ici ! 0.1 — diagnostic\n");
        s.append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append("\nAndroid ").append(Build.VERSION.RELEASE)
            .append(" / API ").append(Build.VERSION.SDK_INT).append("\nBuild ").append(Build.DISPLAY).append('\n');
        s.append("Affichage de l’activité : ").append(getDisplay().getDisplayId()).append('\n');
        for(Display d:displays.getDisplays()) s.append("Écran ").append(d.getDisplayId()).append(" : ").append(d.getName())
            .append(" / ").append(stateName(d)).append(" / flags 0x").append(Integer.toHexString(d.getFlags())).append('\n');
        s.append("Écrans compatibles Presentation : ").append(displays.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).length)
            .append("\nDernier essai : ").append(lastDisplayError);
        return s.toString();
    }
    private void showDiagnostic() {
        String report=diagnostics();
        new AlertDialog.Builder(this).setTitle("Compatibilité des écrans").setMessage(report)
            .setPositiveButton("Copier",(d,w)->{
                getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Diagnostic Regarde ici",report)); toast("Diagnostic copié");
            }).setNegativeButton("Fermer",null).show();
    }
    private String stateName(Display d) { return d.getState()==Display.STATE_ON?"allumé":d.getState()==Display.STATE_OFF?"éteint":"veille ("+d.getState()+")"; }
    private void setCoverStatus(String message) { coverStatus.setText(message); }
    @Override protected void onStart() {
        super.onStart(); foreground=true; displays.registerDisplayListener(this,handler); connectCover();
    }
    @Override protected void onResume() {
        super.onResume();
        if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED && camera==null) startCamera();
    }
    @Override protected void onStop() {
        foreground=false; cancelCountdown(); displays.unregisterDisplayListener(this); dismissCover();
        setCoverStatus(wantsCover?"Animation suspendue":"Écran externe : à tester sur ce téléphone"); super.onStop();
    }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("display",selectedDisplay); state.putBoolean("cover",wantsCover); super.onSaveInstanceState(state);
    }
    @Override public void onDisplayAdded(int id) { if(id==selectedDisplay) connectCover(); }
    @Override public void onDisplayRemoved(int id) { if(id==selectedDisplay) { dismissCover(); setCoverStatus("Écran externe déconnecté"); } }
    @Override public void onDisplayChanged(int id) {
        if(id!=selectedDisplay) return;
        Display d=displays.getDisplay(id);
        if(d==null || d.getState()!=Display.STATE_ON) { dismissCover(); setCoverStatus("Écran externe éteint par le système"); }
        else connectCover();
    }
    @Override public boolean onKeyDown(int keyCode,KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_VOLUME_UP || keyCode==KeyEvent.KEYCODE_VOLUME_DOWN) { if(event.getRepeatCount()==0) takePhoto(); return true; }
        return super.onKeyDown(keyCode,event);
    }
    @Override public boolean onKeyUp(int keyCode,KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_VOLUME_UP || keyCode==KeyEvent.KEYCODE_VOLUME_DOWN) return true;
        return super.onKeyUp(keyCode,event);
    }
    private int dp(float value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private GradientDrawable rounded(int color,int radius) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private TextView text(String label,float size,int color) { TextView t=new TextView(this); t.setText(label); t.setTextSize(size); t.setTextColor(color); return t; }
    private LinearLayout row() { LinearLayout l=new LinearLayout(this); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private void weighted(LinearLayout row,View v) { row.addView(v,new LinearLayout.LayoutParams(0,-2,1)); }
    private Button button(String label,Runnable action) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(12); b.setTextColor(INK);
        b.setMinWidth(0); b.setMinimumWidth(0); b.setMinHeight(dp(48)); b.setPadding(dp(10),dp(4),dp(10),dp(4));
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL)); b.setOnClickListener(v->action.run()); return b;
    }
    private void toast(String message) { Toast.makeText(this,message,Toast.LENGTH_LONG).show(); }
}
