/*******************************************************************************
 * Copyright (c) 2010 Ugo Sangiorgi and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl-2.0.txt
 *
 * Contributors:
 *  Ugo Sangiorgi <android@ugosan.org> 
 *******************************************************************************/

package org.ugosan.theremin;

import java.util.ArrayList;

import org.ugosan.theremin.audio.AndroidAudioDevice;
import org.ugosan.theremin.audio.SoundManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.admob.android.ads.AdManager;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class Main extends Activity implements SensorEventListener, OnClickListener, Runnable {

	static String TAG = "Theremin";

	//TODO: calibration, not hard code.
	static int MAX_MAGNETIC = 40;
	
	static String[] NOTES = new String[]{"C1","D1","E1","F1","G1","A1","B1",
										 "C2","D2","E2","F2","G2","A2","B2",
										 "C3","D3","E3","F3","G3","A3","B3",
										 "C4","D4","E4","F4","G4","A4","B4"};


	private SoundManager mSoundManager;
	SensorManager sm = null;
	TextView note = null;

	ProgressBar zprogress = null;

	
	RadioButton scale_full;
	RadioButton c1;
	RadioButton c2;
	RadioButton c3;
	RadioButton c4;

	CheckBox playFrequencies;
	CheckBox playBaseNote;
	Spinner basenote;
	
	Button buttonReset;
	Button buttonPlay;
	
	ArrayList<Integer> soundlib = new ArrayList<Integer>();


	int scale_min = 0;
	int scale_max = 7;

	float defaultX = 0;
	float defaultY = 0;
	float defaultZ = 0;

	float currentX = 0;
	float currentY = 0;
	float currentZ = 0;
	
	
	float frequency = 440;
	float phase = 100;

	private Thread fthread;
	boolean fthread_running = false;


	int basenotevalue = -1;
	
	int current;
	int semi;
	int fulltone;

	//TODO: better tracking of events
	private GoogleAnalyticsTracker tracker;

	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//ad test mode
		AdManager.setTestDevices( new String[] { "D9CE445239CB74BFC97CF6D6F935A218"} );  
		 
		
		tracker = GoogleAnalyticsTracker.getInstance();
        tracker.start("UA-17934058-4",5, this);
		
		
		
		// get reference to SensorManager
		sm = (SensorManager) getSystemService(SENSOR_SERVICE);

        
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);  
	    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
	                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.main);
		
		LinearLayout mainPanel = (LinearLayout)findViewById(R.id.main);
		mainPanel.setBackgroundResource(R.raw.bg);
		
		
		note = (TextView) findViewById(R.id.offset);

		basenote = (Spinner) findViewById(R.id.BaseNoteSpinner);
		
		playBaseNote = (CheckBox)findViewById(R.id.playbasenote);
		playBaseNote.setOnClickListener(this);
		
		ArrayAdapter spinnerArrayAdapter = new ArrayAdapter(this,
			        android.R.layout.simple_spinner_dropdown_item,
			            NOTES);
		basenote.setAdapter(spinnerArrayAdapter);


		zprogress = (ProgressBar) findViewById(R.id.ProgressBar01);

		scale_full = (RadioButton) findViewById(R.id.RadioButton05);
		scale_full.setOnClickListener(this);
		c1 = (RadioButton) findViewById(R.id.RadioButton01);
		c1.setOnClickListener(this);
		c2 = (RadioButton) findViewById(R.id.RadioButton02);
		c2.setOnClickListener(this);
		c3 = (RadioButton) findViewById(R.id.RadioButton03);
		c3.setOnClickListener(this);
		c4 = (RadioButton) findViewById(R.id.RadioButton04);
		c4.setOnClickListener(this);


		playFrequencies = (CheckBox) findViewById(R.id.CheckBox01);
		playFrequencies.setOnClickListener(this);
		
		mSoundManager = new SoundManager();
		mSoundManager.initSounds(getBaseContext());

		soundlib.add(R.raw.c11); //c1
		soundlib.add(R.raw.c12); //d1
		soundlib.add(R.raw.c13); //e1
		soundlib.add(R.raw.c14); //f1
		soundlib.add(R.raw.c15); //g1
		soundlib.add(R.raw.c16); //a1
		soundlib.add(R.raw.c17); //b1

		soundlib.add(R.raw.c21);
		soundlib.add(R.raw.c22);
		soundlib.add(R.raw.c23);
		soundlib.add(R.raw.c24);
		soundlib.add(R.raw.c25);
		soundlib.add(R.raw.c26);
		soundlib.add(R.raw.c27);	

		soundlib.add(R.raw.c31);
		soundlib.add(R.raw.c32);
		soundlib.add(R.raw.c33);
		soundlib.add(R.raw.c34);
		soundlib.add(R.raw.c35);
		soundlib.add(R.raw.c36);
		soundlib.add(R.raw.c37);	        

		soundlib.add(R.raw.c41); 
		soundlib.add(R.raw.c42); 
		soundlib.add(R.raw.c43); 
		soundlib.add(R.raw.c44); 
		soundlib.add(R.raw.c45); 
		soundlib.add(R.raw.c46); 
		soundlib.add(R.raw.c47);
		soundlib.add(R.raw.c48);


		current = soundlib.get(0);


		for(int i=0;i<soundlib.size();i++){
			mSoundManager.addSound((int)soundlib.get(i));
		}

		buttonReset = (Button)findViewById(R.id.Button01);
		buttonReset.setOnClickListener(this);
		buttonPlay = (Button)findViewById(R.id.play);
		buttonPlay.setOnClickListener(this);

		
		//start the thread
		fthread = new Thread(this);
		fthread.start();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.mainmenu, menu);
	    tracker.trackEvent(
	            "Clicks",  // Category
	            "Menu",  // Action
	            "clicked", // Label
	            1);       // Value
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.about:
	    	showAbout();
	        return true;
	    case R.id.suggest:
	    	Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND); 
	    	emailIntent.setType("plain/text");
	    	String aEmailList[] = { "android@ugosan.org"};  
	    	   
	    	emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, aEmailList);  

	    	emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Theremin");  
	    	   
	    	emailIntent.setType("plain/text");  
	    	emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Hello, \n");  
	    	  
	    	startActivity(emailIntent);
	    	return true;
	    case R.id.exit:
	        this.finish();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	

	public void onClick(View v) {
		if(v == buttonReset){
			defaultX = 0;
			defaultY = 0;
			defaultZ = 0;

		}else if(v == buttonPlay){
			
			if(!playBaseNote.isChecked()){
				mSoundManager.playSound(current);
			}else{
				mSoundManager.playSound(current,(int)soundlib.get(basenotevalue));
			}
			 tracker.trackEvent(
			            "Clicks",  // Category
			            "Button",  // Action
			            "play clicked", // Label
			            1);       // Value
	
		}else if(v == playBaseNote){
			
			if(playBaseNote.isChecked()){				
				basenotevalue = basenote.getSelectedItemPosition();				
			}
			
			
		}else if(v == playFrequencies){
			if(playFrequencies.isChecked()){
				fthread_running = true;
			}else{
				fthread_running = false;
			}
			
		}else if(v == scale_full || v == c4){
			scale_min = 0;
			scale_max = 28;
		}else if(v == c1){
			scale_min = 0;
			scale_max = 7;
		}else if(v == c2){
			scale_min = 8;
			scale_max = 14;
		}else if(v == c3){
			scale_min = 15;
			scale_max = 21;
		}

	}


	public void showAbout(){
		Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("Theremin for Android");
		dialog.setIcon(R.drawable.icon);
		dialog.setMessage(R.string.about_text);
		dialog.setPositiveButton("ok", null);		
		dialog.show();
	}



	//set the sound to be played when Play is pressed or when 
	//the magnetic thread is running
	private void setSound(float f){

		int scalediff = scale_max - scale_min;

		int valor = scale_min + Math.round((scalediff*f)/100);

		frequency = 200 + f*15;

		if(valor >= scale_max){
			valor = scale_max-1;
		}
		
		note.setText("note: "+NOTES[valor]);
		
		current = soundlib.get(valor);
		
		semi = soundlib.get(valor/2);
		
		if(valor-3>0){
			fulltone = soundlib.get(valor-3);
		}


	}




	public void onAccuracyChanged(int sensor, int accuracy) {
		//Log.d(tag,"onAccuracyChanged: " + sensor + ", accuracy: " + accuracy);
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		
		sm.registerListener(this, 
				sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_GAME);
		
		Toast.makeText(getBaseContext(),"Sit your phone on a planar surface and reset the sensors.",
				Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onStop() {
		sm.unregisterListener(this);
		super.onStop();
	}


	@Override
	public void run() {
		float increment; // angular increment for each sample
		float angle = (currentY - defaultY);
		AndroidAudioDevice device = new AndroidAudioDevice( );
		float samples[] = new float[1024];

		while( true )
		{
			if(fthread_running && (Math.abs(currentZ - defaultZ)>1)){
				
				
				increment = (float)(2 *Math.PI) * frequency  / 22050; // angular increment for each sample
				
				for( int i = 0; i < samples.length; i++ ){

					samples[i] = (float)Math.sin( angle );
					angle += increment;
				}

				device.writeSamples( samples );


			}else{
				try {
					Thread.sleep(500);
				} catch (InterruptedException ex) {
					break;
				}
			}

		}        
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		
		currentX = event.values[0];
		currentY = event.values[1];
		currentZ = event.values[2]; //Z axis

		//Log.i(TAG, values[0]+","+values[1]+","+values[2]);

		//calibrates the default values
		if(defaultZ==0){					
			defaultX = event.values[0];
			defaultY = event.values[1];
			defaultZ = event.values[2];
		}


		float d = Math.abs(defaultZ - event.values[2]);

		if(d>MAX_MAGNETIC){
			d = MAX_MAGNETIC;
		}

		//set the sound based on how filled is the bar
		Float f = new Float(100*d/MAX_MAGNETIC);
		zprogress.setProgress(f.intValue());

		//set the sound to be played 
		setSound(f);
	}



}