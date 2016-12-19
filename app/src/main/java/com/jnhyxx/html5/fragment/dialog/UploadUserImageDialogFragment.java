package com.jnhyxx.html5.fragment.dialog;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.jnhyxx.html5.R;
import com.jnhyxx.html5.utils.ToastUtil;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by ${wangJie} on 2016/12/19.
 * 上传用户头像
 */

public class UploadUserImageDialogFragment extends DialogFragment {
    private static final String TAG = "UploadUserImageDialogFr";

    /**
     * 打开相机的请求码
     */
    private static final int REQ_CODE_TAKE_PHONE_FROM_CAMERA = 379;
    /**
     * 打开图册的请求码
     */
    private static final int REQ_CODE_TAKE_PHONE_FROM_PHONES = 600;
    /**
     * 打开裁剪界面的请求码
     */
    private static final int REQ_CODE_CROP_IMAGE = 204;


    @BindView(R.id.takePhoneFromCamera)
    TextView mTakePhoneFromCamera;
    @BindView(R.id.takePhoneFromPhone)
    TextView mTakePhoneFromPhone;
    @BindView(R.id.takePhoneCancel)
    TextView mTakePhoneCancel;
    // TODO: 2016/12/19 测试图片
    @BindView(R.id.test)
    ImageView mTest;

    private Unbinder mBind;
    private File mFile;
    private Uri mUri;
    private FileInputStream mFileInputStream;

    public UploadUserImageDialogFragment() {

    }

    public static UploadUserImageDialogFragment newInstance() {
        Bundle args = new Bundle();
        UploadUserImageDialogFragment fragment = new UploadUserImageDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog_MinWidth);
//        setupDialog(this.getDialog(), STYLE_NO_TITLE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_fragment_upload_user_image, container, false);
        mBind = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBind.unbind();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Dialog dialog = getDialog();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        String filePath = SimpleDateFormat.getDateTimeInstance().format(System.currentTimeMillis()) + ".jpg";
        mFile = new File(Environment.getExternalStorageDirectory(), filePath);
    }


    @OnClick({R.id.takePhoneFromCamera, R.id.takePhoneFromPhone, R.id.takePhoneCancel})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.takePhoneFromCamera:
//                Log.d(TAG, "文件是否存在 " + mFile.exists() + "文件地址 " + mFile.getPath());
//                Uri uri = Uri.fromFile(mFile);
//                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                intent.putExtra(MediaStore.Images.Media.ORIENTATION, 0);
//                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
//                startActivityForResult(intent, REQ_CODE_TAKE_PHONE_FROM_CAMERA);

                Intent openCameraIntent = new Intent(
                        MediaStore.ACTION_IMAGE_CAPTURE);
                mUri = Uri.fromFile(new File(Environment
                        .getExternalStorageDirectory(), "image.jpg"));
                // 指定照片保存路径（SD卡），image.jpg为一个临时文件，每次拍照后这个图片都会被替换
                openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mUri);
                startActivityForResult(openCameraIntent, REQ_CODE_TAKE_PHONE_FROM_CAMERA);
                break;
            case R.id.takePhoneFromPhone:
                break;
            case R.id.takePhoneCancel:
                this.dismiss();
                break;
        }
    }

    public void show(FragmentManager manager) {
        this.show(manager, UploadUserImageDialogFragment.class.getSimpleName());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == getActivity().RESULT_OK) {
            switch (requestCode) {
                case REQ_CODE_TAKE_PHONE_FROM_CAMERA:
                    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

                        String format = SimpleDateFormat.getInstance().format(System.currentTimeMillis());
                        String format1 = SimpleDateFormat.getDateTimeInstance().format(System.currentTimeMillis());
                        Log.d(TAG, "文件名  " + format + " " + format1);

                        Log.d(TAG, "uri " + mUri);
                        cropImage(mUri);


//                            Bitmap bitmap = data.getParcelableExtra("data");
//                            if (bitmap != null) {
//                                mTest.setImageBitmap(bitmap);
//                            }
////                        Uri uri = data.getData();
//                            if (mFile != null){
//                                Uri uri = Uri.fromFile(mFile);
//                                if (uri != null) {
//                                    Log.d(TAG, "uri " + uri.toString());
//                                    cropImage(uri);
//                                }
//                            }
//                            Uri parcelableExtra = data.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
//                            Log.d(TAG, "获取的uri " + parcelableExtra);

//                    }
                    } else {
                        ToastUtil.curt("sd卡不可使用");
                    }
                    break;

                case REQ_CODE_CROP_IMAGE:
                    
                    Bitmap cropBitmap = data.getParcelableExtra("data");
                    if (cropBitmap != null) {
                        mTest.setImageBitmap(cropBitmap);
                    }
                    break;

            }
        }

    }

    private void cropImage(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");// crop=true 有这句才能出来最后的裁剪页面.
        intent.putExtra("aspectX", 1);// 这两项为裁剪框的比例.
        intent.putExtra("aspectY", 1);// x:y=1:1
        intent.putExtra("outputX", 300);//图片输出大小
        intent.putExtra("outputY", 500);
        intent.putExtra("output", uri);
        intent.putExtra("outputFormat", "JPEG");// 返回格式
        startActivityForResult(intent, REQ_CODE_CROP_IMAGE);
    }
}
