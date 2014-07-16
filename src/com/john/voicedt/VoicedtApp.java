package com.john.voicedt;

import android.app.Application;

import com.iflytek.cloud.SpeechUtility;

public class VoicedtApp extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		SpeechUtility.createUtility(this, "appid=53c15a6d");
	}

}
