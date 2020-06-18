package wlkdb.lg.trash;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;



public class PhotoFragment extends Fragment {
    //调用系统相册-选择图片
    private static final int IMAGE = 1;
    //所需权限

    //private ImageView resultsView;
    private TextView resultsView;
    private ImageView imageView;
    private Button search_btn;
    //private Bitmap  bitmap  = null;
    private String  postUrl = "http://192.168.43.190:5555/predict";

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragement_photo, container, false);

        resultsView = view.findViewById(R.id.result_photo);
        imageView = view.findViewById(R.id.image);
        view.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PhotoFragment.this.openPhotos(view);
            }
        });
        view.findViewById(R.id.search_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 给bnt1添加点击响应事件
                Intent intent;
                intent = new Intent(getActivity(),SearchActivity.class);
                //启动
                startActivity(intent);
            }
        });


        return view;
    }

    public void openPhotos(View v) {
        //调用相册
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //获取图片路径
        if (requestCode == IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            String[] filePathColumns = {MediaStore.Images.Media.DATA};
            Cursor c = getActivity().getContentResolver().query(selectedImage, filePathColumns, null, null, null);
            c.moveToFirst();
            int columnIndex = c.getColumnIndex(filePathColumns[0]);
            String imagePath = c.getString(columnIndex);
            showImage(imagePath);
            c.close();
        }
    }

    //加载图片
    private void showImage(String imagePath){
        Bitmap bm = BitmapFactory.decodeFile(imagePath);
        imageView.setImageBitmap(bm);
        int INPUT_SIZE = TensorFlowImageClassifier.INPUT_SIZE;
        Bitmap bitmap = Utils.zoomImg(bm, INPUT_SIZE, INPUT_SIZE);
        renderResult(bitmap);
    }

    protected void renderResult(Bitmap bitmap) {
//        final long startTime = SystemClock.uptimeMillis();
        postRequest(postUrl,bitmap);
//        Utils.setResult(getActivity(), resultsView, results);
    }

    public byte[] getBitmapByte(Bitmap bitmap){   //将bitmap转化为byte[]类型也就是转化为二进制
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        try {
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    void postRequest(String postUrl, Bitmap bitmap) {

        //step1. 创建一个OkHttpClient对象
        OkHttpClient okHttpClient = new OkHttpClient();


        // 将Bitmap转换为byte[]
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] byteArray = getBitmapByte(bitmap);
        //System.out.println(datas);
        //D/DATAS: [B@236977b
        // D/DATAS: 41476  bytes
        Log.d("DATAS", String.valueOf(byteArray.length));
//        HashMap<String,String> hashMap=new HashMap<String, String>();
//        hashMap.put('image',datas);
//
//        JSONObject object1 = new JSONObject();
//        try {
//            object1.put("image", datas);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }

        //step 2: 创建  FormBody.Builder
        //通过FormBody.Builder添加多个String键值对
//        FormBody formBody = new FormBody.Builder()
//                .add("image", datas.toString())
//                .build();

        RequestBody postBodyImage = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "androidFlask.jpg", RequestBody.create(MediaType.parse("image/*jpg"), byteArray))
                .build();

        //step3: 创建请求
        Request request = new Request.Builder()

                .url(postUrl)
                .post(postBodyImage)
                .build();

        //step 4 建立联系，异步请求
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // TODO: 17-1-4  请求失败
                // Cancel the post on failure.
                call.cancel();
                Log.d("FAIL", e.getMessage());
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView responseText = resultsView.findViewById(R.id.results);
                        responseText.setText("Failed to Connect to Server. Please Try Again.");
                    }
                });
                if( call.isCanceled()){
                    Log.d("ISCANCELED","call is canceled!");
                }
                if (!call.isCanceled()){
                    Log.d("ISCANCELED","call is not canceled!");
                }
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                // TODO: 17-1-4 请求成功;
                // In order to access the TextView inside the UI thread, the code is executed inside runOnUiThread()
                Log.d("SUCCESS","the request is  success to connect the flask!");

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView responseText = resultsView.findViewById(R.id.result_photo);
                        try {
                            //D/the response's body is:: {"prediction":"glass"}
                            //Log.d("the response's body is:", response.body().string());
                            //String  str = response.body().string();
                            //System.out.print(str);
                            //String转JSONObject
                            JSONObject result = null;
                            try {
                                result = new JSONObject(response.body().string());
                                //取数据
                                String prediction= String.valueOf(result.get("prediction"));
                                Log.d("PREDICT IS:", prediction);
                                responseText.setText(prediction);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } //onResponse
        }); // new Call
    }   //void postRequest()
}
