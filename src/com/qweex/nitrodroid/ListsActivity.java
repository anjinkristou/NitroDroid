/*
Copyright (c) 2012 Qweex
Copyright (c) 2012 Jon Petraglia

This software is provided 'as-is', without any express or implied warranty. In no event will the authors be held liable for any damages arising from the use of this software.

Permission is granted to anyone to use this software for any purpose, including commercial applications, and to alter it and redistribute it freely, subject to the following restrictions:

    1. The origin of this software must not be misrepresented; you must not claim that you wrote the original software. If you use this software in a product, an acknowledgment in the product documentation would be appreciated but is not required.

    2. Altered source versions must be plainly marked as such, and must not be misrepresented as being the original software.

    3. This notice may not be removed or altered from any source distribution.
 */
package com.qweex.nitrodroid;

import java.lang.reflect.Field;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ViewFlipper;

/*	
-arabic.js
-basque.js
-bulgarian.js
*chinese.js
*dutch.js
*finnish.js
*french.js
*german.js
-hungarian.js
*italian.js
*pirate.js
*polish.js
*portuguese.js
*russian.js
*spanish.js
*turkish.js
*vietnamese.js
*/

public class ListsActivity extends Activity
{
	
	/** Static variables concerning the preferences of the program **/
	public static int themeID;
	public static boolean forcePhone;
	public static String locale = null;
	public static boolean isTablet = false;
	boolean splashEnabled = false;
	
	/** Static variables that are used across files **/
	public static String listHash;
	public static View currentList;
	public static TasksActivity ta;
	public static ViewFlipper flip;
	public static SyncHelper syncHelper;
	public static float DP;
	public static String lastList;
	
	/** Local variables **/
	private static int list_normalDrawable, list_selectedDrawable;
	private EditText newList;
	private Builder newListDialog;
	private static Context context;
	private boolean loadingApp  = true, loadingOnCreate = true;
	private View splash;
	private ListView mainListView;
	public static ListAdapter listAdapter;
	public static ImageButton syncLoading;
	
	/************************** Activity Lifecycle methods **************************/ 
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Log.d("ListsActivity::()", "Creating ListsActivity");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		//Load preferences
		String new_theme = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("theme", "Default");
		themeID = getResources().getIdentifier(new_theme, "style", getApplicationContext().getPackageName());
		forcePhone = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("force_phone", false);
		locale = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("locale", "en");
		lastList = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("last_list", "today");
		doViewStuff();
		
		//Create/set locals
		context = this;
		syncHelper = new SyncHelper(context);
		
