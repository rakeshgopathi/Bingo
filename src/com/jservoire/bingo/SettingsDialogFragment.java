package com.jservoire.bingo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import Interfaces.PreferencesListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.island.android.game.bingo.utils.Constants;
import com.island.android.game.bingo.utils.IAsyncListener;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class SettingsDialogFragment extends DialogFragment implements IAsyncListener
{
	public static SettingsDialogFragment newInstance() {
		return new SettingsDialogFragment();
	}

	private DialogInterface.OnClickListener saveListener = new DialogInterface.OnClickListener() 
	{
		@Override
		public void onClick(final DialogInterface arg0, final int arg1) {
			savePreferences();
		}
	};

	private DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener() 
	{
		@Override
		public void onClick(final DialogInterface dialog, final int which) {
			Log.d("close dialog","on close SettingsDialogFragment");
		}
	};

	private ToggleButton activMusic;
	private CompoundButton.OnCheckedChangeListener eventActMusic = new CompoundButton.OnCheckedChangeListener() 
	{
		@Override
		public void onCheckedChanged(final CompoundButton buttonView,final boolean isChecked) {
			musicEnable = isChecked;
		}
	};

	private SeekBar seekDelayBar;
	private SeekBar.OnSeekBarChangeListener listenerSeekBar = new SeekBar.OnSeekBarChangeListener() 
	{
		@Override
		public void onProgressChanged(final SeekBar seekBar,final int progress, final boolean fromUser) {
			textDelay.setText(Integer.toString(progress));
		}

		@Override
		public void onStartTrackingTouch(final SeekBar seekBar) {
		}

		@Override
		public void onStopTrackingTouch(final SeekBar seekBar) {
			delay = seekBar.getProgress();
			if ( delay < 3 ) {
				delay = 3;
			}
		}
	};

	private Button btnAvatar;
	private View.OnClickListener clickAvatar = new View.OnClickListener() 
	{
		@Override
		public void onClick(final View v) 
		{
			File zipFile = new File(pathZipAvatar);
			if ( zipFile.isFile() )
			{
				// Start Gallery to choose an avatar
				Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
				photoPickerIntent.setType("image/*");
				startActivityForResult(photoPickerIntent, 0);
			}
			else
			{
				// Download avatar archive
				DownloadAsyncTask dl = new DownloadAsyncTask(getActivity());
				dl.execute(Constants.AVATAR_DOWNLOAD_URL,pathZipAvatar,pathAvatar);
			}
		}
	};

	private TextView textDelay;
	private int delay;
	private String strAvatar;
	private Boolean musicEnable;
	private String pathAvatar;
	private String pathZipAvatar;
	public final static String PREF_FILE = "BingoPref";
	private SharedPreferences preferences;

	private Bitmap loadAvatar() 
	{
		File avatar = new File(Environment.getExternalStorageDirectory()
				+ "/Bingo/avatar.jpg");
		return ( avatar.exists() ) ? BitmapFactory.decodeFile(avatar
				.getAbsolutePath()) : null;
	}

	private void loadPreferences() 
	{
		musicEnable = preferences.getBoolean("activMusic", false);
		delay = preferences.getInt("delay", 3);
		strAvatar = preferences.getString("avatar", null);
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode,final Intent imageReturnedIntent) 
	{
		switch (requestCode) 
		{
		case 0:
			if (resultCode == Activity.RESULT_OK) 
			{
				Uri selectedImage = imageReturnedIntent.getData();
				try 
				{
					InputStream imageStream = getActivity()
							.getContentResolver()
							.openInputStream(selectedImage);
					Bitmap bmpImg = BitmapFactory.decodeStream(imageStream);

					Drawable imgDraw = new BitmapDrawable(getResources(),
							bmpImg);
					imgDraw.setBounds(0, 0, 100, 100);
					btnAvatar.setCompoundDrawables(imgDraw, null, null, null);

					saveImage(bmpImg);
				} 
				catch (FileNotFoundException e) {
					Log.e("File not found",e.getLocalizedMessage());
				}

			}
			break;
		}
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) 
	{
		super.onCreateDialog(savedInstanceState);
		preferences = getActivity().getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
		
		// Inflate layout dialog
		LayoutInflater inflater = (LayoutInflater) getActivity()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View vDialog = inflater.inflate(R.layout.dialog_settings, null);

		activMusic = (ToggleButton) vDialog.findViewById(R.id.toggleMusic);
		activMusic.setOnCheckedChangeListener(eventActMusic);

		seekDelayBar = (SeekBar) vDialog.findViewById(R.id.seekBarDelay);
		seekDelayBar.setOnSeekBarChangeListener(listenerSeekBar);
		textDelay = (TextView) vDialog.findViewById(R.id.textNbDelay);

		btnAvatar = (Button) vDialog.findViewById(R.id.buttonAvatar);
		btnAvatar.setOnClickListener(clickAvatar);

		loadPreferences();
		activMusic.setChecked(musicEnable);
		seekDelayBar.setProgress(delay);
		textDelay.setText(Integer.toString(delay));

		pathAvatar = Environment.getExternalStorageDirectory()+ "/Bingo/";
		File filePathAvatar = new File(pathAvatar);
		if (!filePathAvatar.exists() && !filePathAvatar.isDirectory()) {
			filePathAvatar.mkdir();
		}
		pathZipAvatar = pathAvatar+ "archive.zip";
		btnAvatar.setText(getResources().getString(R.string.chooseAvatar));
		if ( strAvatar != null && strAvatar.length() > 0 ) 
		{
			btnAvatar.setText(getResources().getString(R.string.changeAvatar));
			Bitmap avatar = loadAvatar();
			if (avatar != null) 
			{
				File fileAvatar = new File(strAvatar);
				if ( fileAvatar.exists() )
				{
					Drawable imgDraw = new BitmapDrawable(getResources(), strAvatar);
					imgDraw.setBounds(0, 0, 100, 100);
					btnAvatar.setCompoundDrawables(imgDraw, null, null, null);
				}
				else 
				{
					strAvatar = null;
					savePreferences();
				}
			}
		}

		// Build dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(vDialog);
		builder.setTitle(getResources().getString(R.string.action_settings));
		builder.setPositiveButton(
				getResources().getString(R.string.saveSettings), saveListener);
		builder.setNegativeButton(
				getResources().getString(R.string.cancelSettings),
				cancelListener);

		return builder.create();
	}

	@Override
	public void onError(final Object arg0) {
		Crouton.makeText(getActivity(), getResources().getString(R.string.errSavePref), Style.ALERT);
	}

	@Override
	public void onSucess(final Object arg0) 
	{
		Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
		photoPickerIntent.setType("image/*");
		startActivityForResult(photoPickerIntent, 0);
	}

	private void saveImage(final Bitmap bmp) 
	{
		File savePath = new File(Environment.getExternalStorageDirectory()
				+ "/Bingo/");
		if (!savePath.exists() && !savePath.isDirectory()) {
			savePath.mkdir();
		}

		// Save avatar
		if ( savePath != null ) 
		{
			try {
				OutputStream out = new FileOutputStream(savePath.getPath()
						+ "/avatar.jpg");
				strAvatar = savePath.getPath() + "/avatar.jpg";
				bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void savePreferences()
	{
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("activMusic", musicEnable);
		editor.putInt("delay", delay);
		editor.putString("avatar", strAvatar);
		editor.commit();
		PreferencesListener listener = (PreferencesListener)getActivity();
		listener.onPreferencesChanged();
	}
}
