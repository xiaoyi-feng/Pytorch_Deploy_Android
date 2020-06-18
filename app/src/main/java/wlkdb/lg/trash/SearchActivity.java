package wlkdb.lg.trash;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by 冯晓怡 on 2020/5/9.
 */

public class SearchActivity extends Activity {
    private ImageButton     r_image_btn;
    private ImageButton     h_image_btn;
    private ImageButton     w_image_btn;
    private ImageButton     d_image_btn;
    private SearchView      searchView;
    private String  postUrl = "http://192.168.43.190:5555/search";
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_ui);
        final android.support.v7.widget.SearchView searchView =  findViewById(R.id.searchView);
        r_image_btn = (ImageButton) this.findViewById(R.id.recycle_image_btn);
        h_image_btn = (ImageButton) this.findViewById(R.id.harmful_image_btn);
        w_image_btn = (ImageButton) this.findViewById(R.id.wet_image_btn);
        d_image_btn = (ImageButton) this.findViewById(R.id.dry_image_btn);

        //SearchView的监听事件
        //搜索框展开时后面叉叉按钮的点击事件
        searchView.setOnCloseListener(new android.support.v7.widget.SearchView.OnCloseListener(){
            @Override
            public boolean onClose() {
                //Toast.makeText(getApplicationContext(), "Close", Toast.LENGTH_LONG).show();
                return false;
            }
        });

        //搜索图标按钮(打开搜索框的按钮)的点击事件
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getApplicationContext(), "Open", Toast.LENGTH_LONG).show();
                searchView.setQueryHint("");
            }
        });

        //搜索框文字变化监听
        //输入完成时，提交触发的方法，一般情况是点击输法中的搜索按钮才会触发，表示正式提交
        searchView.setOnQueryTextListener(new android.support.v7.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.e("CSDN_LQR", "TextSubmit : " + s);
                //当点击“搜索”后，应该连接flask，传过去字符串s
                //Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();

                //Toast.makeText(getApplicationContext(),"Submit",Toast.LENGTH_LONG).show();
                String str_dict=bolwingJson(s);
                //Toast.makeText(getApplicationContext(),str_dict,Toast.LENGTH_LONG).show();
                postRequest(postUrl, str_dict);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.e("CSDN_LQR", "TextChange --> " + s);
               // postRequest(postUrl, s);
                return false;
            }
        });

        r_image_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(SearchActivity.this, RecycleImgActivity.class);
                startActivity(intent);
            }
        });
        h_image_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(SearchActivity.this, HarmfulImgActivity.class);
                startActivity(intent);

            }
        });
        w_image_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(SearchActivity.this, WetImgActivity.class);
                startActivity(intent);

            }
        });
        d_image_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(SearchActivity.this, DryImgActivity.class);
                startActivity(intent);
            }
        });
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Search Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    //将string转化为json数据
    public String bolwingJson(String wastename) {
        String waste_name="\"" +wastename +"\"";
        return "{\"wastename\":" + waste_name +"}";
    }

    //请求服务
    void postRequest(String postUrl, String json) {

        //step1. 创建一个OkHttpClient对象
        OkHttpClient okHttpClient = new OkHttpClient();

        MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(postUrl)
                .post(body)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
                Log.d("FAIL", e.getMessage());
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                Log.d("SUCCESS", "the request is  success to connect the flask!");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView responseText = findViewById(R.id.search_result);

                        JSONObject result = null;
                        try {
                            try {
                                result = new JSONObject(response.body().string());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if(! result.has("1")){
                                responseText.setText(R.string.search_is_null);
                            }
                            else {
                                //String return_result= String.valueOf(result);
                                String results ="";

                                //取数据,将从服务器获得的json数据
                                int i =1;
                                while (result.has(String.valueOf(i))){
                                    String return_result = String.valueOf(result.get(String.valueOf(i)));
                                    //Log.d("SEARCH_RESULT IS:", return_result); //D/SEARCH_RESULT IS:: {"1":"手机：可回收垃圾\n"}

                                    results +=return_result;
                                    i++;

//                                    responseText.setText(return_result);
                                }
                                responseText.setText(results);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }); //建立联系，异步请求
    } //postRequest()
}
