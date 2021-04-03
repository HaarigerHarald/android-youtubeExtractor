package at.huber.sampleDownload;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

public class MainActivity extends Activity implements View.OnClickListener {

    private EditText edtYoutubeId;
    private TextView txtResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = LayoutInflater.from(this).inflate(R.layout.activity_main, null, false);
        setContentView(view);
        edtYoutubeId = findViewById(R.id.edtYoutubeId);
        Button btnExtract = findViewById(R.id.btnExtract);
        txtResult = findViewById(R.id.txtResult);
        btnExtract.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        final String link = "http://www.youtube.com/watch?v=" + edtYoutubeId.getText().toString();
        final Context context = this;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @SuppressLint("StaticFieldLeak")
            @Override
            public void run() {
                new YouTubeExtractor(context) {

                    @Override
                    protected void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta videoMeta) {
                        txtResult.setText(videoMeta.getTitle());
                    }
                }.extract(link, false, true);
            }
        });
    }
}
