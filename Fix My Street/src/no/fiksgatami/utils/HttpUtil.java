package no.fiksgatami.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.util.Log;
import no.fiksgatami.FiksGataMi;
import no.fiksgatami.R;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * HttpUtility class
 *
 * @author Roy Sindre Norangshol <roy.sindre at norangshol.no>
 */
public class HttpUtil {
    private static final String LOG_TAG = "HttpUtil";

    private static final String FORM_PHOTO = "photo";
    private static final String FORM_SERVICE = "service";
    private static final String FORM_SUBJECT = "subject";
    private static final String FORM_NAME = "name";
    private static final String FORM_EMAIL = "email";
    private static final String FORM_LATITUDE = "lat";
    private static final String FORM_LONGITUDE = "lon";
    private static final String FORM_CATEGORIES = "categories";

    private static final int TIMEOUT_CONNECTION = 100000;


    public static HttpResponse postReport(Context context, String subject, String name, String email, double latitude, double longitude) throws IOException {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT_CONNECTION);

        HttpClient httpClient = new DefaultHttpClient(params);
        HttpPost httpPost = new HttpPost(context.getString(R.string.postURL));
        httpPost.addHeader("User-Agent", getUserAgent(context));

        File f = new File(Environment.getExternalStorageDirectory(),
                FiksGataMi.PHOTO_FILENAME);

        MultipartEntity reqEntity = new MultipartEntity();
        FileBody fb = new FileBody(f, "image/jpeg");
        Charset utf8 = Charset.forName("UTF-8");
        reqEntity.addPart(FORM_PHOTO, fb);
        reqEntity.addPart(FORM_SERVICE, new StringBody("FiksGataMi4Android", utf8));
        reqEntity.addPart(FORM_SUBJECT, new StringBody(subject, utf8));
        reqEntity.addPart(FORM_NAME, new StringBody(name, utf8));
        reqEntity.addPart(FORM_EMAIL, new StringBody(email, utf8));
        reqEntity.addPart(FORM_LATITUDE, new StringBody(String.valueOf(latitude), utf8));
        reqEntity.addPart(FORM_LONGITUDE, new StringBody(String.valueOf(longitude), utf8));

        httpPost.setEntity(reqEntity);

//			Log.i(LOG_TAG,"executing request " + httpPost.getRequestLine());
        HttpResponse httpResponse = httpClient.execute(httpPost);
        //EntityUtils.consume(resEntity);
        httpClient.getConnectionManager().shutdown();
        return httpResponse;

    }

    /**
     * @param context
     * @param latitude
     * @param longitude
     * @return
     * @throws IOException
     * @todo fixme Webservice should implement HTTP GET resource for requesting categories ..
     */
    public static HttpResponse getCategories(Context context, double latitude, double longitude) throws IOException {
        Log.d(LOG_TAG, String.format("lat %s long %s", latitude, longitude));
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT_CONNECTION);

        HttpClient httpClient = new DefaultHttpClient(params);

        HttpPost httpPost = new HttpPost(context.getString(R.string.postURL));
        httpPost.addHeader("User-Agent", getUserAgent(context));

        final String hack = "hackForFetchingCategories";
        MultipartEntity reqEntity = new MultipartEntity();
        Charset utf8 = Charset.forName("UTF-8");
        reqEntity.addPart(FORM_SERVICE, new StringBody("FiksGataMi4Android", utf8));
        reqEntity.addPart(FORM_CATEGORIES, new StringBody("1", utf8));
        reqEntity.addPart(FORM_SUBJECT, new StringBody(hack, utf8));
        reqEntity.addPart(FORM_NAME, new StringBody(hack, utf8));
        reqEntity.addPart(FORM_EMAIL, new StringBody("noreply@example.com", utf8));
        reqEntity.addPart(FORM_LATITUDE, new StringBody(String.valueOf(latitude), utf8));
        reqEntity.addPart(FORM_LONGITUDE, new StringBody(String.valueOf(longitude), utf8));

        httpPost.setEntity(reqEntity);
        HttpResponse httpResponse = httpClient.execute(httpPost);

        httpClient.getConnectionManager().shutdown();
        return httpResponse;

    }

    /**
     * getCategories from response string
     *
     * @param response
     * @return
     * @throws IOException
     */
    public static List<String> getCategoriesFromResponse(String response) throws IOException {
        String responsePrefixToRemove = "CATEGORY: ";
        List<String> result = new ArrayList<String>();

        String[] rawResponseSplit = response.split("\n");
        if (rawResponseSplit.length >= 2) { // makes sure there's entries in the response
            for (int i = 1; i < rawResponseSplit.length; i++) { // skip first entry which is the SUCCESS boolean.
                result.add(rawResponseSplit[i].replaceFirst(responsePrefixToRemove, ""));
            }
        }
        return result;
    }

    public static String isValidResponse(HttpResponse response) throws IOException {
        if (response.getStatusLine().getStatusCode() == 200) {
            HttpEntity resEntity = response.getEntity();

            String responseString = EntityUtils.toString(resEntity);
            Log.i(LOG_TAG, String.format("Response was %s : %s", response.getStatusLine().getStatusCode(), responseString));

            if (resEntity != null) {
                Log.i(LOG_TAG, "Response content length: " + resEntity.getContentLength());
            }

            // use startswith to workaround bug where CATEGORIES-info
            // is display on every call to import.cgi
            if (responseString.startsWith("SUCCESS")) {
                // launch the Success page
                return responseString;
            }
        }
        return null;
    }
    public static String getUserAgent(Context context) {
        String USER_AGENT = "FiksGataMi4Android/";
        PackageManager pm = context.getPackageManager();
        try {
           PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
           USER_AGENT += pi.versionName + pi.versionCode;
        } catch (NameNotFoundException ex) {
            USER_AGENT += "unknown/-1";
        }
        return USER_AGENT;
     }
}
