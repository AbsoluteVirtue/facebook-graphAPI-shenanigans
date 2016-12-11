package org.example.lab_2;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.NewPermissionsRequest;
import com.facebook.SessionState;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.facebook.widget.ProfilePictureView;

public class MainActivity extends Activity {
	private static final int CAPTURE_IMAGE_REQUEST_CODE = 1;
	private static final List<String> PERMISSIONS = Arrays.asList("publish_actions", "manage_notifications");
	private ProfilePictureView profilePictureView;
	private Button postImageButton;
	private String mDisplayFolder;
	private File mPhotoFile;
	private Uri mPhotoFileUri;//String?
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		final Session.OpenRequest request = new Session.OpenRequest(this);
		request.setPermissions(Arrays.asList("email", "user_birthday"));
		request.setCallback(new Session.StatusCallback()
		{
		    @Override
		    public void call(final Session session, SessionState state, Exception exception)
		    {
		        if (session.isOpened())
		        {
		        	// make request to the /me API
		    		Request.newMeRequest(session, new Request.GraphUserCallback() {
		    		  // callback after Graph API response with user object
		    		  @Override
		    		  public void onCompleted(GraphUser user, Response response) {
		    			  if (user != null) {
		    				  
		    				  TextView welcome = (TextView) findViewById(R.id.welcome);
		    				  welcome.setText(user.getName() + "");
		    				  profilePictureView = (ProfilePictureView) findViewById(R.id.profilePicture);
		    				  profilePictureView.setProfileId(user.getId());
		    				  TextView birthDate = (TextView) findViewById(R.id.birthDate);
		    				  birthDate.setText(user.getBirthday());
		    				  
		    				  postImageButton = (Button) findViewById(R.id.postImage);
		    				  postImageButton.setOnClickListener(new OnClickListener() {
		    						@Override
		    						public void onClick(View view) {
		    							if (session.getPermissions().contains("publish_actions")){
		    								makePhoto();
		    								//postPhoto(mPhotoFile);
		    								//postImg();
		    							} else {
		    								session.requestNewPublishPermissions(new NewPermissionsRequest(MainActivity.this, PERMISSIONS));
		    								makePhoto();
		    								//postPhoto(mPhotoFile);
		    								//postImg();
		    							}
		    						}
		    					});
		    			  }
		    		  }
		    		}).executeAsync();
		    		/*
		    		new Request(session, "/me/notifications", null, HttpMethod.GET, new Request.Callback() {
						@Override
						public void onCompleted(Response response) {	
							GraphObject object = response.getGraphObject();
							if (object != null) {
								Toast.makeText(getApplicationContext(), object.getProperty("data").toString(), Toast.LENGTH_LONG).show();
							} else {
								Toast.makeText(getApplicationContext(), "No notifications", Toast.LENGTH_LONG).show();
							}
						}
					}).executeAsync();*/
		        }
		    }
		});
		Session fbSession = Session.getActiveSession();
		if (fbSession == null || fbSession.isClosed())
		{
		    fbSession = new Session(this);
		    Session.setActiveSession(fbSession);
		}
		fbSession.openForRead(request);

	}

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
		switch (item.getItemId()) {
		case R.id.action_settings:
			break;
		case R.id.action_menu_logout:
			break;
		default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
		if (requestCode == CAPTURE_IMAGE_REQUEST_CODE){
			if (resultCode == RESULT_OK){
				Toast.makeText(this, "Image saved to:\n" + mDisplayFolder, Toast.LENGTH_LONG).show();
				Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
				mediaScanIntent.setData(mPhotoFileUri);
				this.sendBroadcast(mediaScanIntent);
				postPhoto(mPhotoFile);
			} else if (resultCode == RESULT_CANCELED){
				
			} else {
				Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
			}
		}
	}
	
	private void makePhoto(){
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FBDir");
		if (! mediaStorageDir.exists()){
			mediaStorageDir.mkdirs();
		}
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
		mDisplayFolder = "Pictures" + File.separator + "FBDir" + File.separator + "IMG_" + timeStamp + ".jpg";
		mPhotoFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
		mPhotoFileUri = Uri.fromFile(mPhotoFile);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoFileUri);
		startActivityForResult(intent, CAPTURE_IMAGE_REQUEST_CODE);
	}
	
	private void postPhoto(File photoFile) {
		Request request;
		try {
			request = Request.newUploadPhotoRequest(Session.getActiveSession(), photoFile, 
					new Request.Callback() {
				@Override
				public void onCompleted(Response response) {
					String resultMessage = null;
					if (response.getError() != null) {
						resultMessage = response.getError().getErrorMessage();
					} else {
						resultMessage = "Success";
					}
					new AlertDialog.Builder(MainActivity.this).setMessage(resultMessage).show();
				}
			});
			request.executeAsync();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private void postImg(){
			Bitmap img = BitmapFactory.decodeResource(getResources(),
					R.drawable.ic_launcher);
			Request uploadRequest = Request.newUploadPhotoRequest(
					Session.getActiveSession(), img, new Request.Callback() {
						@Override
						public void onCompleted(Response response) {
							if (response.getError() != null) {
								Toast.makeText(MainActivity.this,
										response.getError().getErrorMessage(),
										Toast.LENGTH_LONG).show();
							} else {
								Toast.makeText(MainActivity.this,
									"Photo uploaded successfully",
									Toast.LENGTH_LONG).show();
							}
						}
					});
			uploadRequest.executeAsync();
	}
}
