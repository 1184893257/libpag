package libpag.pagviewer;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.libpag.PAG;
import org.libpag.PAGComposition;
import org.libpag.PAGView;
import org.libpag.VideoDecoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    LinearLayout containerView;
    Button       btPlayFirst;
    Button btPlaySecond;

    PAGPlayerView firstPlayer = null;
    PAGPlayerView secondPlayer = null;
    Handler handler = new Handler();

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
        containerView.removeAllViews();

        for (int i = getPlayCount(); i > 0; --i) {
            final PAGView pagView = createPlayerView().createView(this, "flower.pag");
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                    1F);
            containerView.addView(pagView, lp);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    pagView.play();
                }
            }, 100 * i);
        }
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

    public void playInOneView() {
        containerView.removeAllViews();
        int count = getPlayCount();
        PAGComposition root = PAGComposition.Make(300 * count, 1000);
        Matrix matrix = new Matrix();
        for (int i = 0; i < count; ++i) {
            PAGComposition composition = PAGPlayerView.applyTransform(this, "flower.pag");
            composition.setMatrix(matrix);
            composition.setStartTime(100 * 1000 * i);
            root.addLayer(composition);
            matrix.postTranslate(300, 0);
        }
        PAGView view = new PAGView(this);
        view.setComposition(root);
        view.setRepeatCount(0);
        containerView.addView(view);
        view.play();
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
