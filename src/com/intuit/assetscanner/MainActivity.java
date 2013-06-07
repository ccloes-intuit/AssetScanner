package com.intuit.assetscanner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.googlecode.tesseract.android.TessBaseAPI;

import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	protected Button _button;
	protected ImageView _image;
	protected TextView _field;
	protected String _dir;
	protected String _path;
	protected boolean _taken;
	public static final String lang = "eng";
	public static final String DATA_PATH = Environment.getExternalStorageDirectory() + "/AssetScanner/tesseract/";
	public static Bitmap bitmap;
	private static final String TAG = "AssetScanner.java";
	
	protected static final String PHOTO_TAKEN	= "photo_taken";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
        _image = ( ImageView ) findViewById( R.id.image );
        _field = ( TextView ) findViewById( R.id.field );
        _button = ( Button ) findViewById( R.id.button );
        _button.setOnClickListener( new ButtonClickHandler() );
        
        _path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/make_machine_example.jpg";
        
		if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists()) {
			try {
		        
				File myLocation = new File(DATA_PATH + "tessdata/");
		        myLocation.mkdirs();
		        
				AssetManager assetManager = getAssets();
				InputStream in = assetManager.open("tessdata/eng.traineddata");
				//GZIPInputStream gin = new GZIPInputStream(in);
				OutputStream out = new FileOutputStream(DATA_PATH + "tessdata/eng.traineddata");

				// Transfer bytes from in to out
				byte[] buf = new byte[1024];
				int len;
				//while ((lenf = gin.read(buff)) > 0) {
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				//gin.close();
				out.close();

				Log.v(TAG, "Copied " + lang + " traineddata");
			} catch (IOException e) {
				Log.e(TAG, "Was unable to copy " + lang + " traineddata " + e.toString());
			}
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
    public class ButtonClickHandler implements View.OnClickListener 
    {
    	public void onClick( View view ){
    		Log.i("MakeMachine", "ButtonClickHandler.onClick()" );
    		startCameraActivity();
    	}
    }
    
    protected void startCameraActivity()
    {
    	Log.i("MakeMachine", "startCameraActivity()" );
    	File file = new File( _path );
    	Uri outputFileUri = Uri.fromFile( file );
    	
    	Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE );
    	intent.putExtra( MediaStore.EXTRA_OUTPUT, outputFileUri );
    	
    	startActivityForResult( intent, 0 );
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {	
    	Log.i( "MakeMachine", "resultCode: " + resultCode );
    	switch( resultCode )
    	{
    		case 0:
    			Log.i( "MakeMachine", "User cancelled" );
    			break;
    			
    		case -1:
    			onPhotoTaken();
    			break;
    	}
    }
    
    protected void onPhotoTaken()
    {
    	Log.i( "MakeMachine", "onPhotoTaken" );
    	
    	_taken = true;
    	
    	BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
    	
    	Bitmap bitmap = BitmapFactory.decodeFile( _path, options );
    	
    	_image.setImageBitmap(bitmap);
    	
    	_field.setVisibility( View.GONE );
    	
        ExifInterface exif;
  		try {
  			exif = new ExifInterface(_path);
  	        int exifOrientation = exif.getAttributeInt(
  	                ExifInterface.TAG_ORIENTATION,
  	                ExifInterface.ORIENTATION_NORMAL);
  	        int rotate = 0;

  	        switch (exifOrientation) {
  	        case ExifInterface.ORIENTATION_ROTATE_90:
  	            rotate = 90;
  	            break;
  	        case ExifInterface.ORIENTATION_ROTATE_180:
  	            rotate = 180;
  	            break;
  	        case ExifInterface.ORIENTATION_ROTATE_270:
  	            rotate = 270;
  	            break;
  	        }

  	        if (rotate != 0) {
  				int w = bitmap.getWidth();
  	            int h = bitmap.getHeight();

  	            // Setting pre rotate
  	            Matrix mtx = new Matrix();
  	            mtx.preRotate(rotate);

  	            // Rotating Bitmap & convert to ARGB_8888, required by tess
  	            bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
  	            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
  	        }
  		        
  		    TessBaseAPI baseApi = new TessBaseAPI();
  		    // DATA_PATH = Path to the storage
  		    // lang for which the language data exists, usually "eng"
  		    //baseApi.init(DATA_PATH, lang); 
  		    baseApi.init(DATA_PATH, lang); 
  		    baseApi.setImage(bitmap);
  		    String recognizedText = baseApi.getUTF8Text();
  		    baseApi.end();
  		    
  			Log.v(TAG, "OCRED TEXT: " + recognizedText);

  			if ( lang.equalsIgnoreCase("eng") ) {
  				recognizedText = recognizedText.replaceAll("[^a-zA-Z0-9]+", " ");
  			}

  			recognizedText = recognizedText.trim();

  			if ( recognizedText.length() != 0 ) {
  				_field.setText(_field.getText().toString().length() == 0 ? recognizedText : _field.getText() + " " + recognizedText);
//  				_field.setSelection(_field.getText().toString().length());
  			}
  		    
  		    System.out.println(recognizedText);
  		    
  		    _field.setText(recognizedText);
  		    
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    @Override 
    protected void onRestoreInstanceState( Bundle savedInstanceState){
    	Log.i( "MakeMachine", "onRestoreInstanceState()");
    	if( savedInstanceState.getBoolean( MainActivity.PHOTO_TAKEN ) ) {
    		onPhotoTaken();
    	}
    }
    
    @Override
    protected void onSaveInstanceState( Bundle outState ) {
    	outState.putBoolean( MainActivity.PHOTO_TAKEN, _taken );
    }


}
