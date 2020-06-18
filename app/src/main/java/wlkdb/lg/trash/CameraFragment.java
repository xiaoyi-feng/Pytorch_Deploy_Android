package wlkdb.lg.trash;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

public class CameraFragment extends Fragment {

    private boolean active  = false;
    private Bitmap  bitmap  = null;
    private String  postUrl = "http://192.168.43.190:5555/predict";

    private SurfaceView surfaceView;
    private TextView    resultsView;
    private Button    button_camera;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        surfaceView = view.findViewById(R.id.surfaceView);
        resultsView = view.findViewById(R.id.results);
        button_camera=view.findViewById(R.id.action_button_camera);
        surfaceView.getHolder().addCallback(surfaceCallback);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onActivityCreated(savedInstanceState);
        button_camera = (Button)getActivity().findViewById(R.id.action_button_camera);
        button_camera.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                Toast.makeText(getActivity(), "upload this image", Toast.LENGTH_LONG).show();
                if(bitmap!=null && getActivity()!=null) {
                    postRequest(postUrl, bitmap);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        onHiddenChanged(false);
    }

    @Override
    public void onPause() {
        onHiddenChanged(true);
        super.onPause();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            // 不在最前端界面显示
            releaseCamera();
        } else {
            // 重新显示到最前端中
            if (camera == null) {
                openCamera();
                surfaceView.getHolder().addCallback(surfaceCallback);
            }
            active = true;
        }
    }

    public void openCamera() {
        try {
            camera = Camera.open();
            WindowManager wm = (WindowManager) CameraFragment.this.getActivity().getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Camera.Parameters parameters = camera.getParameters();
//                //设置预览照片的大小
//                parameters.setPreviewSize(display.getWidth(), display.getHeight());
//                //设置每秒3帧
//                parameters.setPreviewFrameRate(3);
            parameters.setPictureFormat(PixelFormat.JPEG);
            //设置照片的质量
            parameters.setJpegQuality(85);
//                parameters.setPictureSize(800, 600);
            camera.setParameters(parameters);
            //通过SurfaceView显示取景画面
            camera.setPreviewDisplay(surfaceView.getHolder());
            followScreenOrientation(camera);
            camera.startPreview();
            isPreview = true;
            camera.setPreviewCallback(previewCallback);
        } catch (IOException e) {

        }
        handler.post(renderResult);
    }

    public void releaseCamera() {
        active = false;
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        bitmap = null;
    }

    private Camera  camera;
    private boolean isPreview;

    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            openCamera();
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        focus();//实现相机的参数初始化
                        camera.cancelAutoFocus();//只有加上了这一句，才会自动对焦。
                    }
                }
            });
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            if (camera != null) {
                if (isPreview) {//如果正在预览
                    camera.stopPreview();
                    camera.release();
                }
            }
        }
    };

    private void focus() {
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPictureFormat(PixelFormat.JPEG);
        //parameters.setPictureSize(surfaceView.getWidth(), surfaceView.getHeight()); // 部分定制手机，无法正常识别该方法。
//        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//1连续对焦
        followScreenOrientation(camera);
        camera.setParameters(parameters);
        camera.startPreview();
        camera.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
    }

    long lastPreviewTime = 0;

    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
//            if (System.currentTimeMillis() - lastPreviewTime < 1000) {
//                return;
//            }
//            lastPreviewTime = System.currentTimeMillis();

            camera.setPreviewCallback(null);

            if (camera == null || !active)
                return;

            Camera.Parameters parameters = camera.getParameters();
            int width = parameters.getPreviewSize().width;
            int height = parameters.getPreviewSize().height;

            YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, width, height), 50, out);

            byte[] bytes = out.toByteArray(); //将bitmap转换为byte[]
            final Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);  //将byte数组转换Wiebitmap

            int INPUT_SIZE = TensorFlowImageClassifier.INPUT_SIZE;
            bitmap = Utils.zoomImg(bm, INPUT_SIZE, INPUT_SIZE);  //将原bitmap进行resize,生成新的bitmap
            camera.setPreviewCallback(this);
//            if (bitmap != null && getActivity() != null) {
//                postRequest(postUrl,bitmap);
//            }
        }
    };


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

    public void followScreenOrientation(Camera camera) {
        final int orientation = getActivity().getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            camera.setDisplayOrientation(180);
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            camera.setDisplayOrientation(90);
        }
    }

    private Handler handler = new Handler();

    private Runnable renderResult = new Runnable() {
        @Override
        public void run() {

//            if (bitmap != null && getActivity() != null) {
//                //**********************************************
//                //应该在这个地方将bitmap-->json数据，并post给flask服务器
//                //并从服务器端接受传过来的分类结果
//                postRequest(postUrl, bitmap);
//            }
            if (active) {
                handler.post(this);
            }
        }
     };


    void postRequest(String postUrl, Bitmap bitmap) {

        //step1. 创建一个OkHttpClient对象
        OkHttpClient okHttpClient = new OkHttpClient();


        // 将Bitmap转换为byte[]
        byte[] byteArray = getBitmapByte(bitmap);
        Log.d("DATAS", String.valueOf(byteArray.length));

        //step 2: 创建Builder，放入传送数据
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
                        TextView responseText = resultsView.findViewById(R.id.results);
                        try {
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

