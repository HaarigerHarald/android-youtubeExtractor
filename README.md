A small android based YouTube url extractor.
=======================================================

These are the literal urls to the YouTube video or audio files, so you can stream or download them.
It features an age verification circumvention and a signature deciphering method (mainly for vevo videos).

## Get it

1. Just import the jar library [youtubeExtractor.jar](https://github.com/HaarigerHarald/android-youtubeExtractor/raw/master/bin/youtubeExtractor.jar)

1. Or build it yourself from the sources, don't forget you will also need this great library if you do so: [js-evaluator-for-android](https://github.com/evgenyneu/js-evaluator-for-android)

## How to Use:

It's basically build around an AsyncTask. Called from an Activity you can write something like that:
	
    String youtubeLink = "http://youtube.com/watch?v=xxxx";
    
    YouTubeUriExtractor ytEx = new YouTubeUriExtractor(this) {
        @Override
        public void onUrisAvailable(String videoId, String videoTitle, SparseArray<YtFile> ytFiles) {
            if(ytFiles!=null){
                int itag = 22; // a YouTube format identifier
                String downloadUrl = ytFiles.get(itag).getUrl();
            }
        }
    };
    
    ytEx.execute(youtubeLink);

The important thing is the ytFiles SparseArray. Because YouTube videos are available in multiple formats we can choose one by
calling ytFiles.get(itag). One ytFile contains the url and its appropriate meta data like: "mp4 1280x720" or "m4a dash aac"

For further infos have a look at the supplied sample YouTube Downloader app. It uses the "Share" function in the official YouTube
app to download the files provided by YouTube. It doesn't have a launcher entry though so don't be irritated. [youtubeSampleDL.apk](https://github.com/HaarigerHarald/android-youtubeExtractor/raw/master/bin/youtubeSampleDL.apk)

## Requirements

Android 3.0 and up for Webview Javascript execution see [js-evaluator-for-android](https://github.com/evgenyneu/js-evaluator-for-android)

Not enciphered Videos may work on lower Android versions (untested).

## Advanced YouTube Downloader App

There is also now an advanced App that addresses the following issues:

1. Some resolutions are only available separated in two files, one for audio and one for video. We need to merge them after the download is completed.
There is a great Java library for doing this with mp4 files: [mp4parser](https://github.com/sannies/mp4parser)

1. Some media players aren't able to read the dash container of the m4a Audio files. This is also fixable via the library mentioned above.

To try the app have a look at: [youtubeDownloader.apk](https://github.com/HaarigerHarald/android-youtubeExtractor/raw/master/bin/youtubeDownloader.apk)

<img src='https://github.com/HaarigerHarald/android-youtubeExtractor/raw/master/Screenshot_2015-04-26-17-04-382.png' width='250' alt='youtubeDownloader Screenshot 1'>

<img src='https://github.com/HaarigerHarald/android-youtubeExtractor/raw/master/Screenshot_2015-04-27-17-05-50.png' width='250' alt='youtubeDownloader Screenshot 2'>

