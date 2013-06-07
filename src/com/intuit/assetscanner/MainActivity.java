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
	
//	protected Button _button;
	public static Button _button;
	public static ImageView _image;
	public static TextView _field;
	public static TextView _field1;
	
	public static String _dir;
	public static String _path;
	public static int _filename_cnt;
	public boolean _taken;
	public static String recognizedText = "Initial value";
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
        _field1 = ( TextView ) findViewById( R.id.field1 );
        _field.setText("OCR Results will show here");
        _button = ( Button ) findViewById( R.id.button );
        _button.setOnClickListener( new ButtonClickHandler() );
        
        _path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/pic" + _filename_cnt;
        
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

				Log.v(TAG, "Copied " + lang + " traineddata to " + myLocation );
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
    		_field1.setText("ButtonClickHandler");
    	}
    }
    
    protected void startCameraActivity()
    {
        _filename_cnt++;
    	_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/pic" + _filename_cnt;
    	Log.i("MakeMachine", "startCameraActivity()" );
    	File file = new File( _path );
    	Uri outputFileUri = Uri.fromFile( file );
    	
    	Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE );
    	intent.putExtra( MediaStore.EXTRA_OUTPUT, outputFileUri );
    	
    	startActivityForResult( intent, 0 );
        _field1.setText("startCameraActivity");
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
        _field1.setText("onActivityResult");
    }
    
    protected void onPhotoTaken()
    {

    	Log.i( "MakeMachine", "onPhotoTaken" );
    	
    	_taken = true;
    	
    	BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
    	
    	Bitmap bitmap = BitmapFactory.decodeFile( _path, options );
    	
    	_image.setImageBitmap(bitmap);
    	
//    	_field.setVisibility( View.GONE );
    	
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

  			if ( lang.equalsIgnoreCase("eng") ) {
  				recognizedText = recognizedText.replaceAll("[^a-zA-Z0-9]+", " ");
  			}

  			Log.v(TAG, "OCRED TEXT: " + recognizedText);
  			
  			recognizedText = recognizedText.trim();

  			if ( recognizedText.length() != 0 ) {
  				_field.setText(recognizedText);
// 				_field.setSelection(_field.getText().toString().length());
  			}
  		      		      		    
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
  		_field1.setText("onPhotoTaken");
    }
    
    @Override 
    protected void onRestoreInstanceState( Bundle savedInstanceState){
    	Log.i( "MakeMachine", "onRestoreInstanceState()");
    	if( savedInstanceState.getBoolean( MainActivity.PHOTO_TAKEN ) ) {
    		onPhotoTaken();
    	}
    	_field1.setText("onRestoreInstanceState");
    }
    
    @Override
    protected void onSaveInstanceState( Bundle outState ) {
    	outState.putBoolean( MainActivity.PHOTO_TAKEN, _taken );
    	_field1.setText("onSaveInstanceState");
    }


}
