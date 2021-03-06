package com.example.driveviewer;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import android.os.Bundle;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ListActivity {

	static final int CHOOSE_ACCOUNT = 0;
	static final int REQUEST_ACCOUNT_PICKER = 1;
	static final int REQUEST_AUTHORIZATION = 2;
	static final int DOWNLOAD_FILES = 3;
	static final int SPEED_ANIMATION_TRANSITION = 5;
	static final String REFERENCE = "com.example.driveviewer.DOWNLOAD_URL";

	private static Drive service;
	private GoogleAccountCredential credential;

	private ProgressDialog m_ProgressDialog = null; 
	private ArrayList<FileDisplay> m_files = null;
	private FileManager m_adapter;
	private Runnable viewOrders;
	private TextView debug;
	private ListView lv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		showToast("Activity Started!");
		m_files = new ArrayList<FileDisplay>();
		this.m_adapter = new FileManager(this, R.layout.row, m_files, service);
		setListAdapter(this.m_adapter);
		debug = (TextView) findViewById(R.id.empty);
		debug.setText("");
		
		lv = getListView();
		lv.setClickable(true);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> av, View v, int pos, long id) {
				showToast("yay a click! item #" + pos + " clicked.");
			}
		});
		
		credential = GoogleAccountCredential.usingOAuth2(this, DriveScopes.DRIVE);
		startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);

		//setListenersForList();
	}
	
	private void setListenersForList() {
		lv = getListView();
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
		    int pos;
			@Override
		    public void onItemClick(AdapterView<?> av, View v, int pos, long id) {
		        this.pos = pos;
		        View viewToExpand = av.getChildAt(pos);
		        showToast("yay a click!");
				expandView(viewToExpand);
		    }
			
			private void expandView(View v){
				//find the containing relative layout of a file
				RelativeLayout fileListing= (RelativeLayout) v.findViewById(R.id.filerow);
				//get settings for the layout of file
				ViewGroup.LayoutParams settings = fileListing.getLayoutParams();
				//set the width to the right size
				settings.width = 86;
				fileListing.setLayoutParams(new RelativeLayout.LayoutParams(settings));
				
				LayoutInflater buttonLayout = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View options = buttonLayout.inflate(R.layout.buttonbar, null);
				
				Button open = (Button) options.findViewById(R.id.open);
				Button delete = (Button) options.findViewById(R.id.delete);
				Button rename = (Button) options.findViewById(R.id.rename);
				
				open.setOnClickListener(new View.OnClickListener() {            
			        public void onClick(View view) {
			        	showToast("Open!");
			        	showToast("Guess it's not working yet...");
			        	//TODO: enable open button
			        	//m_adapter.showFile(this.pos);
			        }
			     });
				delete.setOnClickListener(new View.OnClickListener() {            
			        public void onClick(View view) {
			        	showToast("Delete!");
			        	showToast("Guess it's not working yet...");
			        	//TODO: enable delete button
			        }
			     });
				rename.setOnClickListener(new View.OnClickListener() {            
			        public void onClick(View view) {
			        	showToast("Rename!");
			        	showToast("Guess it's not working yet...");
			        	//TODO: enable rename button
			        }
			     });
				fileListing.addView(options);
				// TODO: modify view size to show/hide buttons
			}
		});
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		reDrawTheScreen();
		switch(requestCode){
			case REQUEST_ACCOUNT_PICKER:
				if (resultCode == RESULT_OK && data != null) {
					String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
					if (accountName != null) {
						credential.setSelectedAccountName(accountName);
						service = getDriveService(credential);
						startFileGet();
					}
				} else {
					showToast("Error: Login failed");
					showToast("Please restart the app");
				}
				break;
			case REQUEST_AUTHORIZATION:
				if (resultCode == Activity.RESULT_OK) {
			        startFileGet();
			      } else {
			        startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
			      }
				break;
			default:
				break;
		}
	}
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	  super.onConfigurationChanged(newConfig);
	  setContentView(R.layout.activity_main);
	}
	private void reDrawTheScreen(){
		m_files = new ArrayList<FileDisplay>();
		this.m_adapter = new FileManager(this, R.layout.row, m_files, service);
		setListAdapter(this.m_adapter);
		m_adapter.notifyDataSetChanged();
	}
    /**
     * Event Handling for Individual menu item selected
     * Identify single menu item by it's id
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_logout){
        	reDrawTheScreen();
        	logout();
        	return true;
        } else {
        	return super.onOptionsItemSelected(item);
        }
    }
    private void logout(){
		startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }
	private void startFileGet(){
		viewOrders = new Runnable(){			
			@Override
			public void run() {				
				try {
					updateData();
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		Thread thread =  new Thread(null, viewOrders, "MagentoBackground");
		thread.start();
		m_ProgressDialog = ProgressDialog.show(MainActivity.this,    
				"Please wait...", "Retrieving data ...", true);
	}
	private void updateData(){
		ArrayList<FileDisplay> files;
		//getFileList from google and add files to array
		List<File> fileList;
		try {
			files = new ArrayList<FileDisplay>();
			fileList = retrieveAllFiles();
			if(fileList != null) { //if returned files not null
				showToast("Number of files in list: " + fileList.size());
				for(File f:fileList){
					//wrap files in file wrapper
					files.add(new FileDisplay(f));
				}
			}
			m_files = files;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/* for updates
		Thread timer = new Thread() {
		    public void run () {
		        for (;;) {
		            // do stuff in a separate thread
		            updateFiles();
		            uiCallback.sendEmptyMessage(0);		            
		            Thread.sleep(3000);    // sleep for 3 seconds
		        }
		    }
		}
		timer.start();*/
		runOnUiThread(returnRes);
	}
	private Drive getDriveService(GoogleAccountCredential credential) {
		return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
		.build();
	}
	public void showToast(final String toast) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
			}
		});
	}
	private Runnable returnRes = new Runnable() {
		@Override
		public void run() {
			if(m_files != null && m_files.size() > 0){
				m_adapter.notifyDataSetChanged();
				for(int i=0;i<m_files.size();i++)
					m_adapter.add(m_files.get(i));
			}
			m_ProgressDialog.dismiss();
			m_adapter.notifyDataSetChanged();
		}
	};
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	/**
	 * Retrieve a list of File resources. (Google Code)
	 *
	 * @param service Drive API service instance.
	 * @return List of File resources.
	 */
	private List<File> retrieveAllFiles() throws IOException {
		List<File> result = new ArrayList<File>();
		Files.List request = service.files().list();
		do {
			try {
				FileList files = request.execute();
				
				result.addAll(files.getItems());
				request.setPageToken(files.getNextPageToken());
			}  catch (UserRecoverableAuthIOException e) {
		          startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
		    } 
			catch (IOException e) {
				showToast("e: " + e);
				System.out.println("An error occurred: " + e);
				request.setPageToken(null);
			}
		} while (request.getPageToken() != null &&
				request.getPageToken().length() > 0);
		return result;
	}
	
	public class FileDisplay{		
		private String id; //(String, used internally, hide from user)
		private File fileLink;
		private String title; //(String, name of file)
		private long fileSize; //size of the file
		private String downloadUrl; //(String, to download file #viewingfiles)
		private DateTime lastViewedByMe; //(DateTime, to allow sorting)
		private boolean viewedYet;
		private Bitmap image;

		public FileDisplay(File file){
			this.fileLink = file;
			this.id = file.getId();
			this.title = file.getTitle();
			this.lastViewedByMe = (file.getLastViewedByMeDate() != null) ? file.getLastViewedByMeDate() : file.getCreatedDate();
			this.viewedYet = file.getLastViewedByMeDate() != null;
			this.fileSize = file.getQuotaBytesUsed().longValue(); 
			this.downloadUrl = file.getSelfLink();
			try {
				this.image = BitmapFactory.decodeStream((InputStream) new URL(file.getIconLink()).getContent());
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public String getId() {
			return id;
		}
		public long getFileSize() {
			updateFileSize();
			return fileSize;
		}
		private void updateFileSize(){
			long newSize = fileLink.getQuotaBytesUsed().longValue();
			if(fileSize != newSize) {
				fileSize = newSize;
			}
		}
		public String getTitle() {
			updateTitle();
			return title;
		}
		private void updateTitle(){
			String newTitle = fileLink.getTitle();
			if(title.compareTo(newTitle) != 0) {
				title = newTitle;
			}
		}
		public String getDownloadUrl() {
			//updateDownloadUrl();
			return downloadUrl;
		}
		private void updateDownloadUrl(){
			String newUrl = fileLink.getExportLinks().get("application/pdf");
			if(downloadUrl.compareTo(newUrl) != 0) {
				downloadUrl = newUrl;			
			}
		}
		public DateTime getLastViewedByMe() {
			updateLastViewedBy();
			return lastViewedByMe;
		}
		public void updateLastViewedBy() {
			Date currentDate = new Date();
			fileLink.setLastViewedByMeDate(new DateTime(currentDate)); //DateTime
		}		
		public Bitmap getImage(){
			return image;
		}
		/*
		private Bitmap determineThumbnail(String mimeType){
			if(mimeType.equalsIgnoreCase("application/vnd.google-apps.document")){
				return BitmapFactory.decodeResource(getResources(), R.drawable.doc);
			}
			if(mimeType.equalsIgnoreCase("application/vnd.google-apps.presentation")){
				return BitmapFactory.decodeResource(getResources(), R.drawable.presentation);
			}
			if(mimeType.equalsIgnoreCase("application/vnd.google-apps.spreadsheet")){
				return BitmapFactory.decodeResource(getResources(), R.drawable.spread);
			}
			if(mimeType.equalsIgnoreCase("application/vnd.google-apps.folder")){
				return BitmapFactory.decodeResource(getResources(), R.drawable.folder);
			}
			if(mimeType.equalsIgnoreCase("application/vnd.google-apps.drawing") ||
			   mimeType.equalsIgnoreCase("application/vnd.google-apps.photo")	||
			   mimeType.equalsIgnoreCase("application/vnd.google-apps.image") ){
				return BitmapFactory.decodeResource(getResources(), R.drawable.image);
			}
			return BitmapFactory.decodeResource(getResources(), R.drawable.other);
		}*/
		public boolean isViewedYet() {
			return viewedYet;
		}
		public void setViewedYet(boolean viewedYet) {
			this.viewedYet = viewedYet;
		}
		
		
	}
	private class FileManager extends ArrayAdapter<FileDisplay>{
		private ArrayList<FileDisplay> files;
		//private Drive service;

		public FileManager(Context context, int textViewResourceId, ArrayList<FileDisplay> files, Drive service) {
			super(context, textViewResourceId, files);
			this.files = files;
			//this.service = service;
		}
		public void showFile(int pos) {
			FileDisplay f = files.get(pos);
			String downloadURL = f.getId();
			Intent intent = new Intent(MainActivity.this, PDFViewer.class);
			intent.putExtra(REFERENCE, downloadURL);
			//showToast(downloadURL);
			showToast(f.getTitle());
			//startActivity(intent);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			//data
			Format format = new SimpleDateFormat("yy.MM.dd", Locale.US);
			FileDisplay f = files.get(position);

			Date dateData = new Date(f.getLastViewedByMe().getValue());
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.row, null);
			}
			//graphical elements
			TextView title = (TextView) v.findViewById(R.id.toptext);
			TextView fileSize = (TextView) v.findViewById(R.id.bottomtext);
			TextView date = (TextView) v.findViewById(R.id.date);
			ImageView icon = (ImageView) v.findViewById(R.id.icon);
			//start filling graphical elements according to data
			if (title != null) {
				title.setText(f.getTitle());           
			}
			if(fileSize != null){
				Long l = (Long) f.getFileSize();
				fileSize.setText(fileSizeFormat(l)); //probably have to apply formatting here
			}
			if(date != null) {
				date.setText("Last Opened: " + format.format(dateData));
			}
			if(icon != null) {
				icon.setImageBitmap(f.getImage());
			}
			
			v.setOnClickListener(new OnClickListener() {
				boolean notExpanded = true;
				boolean heightSet = false;
				DisplayMetrics metrics = new DisplayMetrics();
				int defaultHeight;
				View options;
				@Override
				public void onClick(View v) {
					if(!heightSet) {
						defaultHeight = v.findViewById(R.id.filerow).getHeight();
						options = setupOptions();
						heightSet = true;
					}
					// TODO Auto-generated method stub
					if(notExpanded) {
						expandView(v);
						notExpanded = false;
					} else {
						collapseView(v);
						notExpanded = true;
					}
				}
				private View setupOptions(){
					LayoutInflater buttonLayout = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					View optionsInit = buttonLayout.inflate(R.layout.buttonbar, null);
					
					Button open = (Button) optionsInit.findViewById(R.id.open);
					Button delete = (Button) optionsInit.findViewById(R.id.delete);
					Button rename = (Button) optionsInit.findViewById(R.id.rename);
					
					open.setOnClickListener(new View.OnClickListener() {            
				        public void onClick(View view) {
				        	showToast("Open!");
				        	showToast("Guess it's not working yet...");
				        	//TODO: enable open button
				        	//m_adapter.showFile(this.pos);
				        }
				     });
					delete.setOnClickListener(new View.OnClickListener() {            
				        public void onClick(View view) {
				        	showToast("Delete!");
				        	showToast("Guess it's not working yet...");
				        	//TODO: enable delete button
				        }
				     });
					rename.setOnClickListener(new View.OnClickListener() {            
				        public void onClick(View view) {
				        	showToast("Rename!");
				        	showToast("Guess it's not working yet...");
				        	//TODO: enable rename button
				        }
				     });
					//TODO: re-arrange options bar
					return optionsInit;
				}
				private void expandView(View v){
					//find the containing relative layout of a file
					RelativeLayout fileListing= (RelativeLayout) v.findViewById(R.id.filerow);
					//get settings for the layout of file
					ViewGroup.LayoutParams settings = fileListing.getLayoutParams();
					//set the width to the right size
					showToast("layout height: " + settings.height);
					metrics = new DisplayMetrics();
					getWindowManager().getDefaultDisplay().getMetrics(metrics);
					settings.height = defaultHeight + getDPI(30,metrics);
					//apply the change
					fileListing.setLayoutParams(new AbsListView.LayoutParams(settings));
					fileListing.addView(options);
					fileListing.getChildCount();
				}
				protected void collapseView(View v) {
					//find the containing relative layout of a file
					RelativeLayout fileListing= (RelativeLayout) v.findViewById(R.id.filerow);
					//get settings for the layout of file
					ViewGroup.LayoutParams settings = fileListing.getLayoutParams();
					//set the width to the right size
					View optionsBar = fileListing.findViewById(R.id.options); 
					metrics = new DisplayMetrics();
					getWindowManager().getDefaultDisplay().getMetrics(metrics);
					settings.height = defaultHeight - getDPI(30,metrics);
					//apply the change
					fileListing.setLayoutParams(new AbsListView.LayoutParams(settings));
					
					
					fileListing.removeView(optionsBar);
					//TODO: hide buttons on collapse
				}
			});
			return v;
		}
		
		public int getDPI(int size, DisplayMetrics metrics){
		     return (size * metrics.densityDpi) / DisplayMetrics.DENSITY_DEFAULT;        
		 }

		private String fileSizeFormat(long size) {
		    if(size <= 0) return "0 B";
		    final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
		    int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
		    return new DecimalFormat("#,##0.##").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
		}
	}
}
