package com.jservoire.bingo;

import java.util.List;

import Interfaces.PreferencesListener;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Chronometer.OnChronometerTickListener;
import android.widget.GridView;
import android.widget.ImageView;

import com.island.android.game.bingo.game.GamePlayed;
import com.island.android.game.bingo.game.interfaces.IBingoClient;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class RoomActivity extends FragmentActivity implements PreferencesListener, IBingoClient
{
	private Button btnHome;
	private Button btnSettings;
	private Button btnBingo;
	private ImageView avatarImageView;
	private GridView gridView;
	private boolean musicEnabled;
	private int delay;
	private GridAdapter adapter;
	private MediaPlayer backgroundPlayer;
	private Chronometer chronometer;
	private int time = 0;
	private Dialog dialogChrono;
	
	private View.OnClickListener btnHomeListener = new View.OnClickListener() {

		@Override
		public void onClick(final View v){
			// Close RoomActivity and back to MainActivity
			finish();
		}
	};
	
	private View.OnClickListener btnSettingsListener = new View.OnClickListener() {

		@Override
		public void onClick(final View v) 
		{
			// Show Settings Dialog
			FragmentManager fm = getSupportFragmentManager();
			SettingsDialogFragment.newInstance().show(fm, "test");
		}
	};
	
	private View.OnClickListener btnBingoListener = new View.OnClickListener() {

		@Override
		public void onClick(final View v) {		
			playSound(R.raw.bingo);
			
			// Notify serveur when a user think he wins
			BingoApp.srv.notifyBingo(RoomActivity.this);
		}
	};

	private GridView.OnItemClickListener caseListener = new GridView.OnItemClickListener() 
	{
		@Override
		public void onItemClick(final AdapterView<?> arg0, final View cell, final int index,final long arg3) 
		{
			ImageView circleView = (ImageView)cell.findViewById(R.id.circleImgView);			
			int intVisible = 0;
			boolean isDaubed = false;
			
			if ( circleView.getVisibility() == View.INVISIBLE )
			{
				intVisible = View.VISIBLE;
				isDaubed = true;
			}
			else {
				intVisible = View.INVISIBLE;
			}
			
			BingoApp.srv.notifyNumberDaubed(RoomActivity.this, adapter.getValueItem(index),isDaubed);
			circleView.setVisibility(intVisible);
			playSound(R.raw.daub);
		}
	};
	
	public void buildChronometer()
	{
		View modal = getLayoutInflater().inflate(R.layout.dialog_chronometer, null);
		chronometer = (Chronometer)modal.findViewById(R.id.chronometer);
		chronometer.setBase(SystemClock.elapsedRealtime());
		time = 0;
		chronometer.setOnChronometerTickListener(new OnChronometerTickListener() {
			
			@Override
			public void onChronometerTick(Chronometer mChronometer) 
			{
				if ( time >= 5 ) {
					mChronometer.stop();
					dialogChrono.dismiss();
					startGame();
				}
				time++;
			}
		});
		
		dialogChrono = new Dialog(this);
		dialogChrono.setContentView(modal);
		dialogChrono.setTitle(getResources().getString(R.string.strReady));		
		dialogChrono.show();
		chronometer.start();
	}

	@Override
	public String getId() {
		return "01A";
	}

	@Override
	public String getName() {
		return "Johann";
	}

	private void loadPreferences()
	{
		SharedPreferences preferences = getSharedPreferences(SettingsDialogFragment.PREF_FILE, Context.MODE_PRIVATE);
		String strAvatar = preferences.getString("avatar",null);	
		if ( strAvatar != null && strAvatar.length() > 0 )
		{
			Drawable imgDraw = new BitmapDrawable(getResources(), strAvatar);
			if ( imgDraw != null ) {
				avatarImageView.setImageDrawable(imgDraw);
			}
		}
		
		musicEnabled = preferences.getBoolean("activMusic",false);
		if ( musicEnabled ) {
			startBackgroundMusic();
		}
		else {
			stopBackgroundMusic();
		}
		
		delay = preferences.getInt("delay",3);
		BingoApp.srv.setNumbersDelay(delay);
	}

	@Override
	public void onClientVictory(final String id, final String name) {
		Crouton.makeText(this,"You Win "+name,Style.CONFIRM).show();
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_room);
		playSound(R.raw.welcome);
		btnHome = (Button)findViewById(R.id.btnHome);
		btnHome.setOnClickListener(btnHomeListener);
		btnSettings = (Button)findViewById(R.id.btnSettings);
		btnSettings.setOnClickListener(btnSettingsListener);
		btnBingo = (Button)findViewById(R.id.btnBingo);
		btnBingo.setOnClickListener(btnBingoListener);
		gridView = (GridView)findViewById(R.id.gridView);
		gridView.setOnItemClickListener(caseListener);
		avatarImageView = (ImageView)findViewById(R.id.avatarImageView);
		backgroundPlayer = MediaPlayer.create(this, R.raw.background_music);
		loadPreferences();
		buildChronometer();
	}

	@Override
	public void onGamesPlayedList(final List<GamePlayed> listGame) {
		Log.d("onGamesPlayedList", Integer.toString(listGame.size()));
	}

	@Override
	public void onGameToPlayRestored(final GamePlayed nGame) 
	{
		// Build game grid
		adapter = new GridAdapter(this,nGame);
		gridView.setAdapter(adapter);
	}

	@Override
	public void onNewNumber(final int indexMusic) 
	{
		// Play sound on receive new number
		String nameMusic = "ball_call_"+Integer.toString(indexMusic);
		int idMusic = getResources().getIdentifier(nameMusic,"raw",getPackageName());
		if ( idMusic > 0 ) {
			playSound(idMusic);
		}
	}

	@Override
	public void onPreferencesChanged()
	{
		loadPreferences();		
		Crouton.makeText(this,getResources().getString(R.string.savePref),Style.CONFIRM).show();
	}

	@Override
	public void onRoundOver() 
	{
		// When the game is over
		BingoApp.srv.notifyPauseCurrentGame(this);
		stopBackgroundMusic();
		playSound(R.raw.round_over);
	}

	@Override
	public void onWrongBingo() {
		Crouton.makeText(this,getResources().getString(R.string.wrBingo),Style.ALERT).show();
	}
	
	public void playSound(int idSound)
	{
		MediaPlayer mp = MediaPlayer.create(RoomActivity.this, idSound);
        mp.setOnCompletionListener(new OnCompletionListener() 
        {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }
        });
        
        mp.start();
	}
	
	public void startBackgroundMusic()
	{
	    backgroundPlayer.setLooping(true);
	    backgroundPlayer.setVolume(100,100); 
	    backgroundPlayer.start();
	}
	
	public void stopBackgroundMusic() 
	{
		if ( backgroundPlayer.isPlaying() ) {
			backgroundPlayer.pause();
		}
	}
	
	public void startGame()
	{
		long dateRestor = 0;
		if ( getIntent().hasExtra("when") ) {
			dateRestor = getIntent().getLongExtra("when",0);
		}
		
		// Begin play
		BingoApp.srv.notifyRegistration(this);
		
		// New or resume game
		BingoApp.srv.notifyReadyForGame(this,dateRestor);
		BingoApp.srv.requestResumeCurrentGame(this,dateRestor);		
	}

	@Override
	protected void onResume() 
	{
		super.onResume();
		if ( musicEnabled ) {
			startBackgroundMusic();
		}
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		BingoApp.srv.notifyPauseCurrentGame(this);
		if ( musicEnabled ){
			stopBackgroundMusic();
		}
	}
}
