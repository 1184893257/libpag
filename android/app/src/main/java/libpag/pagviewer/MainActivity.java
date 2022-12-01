package libpag.pagviewer;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import org.libpag.PAGComposition;
import org.libpag.PAGLayer;
import org.libpag.PAGView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    LinearLayout containerView;
    Button       btPlayFirst;
    Button btPlaySecond;

    PAGPlayerView firstPlayer = null;
    PAGPlayerView secondPlayer = null;
    Handler handler = new Handler() {
        int count = 0;
        long lastTime = 0;
        @Override
        public void dispatchMessage(@NonNull Message msg) {
            count++;
            long current = SystemClock.uptimeMillis();
            super.dispatchMessage(msg);
            if (lastTime + 1000 < current) {
                Log.e("lqytest", "count " + count);
                lastTime = current;
                count = 0;
            }
        }
    };
    byte[] flowerData;
    int delayMax = 400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        containerView = (LinearLayout) findViewById(R.id.container_view);
        btPlayFirst = (Button) findViewById(R.id.play_first);
        if (btPlayFirst == null) {
            return;
        }
        btPlayFirst.setOnClickListener(this);
        btPlaySecond = (Button) findViewById(R.id.play_second);
        if (btPlaySecond == null) {
            return;
        }
        btPlaySecond.setOnClickListener(this);

        firstPlayer = createPlayerView();
        secondPlayer = createPlayerView();

        flowerData = readFile("flower.pag");

        activatedView(btPlayFirst.getId());
    }

    private PAGPlayerView createPlayerView() {
        PAGPlayerView pagView = new PAGPlayerView();
        pagView.setRepeatCount(-1);
        return pagView;
    }

    private int getPlayCount() {
        if (getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE) {
            return 15;
        }
        return 6;
    }

    private void play() {
        handler.removeCallbacksAndMessages(this);
        containerView.removeAllViews();

        for (int i = getPlayCount(); i > 0; --i) {
            final PAGView pagView = createPlayerView().createView(this, flowerData);
            pagView.setTag(PAGComposition.Make(300, 1000));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                    1F);
            containerView.addView(pagView, lp);
        }

        handler.postAtTime(new Runnable() {
            Random random = new Random();

            @Override
            public void run() {
                final PAGComposition composition = PAGPlayerView.applyTransform(MainActivity.this, flowerData);
                play((PAGView) containerView.getChildAt(random.nextInt(getPlayCount())), composition);
                long current = SystemClock.uptimeMillis();
                handler.postAtTime(this, MainActivity.this, current + random.nextInt(delayMax));
            }
        }, this, SystemClock.uptimeMillis());
    }

    private void stopPlayPAG(PAGPlayerView player) {
        if (player != null) {
            player.stop();
        }
    }

    private void resumePlayPAG(PAGPlayerView player) {
        if (player != null) {
            player.play();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePagView();
        handler.removeCallbacksAndMessages(this);
    }

    private void releasePagView() {
        if (firstPlayer != null) {
            firstPlayer.onRelease();
        }
        if(secondPlayer != null){
            secondPlayer.onRelease();
        }
    }

    public boolean CheckStoragePermissions(Activity activity) {
        // Check if we have write permission

        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};

        int REQUEST_PERMISSION_CODE = 1;

        int checkStoragePermissions = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (checkStoragePermissions != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
            return false;
        } else {
            Log.i("test", "CheckStoragePermissions: Granted");
            return true;
        }
    }

    private void activatedView(int viewId) {
        switch (viewId) {
            case R.id.play_first:
                btPlayFirst.setActivated(true);
                btPlaySecond.setActivated(false);
                break;
            case R.id.play_second:
                btPlayFirst.setActivated(false);
                btPlaySecond.setActivated(true);
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.play_second) {
//            VideoDecoder.SetMaxHardwareDecoderCount(0);
            playInOneView();
            activatedView(R.id.play_second);
        } else {
//            VideoDecoder.SetMaxHardwareDecoderCount(15);
            play();
            activatedView(R.id.play_first);
        }
    }

    public void play(PAGView pagView, PAGComposition composition) {
        PAGComposition container = (PAGComposition) pagView.getTag();
        long currentTime = container.currentTime();
        for (int i = container.numChildren() - 1; i >= 0; --i) {
            PAGLayer child = container.getLayerAt(i);
            if (child.startTime() + child.duration() <= currentTime) {
                container.removeLayerAt(i);
            } else {
                child.setStartTime(-(child.currentTime() - child.startTime()));
            }
        }
        container.addLayer(composition);

        pagView.setComposition(container);
        if (!pagView.isPlaying()) {
            pagView.play();
        } else {
            pagView.setProgress(0F);
        }
    }

    public void playInOneView() {
        handler.removeCallbacksAndMessages(this);
        containerView.removeAllViews();
        final PAGView pagView = new PAGView(this);
        int           count   = getPlayCount();
        PAGComposition container  = PAGComposition.Make(300 * count, 1000);
        pagView.setTag(container);
        containerView.addView(pagView);

        handler.postAtTime(new Runnable() {
            Random random = new Random();
            Matrix matrix = new Matrix();

            @Override
            public void run() {
                matrix.reset();
                matrix.postTranslate(300 * random.nextInt(getPlayCount()), 0);
                final PAGComposition composition = PAGPlayerView.applyTransform(MainActivity.this, flowerData);
                composition.setMatrix(matrix);
                play(pagView, composition);
                handler.postAtTime(this, MainActivity.this, SystemClock.uptimeMillis() + random.nextInt(delayMax));
            }
        }, this, SystemClock.uptimeMillis());
    }

    public byte[] readFile(String fileName) {
        try {
            InputStream in = getAssets().open(fileName);
            byte[] filecontent = new byte[in.available()];
            in.read(filecontent);
            in.close();
            return filecontent;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String readToString(String fileName) {
        File file = new File(fileName);
        Long filelength = file.length();
        byte[] filecontent = new byte[filelength.intValue()];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(filecontent);
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            return new String(filecontent, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