		//Show Splash
		splash = findViewById(R.id.splash);
		Animation splashAnim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
		splashAnim.setAnimationListener(new AnimationListener()
		{
			@Override
			public void onAnimationEnd(Animation animation)
			{
				for(long i=0; splashEnabled && i<(procUtils.getCPUFreq()>0 ? procUtils.getCPUFreq()*10 : 10000l); i++);
				doCreateAsyncronously.execute();
			}
			@Override
			public void onAnimationRepeat(Animation animation) {}
			@Override
			public void onAnimationStart(Animation animation) { System.out.println("nerts1"); }
		});
		splash.startAnimation(splashAnim);
	}
		
	@Override
    public boolean onCreateOptionsMenu(Menu menu)
    { 
		if(isTablet)
		{
			Log.d("ListsActivity::onCreateOptionsMenu", "'s a tablet. Doing some shit that I forget why I did it.");
			findViewById(R.id.frame).setVisibility(View.VISIBLE);
			((android.widget.Button)findViewById(R.id.add_list)).setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v)
				{
					pressCreateList();
				}
			});
			return false;
		}
		menu.add(0, 42, 0, getResources().getString(R.string.add_list));
		return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
		pressCreateList();
		return true;
    }

	@Override
	public void onResume()
	{
		super.onResume();
		Log.d("ListsActivity::onResume", "Resuming main activity");
		
		//Usually resumes after visiting Preferences.
		//Re-read preferences
		String new_theme = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("theme", "Default");
		int new_themeID = getResources().getIdentifier(new_theme, "style", getApplicationContext().getPackageName());
		boolean new_force = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("force_phone", false);
		String new_locale = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("language", "en");
		
		//If any of them have changed, rebuild the UI
		if(ta==null || new_themeID!=themeID || new_force!=forcePhone || new_locale!=locale)
		{
			java.util.Locale derlocale = new java.util.Locale(new_locale);
			java.util.Locale.setDefault(derlocale);
			Configuration config = new Configuration();
			config.locale = derlocale;
			getBaseContext().getResources().updateConfiguration(config,
			      getBaseContext().getResources().getDisplayMetrics());
			locale = new_locale;
			themeID = new_themeID;
			forcePhone = new_force;
			doCreateStuff();
		}
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		syncHelper.db.close();
		Log.d("ListsActivity::onDestroy", "Herp");
	}
	
    @TargetApi(5)
	public void onBackPressed()
    {
    	doBackThings();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (Integer.parseInt(android.os.Build.VERSION.SDK) < 5
                && keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            doBackThings();
        }

        return super.onKeyDown(keyCode, event);
    }
	
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d("ListsActivity::onConfigurationChanged", "Herp");
    }
	
   /************************** Yoda methods **************************/
   //"Do or do not, there is no try"
   // These methods are essentially the Activity Lifecycle methods, but
   // they have been split off so that they can be called in other methods
	
	public void doViewStuff()
	{
		Log.d("ListsActivity::doViewStuff", "Doing view things");
		//Theme
		setTheme(themeID);
		
		//Force Phone
		forcePhone = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("force_phone", false);
		if(!forcePhone && isTabletDevice(this))
		{
			Log.d("ListsActivity::doViewStuff", "Setting tablet");
		    isTablet = true;
		    if(themeID==R.style.Wunderlist || themeID==R.style.Right)
		    	setContentView(R.layout.tablet_right);
		    else
		    	setContentView(R.layout.tablet);
		    findViewById(R.id.taskTitlebar).setVisibility(View.GONE);
		}
		else
		{
			Log.d("ListsActivity::doViewStuff", "Setting phone");
			isTablet = false;
			setContentView(R.layout.phone);
			findViewById(R.id.taskTitlebar).setVisibility(View.VISIBLE);
		}
		
		//If we are NOT in the middle of the splash screen, remove the splash view
		if(!loadingApp)
		{
			((ViewFlipper)findViewById(R.id.FLIP)).removeViewAt(0);
		}
	}
	
	public void doCreateStuff() { doCreateStuff(false); }
	public void doCreateStuff(boolean noSetMainView)
	{
		Log.d("ListsActivity::doCreateStuff", "Doing create things");
		if(loadingOnCreate)
			return;
		if(!noSetMainView)
			doViewStuff();
		
		flip = (ViewFlipper) findViewById(R.id.FLIP);
		mainListView = (ListView) findViewById(android.R.id.list);
		syncLoading = ((ImageButton) findViewById(R.id.sync));
		
		//Add them onClickListeners
		((android.widget.Button)findViewById(R.id.add_list)).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				newListDialog.show();
			}
		});
		((ImageButton) findViewById(R.id.settings)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent x = new Intent(ListsActivity.this, QuickPrefsActivity.class);
				x.putExtra("show_popup", false);
				startActivity(x);
			}
         });
		syncLoading.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(SyncHelper.isSyncing)
					return;
				if(syncHelper.SERVICE==null)
				{
					Intent x = new Intent(ListsActivity.this, QuickPrefsActivity.class);
					x.putExtra("show_popup", true);
					startActivity(x);
					return;
				}
				((android.graphics.drawable.AnimationDrawable) ListsActivity.syncLoading.getDrawable()).start();
				syncHelper.new performSync().execute();
			}
		});
         mainListView.setOnItemClickListener(selectList);
         
         //Create the ListAdapter & set it
         Cursor r = syncHelper.db.getAllLists();
         listAdapter = new ListAdapter(context, R.layout.list_item, r);
         listAdapter.todayCount = ListsActivity.syncHelper.db.getTodayTasks(TasksActivity.getBeginningOfDayInSeconds()).getCount();
         listAdapter.totalCount = ListsActivity.syncHelper.db.getTasksOfList(null, "order_num").getCount();
         
         if(r.getCount()<3)
         {
        	 syncHelper.readJSONtoSQL("", this);
        	 System.out.println(r.getCount());
        	 listAdapter.changeCursor(syncHelper.db.getAllLists());
         }
         mainListView.setAdapter(listAdapter);
         
         
         //Do animation things with the splash
         if(loadingApp)
         {
			Animation animation = AnimationUtils.loadAnimation(ListsActivity.this, android.R.anim.fade_out);
			animation.setAnimationListener(new AnimationListener(){
				@Override
				public void onAnimationEnd(Animation animation)
				{
					//TODO: WHAT THE FUCK IS THIS SHIT 
					flip.setInAnimation(context, R.anim.slide_in_right);
					doCreateThingsHandler.sendEmptyMessage(1);
				}
				@Override
				public void onAnimationRepeat(Animation animation) {}
				@Override
				public void onAnimationStart(Animation animation) {}
			});
			splash.startAnimation(animation);
         }
         
         loadingApp = false;
	}
	
    void doBackThings()
    {
    	if(ta==null)
    		finish();
    	else
    	{
    		if(!ta.doBackThings())
    			return;
    		
    		if(isTablet)
    			finish();
    		else
    		{
	            flip.setInAnimation(this, android.R.anim.slide_in_left);
	            flip.setOutAnimation(this, android.R.anim.slide_out_right);
	            ta = null;
	    		flip.showPrevious();
    		}
    	}
    }
	
    AsyncTask<Void, Void, Void> doCreateAsyncronously = new AsyncTask<Void, Void, Void>()
	{

		@Override
	    protected Void doInBackground(Void... params) {
			Log.d("ListsActivity::doCreateThingsAsyncronously", "Launching asyncronously");
			syncHelper.db.open();
			DP = context.getResources().getDisplayMetrics().density;

			//Get the drawables for normal & selected according to the theme 
			TypedArray a;
			a = context.getTheme().obtainStyledAttributes(ListsActivity.themeID, new int[] {R.attr.lists_selector});     
        	list_normalDrawable = a.getResourceId(0, 0);
	        a = context.getTheme().obtainStyledAttributes(ListsActivity.themeID, new int[] {R.attr.lists_selected});
        	list_selectedDrawable = a.getResourceId(0, 0);
	        
        	//Create the "New List" dialog
	        newList = new EditText(context);
	        newList.setId(42);
	        newListDialog = new AlertDialog.Builder(context)
	        	.setTitle(R.string.add_list)
	        	.setView(newList)
	        	.setPositiveButton(android.R.string.ok, createList).setNegativeButton(android.R.string.cancel, null);
	        
	        //Done creating
	        loadingOnCreate = false;
	        Log.d("ListsActivity::doCreateThingsAsyncronously", "Done with the async stuff");
	        doCreateThingsHandler.sendEmptyMessage(0);
	        return null;
        } 
   };
   
   Handler doCreateThingsHandler = new Handler()
   {
 	   @Override
 		public void handleMessage(android.os.Message msg) 
 		{
 		   if(msg.what==0)
 			   doCreateStuff(true);
 		   else
 		   {
 			  flip.removeView(splash);
 		   }
 		}
    };;
    
    
    /************************** Misc methods **************************/
   
	void pressCreateList()
	{
		newListDialog.show();
	}
	
	DialogInterface.OnClickListener createList = new DialogInterface.OnClickListener()
	{
        public void onClick(DialogInterface dialog, int whichButton) {
            String newListName = newList.getText().toString();
            if("".equals(newListName))
            	return;
            String new_id = SyncHelper.getID();
            Log.d("ListsActivity::createList", "Creating a List " + newListName + " [" + new_id + "]");
            syncHelper.db.insertList(new_id, newListName, null);
            syncHelper.db.insertListTimes(new_id, (new java.util.Date()).getTime(), 0);
            
            listAdapter.changeCursor(syncHelper.db.getAllLists());
        }
    };
    
	
	static OnItemClickListener selectList = new OnItemClickListener() 
    {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id)
      {
    	  if(SyncHelper.isSyncing)
    		  return;
    	  Log.d("ListsActivity::selectList", "List Selected");
    	  
    	  //Get info
    	  String hash, name;
		  name=(String) ((TextView)view.findViewById(R.id.listName)).getText();
		  hash=(String)((TextView)view.findViewById(R.id.listId)).getText();
		  Log.d("ListsActivity::selectList", "List Selected is: " + name);
		  
		  //parent==null signifies that it is programatically selected so no need to update.
		  if(parent!=null)
		  {
			  Log.d("ListsActivity::selectList", "Updating LastList to " + hash);
    		  Editor e = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).edit();
        	  e.putString("last_list", hash);
        	  e.commit();
		  }
    	  
		  //Set background of last selected to normal & the current to selected
    	  if(isTablet && currentList!=null)
    		  currentList.setBackgroundResource(list_normalDrawable);
    	  currentList = view;
    	  if(isTablet)
    		  currentList.setBackgroundResource(list_selectedDrawable);
    	  
    	  //Yay update shit
    	  TasksActivity.lastClicked = null;
		  TasksActivity.lastClickedID = null;
    	  if(ta==null)
    	  {
    		  Log.d("ListsActivity::selectList", "Instanciating TaskActivity");
    		  ta = new TasksActivity();
    		  ta.context = (Activity) view.getContext();
    		  ta.listHash = hash;
    		  ta.listName = name;
    		  ta.onCreate(null);	//I think I might get away with calling "doCreateThings" or even "sillygoose" but whatevs
    	  }else
    	  {
    		  Log.d("ListsActivity::selectList", "Updating TaskActivity");
    		  ta.listHash = hash;
    		  ta.listName = name;
    		  ((Activity) context).findViewById(R.id.tasksListView).post(new Runnable(){
    			 public void run()
    			 {
    				 ta.createTheAdapterYouSillyGoose();
    			 }
    		  });
    	  }
    		  
    	  //Show the animation & flip the flipper if it is a phone
    	  if(!isTablet)
          {
    		  Log.d("ListsActivity::selectList", "Flipping to that TaskActivity");
    		  flip.setInAnimation(view.getContext(), R.anim.slide_in_right);
    		  flip.setOutAnimation(view.getContext(), R.anim.slide_out_left);
    		  flip.showNext();
          }
      }
    };
    
   
    
    
    /************************** Utility methods **************************/
    
	//http://stackoverflow.com/a/9624844/1526210
	@TargetApi(4)
	public static boolean isTabletDevice(android.content.Context activityContext) {
	    // Verifies if the Generalized Size of the device is XLARGE to be
	    // considered a Tablet
	    boolean xlarge = ((activityContext.getResources().getConfiguration().screenLayout & 
	                        Configuration.SCREENLAYOUT_SIZE_MASK) >=	//Changed this from == to >= because my tablet was returning 8 instead of 4. 
	                        Configuration.SCREENLAYOUT_SIZE_LARGE);
	    
	    
	    // If XLarge, checks if the Generalized Density is at least MDPI (160dpi)
	    if (xlarge) {
	        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
	        Activity activity = (Activity) activityContext;
	        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
	        
	        //This next block lets us get constants that are not available in lower APIs.
	        // If they aren't available, it's safe to assume that the device is not a tablet.
	        // If you have a tablet or TV running Android 1.5, what the fuck is wrong with you.
	        int xhigh = -1, tv = -1;
	        try {
	        	Field f = android.util.DisplayMetrics.class.getDeclaredField("DENSITY_XHIGH");
	        	xhigh = (Integer) f.get(null);
	        	f = android.util.DisplayMetrics.class.getDeclaredField("DENSITY_TV");
	        	xhigh = (Integer) f.get(null);
	        }catch(Exception e){}
	        
	        // MDPI=160, DEFAULT=160, DENSITY_HIGH=240, DENSITY_MEDIUM=160, DENSITY_TV=213, DENSITY_XHIGH=320
	        if (metrics.densityDpi == android.util.DisplayMetrics.DENSITY_DEFAULT
	                || metrics.densityDpi == android.util.DisplayMetrics.DENSITY_HIGH
	                || metrics.densityDpi == android.util.DisplayMetrics.DENSITY_MEDIUM
	                || metrics.densityDpi == tv 
	                || metrics.densityDpi == xhigh
	                ) {

	            return true;
	        }
	    }
	    return false;
	}
    
}