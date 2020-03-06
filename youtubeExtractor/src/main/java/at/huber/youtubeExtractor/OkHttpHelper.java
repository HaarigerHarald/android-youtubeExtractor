package at.huber.youtubeExtractor;

import android.support.annotation.NonNull;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public abstract class OkHttpHelper {

    public static String makeRequest(@NonNull OkHttpClient client, @NonNull String url) throws IOException {
        Response response = null;
        try {
            final Request request;
            try {
                request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();
            } catch (Exception e) {
                throw new IOException("Can't create request: " + e.getMessage());
            }
            response = client.newCall(request).execute();
            if (!response.isSuccessful())
                throw new IOException("Response is not successful: " + response);
            final ResponseBody body = response.body();
            if (body == null)
                throw new IOException("Response body is null: " + response);
            return body.string();
        } finally {
            if (response != null)
                response.close();
        }
    }
}
