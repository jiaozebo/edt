package com.john.voicedt;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;


/**
 * 弹出提示框，下载服务组件
 */
public class ApkInstaller {
	private Activity mActivity ;
	
	public ApkInstaller(Activity activity) {
		mActivity = activity;
	}

	@SuppressWarnings("deprecation")
	public void install(){/*
		final Dialog dialog=new Dialog(mActivity,android.R.style.the);
		LayoutInflater inflater = mActivity.getLayoutInflater();
		View alertDialogView = inflater.inflate(R.layout.superman_alertdialog, null);
		dialog.setContentView(alertDialogView);
		Button okButton = (Button) alertDialogView.findViewById(R.id.ok);
		Button cancelButton = (Button) alertDialogView.findViewById(R.id.cancel);
		TextView comeText=(TextView) alertDialogView.findViewById(R.id.title);
		comeText.setTypeface(Typeface.MONOSPACE,Typeface.ITALIC);
		//确认
		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
				String url = SpeechUtility.getUtility().getComponentUrl();
				String assetsApk="SpeechService.apk";
				processInstall(mActivity, url,assetsApk);
			}
		});
		//取消
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		dialog.show();			
		WindowManager windowManager = mActivity.getWindowManager();
		Display display = windowManager.getDefaultDisplay();
		WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
		lp.width = (int)(display.getWidth()); //设置宽度
		dialog.getWindow().setAttributes(lp);
		return;*/
	}
	/**
	 * 如果服务组件没有安装打开语音服务组件下载页面，进行下载后安装�?
	 */
	private boolean processInstall(Context context ,String url,String assetsApk){
		//直接下载方式
		Uri uri = Uri.parse(url);
		Intent it = new Intent(Intent.ACTION_VIEW, uri);
		context.startActivity(it);
		return true;		
	}
}
