package at.huber.sampleDownload;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import at.huber.youtubeExtractor.R;
import at.huber.youtubeExtractor.YtFile;
import at.huber.youtubeExtractor.YouTubeUriExtractor;

public class SampleDownloadActivity extends Activity {
	
	LinearLayout mainLayout;
	ProgressBar mainProgressBar;
	Context activityContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sample_download);
		mainLayout=(LinearLayout)findViewById(R.id.main_layout);
		mainProgressBar=(ProgressBar)findViewById(R.id.prgrBar);
		activityContext=this;
		
		//Check how it was started and if we can get the youtubelink somehow
		if (savedInstanceState == null && ((Intent.ACTION_SEND.equals(getIntent().getAction()) && getIntent().getType() != null) || 
				Intent.ACTION_VIEW.equals(getIntent().getAction()))) {
			String youtubeLink=null;
			if(Intent.ACTION_SEND.equals(getIntent().getAction()) && "text/plain".equals(getIntent().getType())){
				youtubeLink=getIntent().getStringExtra(Intent.EXTRA_TEXT);
			}else if(Intent.ACTION_VIEW.equals(getIntent().getAction())){
				youtubeLink=getIntent().getDataString();
			}
			
			if (youtubeLink != null && (youtubeLink.contains("http://youtu.be/") || youtubeLink
							.contains("youtube.com/watch?v="))){
				//We have a valid link such as: http://youtu.be/xxxx or http://youtube.com/watch?v=xxxx
				getYoutubeDownloadUrl(youtubeLink);
			}else{
				Toast.makeText(this, R.string.error_no_yt_link, Toast.LENGTH_LONG).show();
				if (youtubeLink != null){
					Log.d("Link:", youtubeLink);
				}
				finish();
			}
		}else{
			finish();
		}
	}
	
	private void getYoutubeDownloadUrl(String youtubeLink) {
		YouTubeUriExtractor ytEx=new YouTubeUriExtractor(this) {

			@Override
			public void onSourcesAvailable(String videoId, String videoTitle, SparseArray<YtFile> ytFiles) {
				mainProgressBar.setVisibility(View.GONE);
				
				if(ytFiles==null){
					//Something went wrong we got no links. Please always check this.
					finish();
					return;
				}
				//Iterate over itags
				for(int i=0, itag=0; i < ytFiles.size(); i++){
					itag=ytFiles.keyAt(i);
					//source represents one download link with its meta data
					YtFile ytFile=ytFiles.get(itag);
					
					//Ignore the google proprietary webm format
					if(!ytFile.meta.ext.equalsIgnoreCase("webm")){
						//Just add videos in a decent format => height -1 = audio
						if(ytFile.meta.height>0 && ytFile.meta.height<360){
							continue;
						}
						addButtonToMainLayout(videoTitle, ytFile);
					}		
				}
			}
		};
		//Lets execute the request
		ytEx.execute(youtubeLink);
		
	}
	
	private void addButtonToMainLayout(final String videoTitle, final YtFile ytfile){
		//Lets display some buttons to let the user choose the format he wants to download
		Button btn=new Button(activityContext);
		btn.setText(ytfile.meta.info);
		btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String filename;
				if (videoTitle.length() > 55){
					filename=videoTitle.substring(0, 55) + "." + ytfile.meta.ext;
				}else{
					filename=videoTitle + "." + ytfile.meta.ext;
				}
				filename=filename.replaceAll("\\\\|>|<|\"|\\||\\*|\\?|%|:", "");
				downloadFromUrl(ytfile.url, videoTitle, filename);
				finish();
			}
		});
		mainLayout.addView(btn);
	}
	
	private void downloadFromUrl(String youtubeDlUrl,String downloadTitle, String fileName) {
		Uri uri=Uri.parse(youtubeDlUrl);
		DownloadManager.Request request = new DownloadManager.Request(uri);
		request.setTitle(downloadTitle);
		// in order for this if to run, you must use the android 3.2 to compile your app
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
		    request.allowScanningByMediaScanner();
		    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		}
		request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

		// get download service and enqueue file
		DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
		manager.enqueue(request);
	}

}
