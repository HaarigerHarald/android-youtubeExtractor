package at.huber.youtubeDownloader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;

public class DownloadFinishedReceiver extends BroadcastReceiver{
	
	
	private static final String TEMP_FILE_NAME="tmp-";
	
	@Override
	public void onReceive(final Context context, Intent intent) {
		String action=intent.getAction();
		if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)){
			Bundle extras=intent.getExtras();
			DownloadManager.Query q=new DownloadManager.Query();
			long downloadId=extras.getLong(DownloadManager.EXTRA_DOWNLOAD_ID);
			q.setFilterById(downloadId);
			Cursor c=((DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).query(q);
			if (c.moveToFirst()){
				int status=c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
				if (status == DownloadManager.STATUS_SUCCESSFUL){
					String inPath=c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
					c.close();
					DownloadStatus dlStatus=getMultiFileDlStatus(context, downloadId, inPath);
					if(dlStatus!=null && dlStatus.readyToMerge){
						if (!dlStatus.hasVideo){
							convertM4a(inPath);
						}else{
							if(inPath.endsWith(".mp4")){
								mergeMp4(dlStatus.otherFilePath, inPath);
							}else if(inPath.endsWith(".m4a")){
								mergeMp4(inPath, dlStatus.otherFilePath);
							}	
						}
					}
				}else if(status == DownloadManager.STATUS_FAILED){
					removeTempOnFailure(context, downloadId);
				}
			}

		}
	}
	
	private void removeTempOnFailure(Context con, long downloadId){
		File cacheFileDir=new File(con.getCacheDir().getAbsolutePath());
		for(File f: cacheFileDir.listFiles()){
			if(f.getName().contains(downloadId+"")){
				f.delete();
				break;
			}
		}
	}
	
	private DownloadStatus getMultiFileDlStatus(Context con, long downloadId, String filePath) {
		File cacheFileDir=new File(con.getCacheDir().getAbsolutePath());
		File cacheFile=null;
		for(File f: cacheFileDir.listFiles()){
			if(f.getName().contains(downloadId+"")){
				cacheFile=f;
				break;
			}
		}
		if (cacheFile!=null && cacheFile.exists()){
			DownloadStatus dlStatus= new DownloadStatus();
			dlStatus.hasVideo=cacheFile.getName().contains("-");
			BufferedReader reader=null;
			BufferedWriter writer=null;
			try{
				reader=new BufferedReader(new InputStreamReader(new FileInputStream(cacheFile), "UTF-8"));
				dlStatus.otherFilePath=reader.readLine();
				reader.close();
				if(dlStatus.otherFilePath!=null || !dlStatus.hasVideo){
					cacheFile.delete();
					dlStatus.readyToMerge=true;
				}else{
					writer=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(cacheFile)));
					writer.write(filePath);
				}
				return dlStatus;
			}catch (Exception e){
				e.printStackTrace();
			}finally{
				if (reader != null){
					try{
						reader.close();
					}catch (IOException e){
						e.printStackTrace();
					}
				}
				if (writer != null){
					try{
						writer.close();
					}catch (IOException e){
						e.printStackTrace();
					}
				}
			}
		}
		return null;
	}
	
	private void convertM4a(String inFilePath){
		String path=inFilePath.substring(0,inFilePath.lastIndexOf("/"));
		try{
			Movie inVideo=MovieCreator.build(inFilePath);
			Container out =  new DefaultMp4Builder().build(inVideo);
			long currentMillis=System.currentTimeMillis();
			FileOutputStream fos=new FileOutputStream(new File(path+TEMP_FILE_NAME+currentMillis+".m4a"));
			out.writeContainer(fos.getChannel());
			fos.close();
			//TODO Guess artist and song from title and write it into the container
			File inFile= new File(inFilePath);
			if(inFile.delete()){
				File tempOutFile= new File(path+TEMP_FILE_NAME+currentMillis+".m4a");
				tempOutFile.renameTo(inFile);
			}		
		}catch (IOException e){
			e.printStackTrace();
		}
	}
	
	private void mergeMp4(String inFilePathAudio, String inFilePathVideo){
		String path=inFilePathVideo.substring(0,inFilePathVideo.lastIndexOf("/"));
		try{
			Movie video = MovieCreator.build(inFilePathVideo);
			Movie audio = MovieCreator.build(inFilePathAudio);
			video.addTrack(audio.getTracks().get(0));
			Container out=new DefaultMp4Builder().build(video);
			long currentMillis=System.currentTimeMillis();
			FileOutputStream fos=new FileOutputStream(new File(path+TEMP_FILE_NAME+currentMillis+".mp4"));
			out.writeContainer(fos.getChannel());
			fos.close();
			File inAudioFile= new File(inFilePathAudio);
			inAudioFile.delete();
			File inVideoFile= new File(inFilePathVideo);
			if(inVideoFile.delete()){
				File tempOutFile= new File(path+TEMP_FILE_NAME+currentMillis+".mp4");
				tempOutFile.renameTo(inVideoFile);
			}
        }catch(IOException e){
        	e.printStackTrace();
        }
	}
	
	private class DownloadStatus{
		String otherFilePath;
		boolean readyToMerge=false;
		boolean hasVideo;
	}
}
