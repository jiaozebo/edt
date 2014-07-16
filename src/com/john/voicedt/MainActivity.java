package com.john.voicedt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;

public class MainActivity extends ActionBarActivity implements CompoundButton.OnCheckedChangeListener {

	public class Question {
		public byte currectAnswer = -1;
		public String explain = null;
	}

	protected static final String KEY_POSITION = "key_position";

	private DBHelper mDBHelper;
	private ViewSwitcher mViewSwitcer;
	private AsyncTask<Void, Integer, Cursor> mTask;

	protected Cursor mCusor;
	// 语音合成对象
	private SpeechSynthesizer mTts;

	// 默认发音人
	private String voicer = "xiaoyan";
	private String[] cloudVoicersEntries;
	private String[] cloudVoicersValue;

	// 缓冲进度
	private int mPercentForBuffering = 0;
	// 播放进度
	private int mPercentForPlaying = 0;

	// 云端/本地选择按钮
	private RadioGroup mRadioGroup;
	// 引擎类型
	private String mEngineType = SpeechConstant.TYPE_CLOUD;
	// 语音+安装助手类
	ApkInstaller mInstaller;
	// 语音听写对象
	private SpeechRecognizer mIat;
	// 听写结果内容
	private EditText mResultText;
	// 用户词表下载结果
	private String mDownloadResult = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
		}
		mTts = SpeechSynthesizer.createSynthesizer(this, mTtsInitListener);
		mIat = SpeechRecognizer.createRecognizer(this, mInitListener);
	}

	/**
	 * 初始化监听器。
	 */
	private InitListener mInitListener = new InitListener() {

		@Override
		public void onInit(int code) {
			if (code == ErrorCode.SUCCESS) {
			} else {
				mIat = null;
				showTip("初始化语音模块失败.");
			}
		}
	};
	/**
	 * 初期化监听。
	 */
	private InitListener mTtsInitListener = new InitListener() {
		@Override
		public void onInit(int code) {
			if (code == ErrorCode.SUCCESS) {
			} else {
				mTts = null;
				showTip("初始化语音模块失败.");
			}
		}

	};

	private void showTip(String msg) {
		Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onPostCreate(savedInstanceState);

		mDBHelper = new DBHelper(this);
		mViewSwitcer = (ViewSwitcher) findViewById(R.id.switcher);
		mTask = new AsyncTask<Void, Integer, Cursor>() {

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				findViewById(R.id.loader).setVisibility(View.VISIBLE);
			}

			@Override
			protected void onPostExecute(Cursor result) {
				super.onPostExecute(result);
				findViewById(R.id.loader).setVisibility(View.GONE);
				if (result.moveToPosition(getPreferences(MODE_PRIVATE).getInt(KEY_POSITION, 0))) {
					initQuestionWithCursor(result, mViewSwitcer.getCurrentView());
				}
				mCusor = result;
			}

			@Override
			protected Cursor doInBackground(Void... params) {

				String dpPath = String.format("/data/data/%s/databases/", getPackageName());
				File file = new File(dpPath);
				file.mkdirs();
				file = new File(file, "jxedt_user.db");
				if (!file.exists()) {
					try {
						InputStream fis = getResources().openRawResource(R.raw.jxedt_user);
						OutputStream fos = new FileOutputStream(file);
						byte[] buffer = new byte[1024];
						int length;
						while ((length = fis.read(buffer)) > 0) {
							fos.write(buffer, 0, length);
						}

						// Close the streams
						fos.flush();
						fos.close();
						fis.close();
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				Cursor c = mDBHelper.getReadableDatabase().rawQuery("Select * From web_note where sinaimg='' or sinaimg isnull;", null);
				if (isCancelled()) {
					c.close();
				}
				return c;
			}
		}.execute();
	}

	private void initQuestionWithCursor(Cursor c, View currentView) {
		TextView question = (TextView) currentView.findViewById(R.id.question);
		int id = c.getInt(c.getColumnIndex(BaseColumns._ID));
		String questionText = c.getString(c.getColumnIndex("Question"));
		question.setText(id + "、" + questionText);

		RadioButton aa = (RadioButton) currentView.findViewById(R.id.answer_a);
		RadioButton ab = (RadioButton) currentView.findViewById(R.id.answer_b);
		RadioButton ac = (RadioButton) currentView.findViewById(R.id.answer_c);
		RadioButton ad = (RadioButton) currentView.findViewById(R.id.answer_d);

		aa.setChecked(false);
		ab.setChecked(false);
		ac.setChecked(false);
		ad.setChecked(false);

		String allAnswer = "a、";
		String answerA = c.getString(c.getColumnIndex("An1"));

		aa.setText(TextUtils.isEmpty(answerA) ? "正确" : answerA);
		allAnswer += TextUtils.isEmpty(answerA) ? "正确" : answerA;
		allAnswer += "\nb、";
		String answerB = c.getString(c.getColumnIndex("An2"));
		ab.setText(TextUtils.isEmpty(answerB) ? "错误" : answerB);
		allAnswer += TextUtils.isEmpty(answerB) ? "错误" : answerB;
		allAnswer += "\nc、";

		String answer = c.getString(c.getColumnIndex("An3"));
		ac.setVisibility(TextUtils.isEmpty(answer) ? View.GONE : View.VISIBLE);
		ac.setText(answer);
		allAnswer += answer;
		allAnswer += "\nd、";

		answer = c.getString(c.getColumnIndex("An4"));
		ad.setVisibility(TextUtils.isEmpty(answer) ? View.GONE : View.VISIBLE);
		ad.setText(answer);
		allAnswer += answer;

		Question q = (Question) currentView.getTag();
		if (q == null) {
			q = new Question();
			currentView.setTag(q);
		}
		q.explain = c.getString(c.getColumnIndex("explain"));
		q.currectAnswer = (byte) c.getInt(c.getColumnIndex("AnswerTrue"));
		TextView explain = (TextView) findViewById(R.id.explain);
		explain.setText(q.explain);

		SpeechSynthesizer tts = mTts;
		if (tts != null) {
//			tts.startSpeaking(String.format("%s\n%s\n", questionText, allAnswer), mTtsListener);
			
			// 不显示听写对话框
						
		}
	}

	/**
	 * 听写监听器。
	 */
	private RecognizerListener recognizerListener = new RecognizerListener() {

		@Override
		public void onBeginOfSpeech() {
		}

		@Override
		public void onError(SpeechError error) {
			showTip(error.getPlainDescription(true));
		}

		@Override
		public void onEndOfSpeech() {
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, String msg) {

		}

		@Override
		public void onResult(RecognizerResult results, boolean isLast) {
			String text = JsonParser.parseIatResult(results.getResultString());
			mResultText.append(text);
			mResultText.setSelection(mResultText.length());
			if (isLast) {
				// TODO 最后的结果
				showTip(text);
				if ("a".equals(text)) {

				} else if ("b".equals(text)) {

				} else if ("过".equals(text) || "pass".equals(text)) {

				}
			}
		}

		@Override
		public void onVolumeChanged(int volume) {
			showTip("当前正在说话，音量大小：" + volume);
		}

	};

	/**
	 * 合成回调监听。
	 */
	private SynthesizerListener mTtsListener = new SynthesizerListener() {
		@Override
		public void onSpeakBegin() {
			showTip("开始播放");

		}

		@Override
		public void onSpeakPaused() {
			showTip("暂停播放");
		}

		@Override
		public void onSpeakResumed() {
			showTip("继续播放");
		}

		@Override
		public void onBufferProgress(int percent, int beginPos, int endPos, String info) {
			mPercentForBuffering = percent;
		}

		@Override
		public void onSpeakProgress(int percent, int beginPos, int endPos) {
			mPercentForPlaying = percent;
		}

		@Override
		public void onCompleted(SpeechError error) {
			if (error == null) {
				showTip("播放完成");
			} else if (error != null) {
				showTip(error.getPlainDescription(true));
			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			return rootView;
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			final ViewSwitcher vs = (ViewSwitcher) getView().findViewById(R.id.switcher);
			vs.setFactory(new ViewSwitcher.ViewFactory() {

				@Override
				public View makeView() {
					View view = getLayoutInflater(null).inflate(R.layout.question_item, vs, false);
					RadioButton aa = (RadioButton) view.findViewById(R.id.answer_a);
					CompoundButton.OnCheckedChangeListener listener = (OnCheckedChangeListener) getActivity();
					aa.setOnCheckedChangeListener(listener);

					aa = (RadioButton) view.findViewById(R.id.answer_b);
					aa.setOnCheckedChangeListener(listener);

					aa = (RadioButton) view.findViewById(R.id.answer_c);
					aa.setOnCheckedChangeListener(listener);

					aa = (RadioButton) view.findViewById(R.id.answer_d);
					aa.setOnCheckedChangeListener(listener);
					return view;
				}
			});

			vs.setInAnimation(getActivity(), android.R.anim.slide_in_left);
			vs.setOutAnimation(getActivity(), android.R.anim.slide_out_right);
		}

	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (isChecked) {
			// 答对了，下一题，打错了，弹出解释
			boolean correct = false;
			ViewGroup p = (ViewGroup) buttonView.getParent();
			p = (ViewGroup) p.getParent();
			Question q = (Question) p.getTag();
			switch (q.currectAnswer - 1) {
			case 0:
				correct = (buttonView.getId() == R.id.answer_a);
				break;
			case 1:
				correct = (buttonView.getId() == R.id.answer_b);
				break;
			case 2:
				correct = (buttonView.getId() == R.id.answer_c);
				break;
			case 3:
				correct = (buttonView.getId() == R.id.answer_d);
				break;
			default:
				break;

			}
			if (correct) {
				mViewSwitcer.showNext();
				mCusor.moveToNext();
				initQuestionWithCursor(mCusor, mViewSwitcer.getCurrentView());
				mViewSwitcer.getCurrentView().findViewById(R.id.explain).setVisibility(View.GONE);
			} else {
				p.findViewById(R.id.explain).setVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	protected void onDestroy() {
		if (mCusor != null) {
			mCusor.close();
			mCusor = null;
		}
		super.onDestroy();
	}

	/**
	 * 参数设置
	 * 
	 * @param param
	 * @return
	 */
	private void setParam() {

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		// 设置合成
		if (mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
			mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
		} else {
			mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
		}

		// 设置发音人
		mTts.setParameter(SpeechConstant.VOICE_NAME, voicer);

		// 设置语速
		mTts.setParameter(SpeechConstant.SPEED, pref.getString("speed_preference", "50"));

		// 设置音调
		mTts.setParameter(SpeechConstant.PITCH, pref.getString("pitch_preference", "50"));

		// 设置音量
		mTts.setParameter(SpeechConstant.VOLUME, pref.getString("volume_preference", "50"));

		// 设置播放器音频流类型
		mTts.setParameter(SpeechConstant.STREAM_TYPE, pref.getString("stream_preference", "3"));

		String lag = pref.getString("iat_language_preference", "mandarin");
		if (lag.equals("en_us")) {
			// 设置语言
			mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
		} else {
			// 设置语言
			mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
			// 设置语言区域
			mIat.setParameter(SpeechConstant.ACCENT, lag);
		}
		// 设置语音前端点
		mIat.setParameter(SpeechConstant.VAD_BOS, pref.getString("iat_vadbos_preference", "4000"));
		// 设置语音后端点
		mIat.setParameter(SpeechConstant.VAD_EOS, pref.getString("iat_vadeos_preference", "1000"));
		// 设置标点符号
		mIat.setParameter(SpeechConstant.ASR_PTT, pref.getString("iat_punc_preference", "1"));
		// 设置音频保存路径
		mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, "/sdcard/iflytek/wavaudio.pcm");

	}
}
