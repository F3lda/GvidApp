package cz.gvid.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

@SuppressWarnings({"ConstantConditions", "StringConcatenationInLoop"})
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    static int DrawerMenuSelectedItem = R.id.nav_aktuality;
    static boolean DrawerItemClicked = false;
    static Context appContext;
    Fragment currentFragment;
    static FragmentReloader fragmentReloader;

    static boolean aktualityUpdating = false;
    static boolean suplovaniUpdating = false;
    static boolean znamkyUpdating = false;
    static boolean obedyUpdating = false;

    static int oldConfigInt;
    static int obedyPagePosition = 0;

    //static public int colorDefaultBackground;
    /*public Context getAppContext() {
        return appContext;
    }*/
    //System.out.println("Out");
    //comment-todo - ONCREATE

    static OkHttpClient okHttpClient = null;
    static HttpClient callClient;

    private static final int PERMISSION_ID_READ_WRITE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        appContext = this;
        /*TypedArray arrayStyles = getTheme().obtainStyledAttributes(new int[] {android.R.attr.colorBackground});
        colorDefaultBackground = arrayStyles.getColor(0, 0xFF00FF); //(some View).setBackgroundColor(MainActivity.colorDefaultBackground);
        arrayStyles.recycle();*/

        final NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        if (savedInstanceState == null) {
            DrawerMenuSelectedItem = R.id.nav_aktuality;

            getFragmentManager().beginTransaction().replace(R.id.content_frame, new FragmentAktuality()).commit();

            String permission = "android.permission.WRITE_EXTERNAL_STORAGE";
            int res = checkCallingOrSelfPermission(permission);
            permission = "android.permission.READ_EXTERNAL_STORAGE";
            int res2 = checkCallingOrSelfPermission(permission);
            if(res != PackageManager.PERMISSION_GRANTED || res2 != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_ID_READ_WRITE);
                }
            }
        }
        navigationView.setCheckedItem(DrawerMenuSelectedItem);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        drawer.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {}
            @Override public void onDrawerStateChanged(int newState) {}

            @Override public void onDrawerOpened(@NonNull View drawerView) {
                DrawerItemClicked = false;
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                if (DrawerItemClicked){
                    DrawerItemClicked = false;

                    int id = DrawerMenuSelectedItem;
                    Fragment newFragment = null;
                    if (id == R.id.nav_aktuality) {
                        newFragment = new FragmentAktuality();
                    }else if (id == R.id.nav_suplovani) {
                        newFragment = new FragmentSuplovani();
                    }else if (id == R.id.nav_znamky) {
                        newFragment = new FragmentZnamky();
                    }else if (id == R.id.nav_obedy) {
                        newFragment = new FragmentObedy();
                    }

                    if (newFragment != null) {
                        getFragmentManager().beginTransaction().replace(R.id.content_frame, newFragment).commit();
                    } else {
                        navigationView.setCheckedItem(DrawerMenuSelectedItem);
                    }
                } else {
                    navigationView.setCheckedItem(DrawerMenuSelectedItem);
                }
            }
        });
        toggle.syncState();

        FloatingActionButton floatingActionButton = findViewById(R.id.floating_button);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickFloatingActionButton(view);
            }
        });

        fragmentReloader = new FragmentReloader();
        fragmentReloader.setOnNeedReloadFragmentListener(new OnNeedReloadFragmentListener() {
            @Override
            public void onReloadFragment(String reloadFragment) {
                try {
                    navigationView.setCheckedItem(DrawerMenuSelectedItem);

                    currentFragment = getFragmentManager().findFragmentById(R.id.content_frame);
                    if(reloadFragment.equals("current") || currentFragment.toString().contains(reloadFragment)) {
                        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                        if (Build.VERSION.SDK_INT >= 26) {
                            fragmentTransaction.setReorderingAllowed(false);
                        }
                        fragmentTransaction.detach(currentFragment).attach(currentFragment).commit();
                    }
                } catch (IllegalStateException ignored) {
                    // There's no way to avoid getting this if saveInstanceState has already been called.
                }
            }
        });

        reCreateView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if(!(grantResults.length > 0)) {
            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.string_no_permission), Toast.LENGTH_LONG).show();
        } else if(requestCode == PERMISSION_ID_READ_WRITE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.string_permission_ok), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.string_no_permission), Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        boolean closeDrawer = true;
        Toolbar toolbar = findViewById(R.id.toolbar);

        if (id == R.id.nav_aktuality) {
            toolbar.setTitle("Aktuality");
            setScreenOrientationLock();
        }else if (id == R.id.nav_suplovani) {
            toolbar.setTitle("Suplování");
            setScreenOrientationLock();
        }else if (id == R.id.nav_znamky) {
            toolbar.setTitle("Známky");
            setScreenOrientationLock();
        }else if (id == R.id.nav_obedy) {
            toolbar.setTitle("Obědy");
            setScreenOrientationLock();
            obedyPagePosition = 0;
        } else if (id == R.id.nav_nastaveni) {
            closeDrawer = false;
            Intent startActivityIntent = new Intent(this, SettingsActivity.class);
            startActivity(startActivityIntent);
        }else if (id == R.id.nav_aktualizace) {
            closeDrawer = false;
            onClickUpdateAll();
        } else if (id == R.id.nav_web) {
            closeDrawer = false;
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.gvid.cz"));
                startActivity(browserIntent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(appContext, R.string.error_any_webbrowser, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        } else if (id == R.id.nav_facebook) {
            closeDrawer = false;
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.facebook.com/gvidenska"));
                startActivity(browserIntent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(appContext, R.string.error_any_webbrowser, Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }

        if (closeDrawer) {
            FloatingActionButton floatingActionButton = findViewById(R.id.floating_button);
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            if(!sharedPref.getBoolean("floating_button_update", true)) {
                floatingActionButton.hide();
            }else{
                floatingActionButton.show();
            }

            Fragment removeFragment = getFragmentManager().findFragmentById(R.id.content_frame);
            if(removeFragment != null) {
                getFragmentManager().beginTransaction().remove(removeFragment).commit();
            }
            findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);

            DrawerItemClicked = true;
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            DrawerMenuSelectedItem = id;
        }
        return true;
    }

    public void reCreateView() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if(!(DrawerMenuSelectedItem == R.id.nav_obedy && obedyPagePosition == 1)) {
            FloatingActionButton floatingActionButton = findViewById(R.id.floating_button);
            if (!sharedPref.getBoolean("floating_button_update", true)) {
                floatingActionButton.hide();
            } else {
                floatingActionButton.show();
            }
        }

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setCheckedItem(DrawerMenuSelectedItem);
        Menu navigationViewMenu = navigationView.getMenu();
        if(!sharedPref.getBoolean("update_all_button", true)) {
            navigationViewMenu.findItem(R.id.nav_aktualizace).setVisible(false);
        }else{
            navigationViewMenu.findItem(R.id.nav_aktualizace).setVisible(true);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        reCreateView();

        if (!((oldConfigInt & ActivityInfo.CONFIG_ORIENTATION) == ActivityInfo.CONFIG_ORIENTATION)) {
            // not rotated
            fragmentReloader.reloadFragment("current");
        } // else
            // rotated
    }

    @Override
    protected void onPause() {
        super.onPause();
        oldConfigInt = getChangingConfigurations();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isFinishing()) {
            if (SettingsActivity.SettingsPreferenceFragment.okHttpClient != null) {
                SettingsActivity.SettingsPreferenceFragment.okHttpClient.dispatcher().cancelAll();
            }
            if (okHttpClient != null) {
                okHttpClient.dispatcher().cancelAll();
            }
        }
    }

    //comment-todo - FRAGMENTS
    public static class FragmentAktuality extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final View view = inflater.inflate(R.layout.fragment_aktuality, container, false);

            final CustomList aktualityListAdapter = new CustomList(appContext,R.layout.listview_aktuality);
            ListView aktualityList = view.findViewById(R.id.listview_aktuality);
            aktualityList.setAdapter(aktualityListAdapter);
            aktualityList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String url = aktualityListAdapter.getParam(4,position);
                    if(!url.equals("")) {
                        try {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            startActivity(browserIntent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(appContext, R.string.error_any_webbrowser, Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }
                }
            });

            int gvidCount = 0;
            int moodleCount = 0;
            String aktualityPath = MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "aktuality.dat";
            File file = new File(aktualityPath);
            if(file.exists()) {
                try {
                    InputStream inputStream = MainActivity.appContext.openFileInput("aktuality.dat");

                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String receivedString;

                    aktualityListAdapter.addItem("gvid","","","","","");
                    while((receivedString = bufferedReader.readLine()) != null) {
                        if(receivedString.contains("<article>") && receivedString.contains("</article>")){
                            String type = receivedString.substring(receivedString.indexOf("<type>") + "<type>".length(), receivedString.indexOf("</type>"));
                            String date = receivedString.substring(receivedString.indexOf("<date>") + "<date>".length(), receivedString.indexOf("</date>"));
                            String month = receivedString.substring(receivedString.indexOf("<month>") + "<month>".length(), receivedString.indexOf("</month>"));
                            String title = receivedString.substring(receivedString.indexOf("<title>") + "<title>".length(), receivedString.indexOf("</title>"));
                            String url = receivedString.substring(receivedString.indexOf("<url>") + "<url>".length(), receivedString.indexOf("</url>"));
                            if(type.equals("gvid")) {
                                gvidCount++;
                                aktualityListAdapter.addItem("article",date,month,title,url,"");
                            } else if(type.equals("moodle")) {
                                if(moodleCount == 0) {
                                    if (gvidCount == 0) {
                                        aktualityListAdapter.addItem("noarticle","","","","","");
                                    }
                                    aktualityListAdapter.addItem("moodle","","","","","");
                                    aktualityListAdapter.addItem("article",date,month,title,url,"");
                                } else {
                                    aktualityListAdapter.addItem("article",date,month,title,url,"");
                                }
                                moodleCount++;
                            }
                        }
                    }
                    if(moodleCount == 0) {
                        aktualityListAdapter.addItem("moodle","","","","","");
                        aktualityListAdapter.addItem("noarticle", "", "", "", "","");
                    }

                    inputStream.close();
                } catch (FileNotFoundException e) {
                    Log.e("file", "File not found: " + e.toString());
                } catch (IOException e) {
                    Log.e("file", "Can not read file: " + e.toString());
                }
            } else {
                aktualityListAdapter.addItem("gvid","","","","","");
                aktualityListAdapter.addItem("noarticle", "", "", "", "","");
                aktualityListAdapter.addItem("moodle","","","","","");
                aktualityListAdapter.addItem("noarticle", "", "", "", "","");
            }
            aktualityListAdapter.reload();

            ViewTreeObserver observer = view .getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                @SuppressWarnings("deprecation")
                public void onGlobalLayout() {
                    new NonStaticMethod(appContext,getActivity()).setScreenOrientationUnlock();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                }
            });

            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getActivity().setTitle("Aktuality");
            getActivity().findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        }
    }

    public static class FragmentSuplovani extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final View view = inflater.inflate(R.layout.fragment_suplovani, container, false);

            final CustomList suplovaniListAdapter = new CustomList(appContext,R.layout.listview_suplovani);
            ListView suplovaniList = view.findViewById(R.id.listview_suplovani);
            suplovaniList.setAdapter(suplovaniListAdapter);
            suplovaniList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String itemType = suplovaniListAdapter.getParam(0,position);
                    String fileName = suplovaniListAdapter.getParam(4,position);
                    if(itemType.equals("file")) {
                        String sdPath = Environment.getExternalStorageDirectory() + File.separator;
                        sdPath = sdPath + "GvidApp" + File.separator + fileName;
                        File file = new File(sdPath);
                        if (!fileName.equals("") && file.exists()) {
                            Uri fileURI;
                            if(Build.VERSION.SDK_INT >= 24) {
                                fileURI = FileProvider.getUriForFile(appContext, BuildConfig.APPLICATION_ID + ".provider", file);
                            } else {
                                fileURI = Uri.fromFile(file);
                            }
                            Intent target = new Intent(Intent.ACTION_VIEW);
                            target.setDataAndType(fileURI, "application/pdf");
                            target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            Intent intent = Intent.createChooser(target, MainActivity.appContext.getString(R.string.pdfreader));
                            try {
                                startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.error_any_pdfreader), Toast.LENGTH_LONG).show();
                            }
                        }
                    } else if(itemType.equals("fileold")) {
                        String sdPath = Environment.getExternalStorageDirectory() + File.separator;
                        sdPath = sdPath + "GvidApp" + File.separator + "old" + File.separator + fileName;
                        File file = new File(sdPath);
                        if (!fileName.equals("") && file.exists()) {
                            Uri fileURI;
                            if(Build.VERSION.SDK_INT >= 24) {
                                fileURI = FileProvider.getUriForFile(appContext, BuildConfig.APPLICATION_ID + ".provider", file);
                            } else {
                                fileURI = Uri.fromFile(file);
                            }
                            Intent target = new Intent(Intent.ACTION_VIEW);
                            target.setDataAndType(fileURI, "application/pdf");
                            target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            Intent intent = Intent.createChooser(target, MainActivity.appContext.getString(R.string.pdfreader));
                            try {
                                startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.error_any_pdfreader), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }
            });

            suplovaniListAdapter.addItem("latest","","","","","");
            String sdPath = Environment.getExternalStorageDirectory() + File.separator;
            sdPath = sdPath + "GvidApp" + File.separator;
            File GvidApp = new File(sdPath);
            if(GvidApp.isDirectory()) {
                File[] dirFiles = GvidApp.listFiles();
                Arrays.sort(dirFiles, Collections.reverseOrder());
                if(dirFiles.length > 0) {
                    int suplyFilesCount = 0;
                    for (File file : dirFiles) {
                        String fileName = file.getName();
                        if(fileName.contains(".pdf") && fileName.contains("___") && fileName.substring(fileName.indexOf("___")+"___".length()).contains("___")) {
                            String postDate = fileName.substring(0,fileName.indexOf("___"));
                            String suplyTitle = fileName.substring(fileName.indexOf("___")+"___".length());
                            String suplyFileName = suplyTitle.substring(suplyTitle.indexOf("___")+"___".length());
                            suplyTitle = suplyTitle.substring(0,suplyTitle.indexOf("___"));
                            suplovaniListAdapter.addItem("file",suplyTitle,suplyFileName,postDate,fileName,"");
                            suplyFilesCount++;
                        }
                    }
                    if(suplyFilesCount == 0) {
                        suplovaniListAdapter.addItem("nofile","","","","","");
                    }
                } else {
                    suplovaniListAdapter.addItem("nofile","","","","","");
                }
            } else {
                suplovaniListAdapter.addItem("nofile","","","","","");
            }

            suplovaniListAdapter.addItem("old","","","","","");
            sdPath = Environment.getExternalStorageDirectory() + File.separator;
            sdPath = sdPath + "GvidApp" + File.separator + "old" + File.separator;
            File oldGvidApp = new File(sdPath);
            if(oldGvidApp.isDirectory()) {
                File[] dirFiles = oldGvidApp.listFiles();
                Arrays.sort(dirFiles, Collections.reverseOrder());
                if(dirFiles.length > 0) {
                    int suplyFilesCount = 0;
                    for (File file : dirFiles) {
                        String fileName = file.getName();
                        if(fileName.contains(".pdf") && fileName.contains("___") && fileName.substring(fileName.indexOf("___")+"___".length()).contains("___")) {
                            String postDate = fileName.substring(0,fileName.indexOf("___"));
                            String suplyTitle = fileName.substring(fileName.indexOf("___")+"___".length());
                            String suplyFileName = suplyTitle.substring(suplyTitle.indexOf("___")+"___".length());
                            suplyTitle = suplyTitle.substring(0,suplyTitle.indexOf("___"));
                            suplovaniListAdapter.addItem("fileold",suplyTitle,suplyFileName,postDate,fileName,"");
                            suplyFilesCount++;
                        }
                    }
                    if(suplyFilesCount == 0) {
                        suplovaniListAdapter.addItem("nofile","","","","","");
                    }
                } else {
                    suplovaniListAdapter.addItem("nofile","","","","","");
                }
            } else {
                suplovaniListAdapter.addItem("nofile","","","","","");
            }
            suplovaniListAdapter.reload();

            ViewTreeObserver observer = view .getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                @SuppressWarnings("deprecation")
                public void onGlobalLayout() {
                    new NonStaticMethod(appContext,getActivity()).setScreenOrientationUnlock();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                }
            });

            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getActivity().setTitle("Suplování");
            getActivity().findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        }
    }

    public static class FragmentZnamky extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final View view = inflater.inflate(R.layout.fragment_znamky, container, false);

            final ArrayList<String> TabsTitles = new ArrayList<>();
            TabsTitles.add(MainActivity.appContext.getString(R.string.znamky_tab1));
            TabsTitles.add(MainActivity.appContext.getString(R.string.znamky_tab2));
            TabsTitles.add(MainActivity.appContext.getString(R.string.znamky_tab3));

            final CustomList znamkyDatumListAdapter = new CustomList(appContext,R.layout.listview_znamky);
            znamkyDatumListAdapter.addItem("zahlavi","","","","","");

            final CustomList znamkyPredmetyListAdapter = new CustomList(appContext,R.layout.listview_znamky);
            znamkyPredmetyListAdapter.addItem("zahlavi","","","","","");

            final CustomList znamkyHodnoceniListAdapter = new CustomList(appContext,R.layout.listview_znamky);
            znamkyHodnoceniListAdapter.addItem("zahlavi_hodnoceni","","","","","");

            String lunchPath = MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "marks.dat";
            File file = new File(lunchPath);
            if(file.exists()) {
                try {
                    InputStream inputStream = MainActivity.appContext.openFileInput("marks.dat");

                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String receivedString;

                    while((receivedString = bufferedReader.readLine()) != null){
                        if(receivedString.contains("<datum>") && receivedString.contains("</datum>")) {
                            while(receivedString.contains("<item>") && receivedString.contains("</item>")) {
                                String date = "";
                                String subject = "";
                                String mark = "";
                                //String value = "";
                                String weight = "";
                                String description = "";
                                if(receivedString.contains("<date>") && receivedString.contains("</date>")) {
                                    date = stringBetweenSubstrings(receivedString,"<date>","</date>");
                                }
                                if(receivedString.contains("<subject>") && receivedString.contains("</subject>")) {
                                    subject = stringBetweenSubstrings(receivedString,"<subject>","</subject>");
                                }
                                if(receivedString.contains("<mark>") && receivedString.contains("</mark>")) {
                                    mark = stringBetweenSubstrings(receivedString,"<mark>","</mark>");
                                }
                                //if(receivedString.contains("<value>") && receivedString.contains("</value>")) {
                                //    value = stringBetweenSubstrings(receivedString,"<value>","</value>");
                                //}
                                if(receivedString.contains("<weight>") && receivedString.contains("</weight>")) {
                                    weight = stringBetweenSubstrings(receivedString,"<weight>","</weight>");
                                }
                                if(receivedString.contains("<description>") && receivedString.contains("</description>")) {
                                    description = stringBetweenSubstrings(receivedString,"<description>","</description>");
                                }
                                receivedString = receivedString.substring(receivedString.indexOf("</item>")+"</item>".length());
                                znamkyDatumListAdapter.addItem("znamka",date,subject,mark,weight,description);
                            }
                        } else if(receivedString.contains("<predmety>") && receivedString.contains("</predmety>")) {
                            while((receivedString.contains("<item>") && receivedString.contains("</item>")) || (receivedString.contains("<item2>") && receivedString.contains("</item2>"))) {
                                if(receivedString.contains("<item>") && receivedString.indexOf("<item>") < receivedString.indexOf("<item2>")) {
                                    String date = "";
                                    String subject = "";
                                    String mark = "";
                                    //String value = "";
                                    String weight = "";
                                    String description = "";
                                    if (receivedString.contains("<date>") && receivedString.contains("</date>")) {
                                        date = stringBetweenSubstrings(receivedString, "<date>", "</date>");
                                    }
                                    if (receivedString.contains("<subject>") && receivedString.contains("</subject>")) {
                                        subject = stringBetweenSubstrings(receivedString, "<subject>", "</subject>");
                                    }
                                    if (receivedString.contains("<mark>") && receivedString.contains("</mark>")) {
                                        mark = stringBetweenSubstrings(receivedString, "<mark>", "</mark>");
                                    }
                                    //if (receivedString.contains("<value>") && receivedString.contains("</value>")) {
                                    //    value = stringBetweenSubstrings(receivedString, "<value>", "</value>");
                                    //}
                                    if (receivedString.contains("<weight>") && receivedString.contains("</weight>")) {
                                        weight = stringBetweenSubstrings(receivedString, "<weight>", "</weight>");
                                    }
                                    if (receivedString.contains("<description>") && receivedString.contains("</description>")) {
                                        description = stringBetweenSubstrings(receivedString, "<description>", "</description>");
                                    }
                                    receivedString = receivedString.substring(receivedString.indexOf("</item>")+"</item>".length());
                                    znamkyPredmetyListAdapter.addItem("znamka",date,subject,mark,weight,description);
                                } else {
                                    String subject = "";
                                    String average = "";
                                    if (receivedString.contains("<subject>") && receivedString.contains("</subject>")) {
                                        subject = stringBetweenSubstrings(receivedString, "<subject>", "</subject>");
                                    }
                                    if (receivedString.contains("<average>") && receivedString.contains("</average>")) {
                                        average = stringBetweenSubstrings(receivedString, "<average>", "</average>");
                                    }
                                    receivedString = receivedString.substring(receivedString.indexOf("</item2>")+"</item2>".length());
                                    znamkyPredmetyListAdapter.addItem("prumer",average,subject,"","","");
                                }
                            }
                        } else if(receivedString.contains("<hodnoceni>") && receivedString.contains("</hodnoceni>")) {
                            while(receivedString.contains("<item>") && receivedString.contains("</item>")) {
                                String subject = "";
                                String average = "";
                                String half1 = "";
                                String half2 = "";
                                if(receivedString.contains("<subject>") && receivedString.contains("</subject>")) {
                                    subject = stringBetweenSubstrings(receivedString,"<subject>","</subject>");
                                }
                                if(receivedString.contains("<average>") && receivedString.contains("</average>")) {
                                    average = stringBetweenSubstrings(receivedString,"<average>","</average>");
                                }
                                if(receivedString.contains("<half1>") && receivedString.contains("</half1>")) {
                                    half1 = stringBetweenSubstrings(receivedString,"<half1>","</half1>");
                                }
                                if(receivedString.contains("<half2>") && receivedString.contains("</half2>")) {
                                    half2 = stringBetweenSubstrings(receivedString,"<half2>","</half2>");
                                }
                                receivedString = receivedString.substring(receivedString.indexOf("</item>")+"</item>".length());
                                znamkyHodnoceniListAdapter.addItem("znamka_hodnoceni",subject,average,half1,half2,"");
                            }
                        }
                    }

                    inputStream.close();
                } catch (FileNotFoundException e) {
                    Log.e("file", "File not found: " + e.toString());
                } catch (IOException e) {
                    Log.e("file", "Can not read file: " + e.toString());
                }

                znamkyDatumListAdapter.reload();
                znamkyPredmetyListAdapter.reload();
                znamkyHodnoceniListAdapter.reload();
            }

            ViewPager viewPager = view.findViewById(R.id.znamkyViewPager);
            viewPager.setOffscreenPageLimit(TabsTitles.size());
            final TabLayout tabLayout = view.findViewById(R.id.znamkyTabLayout);
            viewPager.setAdapter(new PagerAdapter() {
                @NonNull
                @Override
                public Object instantiateItem(@NonNull ViewGroup container, int position) {
                    LayoutInflater inflater = (LayoutInflater) MainActivity.appContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View layout = null;

                    if(position == 0) {
                        layout = inflater.inflate(R.layout.tab_znamky_datum, container, false);

                        ListView znamkyDatumList = layout.findViewById(R.id.listview_znamky_datum);
                        znamkyDatumList.setAdapter(znamkyDatumListAdapter);

                        if(znamkyDatumListAdapter.getCount() > 1) {
                            layout.findViewById(R.id.tabZnamkyNothink).setVisibility(View.GONE);
                        } else {
                            layout.findViewById(R.id.listview_znamky_datum).setVisibility(View.GONE);
                        }
                    } else if (position == 1) {
                        layout = inflater.inflate(R.layout.tab_znamky_predmety, container, false);

                        ListView znamkyPredmetyList = layout.findViewById(R.id.listview_znamky_predmety);
                        znamkyPredmetyList.setAdapter(znamkyPredmetyListAdapter);

                        if(znamkyPredmetyListAdapter.getCount() > 1) {
                            layout.findViewById(R.id.tabZnamkyNothink).setVisibility(View.GONE);
                        } else {
                            layout.findViewById(R.id.listview_znamky_predmety).setVisibility(View.GONE);
                        }
                    } else if(position == 2) {
                        layout = inflater.inflate(R.layout.tab_znamky_hodnoceni, container, false);

                        ListView znamkyHodnoceniList = layout.findViewById(R.id.listview_znamky_hodnoceni);
                        znamkyHodnoceniList.setAdapter(znamkyHodnoceniListAdapter);

                        if(znamkyHodnoceniListAdapter.getCount() > 1) {
                            layout.findViewById(R.id.tabZnamkyNothink).setVisibility(View.GONE);
                        } else {
                            layout.findViewById(R.id.listview_znamky_hodnoceni).setVisibility(View.GONE);
                        }

                        getActivity().findViewById(R.id.tabZnamkyLoading).setVisibility(View.GONE);
                    }

                    container.addView(layout);
                    return layout;
                }

                @Override
                public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object view) {
                    container.removeView((View) view);
                }

                @Override
                public CharSequence getPageTitle(int position) {
                    return TabsTitles.get(position);
                }

                @Override
                public int getCount() {
                    return TabsTitles.size();
                }

                @Override
                public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
                    return view == object;
                }
            });
            tabLayout.setupWithViewPager(viewPager);

            ViewTreeObserver observer = view .getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                @SuppressWarnings("deprecation")
                public void onGlobalLayout() {
                    getActivity().findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                    getActivity().findViewById(R.id.znamkyViewPager).setVisibility(View.VISIBLE);
                    new NonStaticMethod(appContext,getActivity()).setScreenOrientationUnlock();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                }
            });

            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getActivity().setTitle("Známky");
        }
    }

    public static class FragmentObedy extends Fragment {

        ProgressDialog progressdialog = null;
        TabLayout tabLayoutT = null;
        CustomList objednavaniListAdapter = null;

        static int todayYear = 0;
        static int todayMonth = 0;
        static int todayDay = 0;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final View view = inflater.inflate(R.layout.fragment_obedy, container, false);

            String lunchPath = MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "lunch_info.dat";
            File file = new File(lunchPath);
            if(file.exists()) {
                try {
                    InputStream inputStream = MainActivity.appContext.openFileInput("lunch_info.dat");

                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String receivedString;

                    while((receivedString = bufferedReader.readLine()) != null){
                        if(receivedString.contains("<lunch_info>") && receivedString.contains("</lunch_info>")){
                            if (receivedString.contains("<lastName>") && receivedString.contains("</lastName>") && receivedString.contains("<firstName>") && receivedString.contains("</firstName>")) {
                                ((TextView) view.findViewById(R.id.obedyName)).setText(stringBetweenSubstrings(receivedString,"<lastName>","</lastName>") + " " + stringBetweenSubstrings(receivedString,"<firstName>","</firstName>"));
                            }
                            if (receivedString.contains("<credit>") && receivedString.contains("</credit>")) {
                                String credit = stringBetweenSubstrings(receivedString,"<credit>","</credit>");
                                int credit_rest = !credit.substring(credit.indexOf(",")+1).equals("") ? Integer.valueOf(credit.substring(credit.indexOf(",")+1)) : 0;
                                if(credit_rest == 0) {
                                    credit = credit.contains(",") ? credit.substring(0,credit.indexOf(",")) : credit;
                                }
                                ((TextView) view.findViewById(R.id.obedyKredit)).setText("" + credit + " Kč");
                            }
                            if (receivedString.contains("<consumption>") && receivedString.contains("</consumption>")) {
                                String consumption = stringBetweenSubstrings(receivedString,"<consumption>","</consumption>");
                                int consuption_rest = !consumption.substring(consumption.indexOf(",")+1).equals("") ? Integer.valueOf(consumption.substring(consumption.indexOf(",")+1)) : 0;
                                if(consuption_rest == 0) {
                                    consumption = consumption.contains(",") ? consumption.substring(0,consumption.indexOf(",")) : consumption;
                                }
                                ((TextView) view.findViewById(R.id.obedySpotreba)).setText(""+ consumption + " Kč");
                            }
                        }
                    }

                    inputStream.close();
                } catch (FileNotFoundException e) {
                    Log.e("file", "File not found: " + e.toString());
                } catch (IOException e) {
                    Log.e("file", "Can not read file: " + e.toString());
                }
            }

            final ArrayList<String> TabsTitles = new ArrayList<>();
            TabsTitles.add(MainActivity.appContext.getString(R.string.obedy_tab1));
            TabsTitles.add(MainActivity.appContext.getString(R.string.obedy_tab2));
            //TabsTitles.add(MainActivity.appContext.getString(R.string.obedy_tab3));
            //TODO burza

            final NonSwipeableViewPager viewPager = view.findViewById(R.id.obedyViewPager);
            viewPager.setOffscreenPageLimit(1);
            viewPager.setPagingEnabled(false);
            final TabLayout tabLayout = view.findViewById(R.id.obedyTabLayout);
            tabLayoutT = tabLayout;
            viewPager.setAdapter(new PagerAdapter() {
                @NonNull
                @Override
                public Object instantiateItem(@NonNull ViewGroup container, int position) {
                    LayoutInflater inflater = (LayoutInflater) MainActivity.appContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View layout = null;

                    if(position == 0) {
                        layout = inflater.inflate(R.layout.tab_obedy_tento_tyden, container, false);

                        ArrayList<String> Lunchs = new ArrayList<>();

                        String lunchPath = MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "lunch_info.dat";
                        File file = new File(lunchPath);
                        if(file.exists()) {
                            try {
                                InputStream inputStream = MainActivity.appContext.openFileInput("lunch_info.dat");

                                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                                String receivedString;

                                while((receivedString = bufferedReader.readLine()) != null){
                                     if(receivedString.contains("<lunch_info>") && receivedString.contains("</lunch_info>")){
                                        if (receivedString.contains("<time>") && receivedString.contains("</time>") && receivedString.contains("<date>") && receivedString.contains("</date>") && receivedString.contains("<day>") && receivedString.contains("</day>")) {
                                            String time = stringBetweenSubstrings(receivedString,"<time>","</time>");
                                            ((TextView) layout.findViewById(R.id.tabObedyLastUpdate)).setText(stringBetweenSubstrings(receivedString,"<day>","</day>") + " - " + stringBetweenSubstrings(receivedString,"<date>","</date>") + ", " + time.substring(0,time.indexOf(".")));
                                        }
                                    } else if(receivedString.contains("<day>") && receivedString.contains("</day>")){
                                        Lunchs.add(stringBetweenSubstrings(receivedString,"<day>","</day>"));
                                    }
                                }

                                inputStream.close();
                            } catch (FileNotFoundException e) {
                                Log.e("file", "File not found: " + e.toString());
                            } catch (IOException e) {
                                Log.e("file", "Can not read file: " + e.toString());
                            }
                        }

                        if(Lunchs.size() > 0) {
                            layout.findViewById(R.id.tabObedyTentoTydenNothink).setVisibility(View.GONE);

                            final CustomList obedyListAdapter = new CustomList(appContext,R.layout.listview_obedy);
                            ListView obedyList = layout.findViewById(R.id.listview_obedy_tento_tyden);
                            obedyList.setAdapter(obedyListAdapter);
                            for (String day : Lunchs) {
                                String lunchDate = stringBetweenSubstrings(day,"<date>","</date>");
                                String lunchDay = stringBetweenSubstrings(day,"<day_name>","</day_name>");
                                obedyListAdapter.addItem("top", "", "", "", "","");
                                obedyListAdapter.addItem("lunchdate", lunchDay, lunchDate, "", "","");
                                while(day.contains("<item>")) {
                                    String item = day.substring(day.indexOf("<item>")+"<item>".length(),day.indexOf("</item>"));
                                    day = day.substring(day.indexOf("</item>")+"</item>".length());
                                    String foodType = stringBetweenSubstrings(item,"<foodType>","</foodType>");
                                    String foodName = stringBetweenSubstrings(item,"<foodName>","</foodName>");
                                    String foodStatus = stringBetweenSubstrings(item,"<foodStatus>","</foodStatus>");
                                    if(foodType.equals("Oběd 1")) {
                                        if(foodName.contains(";")) {
                                            obedyListAdapter.addItem("lunch", "Polévka", foodName.substring(0, foodName.indexOf(";")), "", "", "");
                                            foodName = foodName.substring(foodName.indexOf(";")+1);
                                        } else if(foodName.contains("Polévka") && foodName.contains(",")) {
                                            obedyListAdapter.addItem("lunch", "Polévka", foodName.substring(0, foodName.indexOf(",")), "", "", "");
                                            foodName = foodName.substring(foodName.indexOf(",")+1);
                                        }
                                    }
                                    foodName = foodName.replaceAll(";","").trim();
                                    if(foodStatus.equals("zrušit") || foodStatus.equals("nelze zrušit")) {
                                        obedyListAdapter.addItem("lunchselected", foodType, foodName, "", "","");
                                    } else {
                                        obedyListAdapter.addItem("lunch", foodType, foodName, "", "","");
                                    }
                                }
                                obedyListAdapter.addItem("bottom", "", "", "", "","");
                            }
                            obedyListAdapter.reload();
                        } else {
                            layout.findViewById(R.id.listview_obedy_tento_tyden).setVisibility(View.GONE);
                        }

                        getActivity().findViewById(R.id.tabObedyLoading).setVisibility(View.GONE);
                    } else if (position == 1) {
                        layout = inflater.inflate(R.layout.tab_obedy_objednavani, container, false);

                        Button buttonBack = layout.findViewById(R.id.obedyButtonBack);
                        buttonBack.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                new NonStaticMethod(appContext,getActivity()).setScreenOrientationLock();
                                progressdialog = new ProgressDialog(getActivity());
                                progressdialog.setCancelable(false);
                                progressdialog.setMessage("Načítání obědu...");
                                progressdialog.show();
                                todayDay--;
                                if(todayDay == 0) {
                                    if(todayMonth == 1) {
                                        todayYear--;
                                        todayMonth = 12;
                                        Calendar lunchCal = new GregorianCalendar(todayYear, todayMonth-1, 1);
                                        todayDay = lunchCal.getActualMaximum(Calendar.DAY_OF_MONTH);
                                    } else {
                                        todayMonth--;
                                        Calendar lunchCal = new GregorianCalendar(todayYear, todayMonth-1, 1);
                                        todayDay = lunchCal.getActualMaximum(Calendar.DAY_OF_MONTH);
                                    }
                                }
                                LoadLunch();
                            }
                        });
                        Button buttonNext = layout.findViewById(R.id.obedyButtonNext);
                        buttonNext.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                new NonStaticMethod(appContext,getActivity()).setScreenOrientationLock();
                                progressdialog = new ProgressDialog(getActivity());
                                progressdialog.setCancelable(false);
                                progressdialog.setMessage("Načítání obědu...");
                                progressdialog.show();
                                todayDay++;
                                Calendar lunchCal = new GregorianCalendar(todayYear, todayMonth-1, 1);
                                int daysInMonth = lunchCal.getActualMaximum(Calendar.DAY_OF_MONTH);
                                if(todayDay > daysInMonth) {
                                    if(todayMonth == 12) {
                                        todayYear++;
                                        todayMonth = 1;
                                        todayDay = 1;
                                    } else {
                                        todayMonth++;
                                        todayDay = 1;
                                    }
                                }
                                LoadLunch();
                            }
                        });

                        objednavaniListAdapter = new CustomList(appContext,R.layout.listview_objednavani);
                        ListView objednavaniList = layout.findViewById(R.id.tabObedyObjednavaniListView);
                        objednavaniList.setAdapter(objednavaniListAdapter);
                        objednavaniList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                String itemType = objednavaniListAdapter.getParam(0,position);
                                String itemStatus = objednavaniListAdapter.getParam(2,position);
                                if(itemType.equals("item") && (itemStatus.equals("zrušit") || itemStatus.equals("přeobjednat") || itemStatus.equals("objednat"))) {
                                    String itemUrl = "http://jidelna.gvid.cz/faces/secured/"+objednavaniListAdapter.getParam(3,position).replaceAll("amp;","");
                                    Call<String> call2 = callClient.getUrl(itemUrl);
                                    call2.enqueue(new Callback<String>() {
                                        @Override
                                        public void onResponse(Call<String> call, final Response<String> response) {
                                            if (response.isSuccessful()) {
                                                if (response.code() == 200) {
                                                    String htmlResponse = response.body();
                                                    //Response - END ----------------------------------------------------------------------------------
                                                    if(htmlResponse.contains("kredit") && htmlResponse.contains("spotřeba")) {
                                                        String credit = MainActivity.stringBetweenSubstrings(htmlResponse, "<span id=\"Kredit\" style=\"font-weight: bold; color: #99cc00;\">", " K");
                                                        int credit_rest = !credit.substring(credit.indexOf(",")+1).equals("") ? Integer.valueOf(credit.substring(credit.indexOf(",")+1)) : 0;
                                                        if(credit_rest == 0) {
                                                            credit = credit.contains(",") ? credit.substring(0,credit.indexOf(",")) : credit;
                                                        }
                                                        ((TextView) getActivity().findViewById(R.id.obedyKredit)).setText("" + credit + " Kč");

                                                        String consumption = MainActivity.stringBetweenSubstrings(htmlResponse, "eba</span>: <strong>", " K");
                                                        int consuption_rest = !consumption.substring(consumption.indexOf(",")+1).equals("") ? Integer.valueOf(consumption.substring(consumption.indexOf(",")+1)) : 0;
                                                        if(consuption_rest == 0) {
                                                            consumption = consumption.contains(",") ? consumption.substring(0,consumption.indexOf(",")) : consumption;
                                                        }
                                                        ((TextView) getActivity().findViewById(R.id.obedySpotreba)).setText(""+ consumption + " Kč");

                                                        LoadLunch();
                                                    } else {
                                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.obedy_fail), Toast.LENGTH_LONG).show();
                                                        noLogin();
                                                    }
                                                    //Response - END ----------------------------------------------------------------------------------
                                                } else {
                                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_code, response.code()), Toast.LENGTH_LONG).show();
                                                    noLogin();
                                                }
                                            } else {
                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, response.errorBody().toString()), Toast.LENGTH_LONG).show();
                                                noLogin();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<String> call, Throwable t) {
                                            if (t instanceof UnknownHostException) {
                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown_host), Toast.LENGTH_LONG).show();
                                            } else if (t instanceof SocketTimeoutException) {
                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_timeout), Toast.LENGTH_LONG).show();
                                            } else {
                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown), Toast.LENGTH_LONG).show();
                                            }
                                            noLogin();
                                        }
                                    });
                                }
                            }
                        });
                        objednavaniListAdapter.reload();
                    } else if(position == 2) {
                        layout = inflater.inflate(R.layout.tab_obedy_burza, container, false);
                        //TODO burza
                    }

                    container.addView(layout);
                    return layout;
                }

                @Override
                public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object view) {
                    container.removeView((View) view);
                }

                @Override
                public CharSequence getPageTitle(int position) {
                    return TabsTitles.get(position);
                }

                @Override
                public int getCount() {
                    return TabsTitles.size();
                }

                @Override
                public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
                    return view == object;
                }
            });
            viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrollStateChanged(int state) {
                    if(state == ViewPager.SCROLL_STATE_IDLE) {
                        if(obedyPagePosition == 0) {
                            ((TextView) getActivity().findViewById(R.id.obedyViewPager).findViewById(R.id.tabObedyObjednavaniTextView)).setText(appContext.getResources().getString(R.string.string_loading));
                            getActivity().findViewById(R.id.obedyViewPager).findViewById(R.id.tabObedyObjednavaniTextView).setVisibility(View.VISIBLE);
                            getActivity().findViewById(R.id.obedyViewPager).findViewById(R.id.tabObedyObjednavaniListView).setVisibility(View.GONE);
                            FloatingActionButton floatingActionButton = getActivity().findViewById(R.id.floating_button);
                            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                            if(!sharedPref.getBoolean("floating_button_update", true)) {
                                floatingActionButton.hide();
                            }else{
                                floatingActionButton.show();
                            }

                            String lunchPath = MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "lunch_info.dat";
                            File file = new File(lunchPath);
                            if(file.exists()) {
                                try {
                                    InputStream inputStream = MainActivity.appContext.openFileInput("lunch_info.dat");

                                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                                    String receivedString;

                                    while((receivedString = bufferedReader.readLine()) != null){
                                        if(receivedString.contains("<lunch_info>") && receivedString.contains("</lunch_info>")){
                                            if (receivedString.contains("<lastName>") && receivedString.contains("</lastName>") && receivedString.contains("<firstName>") && receivedString.contains("</firstName>")) {
                                                ((TextView) view.findViewById(R.id.obedyName)).setText(stringBetweenSubstrings(receivedString,"<lastName>","</lastName>") + " " + stringBetweenSubstrings(receivedString,"<firstName>","</firstName>"));
                                            }
                                            if (receivedString.contains("<credit>") && receivedString.contains("</credit>")) {
                                                String credit = stringBetweenSubstrings(receivedString,"<credit>","</credit>");
                                                int credit_rest = !credit.substring(credit.indexOf(",")+1).equals("") ? Integer.valueOf(credit.substring(credit.indexOf(",")+1)) : 0;
                                                if(credit_rest == 0) {
                                                    credit = credit.contains(",") ? credit.substring(0,credit.indexOf(",")) : credit;
                                                }
                                                ((TextView) view.findViewById(R.id.obedyKredit)).setText("" + credit + " Kč");
                                            }
                                            if (receivedString.contains("<consumption>") && receivedString.contains("</consumption>")) {
                                                String consumption = stringBetweenSubstrings(receivedString,"<consumption>","</consumption>");
                                                int consuption_rest = !consumption.substring(consumption.indexOf(",")+1).equals("") ? Integer.valueOf(consumption.substring(consumption.indexOf(",")+1)) : 0;
                                                if(consuption_rest == 0) {
                                                    consumption = consumption.contains(",") ? consumption.substring(0,consumption.indexOf(",")) : consumption;
                                                }
                                                ((TextView) view.findViewById(R.id.obedySpotreba)).setText(""+ consumption + " Kč");
                                            }
                                        }
                                    }

                                    inputStream.close();
                                } catch (FileNotFoundException e) {
                                    Log.e("file", "File not found: " + e.toString());
                                } catch (IOException e) {
                                    Log.e("file", "Can not read file: " + e.toString());
                                }
                            }
                        }else if(obedyPagePosition == 1) {
                            FloatingActionButton floatingActionButton = getActivity().findViewById(R.id.floating_button);
                            floatingActionButton.hide();

                            new NonStaticMethod(appContext,getActivity()).setScreenOrientationLock();
                            progressdialog = new ProgressDialog(getActivity());
                            progressdialog.setCancelable(false);
                            progressdialog.setMessage("Přihlašování k jídelně...");
                            progressdialog.show();

                            String accountsPath = MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "accounts.cfg";
                            File file = new File(accountsPath);
                            if (file.exists()) {
                                int file_length = (int) file.length();
                                byte[] file_bytes = new byte[file_length];
                                try {
                                    FileInputStream file_stream = new FileInputStream(file);
                                    int readResult = file_stream.read(file_bytes);
                                    file_stream.close();
                                    if (file_length != readResult) {
                                        Log.e("file", "READING FILE: " + readResult + "/" + file_length);
                                    }
                                } catch (IOException e) {
                                    Log.e("file", "READING FILE: " + e.toString());
                                }
                                String file_contents = new String(file_bytes);

                                if (file_contents.contains("<icanteen_username>") && file_contents.contains("</icanteen_username>")) {
                                    final String username = file_contents.substring(file_contents.indexOf("<icanteen_username>") + "<icanteen_username>".length(), file_contents.indexOf("</icanteen_username>"));
                                    final String password = file_contents.substring(file_contents.indexOf("<icanteen_password>") + "<icanteen_password>".length(), file_contents.indexOf("</icanteen_password>"));

                                    String API_BASE_URL = "http://jidelna.gvid.cz/";
                                    CookieManager cookieManager = new CookieManager();
                                    cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
                                    okHttpClient = new OkHttpClient();
                                    OkHttpClient.Builder httpClient = okHttpClient.newBuilder();
                                    httpClient.cookieJar(new JavaNetCookieJar(cookieManager));
                                    httpClient.addInterceptor(new Interceptor() {
                                        @Override
                                        public okhttp3.Response intercept(Chain chain) throws IOException {
                                            Request original = chain.request();
                                            Request request = original.newBuilder()
                                            .header("Accept-Language", "cs")//Locale.getDefault().getDisplayLanguage()
                                            .method(original.method(), original.body())
                                            .build();
                                            return chain.proceed(request);
                                        }
                                    });
                                    Retrofit.Builder retrofitBuilder = new Retrofit.Builder().baseUrl(API_BASE_URL).addConverterFactory(ScalarsConverterFactory.create());
                                    Retrofit retrofit = retrofitBuilder.client(httpClient.build()).build();
                                    callClient = retrofit.create(HttpClient.class);

                                    Call<String> call = callClient.getUrl("http://jidelna.gvid.cz");
                                    call.enqueue(new Callback<String>() {
                                        @Override
                                        public void onResponse(Call<String> call, Response<String> response) {
                                            if (response.isSuccessful()) {
                                                if (response.code() == 200) {
                                                    String htmlResponse = response.body();
                                                    //Response - END ----------------------------------------------------------------------------------
                                                    String hash = stringBetweenSubstrings(htmlResponse, "<input type=\"hidden\" name=\"_csrf\" value=\"", "\"/>");

                                                    Call<String> call1 = callClient.postiCanteenLogin(username, password, "false", "web", hash);
                                                    call1.enqueue(new Callback<String>() {
                                                        @Override
                                                        public void onResponse(Call<String> call, Response<String> response) {
                                                            if (response.isSuccessful()) {
                                                                if (response.code() == 200) {
                                                                    String htmlResponse = response.body();
                                                                    //Response - END ----------------------------------------------------------------------------------
                                                                    if (htmlResponse.contains("j_spring_security_logout")) {
                                                                        progressdialog.setMessage("Načítání obědu...");
                                                                        String date = MainActivity.stringBetweenSubstrings(htmlResponse, "<span class=\"important\">", "</span>").trim();
                                                                        todayYear = Integer.valueOf(date.substring(MainActivity.indexOfFromEnd(date, ".") + 1));
                                                                        todayMonth = Integer.valueOf(MainActivity.stringBetweenSubstrings(date, ".", "."));
                                                                        todayDay = Integer.valueOf(date.substring(0, date.indexOf(".")));

                                                                        String credit = MainActivity.stringBetweenSubstrings(htmlResponse, "<span id=\"Kredit\" style=\"font-weight: bold; color: #99cc00;\">", " K");
                                                                        int credit_rest = !credit.substring(credit.indexOf(",")+1).equals("") ? Integer.valueOf(credit.substring(credit.indexOf(",")+1)) : 0;
                                                                        if(credit_rest == 0) {
                                                                            credit = credit.contains(",") ? credit.substring(0,credit.indexOf(",")) : credit;
                                                                        }
                                                                        ((TextView) getActivity().findViewById(R.id.obedyKredit)).setText("" + credit + " Kč");

                                                                        String consumption = MainActivity.stringBetweenSubstrings(htmlResponse, "eba</span>: <strong>", " K");
                                                                        int consuption_rest = !consumption.substring(consumption.indexOf(",")+1).equals("") ? Integer.valueOf(consumption.substring(consumption.indexOf(",")+1)) : 0;
                                                                        if(consuption_rest == 0) {
                                                                            consumption = consumption.contains(",") ? consumption.substring(0,consumption.indexOf(",")) : consumption;
                                                                        }
                                                                        ((TextView) getActivity().findViewById(R.id.obedySpotreba)).setText(""+ consumption + " Kč");

                                                                        LoadLunch();
                                                                    } else {
                                                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_login_unsuccessful), Toast.LENGTH_LONG).show();
                                                                        noLogin();
                                                                    }
                                                                    //Response - END ----------------------------------------------------------------------------------
                                                                } else {
                                                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_code, response.code()), Toast.LENGTH_LONG).show();
                                                                    noLogin();
                                                                }
                                                            } else {
                                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, response.errorBody().toString()), Toast.LENGTH_LONG).show();
                                                                noLogin();
                                                            }
                                                        }

                                                        @Override
                                                        public void onFailure(Call<String> call, Throwable t) {
                                                            if (t instanceof UnknownHostException) {
                                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown_host), Toast.LENGTH_LONG).show();
                                                            } else if (t instanceof SocketTimeoutException) {
                                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_timeout), Toast.LENGTH_LONG).show();
                                                            } else {
                                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown), Toast.LENGTH_LONG).show();
                                                            }
                                                            noLogin();
                                                        }
                                                    });
                                                } else {
                                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_code, response.code()), Toast.LENGTH_LONG).show();
                                                    noLogin();
                                                }
                                            } else {
                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, response.errorBody().toString()), Toast.LENGTH_LONG).show();
                                                noLogin();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<String> call, Throwable t) {
                                            if (t instanceof UnknownHostException) {
                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown_host), Toast.LENGTH_LONG).show();
                                            } else if (t instanceof SocketTimeoutException) {
                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_timeout), Toast.LENGTH_LONG).show();
                                            } else {
                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown), Toast.LENGTH_LONG).show();
                                            }
                                            noLogin();
                                        }
                                    });
                                } else {
                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.aktuality_nologin), Toast.LENGTH_LONG).show();
                                    noLogin();
                                }
                            } else {
                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.aktuality_nologin), Toast.LENGTH_LONG).show();
                                noLogin();
                            }
                        } else if(obedyPagePosition == 2) {
                            ((TextView) getActivity().findViewById(R.id.obedyViewPager).findViewById(R.id.tabObedyObjednavaniTextView)).setText(appContext.getResources().getString(R.string.string_loading));
                            getActivity().findViewById(R.id.obedyViewPager).findViewById(R.id.tabObedyObjednavaniTextView).setVisibility(View.VISIBLE);
                            getActivity().findViewById(R.id.obedyViewPager).findViewById(R.id.tabObedyObjednavaniListView).setVisibility(View.GONE);
                            //TODO burza
                            TabLayout.Tab tab = tabLayout.getTabAt(0);
                            if(tab != null) {
                                tab.select();
                            }
                        }
                    }
                }

                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

                @Override
                public void onPageSelected(int position) {
                    obedyPagePosition = position;
                }
            });
            tabLayout.setupWithViewPager(viewPager);

            ViewTreeObserver observer = view .getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                @SuppressWarnings("deprecation")
                public void onGlobalLayout() {
                    getActivity().findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                    getActivity().findViewById(R.id.obedyViewPager).setVisibility(View.VISIBLE);
                    new NonStaticMethod(appContext,getActivity()).setScreenOrientationUnlock();
                    if(obedyPagePosition == 1) {
                        new NonStaticMethod(appContext, getActivity()).setScreenOrientationLock();
                        progressdialog = new ProgressDialog(getActivity());
                        progressdialog.setCancelable(false);
                        progressdialog.setMessage("Načítání obědu...");
                        progressdialog.show();
                        LoadLunch();
                    } /*else {
                        //TODO burza
                    }*/

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                }
            });

            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getActivity().setTitle("Obědy");
        }

        private void LoadLunch() {
            Call<String> call2 = callClient.getUrl("http://jidelna.gvid.cz/faces/secured/db/dbJidelnicekOnDay.jsp?day=" + todayYear + "-" + todayMonth + "-" + todayDay);
            call2.enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, final Response<String> response) {
                    if (response.isSuccessful()) {
                        if (response.code() == 200) {
                            String htmlResponse = response.body();
                            //Response - END ----------------------------------------------------------------------------------
                            String date = MainActivity.stringBetweenSubstrings(htmlResponse, "<span class=\"important\">", "</span>").trim();
                            String day_name = "";
                            if (htmlResponse.contains("Pondělí")) {
                                day_name = "Pondělí";
                            } else if (htmlResponse.contains("Úterý")) {
                                day_name = "Úterý";
                            } else if (htmlResponse.contains("Středa")) {
                                day_name = "Středu";
                            } else if (htmlResponse.contains("Čtvrtek")) {
                                day_name = "Čtvrtek";
                            } else if (htmlResponse.contains("Pátek")) {
                                day_name = "Pátek";
                            } else if (htmlResponse.contains("Sobota")) {
                                day_name = "Sobotu";
                            } else if (htmlResponse.contains("Neděle")) {
                                day_name = "Neděli";
                            }

                            getActivity().findViewById(R.id.obedyViewPager).findViewById(R.id.tabObedyObjednavaniTextView).setVisibility(View.GONE);
                            getActivity().findViewById(R.id.obedyViewPager).findViewById(R.id.tabObedyObjednavaniListView).setVisibility(View.VISIBLE);

                            objednavaniListAdapter.clear();
                            objednavaniListAdapter.reload();
                            objednavaniListAdapter.addItem("day",day_name,date,"","","");
                            if (htmlResponse.contains("jidelnicekItem ")) {
                                while (htmlResponse.contains("jidelnicekItem ")) {
                                    String oneItem;
                                    if (htmlResponse.substring(htmlResponse.indexOf("jidelnicekItem ") + "jidelnicekItem ".length()).contains("jidelnicekItem ")) {
                                        oneItem = MainActivity.stringBetweenSubstrings(htmlResponse, "<div class=\"jidelnicekItem ", "<div class=\"jidelnicekItem ");
                                        htmlResponse = htmlResponse.substring(0, htmlResponse.indexOf("jidelnicekItem ")) + htmlResponse.substring(htmlResponse.substring(htmlResponse.indexOf("jidelnicekItem ") + "jidelnicekItem ".length()).indexOf("<div class=\"jidelnicekItem "));
                                    } else {
                                        oneItem = htmlResponse.substring(htmlResponse.indexOf("jidelnicekItem "));
                                        htmlResponse = " ";
                                    }
                                    String foodName = MainActivity.stringBetweenSubstrings(oneItem, "<span style=\"min-width: 250px; display: inline-block; word-wrap: break-word; vertical-align: middle;\">", "<br>").trim();
                                    String foodType = MainActivity.stringBetweenSubstrings(oneItem, "<span class=\"smallBoldTitle button-link-align\">", "<");
                                    String foodStatus = MainActivity.stringBetweenSubstrings(oneItem, "<span class=\"button-link-align\">", "<");
                                    String foodUrl = MainActivity.stringBetweenSubstrings(oneItem, " ajaxOrder(this, '", "', '");
                                    if(!foodName.equals("")) {
                                        objednavaniListAdapter.addItem("item",foodType,foodStatus,foodUrl,"","");
                                        objednavaniListAdapter.addItem("text",foodName,"","","","");
                                    }
                                }
                            } else if(htmlResponse.contains("Litujeme, ale na vybraný den nejsou zadána v jídelníčku žádná jídla.")) {
                                objednavaniListAdapter.addItem("text","Litujeme, ale na vybraný den nejsou zadána v jídelníčku žádná jídla.","","","","");
                            } else {
                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_timeout), Toast.LENGTH_LONG).show();
                                noLogin();
                            }
                            objednavaniListAdapter.reload();

                            if (progressdialog != null && progressdialog.isShowing()) {
                                progressdialog.dismiss();
                                progressdialog = null;
                            }
                            new NonStaticMethod(appContext,getActivity()).setScreenOrientationUnlock();
                            //Response - END ----------------------------------------------------------------------------------
                        } else {
                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_code, response.code()), Toast.LENGTH_LONG).show();
                            noLogin();
                        }
                    } else {
                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, response.errorBody().toString()), Toast.LENGTH_LONG).show();
                        noLogin();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    if (t instanceof UnknownHostException) {
                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown_host), Toast.LENGTH_LONG).show();
                    } else if (t instanceof SocketTimeoutException) {
                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_timeout), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown), Toast.LENGTH_LONG).show();
                    }
                    noLogin();
                }
            });
        }

        private void noLogin() {
            if (progressdialog != null && progressdialog.isShowing()) {
                progressdialog.dismiss();
                progressdialog = null;
            }
            new NonStaticMethod(appContext,getActivity()).setScreenOrientationUnlock();

            TabLayout.Tab tab = tabLayoutT.getTabAt(0);
            if(tab != null) {
                tab.select();
            }
        }
    }

    //comment-todo - FLOATING_BUTTON
    public void onClickFloatingActionButton(View view) {
        currentFragment = getFragmentManager().findFragmentById(R.id.content_frame);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if(sharedPref.getBoolean("update_all_button", true)) {
            if (currentFragment != null) {
                String CurrentFragment = currentFragment.toString();
                if (CurrentFragment.contains("FragmentAktuality")) {
                    if (aktualityUpdating) {
                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.string_already_updating), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.aktuality_update), Toast.LENGTH_LONG).show();
                        aktualityUpdating = true;

                        String API_BASE_URL = "https://www.gvid.cz/";
                        CookieManager cookieManager = new CookieManager();
                        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
                        okHttpClient = new OkHttpClient();
                        OkHttpClient.Builder httpClient = okHttpClient.newBuilder();
                        httpClient.cookieJar(new JavaNetCookieJar(cookieManager));
                        httpClient.addInterceptor(new Interceptor() {
                            @Override
                            public okhttp3.Response intercept(Chain chain) throws IOException {
                                Request original = chain.request();
                                Request request = original.newBuilder()
                                        .header("Accept-Language", "cs")//Locale.getDefault().getDisplayLanguage()
                                        .method(original.method(), original.body())
                                        .build();
                                return chain.proceed(request);
                            }
                        });
                        Retrofit.Builder retrofitBuilder = new Retrofit.Builder().baseUrl(API_BASE_URL).addConverterFactory(ScalarsConverterFactory.create());
                        Retrofit retrofit = retrofitBuilder.client(httpClient.build()).build();
                        callClient = retrofit.create(HttpClient.class);

                        Call<String> call = callClient.getUrl("http://www.gvid.cz/feed/");
                        call.enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(Call<String> call, Response<String> response) {
                                if (response.isSuccessful()) {
                                    if (response.code() == 200) {
                                        String htmlResponse = response.body();
                                        //Response - END ----------------------------------------------------------------------------------
                                        String newString = "";
                                        int articleCount = 10;
                                        while (htmlResponse.contains("<item>") && articleCount != 0) {
                                            String result = htmlResponse.substring(htmlResponse.indexOf("<item>") + "<item>".length(), htmlResponse.indexOf("</item>"));
                                            htmlResponse = htmlResponse.replace(htmlResponse.substring(htmlResponse.indexOf("<item>"), htmlResponse.indexOf("</item>") + "</item>".length()), "");
                                            String title = result.substring(result.indexOf("<title>") + "<title>".length(), result.indexOf("</title>"));
                                            String date = result.substring(result.indexOf("<pubDate>") + "<pubDate>".length(), result.indexOf("</pubDate>"));
                                            String day = date.substring(5, 7);
                                            String month = date.substring(8, 11);
                                            switch (month) {
                                                case "Jan":
                                                    month = "ledna";
                                                    break;
                                                case "Feb":
                                                    month = "února";
                                                    break;
                                                case "Mar":
                                                    month = "března";
                                                    break;
                                                case "Apr":
                                                    month = "dubna";
                                                    break;
                                                case "May":
                                                    month = "května";
                                                    break;
                                                case "Jun":
                                                    month = "června";
                                                    break;
                                                case "Jul":
                                                    month = "července";
                                                    break;
                                                case "Aug":
                                                    month = "srpna";
                                                    break;
                                                case "Sep":
                                                    month = "září";
                                                    break;
                                                case "Oct":
                                                    month = "října";
                                                    break;
                                                case "Nov":
                                                    month = "listopadu";
                                                    break;
                                                case "Dec":
                                                    month = "prosince";
                                                    break;
                                            }
                                            String year = date.substring(12, 17);
                                            String link = result.substring(result.indexOf("<link>") + "<link>".length(), result.indexOf("</link>"));
                                            articleCount--;
                                            newString += "<article><type>gvid</type><date>" + Integer.valueOf(day).toString() + ".</date><month>" + month + "</month><year>" + year + "</year><title>" + title + "</title><url>" + link + "</url></article>\n";
                                        }

                                        String aktualityPath = MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "aktuality.dat";
                                        File file = new File(aktualityPath);
                                        if (file.exists()) {
                                            try {
                                                InputStream inputStream = MainActivity.appContext.openFileInput("aktuality.dat");

                                                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                                                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                                                String receivedString;

                                                while ((receivedString = bufferedReader.readLine()) != null) {
                                                    if (receivedString.contains("<article>") && receivedString.contains("</article>") && (receivedString.substring(receivedString.indexOf("<type>") + "<type>".length(), receivedString.indexOf("</type>"))).equals("moodle")) {
                                                        newString += receivedString + "\n";
                                                    }
                                                }

                                                inputStream.close();
                                            } catch (FileNotFoundException e) {
                                                Log.e("file", "File not found: " + e.toString());
                                            } catch (IOException e) {
                                                Log.e("file", "Can not read file: " + e.toString());
                                            }
                                        }

                                        try {
                                            FileOutputStream stream = MainActivity.appContext.openFileOutput("aktuality.dat", Context.MODE_PRIVATE);
                                            stream.write(newString.getBytes());
                                            stream.close();
                                        } catch (IOException e) {
                                            Log.e("file", "WRITING FILE: " + e.toString());
                                        }

                                        fragmentReloader.reloadFragment("FragmentAktuality");

                                        Call<String> call2 = callClient.getUrl("https://moodle3.gvid.cz/");
                                        call2.enqueue(new Callback<String>() {
                                            @Override
                                            public void onResponse(Call<String> call, Response<String> response) {
                                                if (response.isSuccessful()) {
                                                    if (response.code() == 200) {
                                                        String htmlResponse = response.body();
                                                        //Response - END ----------------------------------------------------------------------------------
                                                        String newString = "";

                                                        String aktualityPath = MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "aktuality.dat";
                                                        File file = new File(aktualityPath);
                                                        if (file.exists()) {
                                                            try {
                                                                InputStream inputStream = MainActivity.appContext.openFileInput("aktuality.dat");

                                                                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                                                                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                                                                String receivedString;

                                                                while ((receivedString = bufferedReader.readLine()) != null) {
                                                                    if (receivedString.contains("<article>") && receivedString.contains("</article>") && (receivedString.substring(receivedString.indexOf("<type>") + "<type>".length(), receivedString.indexOf("</type>"))).equals("gvid")) {
                                                                        newString += receivedString + "\n";
                                                                    }
                                                                }

                                                                inputStream.close();
                                                            } catch (FileNotFoundException e) {
                                                                Log.e("file", "File not found: " + e.toString());
                                                            } catch (IOException e) {
                                                                Log.e("file", "Can not read file: " + e.toString());
                                                            }
                                                        }

                                                        int articleCount = 3;
                                                        while (htmlResponse.contains("<div class=\"forumpost clearfix firstpost starter\"") && articleCount != 0) {
                                                            String result = htmlResponse.substring(htmlResponse.indexOf("<div class=\"forumpost clearfix firstpost starter\"") + "<div class=\"forumpost clearfix firstpost starter\"".length(), htmlResponse.indexOf("</a></div></div></div></div>"));
                                                            htmlResponse = htmlResponse.replace(htmlResponse.substring(htmlResponse.indexOf("<div class=\"forumpost clearfix firstpost starter\""), htmlResponse.indexOf("</a></div></div></div></div>") + "</a></div></div></div></div>".length()), "");
                                                            String title = result.substring(result.indexOf("<div class=\"subject\" role=\"heading\" aria-level=\"2\">") + "<div class=\"subject\" role=\"heading\" aria-level=\"2\">".length(), result.indexOf("</div><div class=\"author\" role=\"heading\" aria-level=\"2\">"));
                                                            String dates = result.substring(result.indexOf("<div class=\"author\" role=\"heading\" aria-level=\"2\">") + "<div class=\"author\" role=\"heading\" aria-level=\"2\">".length(), result.indexOf("</div></div></div><div class=\"row maincontent clearfix\">"));
                                                            String day = dates.substring(dates.indexOf(", ") + ", ".length());
                                                            day = day.substring(0, day.indexOf(". "));
                                                            String month = dates.substring(dates.indexOf(", " + day + ". ") + (", " + day + ". ").length());
                                                            month = month.substring(0, month.indexOf(" "));
                                                            String year = dates.substring(dates.indexOf(month + " ") + (month + " ").length());
                                                            year = year.substring(0, year.indexOf(", "));
                                                            switch (month) {
                                                                case "leden":
                                                                    month = "ledna";
                                                                    break;
                                                                case "únor":
                                                                    month = "února";
                                                                    break;
                                                                case "březen":
                                                                    month = "března";
                                                                    break;
                                                                case "duben":
                                                                    month = "dubna";
                                                                    break;
                                                                case "květen":
                                                                    month = "května";
                                                                    break;
                                                                case "červen":
                                                                    month = "června";
                                                                    break;
                                                                case "červenec":
                                                                    month = "července";
                                                                    break;
                                                                case "srpen":
                                                                    month = "srpna";
                                                                    break;
                                                                case "září":
                                                                    month = "září";
                                                                    break;
                                                                case "říjen":
                                                                    month = "října";
                                                                    break;
                                                                case "listopad":
                                                                    month = "listopadu";
                                                                    break;
                                                                case "prosinec":
                                                                    month = "prosince";
                                                                    break;
                                                            }
                                                            String link = "https://moodle3.gvid.cz/";
                                                            articleCount--;
                                                            newString += "<article><type>moodle</type><date>" + Integer.valueOf(day).toString() + ".</date><month>" + month + "</month><year>" + year + "</year><title>" + title + "</title><url>" + link + "</url></article>\n";
                                                            articleCount--;
                                                        }

                                                        try {
                                                            FileOutputStream stream = MainActivity.appContext.openFileOutput("aktuality.dat", Context.MODE_PRIVATE);
                                                            stream.write(newString.getBytes());
                                                            stream.close();
                                                        } catch (IOException e) {
                                                            Log.e("file", "WRITING FILE: " + e.toString());
                                                        }

                                                        fragmentReloader.reloadFragment("FragmentAktuality");
                                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.aktuality_updated), Toast.LENGTH_LONG).show();
                                                        aktualityUpdating = false;
                                                        //Response - END ----------------------------------------------------------------------------------
                                                    } else {
                                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_code, response.code()), Toast.LENGTH_LONG).show();
                                                        aktualityUpdating = false;
                                                    }
                                                } else {
                                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, response.errorBody().toString()), Toast.LENGTH_LONG).show();
                                                    aktualityUpdating = false;
                                                }
                                            }

                                            @Override
                                            public void onFailure(Call<String> call, Throwable t) {
                                                if (t instanceof UnknownHostException) {
                                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown_host), Toast.LENGTH_LONG).show();
                                                } else if (t instanceof SocketTimeoutException) {
                                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_timeout), Toast.LENGTH_LONG).show();
                                                } else {
                                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown), Toast.LENGTH_LONG).show();
                                                }
                                                aktualityUpdating = false;
                                            }
                                        });
                                        //Response - END ----------------------------------------------------------------------------------
                                    } else {
                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_code, response.code()), Toast.LENGTH_LONG).show();
                                        aktualityUpdating = false;
                                    }
                                } else {
                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, response.errorBody().toString()), Toast.LENGTH_LONG).show();
                                    aktualityUpdating = false;
                                }
                            }

                            @Override
                            public void onFailure(Call<String> call, Throwable t) {
                                if (t instanceof UnknownHostException) {
                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown_host), Toast.LENGTH_LONG).show();
                                } else if (t instanceof SocketTimeoutException) {
                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_timeout), Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown), Toast.LENGTH_LONG).show();
                                }
                                aktualityUpdating = false;
                            }
                        });
                    }
                } else if (CurrentFragment.contains("FragmentSuplovani")) {
                    if (suplovaniUpdating) {
                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.string_already_updating), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.aktuality_update), Toast.LENGTH_LONG).show();
                        suplovaniUpdating = true;

                        String accountsPath = MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "accounts.cfg";
                        File file = new File(accountsPath);
                        if (file.exists()) {
                            int file_length = (int) file.length();
                            byte[] file_bytes = new byte[file_length];
                            try {
                                FileInputStream file_stream = new FileInputStream(file);
                                int readResult = file_stream.read(file_bytes);
                                file_stream.close();
                                if (file_length != readResult) {
                                    Log.e("file", "READING FILE: " + readResult + "/" + file_length);
                                }
                            } catch (IOException e) {
                                Log.e("file", "READING FILE: " + e.toString());
                            }
                            String file_contents = new String(file_bytes);

                            if (file_contents.contains("<moodle_username>") && file_contents.contains("</moodle_username>")) {
                                String username = file_contents.substring(file_contents.indexOf("<moodle_username>") + "<moodle_username>".length(), file_contents.indexOf("</moodle_username>"));
                                String password = file_contents.substring(file_contents.indexOf("<moodle_password>") + "<moodle_password>".length(), file_contents.indexOf("</moodle_password>"));

                                String API_BASE_URL = "https://moodle3.gvid.cz/";
                                CookieManager cookieManager = new CookieManager();
                                cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
                                okHttpClient = new OkHttpClient();
                                OkHttpClient.Builder httpClient = okHttpClient.newBuilder();
                                httpClient.cookieJar(new JavaNetCookieJar(cookieManager));
                                httpClient.addInterceptor(new Interceptor() {
                                    @Override
                                    public okhttp3.Response intercept(Chain chain) throws IOException {
                                        Request original = chain.request();
                                        Request request = original.newBuilder()
                                                .header("Accept-Language", "cs")//Locale.getDefault().getDisplayLanguage()
                                                .method(original.method(), original.body())
                                                .build();
                                        return chain.proceed(request);
                                    }
                                });
                                Retrofit.Builder retrofitBuilder = new Retrofit.Builder().baseUrl(API_BASE_URL).addConverterFactory(ScalarsConverterFactory.create());
                                Retrofit retrofit = retrofitBuilder.client(httpClient.build()).build();
                                callClient = retrofit.create(HttpClient.class);

                                Call<String> call = callClient.postMoodleLogin(username, password);
                                call.enqueue(new Callback<String>() {
                                    @Override
                                    public void onResponse(Call<String> call, Response<String> response) {
                                        if (response.isSuccessful()) {
                                            if (response.code() == 200) {
                                                String htmlResponse = response.body();
                                                //Response - END ----------------------------------------------------------------------------------
                                                if (htmlResponse.contains("https://moodle3.gvid.cz/login/logout.php?sesskey=")) {
                                                    Call<String> call2 = callClient.getMoodleCourse("3");
                                                    call2.enqueue(new Callback<String>() {
                                                        @Override
                                                        public void onResponse(Call<String> call, final Response<String> response) {
                                                            if (response.isSuccessful()) {
                                                                if (response.code() == 200) {
                                                                    String htmlResponse = response.body();
                                                                    //Response - END ----------------------------------------------------------------------------------
                                                                    if (htmlResponse.contains("<input name=\"id\" type=\"hidden\" value=\"3\" />") && htmlResponse.contains("<input name=\"sesskey\" type=\"hidden\" value=")) {
                                                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_moodle_suply_not_registered), Toast.LENGTH_LONG).show();
                                                                        suplovaniUpdating = false;
                                                                    } else if (htmlResponse.contains("course-content")) {
                                                                        final Map<String, String> suplyFiles = new HashMap<>();
                                                                        while (htmlResponse.contains("<div class=\"forumpost clearfix firstpost starter\"")) {
                                                                            String result = htmlResponse.substring(htmlResponse.indexOf("<div class=\"forumpost clearfix firstpost starter\"") + "<div class=\"forumpost clearfix firstpost starter\"".length(), htmlResponse.indexOf(")</div></div></div></div>"));
                                                                            htmlResponse = htmlResponse.replace(htmlResponse.substring(htmlResponse.indexOf("<div class=\"forumpost clearfix firstpost starter\""), htmlResponse.indexOf(")</div></div></div></div>") + ")</div></div></div></div>".length()), "");
                                                                            //String title = result.substring(result.indexOf("<div class=\"subject\" role=\"heading\" aria-level=\"2\">")+"<div class=\"subject\" role=\"heading\" aria-level=\"2\">".length(),result.indexOf("</div><div class=\"author\" role=\"heading\" aria-level=\"2\">"));
                                                                            String dates = result.substring(result.indexOf("<div class=\"author\" role=\"heading\" aria-level=\"2\">") + "<div class=\"author\" role=\"heading\" aria-level=\"2\">".length(), result.indexOf("</div></div></div><div class=\"row maincontent clearfix\">"));
                                                                            String day = dates.substring(dates.indexOf(", ") + ", ".length());
                                                                            day = day.substring(0, day.indexOf(". "));
                                                                            String month = dates.substring(dates.indexOf(", " + day + ". ") + (", " + day + ". ").length());
                                                                            month = month.substring(0, month.indexOf(" "));
                                                                            String year = dates.substring(dates.indexOf(month + " ") + (month + " ").length());
                                                                            year = year.substring(0, year.indexOf(", "));
                                                                            String description = result.substring(result.indexOf("<div class=\"posting fullpost\"><p>") + "<div class=\"posting fullpost\"><p>".length());
                                                                            description = description.substring(0, description.indexOf("</p>"));
                                                                            description = stripHTML(description);
                                                                            String file = result.substring(result.indexOf("src=\"https://moodle3.gvid.cz/theme/image.php/more/core/1497606088/f/pdf\" /></a> <a href=\"") + "src=\"https://moodle3.gvid.cz/theme/image.php/more/core/1497606088/f/pdf\" /></a> <a href=\"".length());
                                                                            String fileName = file.substring(file.indexOf("\">") + "\">".length(), file.indexOf("</a>"));
                                                                            file = file.substring(0, file.indexOf("\">"));
                                                                            switch (month) {
                                                                                case "leden":
                                                                                    month = "01";
                                                                                    break;
                                                                                case "únor":
                                                                                    month = "02";
                                                                                    break;
                                                                                case "březen":
                                                                                    month = "03";
                                                                                    break;
                                                                                case "duben":
                                                                                    month = "04";
                                                                                    break;
                                                                                case "květen":
                                                                                    month = "05";
                                                                                    break;
                                                                                case "červen":
                                                                                    month = "06";
                                                                                    break;
                                                                                case "červenec":
                                                                                    month = "07";
                                                                                    break;
                                                                                case "srpen":
                                                                                    month = "08";
                                                                                    break;
                                                                                case "září":
                                                                                    month = "09";
                                                                                    break;
                                                                                case "říjen":
                                                                                    month = "10";
                                                                                    break;
                                                                                case "listopad":
                                                                                    month = "11";
                                                                                    break;
                                                                                case "prosinec":
                                                                                    month = "12";
                                                                                    break;
                                                                            }
                                                                            day = Integer.valueOf(day).toString();
                                                                            if (day.length() == 1) {
                                                                                day = "0" + day;
                                                                            }
                                                                            String pdfFileName = (year + month + day + "___" + description + "___" + fileName).replaceAll("/","-");
                                                                            suplyFiles.put(pdfFileName, file);
                                                                        }

                                                                        new DownloadSuply(suplyFiles).execute();
                                                                    } else {
                                                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_login_unsuccessful), Toast.LENGTH_LONG).show();
                                                                        suplovaniUpdating = false;
                                                                    }
                                                                    //Response - END ----------------------------------------------------------------------------------
                                                                } else {
                                                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_code, response.code()), Toast.LENGTH_LONG).show();
                                                                    suplovaniUpdating = false;
                                                                }
                                                            } else {
                                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, response.errorBody().toString()), Toast.LENGTH_LONG).show();
                                                                suplovaniUpdating = false;
                                                            }
                                                        }

                                                        @Override
                                                        public void onFailure(Call<String> call, Throwable t) {
                                                            if (t instanceof UnknownHostException) {
                                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown_host), Toast.LENGTH_LONG).show();
                                                            } else if (t instanceof SocketTimeoutException) {
                                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_timeout), Toast.LENGTH_LONG).show();
                                                            } else {
                                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown), Toast.LENGTH_LONG).show();
                                                            }
                                                            suplovaniUpdating = false;
                                                        }
                                                    });
                                                } else {
                                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_login_unsuccessful), Toast.LENGTH_LONG).show();
                                                    suplovaniUpdating = false;
                                                }
                                                //Response - END ----------------------------------------------------------------------------------
                                            } else {
                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_code, response.code()), Toast.LENGTH_LONG).show();
                                                suplovaniUpdating = false;
                                            }
                                        } else {
                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, response.errorBody().toString()), Toast.LENGTH_LONG).show();
                                            suplovaniUpdating = false;
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<String> call, Throwable t) {
                                        if (t instanceof UnknownHostException) {
                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown_host), Toast.LENGTH_LONG).show();
                                        } else if (t instanceof SocketTimeoutException) {
                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_timeout), Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown), Toast.LENGTH_LONG).show();
                                        }
                                        suplovaniUpdating = false;
                                    }
                                });
                            } else {
                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.aktuality_nologin), Toast.LENGTH_LONG).show();
                                suplovaniUpdating = false;
                            }
                        } else {
                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.aktuality_nologin), Toast.LENGTH_LONG).show();
                            suplovaniUpdating = false;
                        }
                    }
                } else if (CurrentFragment.contains("FragmentZnamky")) {
                    if (znamkyUpdating) {
                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.string_already_updating), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.aktuality_update), Toast.LENGTH_LONG).show();
                        znamkyUpdating = true;

                        String accountsPath = MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "accounts.cfg";
                        File file = new File(accountsPath);
                        if (file.exists()) {
                            int file_length = (int) file.length();
                            byte[] file_bytes = new byte[file_length];
                            try {
                                FileInputStream file_stream = new FileInputStream(file);
                                int readResult = file_stream.read(file_bytes);
                                file_stream.close();
                                if (file_length != readResult) {
                                    Log.e("file", "READING FILE: " + readResult + "/" + file_length);
                                }
                            } catch (IOException e) {
                                Log.e("file", "READING FILE: " + e.toString());
                            }
                            String file_contents = new String(file_bytes);

                            if (file_contents.contains("<sas_username>") && file_contents.contains("</sas_username>")) {
                                String username = file_contents.substring(file_contents.indexOf("<sas_username>") + "<sas_username>".length(), file_contents.indexOf("</sas_username>"));
                                String password = file_contents.substring(file_contents.indexOf("<sas_password>") + "<sas_password>".length(), file_contents.indexOf("</sas_password>"));

                                String API_BASE_URL = "https://aplikace.skolaonline.cz";
                                CookieManager cookieManager = new CookieManager();
                                cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
                                okHttpClient = new OkHttpClient();
                                OkHttpClient.Builder httpClient = okHttpClient.newBuilder();
                                httpClient.cookieJar(new JavaNetCookieJar(cookieManager));
                                httpClient.addInterceptor(new Interceptor() {
                                    @Override
                                    public okhttp3.Response intercept(Chain chain) throws IOException {
                                        Request original = chain.request();
                                        Request request = original.newBuilder()
                                                .header("Accept-Language", "cs")//Locale.getDefault().getDisplayLanguage()
                                                .method(original.method(), original.body())
                                                .build();
                                        return chain.proceed(request);
                                    }
                                });
                                Retrofit.Builder retrofitBuilder = new Retrofit.Builder().baseUrl(API_BASE_URL).addConverterFactory(ScalarsConverterFactory.create());
                                Retrofit retrofit = retrofitBuilder.client(httpClient.build()).build();
                                callClient = retrofit.create(HttpClient.class);

                                final ArrayList<String> items = new ArrayList<>();
                                final ArrayList<String> responses = new ArrayList<>();
                                Call<String> call = callClient.postSkolaOnlineLogin("dnn$ctr994$SOLLogin$btnODeslat","","","","","","","",username, password,"","","");
                                call.enqueue(new Callback<String>() {
                                    @Override
                                    public void onResponse(Call<String> call, Response<String> response) {
                                        if (response.isSuccessful()) {
                                            if (response.code() == 200) {
                                                String htmlResponse = response.body();
                                                //Response - END ----------------------------------------------------------------------------------
                                                if (htmlResponse.contains("Logout.aspx")) {
                                                    Call<String> call1 = callClient.getUrl("https://aplikace.skolaonline.cz/SOL/App/Hodnoceni/KZH001_HodnVypisStud.aspx");
                                                    call1.enqueue(new Callback<String>() {
                                                        @Override
                                                        public void onResponse(Call<String> call, Response<String> response) {
                                                            if (response.isSuccessful()) {
                                                                if (response.code() == 200) {
                                                                    String htmlResponse = response.body();
                                                                    //Response - END ----------------------------------------------------------------------------------
                                                                    if(htmlResponse.contains("<table id='G")) {
                                                                        String table = stringBetweenSubstrings(htmlResponse, "<table id='G", "</table>");
                                                                        String newContent = "<hodnoceni>";
                                                                        while (table.contains("<tr id='")) {
                                                                            String tableRow = stringBetweenSubstrings(table, "<tr id='", "</tr>");
                                                                            table = table.substring(table.indexOf(tableRow));

                                                                            String subject = stringBetweenSubstrings(tableRow,"height_transp.gif'>","</td>");
                                                                            String shortSubject;
                                                                            if(subject.contains("(")){
                                                                                shortSubject = stringBetweenSubstrings(subject,"(","").trim();
                                                                                subject = stringBetweenSubstrings(subject,"(",")").trim();
                                                                            } else {
                                                                                shortSubject = subject;
                                                                            }
                                                                            String average = stripHTML(stringBetweenSubstrings(tableRow,"VHCelkPrumer\">","</td>"));
                                                                            String finish = stripHTML(stringBetweenSubstrings(tableRow,"VHUzaverka\">","</td>"));
                                                                            if(finish.contains("<span")){
                                                                                finish = stripHTML(stringBetweenSubstrings(tableRow,"'>","</span>"));
                                                                            }

                                                                            if(!subject.equals("")) {
                                                                                String item = "<item><subject>" + shortSubject + "</subject><subjectlong>" + subject + "</subjectlong><average>" + average + "</average><half1></half1><half2>" + finish + "</half2></item>";
                                                                                newContent += item;
                                                                                items.add(item);
                                                                            }
                                                                        }
                                                                        newContent += "</hodnoceni>";
                                                                        responses.add(newContent);

                                                                        Call<String> call2 = callClient.getUrl("https://aplikace.skolaonline.cz/SOL/App/Hodnoceni/KZH003_PrubezneHodnoceni.aspx");
                                                                        call2.enqueue(new Callback<String>() {
                                                                            @Override
                                                                            public void onResponse(Call<String> call, Response<String> response) {
                                                                                if (response.isSuccessful()) {
                                                                                    if (response.code() == 200) {
                                                                                        String htmlResponse = response.body();
                                                                                        //Response - END ----------------------------------------------------------------------------------
                                                                                        if(htmlResponse.contains("<table id=")) {
                                                                                            String obdobi = stringBetweenSubstrings(htmlResponse, "<select name=\"ctl00$main$ddlObdobi\"", "</select>");
                                                                                            obdobi = stringBetweenSubstrings(obdobi, "<option selected=\"selected\"", "option>");
                                                                                            obdobi = fromHTML(stringBetweenSubstrings(obdobi, "\">", "</"));
                                                                                            System.out.println(obdobi);

                                                                                            String table = stringBetweenSubstrings(htmlResponse, "<table id=", "</table>");

                                                                                            String newContent = "<datum>";
                                                                                            while (table.contains("<tr id='")) {
                                                                                                String tableRow = stringBetweenSubstrings(table, "<tr id='", "</tr>");
                                                                                                table = table.substring(table.indexOf(tableRow));

                                                                                                String date = stringBetweenSubstrings(tableRow, "_3\"><nobr>", "</nobr>");
                                                                                                String subject = stringBetweenSubstrings(tableRow, "_4\"><nobr>", "</nobr>");
                                                                                                String description = stringBetweenSubstrings(tableRow, "_5\"><nobr>", "</nobr>");
                                                                                                String weight = stringBetweenSubstrings(tableRow, "_6\"><nobr>", "</nobr>");
                                                                                                String mark = stringBetweenSubstrings(tableRow, "_7\"><nobr>", "</nobr>");
                                                                                                String subjectShort = "";

                                                                                                for (int i = 0; i < items.size(); i++) {
                                                                                                    String item = items.get(i);
                                                                                                    if(item.contains(subject)){
                                                                                                        subjectShort = stringBetweenSubstrings(item,"<subject>","</subject>");
                                                                                                        i = items.size();
                                                                                                    }
                                                                                                }

                                                                                                newContent += "<item><date>" + date + "</date><subject>" + subjectShort + "</subject><mark>" + mark + "</mark><value></value><weight>" + weight + "</weight><description>" + description + "</description></item>";

                                                                                            }
                                                                                            newContent += "</datum>\n";

                                                                                            newContent += "<predmety>";
                                                                                            String subjects = responses.get(0);
                                                                                            String marks = stringBetweenSubstrings(newContent,"<datum>","</datum>");
                                                                                            while (subjects.contains("<item>")) {
                                                                                                String subjectRow = stringBetweenSubstrings(subjects, "<item>", "</item>");
                                                                                                subjects = subjects.substring(subjects.indexOf(subjectRow) + subjectRow.length());

                                                                                                String subjectshort = stringBetweenSubstrings(subjectRow,"<subject>","</subject>");
                                                                                                String average = stringBetweenSubstrings(subjectRow,"<average>","</average>");

                                                                                                if(containsNumber(average)) {
                                                                                                    String tempMarks = marks;
                                                                                                    while (tempMarks.contains("<item>")) {
                                                                                                        String markRow = stringBetweenSubstrings(tempMarks, "<item>", "</item>");
                                                                                                        tempMarks = tempMarks.substring(tempMarks.indexOf(markRow) + markRow.length());

                                                                                                        if (markRow.contains(subjectshort)) {
                                                                                                            newContent += "<item>" + markRow + "</item>";
                                                                                                        }
                                                                                                    }
                                                                                                    newContent += "<item2><subject>" + subjectshort + "</subject><average>" + average + "</average></item2>";
                                                                                                }
                                                                                            }
                                                                                            newContent += "</predmety>\n";

                                                                                            newContent += responses.get(0);

                                                                                            try {
                                                                                                FileOutputStream stream = MainActivity.appContext.openFileOutput("marks.dat", 0);
                                                                                                stream.write(newContent.getBytes());
                                                                                                stream.close();
                                                                                            } catch (IOException e) {
                                                                                                Log.e("file", "WRITING FILE: " + e.toString());
                                                                                            }
                                                                                            MainActivity.fragmentReloader.reloadFragment("FragmentZnamky");
                                                                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.aktuality_updated), Toast.LENGTH_LONG).show();
                                                                                            znamkyUpdating = false;
                                                                                        } else {
                                                                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_parsing), Toast.LENGTH_LONG).show();
                                                                                            znamkyUpdating = false;
                                                                                        }
                                                                                        //Response - END ----------------------------------------------------------------------------------
                                                                                    } else {
                                                                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_code, response.code()), Toast.LENGTH_LONG).show();
                                                                                        znamkyUpdating = false;
                                                                                    }
                                                                                } else {
                                                                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, response.errorBody().toString()), Toast.LENGTH_LONG).show();
                                                                                    znamkyUpdating = false;
                                                                                }
                                                                            }

                                                                            @Override
                                                                            public void onFailure(Call<String> call, Throwable t) {
                                                                                if (t instanceof UnknownHostException) {
                                                                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown_host), Toast.LENGTH_LONG).show();
                                                                                } else if (t instanceof SocketTimeoutException) {
                                                                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_timeout), Toast.LENGTH_LONG).show();
                                                                                } else {
                                                                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown), Toast.LENGTH_LONG).show();
                                                                                }
                                                                                znamkyUpdating = false;
                                                                            }
                                                                        });
                                                                    } else {
                                                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_parsing), Toast.LENGTH_LONG).show();
                                                                        znamkyUpdating = false;
                                                                    }
                                                                    //Response - END ----------------------------------------------------------------------------------
                                                                } else {
                                                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_code, response.code()), Toast.LENGTH_LONG).show();
                                                                    znamkyUpdating = false;
                                                                }
                                                            } else {
                                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, response.errorBody().toString()), Toast.LENGTH_LONG).show();
                                                                znamkyUpdating = false;
                                                            }
                                                        }

                                                        @Override
                                                        public void onFailure(Call<String> call, Throwable t) {
                                                            if (t instanceof UnknownHostException) {
                                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown_host), Toast.LENGTH_LONG).show();
                                                            } else if (t instanceof SocketTimeoutException) {
                                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_timeout), Toast.LENGTH_LONG).show();
                                                            } else {
                                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown), Toast.LENGTH_LONG).show();
                                                            }
                                                            znamkyUpdating = false;
                                                        }
                                                    });
                                                } else {
                                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_login_bad_data), Toast.LENGTH_LONG).show();
                                                    znamkyUpdating = false;
                                                }
                                                //Response - END ----------------------------------------------------------------------------------
                                            } else {
                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_code, response.code()), Toast.LENGTH_LONG).show();
                                                znamkyUpdating = false;
                                            }
                                        } else {
                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, response.errorBody().toString()), Toast.LENGTH_LONG).show();
                                            znamkyUpdating = false;
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<String> call, Throwable t) {
                                        if (t instanceof UnknownHostException) {
                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown_host), Toast.LENGTH_LONG).show();
                                        } else if (t instanceof SocketTimeoutException) {
                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_timeout), Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown), Toast.LENGTH_LONG).show();
                                        }
                                        znamkyUpdating = false;
                                    }
                                });
                            } else {
                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.aktuality_nologin), Toast.LENGTH_LONG).show();
                                znamkyUpdating = false;
                            }
                        } else {
                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.aktuality_nologin), Toast.LENGTH_LONG).show();
                            znamkyUpdating = false;
                        }
                    }
                } else if (CurrentFragment.contains("FragmentObedy")) {
                    if (obedyUpdating) {
                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.string_already_updating), Toast.LENGTH_LONG).show();
                    } else {
                        obedyUpdating = true;
                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.aktuality_update), Toast.LENGTH_LONG).show();

                        String accountsPath = MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "accounts.cfg";
                        File file = new File(accountsPath);
                        if (file.exists()) {
                            int file_length = (int) file.length();
                            byte[] file_bytes = new byte[file_length];
                            try {
                                FileInputStream file_stream = new FileInputStream(file);
                                int readResult = file_stream.read(file_bytes);
                                file_stream.close();
                                if (file_length != readResult) {
                                    Log.e("file", "READING FILE: " + readResult + "/" + file_length);
                                }
                            } catch (IOException e) {
                                Log.e("file", "READING FILE: " + e.toString());
                            }
                            String file_contents = new String(file_bytes);

                            if (file_contents.contains("<icanteen_username>") && file_contents.contains("</icanteen_username>")) {
                                final String username = file_contents.substring(file_contents.indexOf("<icanteen_username>") + "<icanteen_username>".length(), file_contents.indexOf("</icanteen_username>"));
                                final String password = file_contents.substring(file_contents.indexOf("<icanteen_password>") + "<icanteen_password>".length(), file_contents.indexOf("</icanteen_password>"));

                                String API_BASE_URL = "http://jidelna.gvid.cz/";
                                CookieManager cookieManager = new CookieManager();
                                cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
                                okHttpClient = new OkHttpClient();
                                OkHttpClient.Builder httpClient = okHttpClient.newBuilder();
                                httpClient.cookieJar(new JavaNetCookieJar(cookieManager));
                                httpClient.addInterceptor(new Interceptor() {
                                    @Override
                                    public okhttp3.Response intercept(Chain chain) throws IOException {
                                        Request original = chain.request();
                                        Request request = original.newBuilder()
                                                .header("Accept-Language", "cs")//Locale.getDefault().getDisplayLanguage()
                                                .method(original.method(), original.body())
                                                .build();
                                        return chain.proceed(request);
                                    }
                                });
                                Retrofit.Builder retrofitBuilder = new Retrofit.Builder().baseUrl(API_BASE_URL).addConverterFactory(ScalarsConverterFactory.create());
                                Retrofit retrofit = retrofitBuilder.client(httpClient.build()).build();
                                callClient = retrofit.create(HttpClient.class);

                                Call<String> call = callClient.getUrl("http://jidelna.gvid.cz");
                                call.enqueue(new Callback<String>() {
                                    @Override
                                    public void onResponse(Call<String> call, Response<String> response) {
                                        if (response.isSuccessful()) {
                                            if (response.code() == 200) {
                                                String htmlResponse = response.body();
                                                //Response - END ----------------------------------------------------------------------------------
                                                String hash = stringBetweenSubstrings(htmlResponse,"<input type=\"hidden\" name=\"_csrf\" value=\"","\"/>");

                                                Call<String> call1 = callClient.postiCanteenLogin(username, password, "false", "web", hash);
                                                call1.enqueue(new Callback<String>() {
                                                    @Override
                                                    public void onResponse(Call<String> call, Response<String> response) {
                                                        if (response.isSuccessful()) {
                                                            if (response.code() == 200) {
                                                                String htmlResponse = response.body();
                                                                //Response - END ----------------------------------------------------------------------------------
                                                                if (htmlResponse.contains("j_spring_security_logout")) {
                                                                    String firstName = MainActivity.stringBetweenSubstrings(htmlResponse, "<span id=\"top:status:firstName\">", "</span>");
                                                                    String lastName = MainActivity.stringBetweenSubstrings(htmlResponse, "<span id=\"top:status:lastName\">", "</span>");
                                                                    String credit = MainActivity.stringBetweenSubstrings(htmlResponse, "<span id=\"Kredit\" style=\"font-weight: bold; color: #99cc00;\">", " K");
                                                                    String consumption = MainActivity.stringBetweenSubstrings(htmlResponse, "eba</span>: <strong>", " K");
                                                                    String time = MainActivity.stringBetweenSubstrings(htmlResponse, "<span id=\"Clock\" style=\"font-weight: bold;\" >", "</span>");

                                                                    Calendar calendar = Calendar.getInstance();
                                                                    if (calendar.get(Calendar.HOUR_OF_DAY) > 12) {
                                                                        time = ((Integer.valueOf(time.substring(0, time.indexOf(":")))) + 12) + time.substring(time.indexOf(":"));
                                                                    }
                                                                    String day_name = "neznámý";
                                                                    int dayWeek = calendar.get(Calendar.DAY_OF_WEEK);
                                                                    if(dayWeek == Calendar.MONDAY) {
                                                                        day_name = "Pondělí";
                                                                    } else if(dayWeek == Calendar.TUESDAY) {
                                                                        day_name = "Úterý";
                                                                    } else if(dayWeek == Calendar.WEDNESDAY) {
                                                                        day_name = "Středa";
                                                                    } else if(dayWeek == Calendar.THURSDAY) {
                                                                        day_name = "Čtvrtek";
                                                                    } else if(dayWeek == Calendar.FRIDAY) {
                                                                        day_name = "Pátek";
                                                                    } else if(dayWeek == Calendar.SATURDAY) {
                                                                        day_name = "Sobota";
                                                                    } else if(dayWeek == Calendar.SUNDAY) {
                                                                        day_name = "Neděle";
                                                                    }
                                                                    String date = calendar.get(Calendar.DAY_OF_MONTH)+"."+(calendar.get(Calendar.MONTH)+1)+"."+calendar.get(Calendar.YEAR);


                                                                    final String lunchNewContent = "<lunch_info><firstName>" + firstName + "</firstName><lastName>" + lastName + "</lastName><credit>" + credit + "</credit><consumption>" + consumption + "</consumption><day>" + day_name + "</day><time>" + time + "</time><date>" + date + "</date></lunch_info>\n";
                                                                    String thisMonth = htmlResponse.substring(MainActivity.indexOfFromEnd(htmlResponse.substring(0, htmlResponse.indexOf("<div>1</div>")), "<td"));
                                                                    if (thisMonth.contains("<div>" + Integer.valueOf(date.substring(0, date.indexOf("."))) + "</div>")) {
                                                                        int year = Integer.valueOf(date.substring(MainActivity.indexOfFromEnd(date, ".") + 1));
                                                                        int month = Integer.valueOf(MainActivity.stringBetweenSubstrings(date, ".", "."));
                                                                        int day = Integer.valueOf(date.substring(0, date.indexOf(".")));
                                                                        final ArrayList<String> weekDates = new ArrayList<>();
                                                                        int KalDnesPos = thisMonth.indexOf("<div>" + Integer.valueOf(date.substring(0, date.indexOf("."))) + "</div>") + htmlResponse.indexOf(thisMonth);
                                                                        String stringWeek = htmlResponse.substring(MainActivity.indexOfFromEnd(htmlResponse.substring(0, KalDnesPos), "<tr>"), htmlResponse.substring(KalDnesPos).indexOf("</tr>") + KalDnesPos);
                                                                        if (day > Integer.valueOf(stringWeek.substring(MainActivity.indexOfFromEnd(stringWeek, "<div>") + "<div>".length(), MainActivity.indexOfFromEnd(stringWeek, "</div>")))) {
                                                                            month++;
                                                                            if (month == 13) {
                                                                                month = 1;
                                                                                year++;
                                                                            }
                                                                        }
                                                                        while (stringWeek.contains("</td>")) {
                                                                            String stringDay = stringWeek.substring(MainActivity.indexOfFromEnd(stringWeek, "<td"));
                                                                            stringWeek = stringWeek.substring(0, MainActivity.indexOfFromEnd(stringWeek, "<td"));
                                                                            day = Integer.valueOf(MainActivity.stringBetweenSubstrings(stringDay, "<div>", "</div>"));
                                                                            weekDates.add(year + "-" + month + "-" + day);
                                                                            if (day == 1) {
                                                                                month--;
                                                                                if (month == 0) {
                                                                                    year--;
                                                                                    month = 12;
                                                                                }
                                                                            }
                                                                        }
                                                                        Collections.reverse(weekDates);
                                                                        final Map<String, String> weekResponsesCount = new HashMap<>();
                                                                        for (int i = 0; i < weekDates.size(); i++) {
                                                                            Call<String> call2 = callClient.getUrl("http://jidelna.gvid.cz/faces/secured/db/dbJidelnicekOnDay.jsp?day=" + weekDates.get(i));
                                                                            call2.enqueue(new Callback<String>() {
                                                                                @Override
                                                                                public void onResponse(Call<String> call, final Response<String> response) {
                                                                                    if (response.isSuccessful()) {
                                                                                        if (response.code() == 200) {
                                                                                            String htmlResponse = response.body();
                                                                                            //Response - END ----------------------------------------------------------------------------------
                                                                                            String date = MainActivity.stringBetweenSubstrings(htmlResponse, "<span class=\"important\">", "</span>").trim();
                                                                                            String sortDate = date.substring(indexOfFromEnd(date, ".") + 1) + "" + stringBetweenSubstrings(date, ".", ".") + "" + date.substring(0, date.indexOf("."));

                                                                                            String day_name = "";
                                                                                            if (htmlResponse.contains("Pondělí")) {
                                                                                                day_name = "Pondělí";
                                                                                            } else if (htmlResponse.contains("Úterý")) {
                                                                                                day_name = "Úterý";
                                                                                            } else if (htmlResponse.contains("Středa")) {
                                                                                                day_name = "Středu";
                                                                                            } else if (htmlResponse.contains("Čtvrtek")) {
                                                                                                day_name = "Čtvrtek";
                                                                                            } else if (htmlResponse.contains("Pátek")) {
                                                                                                day_name = "Pátek";
                                                                                            } else if (htmlResponse.contains("Sobota")) {
                                                                                                day_name = "Sobotu";
                                                                                            } else if (htmlResponse.contains("Neděle")) {
                                                                                                day_name = "Neděli";
                                                                                            }
                                                                                            String weekDay = "<day><date>" + date + "</date><day_name>" + day_name + "</day_name><items>";
                                                                                            if (!htmlResponse.contains("Litujeme, ale na vybraný den nejsou zadána v jídelníčku žádná jídla.")) {
                                                                                                while (htmlResponse.contains("jidelnicekItem ")) {
                                                                                                    String oneItem;
                                                                                                    if (htmlResponse.substring(htmlResponse.indexOf("jidelnicekItem ") + "jidelnicekItem ".length()).contains("jidelnicekItem ")) {
                                                                                                        oneItem = MainActivity.stringBetweenSubstrings(htmlResponse, "<div class=\"jidelnicekItem ", "<div class=\"jidelnicekItem ");
                                                                                                        htmlResponse = htmlResponse.substring(0, htmlResponse.indexOf("jidelnicekItem ")) + htmlResponse.substring(htmlResponse.substring(htmlResponse.indexOf("jidelnicekItem ") + "jidelnicekItem ".length()).indexOf("<div class=\"jidelnicekItem "));
                                                                                                    } else {
                                                                                                        oneItem = htmlResponse.substring(htmlResponse.indexOf("jidelnicekItem "));
                                                                                                        htmlResponse = " ";
                                                                                                    }
                                                                                                    String foodName = MainActivity.stringBetweenSubstrings(oneItem, "<span style=\"min-width: 250px; display: inline-block; word-wrap: break-word; vertical-align: middle;\">", "<br>").trim();
                                                                                                    String foodType = MainActivity.stringBetweenSubstrings(oneItem, "<span class=\"smallBoldTitle button-link-align\">", "<");
                                                                                                    String foodStatus = MainActivity.stringBetweenSubstrings(oneItem, "<span class=\"button-link-align\">", "<");
                                                                                                    String foodUrl = MainActivity.stringBetweenSubstrings(oneItem, " ajaxOrder(this, '", "', '");
                                                                                                    if(!foodName.equals("")) {
                                                                                                        weekDay = weekDay + "<item><foodType>" + foodType + "</foodType><foodName>" + foodName + "</foodName><foodStatus>" + foodStatus + "</foodStatus><foodUrl>" + foodUrl + "</foodUrl></item>";
                                                                                                    }
                                                                                                }
                                                                                            } else {
                                                                                                weekDay = weekDay + "nothing";
                                                                                            }
                                                                                            weekDay = weekDay + "</items></day>";
                                                                                            weekResponsesCount.put(sortDate, weekDay);

                                                                                            if (weekDates.size() == weekResponsesCount.size()) {
                                                                                                ArrayList<String> sortedKeys = new ArrayList<>(weekResponsesCount.keySet());
                                                                                                Collections.sort(sortedKeys);
                                                                                                String lunchNewContent2 = lunchNewContent;
                                                                                                for (int i = 0; i < sortedKeys.size(); i++) {
                                                                                                    lunchNewContent2 += weekResponsesCount.get(sortedKeys.get(i)) + "\n";
                                                                                                }
                                                                                                try {
                                                                                                    FileOutputStream stream = MainActivity.appContext.openFileOutput("lunch_info.dat", 0);
                                                                                                    stream.write(lunchNewContent2.getBytes());
                                                                                                    stream.close();
                                                                                                } catch (IOException e) {
                                                                                                    Log.e("file", "WRITING FILE: " + e.toString());
                                                                                                }
                                                                                                MainActivity.fragmentReloader.reloadFragment("FragmentObedy");
                                                                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.aktuality_updated), Toast.LENGTH_LONG).show();
                                                                                                obedyUpdating = false;
                                                                                            }
                                                                                            //Response - END ----------------------------------------------------------------------------------
                                                                                        } else {
                                                                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_code, response.code()), Toast.LENGTH_LONG).show();
                                                                                            obedyUpdating = false;
                                                                                        }
                                                                                    } else {
                                                                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, response.errorBody().toString()), Toast.LENGTH_LONG).show();
                                                                                        obedyUpdating = false;
                                                                                    }
                                                                                }

                                                                                @Override
                                                                                public void onFailure(Call<String> call, Throwable t) {
                                                                                    if (t instanceof UnknownHostException) {
                                                                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown_host), Toast.LENGTH_LONG).show();
                                                                                    } else if (t instanceof SocketTimeoutException) {
                                                                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_timeout), Toast.LENGTH_LONG).show();
                                                                                    } else {
                                                                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown), Toast.LENGTH_LONG).show();
                                                                                    }
                                                                                    obedyUpdating = false;
                                                                                }
                                                                            });
                                                                        }

                                                                    } else {
                                                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, "1"), Toast.LENGTH_LONG).show();
                                                                        obedyUpdating = false;
                                                                    }
                                                                } else {
                                                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_login_unsuccessful), Toast.LENGTH_LONG).show();
                                                                    obedyUpdating = false;
                                                                }
                                                                //Response - END ----------------------------------------------------------------------------------
                                                            } else {
                                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_code, response.code()), Toast.LENGTH_LONG).show();
                                                                obedyUpdating = false;
                                                            }
                                                        } else {
                                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, response.errorBody().toString()), Toast.LENGTH_LONG).show();
                                                            obedyUpdating = false;
                                                        }
                                                    }

                                                    @Override
                                                    public void onFailure(Call<String> call, Throwable t) {
                                                        if (t instanceof UnknownHostException) {
                                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown_host), Toast.LENGTH_LONG).show();
                                                        } else if (t instanceof SocketTimeoutException) {
                                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_timeout), Toast.LENGTH_LONG).show();
                                                        } else {
                                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown), Toast.LENGTH_LONG).show();
                                                        }
                                                        obedyUpdating = false;
                                                    }
                                                });
                                                //Response - END ----------------------------------------------------------------------------------
                                            } else {
                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_code, response.code()), Toast.LENGTH_LONG).show();
                                                obedyUpdating = false;
                                            }
                                        } else {
                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, response.errorBody().toString()), Toast.LENGTH_LONG).show();
                                            obedyUpdating = false;
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<String> call, Throwable t) {
                                        if (t instanceof UnknownHostException) {
                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown_host), Toast.LENGTH_LONG).show();
                                        } else if (t instanceof SocketTimeoutException) {
                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_timeout), Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown), Toast.LENGTH_LONG).show();
                                        }
                                        obedyUpdating = false;
                                    }
                                });
                            } else {
                                LoadObedyNotLogged();
                            }
                        } else {
                            LoadObedyNotLogged();
                        }
                    }
                }
            }
        } else {
            onClickUpdateAll();
        }
    }

    public void LoadObedyNotLogged() {
        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.obedy_nologin), Toast.LENGTH_LONG).show();

        String API_BASE_URL = "http://jidelna.gvid.cz/";
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        okHttpClient = new OkHttpClient();
        OkHttpClient.Builder httpClient = okHttpClient.newBuilder();
        httpClient.cookieJar(new JavaNetCookieJar(cookieManager));
        httpClient.addInterceptor(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request original = chain.request();
                Request request = original.newBuilder()
                        .header("Accept-Language", "cs")//Locale.getDefault().getDisplayLanguage()
                        .method(original.method(), original.body())
                        .build();
                return chain.proceed(request);
            }
        });
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder().baseUrl(API_BASE_URL).addConverterFactory(ScalarsConverterFactory.create());
        Retrofit retrofit = retrofitBuilder.client(httpClient.build()).build();
        callClient = retrofit.create(HttpClient.class);

        Call<String> call = callClient.getUrl("http://jidelna.gvid.cz/faces/login.jsp");
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    if (response.code() == 200) {
                        String htmlResponse = response.body();
                        //Response - END ----------------------------------------------------------------------------------
                        String info = htmlResponse.substring(htmlResponse.indexOf("<div align=\"center\" class=\"topMenu textGrey noPrint\" style=\"position: relative; clear: both; z-index:1; text-align: center; margin-top: 10px\">")+"<div align=\"center\" class=\"topMenu textGrey noPrint\" style=\"position: relative; clear: both; z-index:1; text-align: center; margin-top: 10px\">".length());
                        info = stringBetweenSubstrings(info," | "," | ");
                        String info_date = info.substring(0,info.indexOf(" ")).replaceAll("-",".");
                        info_date = Integer.valueOf(info_date.substring(indexOfFromEnd(info_date,".")+1))+"."+Integer.valueOf(stringBetweenSubstrings(info_date,".","."))+"."+info_date.substring(0,info_date.indexOf("."));
                        String time = (info.substring(info.indexOf(" "),indexOfFromEnd(info,":"))+".").trim();
                        Calendar calendar = Calendar.getInstance();
                        if (calendar.get(Calendar.HOUR_OF_DAY) > 12) {
                            time = ((Integer.valueOf(time.substring(0, time.indexOf(":")))) + 12) + time.substring(time.indexOf(":"));
                        }
                        String day_name = "neznámý";
                        int dayWeek = calendar.get(Calendar.DAY_OF_WEEK);
                        if(dayWeek == Calendar.MONDAY) {
                            day_name = "Pondělí";
                        } else if(dayWeek == Calendar.TUESDAY) {
                            day_name = "Úterý";
                        } else if(dayWeek == Calendar.WEDNESDAY) {
                            day_name = "Středa";
                        } else if(dayWeek == Calendar.THURSDAY) {
                            day_name = "Čtvrtek";
                        } else if(dayWeek == Calendar.FRIDAY) {
                            day_name = "Pátek";
                        } else if(dayWeek == Calendar.SATURDAY) {
                            day_name = "Sobota";
                        } else if(dayWeek == Calendar.SUNDAY) {
                            day_name = "Neděle";
                        }

                        String fileContent = "<lunch_info><day>" + day_name + "</day><time>" + time + "</time><date>" + info_date + "</date></lunch_info>\n";
                        while(htmlResponse.contains("<div class=\"jidelnicekDen\">")) {
                            String lunchDay = stringBetweenSubstrings(htmlResponse,"<div class=\"jidelnicekDen\">","</div>\n    </div>");
                            htmlResponse = htmlResponse.substring(htmlResponse.indexOf(lunchDay)+lunchDay.length());

                            String date = stringBetweenSubstrings(lunchDay,"<span class=\"important\">","</span> - ").trim();
                            String day = stringBetweenSubstrings(lunchDay,"<span>","</span>").trim();
                            if (day.contains("Pondělí")) {
                                day = "Pondělí";
                            } else if (day.contains("Úterý")) {
                                day = "Úterý";
                            } else if (day.contains("Středa")) {
                                day = "Středu";
                            } else if (day.contains("Čtvrtek")) {
                                day = "Čtvrtek";
                            } else if (day.contains("Pátek")) {
                                day = "Pátek";
                            } else if (day.contains("Sobota")) {
                                day = "Sobotu";
                            } else if (day.contains("Neděle")) {
                                day = "Neděli";
                            }

                            fileContent += "<day><date>" + date + "</date><day_name>" + day + "</day_name><items>";
                            while(lunchDay.contains("<span class=\"smallBoldTitle\" style=\"color: #1b75bb;\">")) {
                                String lunchType = stringBetweenSubstrings(lunchDay,"<span class=\"smallBoldTitle\" style=\"color: #1b75bb;\">","</span>");
                                lunchDay = lunchDay.substring(lunchDay.indexOf(lunchType)+lunchType.length());
                                String lunchName = stringBetweenSubstrings(lunchDay,"</span> -- ","</div>").trim();
                                if(!lunchName.equals("")) {
                                    fileContent += "<item><foodType>" + lunchType + "</foodType><foodName>" + lunchName + "</foodName></item>";
                                }
                            }
                            fileContent += "</items></day>\n";
                        }

                        try {
                            FileOutputStream stream = MainActivity.appContext.openFileOutput("lunch_info.dat", 0);
                            stream.write(fileContent.getBytes());
                            stream.close();
                        } catch (IOException e) {
                            Log.e("file", "WRITING FILE: " + e.toString());
                        }
                        MainActivity.fragmentReloader.reloadFragment("FragmentObedy");
                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.aktuality_updated), Toast.LENGTH_LONG).show();
                        obedyUpdating = false;
                        //Response - END ----------------------------------------------------------------------------------
                    } else {
                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_code, response.code()), Toast.LENGTH_LONG).show();
                        obedyUpdating = false;
                    }
                } else {
                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, response.errorBody().toString()), Toast.LENGTH_LONG).show();
                    obedyUpdating = false;
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                if (t instanceof UnknownHostException) {
                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown_host), Toast.LENGTH_LONG).show();
                } else if (t instanceof SocketTimeoutException) {
                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_timeout), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_unknown), Toast.LENGTH_LONG).show();
                }
                obedyUpdating = false;
            }
        });
    }

    //comment-todo - UPDATE_ALL
    public void onClickUpdateAll() {
        new UpdateAll().execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class UpdateAll extends AsyncTask<Void, String, Integer> {

        private ProgressDialog progressdialog;
        private String returnString = "";
        private String resultString = "";

        @Override
        protected void onPreExecute() {
            setScreenOrientationLock();
            progressdialog = new ProgressDialog(MainActivity.this);
            progressdialog.setCancelable(false);
            progressdialog.setMessage("Aktualizace...");
            progressdialog.show();
        }

        @Override
        protected Integer doInBackground(Void... ignore) {
            int updatedCount = 0;
            publishProgress("Aktuality...");
            String newString = "";

            String API_BASE_URL = "https://www.gvid.cz/";
            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            okHttpClient = new OkHttpClient();
            OkHttpClient.Builder httpClient = okHttpClient.newBuilder();
            httpClient.cookieJar(new JavaNetCookieJar(cookieManager));
            httpClient.addInterceptor(new Interceptor() {
                @Override
                public okhttp3.Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .header("Accept-Language", "cs")//Locale.getDefault().getDisplayLanguage()
                            .method(original.method(), original.body())
                            .build();
                    return chain.proceed(request);
                }
            });
            Retrofit.Builder retrofitBuilder = new Retrofit.Builder().baseUrl(API_BASE_URL).addConverterFactory(ScalarsConverterFactory.create());
            Retrofit retrofit = retrofitBuilder.client(httpClient.build()).build();
            callClient = retrofit.create(HttpClient.class);

            Call<String> call = callClient.getUrl("http://www.gvid.cz/feed/");
            try {
                final Response<String> response = call.execute();
                if (response.isSuccessful()) {
                    if (response.code() == 200) {
                        String htmlResponse = response.body();
                        //Response - END ----------------------------------------------------------------------------------
                        int articleCount = 10;
                        while (htmlResponse.contains("<item>") && articleCount != 0) {
                            String result = htmlResponse.substring(htmlResponse.indexOf("<item>") + "<item>".length(), htmlResponse.indexOf("</item>"));
                            htmlResponse = htmlResponse.replace(htmlResponse.substring(htmlResponse.indexOf("<item>"), htmlResponse.indexOf("</item>") + "</item>".length()), "");
                            String title = result.substring(result.indexOf("<title>") + "<title>".length(), result.indexOf("</title>"));
                            String date = result.substring(result.indexOf("<pubDate>") + "<pubDate>".length(), result.indexOf("</pubDate>"));
                            String day = date.substring(5, 7);
                            String month = date.substring(8, 11);
                            switch (month) {
                                case "Jan":
                                    month = "ledna";
                                    break;
                                case "Feb":
                                    month = "února";
                                    break;
                                case "Mar":
                                    month = "března";
                                    break;
                                case "Apr":
                                    month = "dubna";
                                    break;
                                case "May":
                                    month = "května";
                                    break;
                                case "Jun":
                                    month = "června";
                                    break;
                                case "Jul":
                                    month = "července";
                                    break;
                                case "Aug":
                                    month = "srpna";
                                    break;
                                case "Sep":
                                    month = "září";
                                    break;
                                case "Oct":
                                    month = "října";
                                    break;
                                case "Nov":
                                    month = "listopadu";
                                    break;
                                case "Dec":
                                    month = "prosince";
                                    break;
                            }
                            String year = date.substring(12, 17);
                            String link = result.substring(result.indexOf("<link>") + "<link>".length(), result.indexOf("</link>"));
                            articleCount--;
                            newString += "<article><type>gvid</type><date>" + Integer.valueOf(day).toString() + ".</date><month>" + month + "</month><year>" + year + "</year><title>" + title + "</title><url>" + link + "</url></article>\n";
                        }
                        //Response - END ----------------------------------------------------------------------------------
                    } else {
                        returnString = appContext.getResources().getString(R.string.request_error_code, response.code());
                    }
                } else {
                    returnString = appContext.getResources().getString(R.string.request_error, response.errorBody().toString());
                }
            } catch (UnknownHostException e) {
                returnString = appContext.getResources().getString(R.string.request_error_unknown_host);
            } catch (SocketTimeoutException e) {
                returnString = appContext.getResources().getString(R.string.request_error_timeout);
            } catch (Exception e) {
                returnString = appContext.getResources().getString(R.string.request_error_unknown);
            }

            Call<String> call2 = callClient.getUrl("https://moodle3.gvid.cz/");
            try {
                final Response<String> response = call2.execute();
                if (response.isSuccessful()) {
                    if (response.code() == 200) {
                        String htmlResponse = response.body();
                        //Response - END ----------------------------------------------------------------------------------
                        int articleCount = 3;
                        while (htmlResponse.contains("<div class=\"forumpost clearfix firstpost starter\"") && articleCount != 0) {
                            String result = htmlResponse.substring(htmlResponse.indexOf("<div class=\"forumpost clearfix firstpost starter\"") + "<div class=\"forumpost clearfix firstpost starter\"".length(), htmlResponse.indexOf("</a></div></div></div></div>"));
                            htmlResponse = htmlResponse.replace(htmlResponse.substring(htmlResponse.indexOf("<div class=\"forumpost clearfix firstpost starter\""), htmlResponse.indexOf("</a></div></div></div></div>") + "</a></div></div></div></div>".length()), "");
                            String title = result.substring(result.indexOf("<div class=\"subject\" role=\"heading\" aria-level=\"2\">") + "<div class=\"subject\" role=\"heading\" aria-level=\"2\">".length(), result.indexOf("</div><div class=\"author\" role=\"heading\" aria-level=\"2\">"));
                            String dates = result.substring(result.indexOf("<div class=\"author\" role=\"heading\" aria-level=\"2\">") + "<div class=\"author\" role=\"heading\" aria-level=\"2\">".length(), result.indexOf("</div></div></div><div class=\"row maincontent clearfix\">"));
                            String day = dates.substring(dates.indexOf(", ") + ", ".length());
                            day = day.substring(0, day.indexOf(". "));
                            String month = dates.substring(dates.indexOf(", " + day + ". ") + (", " + day + ". ").length());
                            month = month.substring(0, month.indexOf(" "));
                            String year = dates.substring(dates.indexOf(month + " ") + (month + " ").length());
                            year = year.substring(0, year.indexOf(", "));
                            switch (month) {
                                case "leden":
                                    month = "ledna";
                                    break;
                                case "únor":
                                    month = "února";
                                    break;
                                case "březen":
                                    month = "března";
                                    break;
                                case "duben":
                                    month = "dubna";
                                    break;
                                case "květen":
                                    month = "května";
                                    break;
                                case "červen":
                                    month = "června";
                                    break;
                                case "červenec":
                                    month = "července";
                                    break;
                                case "srpen":
                                    month = "srpna";
                                    break;
                                case "září":
                                    month = "září";
                                    break;
                                case "říjen":
                                    month = "října";
                                    break;
                                case "listopad":
                                    month = "listopadu";
                                    break;
                                case "prosinec":
                                    month = "prosince";
                                    break;
                            }
                            String link = "https://moodle3.gvid.cz/";
                            articleCount--;
                            newString += "<article><type>moodle</type><date>" + Integer.valueOf(day).toString() + ".</date><month>" + month + "</month><year>" + year + "</year><title>" + title + "</title><url>" + link + "</url></article>\n";
                            articleCount--;
                        }
                        //Response - END ----------------------------------------------------------------------------------
                    } else {
                        returnString = appContext.getResources().getString(R.string.request_error_code, response.code());
                    }
                } else {
                    returnString = appContext.getResources().getString(R.string.request_error, response.errorBody().toString());
                }
            } catch (UnknownHostException e) {
                returnString = appContext.getResources().getString(R.string.request_error_unknown_host);
            } catch (SocketTimeoutException e) {
                returnString = appContext.getResources().getString(R.string.request_error_timeout);
            } catch (Exception e) {
                returnString = appContext.getResources().getString(R.string.request_error_unknown);
            }

            if(returnString.equals("")) {
                try {
                    FileOutputStream stream = MainActivity.appContext.openFileOutput("aktuality.dat", Context.MODE_PRIVATE);
                    stream.write(newString.getBytes());
                    stream.close();
                } catch (IOException e) {
                    Log.e("file", "WRITING FILE: " + e.toString());
                }
                updatedCount++;
            } else {
                resultString += "Aktuality: " + returnString + ";\n";
            }
            returnString = "";

            okHttpClient.dispatcher().cancelAll();

            publishProgress("Suplování...");

            String accountsPath = MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "accounts.cfg";
            File file = new File(accountsPath);
            if (file.exists()) {
                int file_length = (int) file.length();
                byte[] file_bytes = new byte[file_length];
                try {
                    FileInputStream file_stream = new FileInputStream(file);
                    int readResult = file_stream.read(file_bytes);
                    file_stream.close();
                    if (file_length != readResult) {
                        Log.e("file", "READING FILE: " + readResult + "/" + file_length);
                    }
                } catch (IOException e) {
                    Log.e("file", "READING FILE: " + e.toString());
                }
                String file_contents = new String(file_bytes);

                if (file_contents.contains("<moodle_username>") && file_contents.contains("</moodle_username>")) {
                    String username = file_contents.substring(file_contents.indexOf("<moodle_username>") + "<moodle_username>".length(), file_contents.indexOf("</moodle_username>"));
                    String password = file_contents.substring(file_contents.indexOf("<moodle_password>") + "<moodle_password>".length(), file_contents.indexOf("</moodle_password>"));

                    API_BASE_URL = "https://moodle3.gvid.cz/";
                    cookieManager = new CookieManager();
                    cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
                    okHttpClient = new OkHttpClient();
                    httpClient = okHttpClient.newBuilder();
                    httpClient.cookieJar(new JavaNetCookieJar(cookieManager));
                    httpClient.addInterceptor(new Interceptor() {
                        @Override
                        public okhttp3.Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();
                            Request request = original.newBuilder()
                                    .header("Accept-Language", "cs")//Locale.getDefault().getDisplayLanguage()
                                    .method(original.method(), original.body())
                                    .build();
                            return chain.proceed(request);
                        }
                    });
                    retrofitBuilder = new Retrofit.Builder().baseUrl(API_BASE_URL).addConverterFactory(ScalarsConverterFactory.create());
                    retrofit = retrofitBuilder.client(httpClient.build()).build();
                    callClient = retrofit.create(HttpClient.class);

                    Call<String> call3 = callClient.postMoodleLogin(username, password);
                    try {
                        Response<String> response = call3.execute();
                        if (response.isSuccessful()) {
                            if (response.code() == 200) {
                                String htmlResponse = response.body();
                                //Response - END ----------------------------------------------------------------------------------
                                if (htmlResponse.contains("https://moodle3.gvid.cz/login/logout.php?sesskey=")) {
                                    Call<String> call4 = callClient.getMoodleCourse("3");
                                    try {
                                        response = call4.execute();
                                        if (response.isSuccessful()) {
                                            if (response.code() == 200) {
                                                htmlResponse = response.body();
                                                //Response - END ----------------------------------------------------------------------------------
                                                if (htmlResponse.contains("<input name=\"id\" type=\"hidden\" value=\"3\" />") && htmlResponse.contains("<input name=\"sesskey\" type=\"hidden\" value=")) {
                                                    returnString = appContext.getResources().getString(R.string.request_error_moodle_suply_not_registered);
                                                } else if (htmlResponse.contains("course-content")) {
                                                    final Map<String, String> suplyFiles = new HashMap<>();
                                                    while (htmlResponse.contains("<div class=\"forumpost clearfix firstpost starter\"")) {
                                                        String result = htmlResponse.substring(htmlResponse.indexOf("<div class=\"forumpost clearfix firstpost starter\"") + "<div class=\"forumpost clearfix firstpost starter\"".length(), htmlResponse.indexOf(")</div></div></div></div>"));
                                                        htmlResponse = htmlResponse.replace(htmlResponse.substring(htmlResponse.indexOf("<div class=\"forumpost clearfix firstpost starter\""), htmlResponse.indexOf(")</div></div></div></div>") + ")</div></div></div></div>".length()), "");
                                                        //String title = result.substring(result.indexOf("<div class=\"subject\" role=\"heading\" aria-level=\"2\">")+"<div class=\"subject\" role=\"heading\" aria-level=\"2\">".length(),result.indexOf("</div><div class=\"author\" role=\"heading\" aria-level=\"2\">"));
                                                        String dates = result.substring(result.indexOf("<div class=\"author\" role=\"heading\" aria-level=\"2\">") + "<div class=\"author\" role=\"heading\" aria-level=\"2\">".length(), result.indexOf("</div></div></div><div class=\"row maincontent clearfix\">"));
                                                        String day = dates.substring(dates.indexOf(", ") + ", ".length());
                                                        day = day.substring(0, day.indexOf(". "));
                                                        String month = dates.substring(dates.indexOf(", " + day + ". ") + (", " + day + ". ").length());
                                                        month = month.substring(0, month.indexOf(" "));
                                                        String year = dates.substring(dates.indexOf(month + " ") + (month + " ").length());
                                                        year = year.substring(0, year.indexOf(", "));
                                                        String description = result.substring(result.indexOf("<div class=\"posting fullpost\"><p>") + "<div class=\"posting fullpost\"><p>".length());
                                                        description = description.substring(0, description.indexOf("</p>"));
                                                        description = stripHTML(description);
                                                        String fileUrl = result.substring(result.indexOf("src=\"https://moodle3.gvid.cz/theme/image.php/more/core/1497606088/f/pdf\" /></a> <a href=\"") + "src=\"https://moodle3.gvid.cz/theme/image.php/more/core/1497606088/f/pdf\" /></a> <a href=\"".length());
                                                        String fileName = fileUrl.substring(fileUrl.indexOf("\">") + "\">".length(), fileUrl.indexOf("</a>"));
                                                        fileUrl = fileUrl.substring(0, fileUrl.indexOf("\">"));
                                                        switch (month) {
                                                            case "leden":
                                                                month = "01";
                                                                break;
                                                            case "únor":
                                                                month = "02";
                                                                break;
                                                            case "březen":
                                                                month = "03";
                                                                break;
                                                            case "duben":
                                                                month = "04";
                                                                break;
                                                            case "květen":
                                                                month = "05";
                                                                break;
                                                            case "červen":
                                                                month = "06";
                                                                break;
                                                            case "červenec":
                                                                month = "07";
                                                                break;
                                                            case "srpen":
                                                                month = "08";
                                                                break;
                                                            case "září":
                                                                month = "09";
                                                                break;
                                                            case "říjen":
                                                                month = "10";
                                                                break;
                                                            case "listopad":
                                                                month = "11";
                                                                break;
                                                            case "prosinec":
                                                                month = "12";
                                                                break;
                                                        }
                                                        day = Integer.valueOf(day).toString();
                                                        if (day.length() == 1) {
                                                            day = "0" + day;
                                                        }
                                                        String pdfFileName = (year + month + day + "___" + description + "___" + fileName).replaceAll("/","-");
                                                        suplyFiles.put(pdfFileName, fileUrl);
                                                    }

                                                    String latestFiles = "";
                                                    for (final String key : suplyFiles.keySet()) {
                                                        final String value = suplyFiles.get(key);
                                                        latestFiles += key + ";";

                                                        File pdfFile = new File(Environment.getExternalStorageDirectory() + File.separator + "GvidApp" + File.separator + key);
                                                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.appContext);
                                                        if (!pdfFile.exists() || sharedPref.getBoolean("download_all_button", true)) {
                                                            //responsesErrorCount++;
                                                            //responsesCount++;
                                                            Call<ResponseBody> call5 = callClient.download(value);
                                                            try {
                                                                final Response<ResponseBody> responseB = call5.execute();
                                                                if (responseB.isSuccessful()) {
                                                                    if (responseB.code() == 200) {
                                                                        //Response - END ----------------------------------------------------------------------------------
                                                                        String sdPath = Environment.getExternalStorageDirectory() + File.separator;
                                                                        sdPath = sdPath + "GvidApp" + File.separator;
                                                                        File Dir = new File(sdPath);
                                                                        if (!Dir.isDirectory()) {
                                                                            if (!Dir.mkdirs()) {
                                                                                Log.e("dir", "Can not create dir");
                                                                            }
                                                                        }

                                                                        sdPath = sdPath + key;
                                                                        file = new File(sdPath);
                                                                        try {
                                                                            OutputStream output = new FileOutputStream(file);
                                                                            InputStream input = responseB.body().byteStream();
                                                                            byte[] buffer = new byte[1024];
                                                                            int read;

                                                                            while ((read = input.read(buffer)) != -1) {
                                                                                output.write(buffer, 0, read);
                                                                            }
                                                                            output.flush();
                                                                            output.close();
                                                                            input.close();
                                                                        } catch (IOException e) {
                                                                            e.printStackTrace();
                                                                        }
                                                                        //Response - END ----------------------------------------------------------------------------------
                                                                    } else {
                                                                        returnString = appContext.getResources().getString(R.string.request_error_code, responseB.code());
                                                                    }
                                                                } else {
                                                                    returnString = appContext.getResources().getString(R.string.request_error, responseB.errorBody().toString());
                                                                }
                                                            } catch (UnknownHostException e) {
                                                                returnString = appContext.getResources().getString(R.string.request_error_unknown_host);
                                                            } catch (SocketTimeoutException e) {
                                                                returnString = appContext.getResources().getString(R.string.request_error_timeout);
                                                            } catch (Exception e) {
                                                                returnString = appContext.getResources().getString(R.string.request_error_unknown);
                                                            }
                                                        }
                                                    }
                                                    String sdPath = Environment.getExternalStorageDirectory() + File.separator;
                                                    sdPath = sdPath + "GvidApp" + File.separator;
                                                    File Dir = new File(sdPath);
                                                    String sdPathOld = Environment.getExternalStorageDirectory() + File.separator;
                                                    sdPathOld = sdPathOld + "GvidApp" + File.separator + "old" + File.separator;
                                                    File oldDir = new File(sdPathOld);
                                                    if (!oldDir.isDirectory()) {
                                                        if (!oldDir.mkdirs()) {
                                                            Log.e("dir", "Can not create dir");
                                                        }
                                                    }

                                                    if (Dir.isDirectory()) {
                                                        File[] dirFiles = Dir.listFiles();
                                                        Arrays.sort(dirFiles);
                                                        for (File oldFile : dirFiles) {
                                                            String fileName = oldFile.getName();
                                                            if (!fileName.equals("old") && !latestFiles.contains(fileName)) {
                                                                moveFile(sdPath, fileName, sdPathOld);
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    returnString = appContext.getResources().getString(R.string.dialog_login_unsuccessful);
                                                }
                                                //Response - END ----------------------------------------------------------------------------------
                                            } else {
                                                returnString = appContext.getResources().getString(R.string.request_error_code, response.code());
                                            }
                                        } else {
                                            returnString = appContext.getResources().getString(R.string.request_error, response.errorBody().toString());
                                        }
                                    } catch (UnknownHostException e) {
                                        returnString = appContext.getResources().getString(R.string.request_error_unknown_host);
                                    } catch (SocketTimeoutException e) {
                                        returnString = appContext.getResources().getString(R.string.request_error_timeout);
                                    } catch (Exception e) {
                                        returnString = appContext.getResources().getString(R.string.request_error_unknown);
                                    }
                                } else {
                                    returnString = appContext.getResources().getString(R.string.dialog_login_unsuccessful);
                                }
                                //Response - END ----------------------------------------------------------------------------------
                            } else {
                                returnString = appContext.getResources().getString(R.string.request_error_code, response.code());
                            }
                        } else {
                            returnString = appContext.getResources().getString(R.string.request_error, response.errorBody().toString());
                        }
                    } catch (UnknownHostException e) {
                        returnString = appContext.getResources().getString(R.string.request_error_unknown_host);
                    } catch (SocketTimeoutException e) {
                        returnString = appContext.getResources().getString(R.string.request_error_timeout);
                    } catch (Exception e) {
                        returnString = appContext.getResources().getString(R.string.request_error_unknown);
                    }
                } else {
                    returnString = appContext.getResources().getString(R.string.aktuality_nologin);
                }
            } else {
                returnString = appContext.getResources().getString(R.string.aktuality_nologin);
            }

            if(returnString.equals("")) {
                updatedCount++;
            } else {
                resultString += "Suplování: " + returnString + ";\n";
            }
            returnString = "";

            okHttpClient.dispatcher().cancelAll();

            publishProgress("Známky...");

            accountsPath = MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "accounts.cfg";
            file = new File(accountsPath);
            if (file.exists()) {
                int file_length = (int) file.length();
                byte[] file_bytes = new byte[file_length];
                try {
                    FileInputStream file_stream = new FileInputStream(file);
                    int readResult = file_stream.read(file_bytes);
                    file_stream.close();
                    if (file_length != readResult) {
                        Log.e("file", "READING FILE: " + readResult + "/" + file_length);
                    }
                } catch (IOException e) {
                    Log.e("file", "READING FILE: " + e.toString());
                }
                String file_contents = new String(file_bytes);

                if (file_contents.contains("<sas_username>") && file_contents.contains("</sas_username>")) {
                    String username = file_contents.substring(file_contents.indexOf("<sas_username>") + "<sas_username>".length(), file_contents.indexOf("</sas_username>"));
                    String password = file_contents.substring(file_contents.indexOf("<sas_password>") + "<sas_password>".length(), file_contents.indexOf("</sas_password>"));

                    API_BASE_URL = "https://aplikace.skolaonline.cz";
                    cookieManager = new CookieManager();
                    cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
                    okHttpClient = new OkHttpClient();
                    httpClient = okHttpClient.newBuilder();
                    httpClient.cookieJar(new JavaNetCookieJar(cookieManager));
                    httpClient.addInterceptor(new Interceptor() {
                        @Override
                        public okhttp3.Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();
                            Request request = original.newBuilder()
                                    .header("Accept-Language", "cs")//Locale.getDefault().getDisplayLanguage()
                                    .method(original.method(), original.body())
                                    .build();
                            return chain.proceed(request);
                        }
                    });
                    retrofitBuilder = new Retrofit.Builder().baseUrl(API_BASE_URL).addConverterFactory(ScalarsConverterFactory.create());
                    retrofit = retrofitBuilder.client(httpClient.build()).build();
                    callClient = retrofit.create(HttpClient.class);

                    Call<String> call6 = callClient.postSkolaOnlineLogin("dnn$ctr994$SOLLogin$btnODeslat","","","","","","","",username, password,"","","");
                    try {
                        Response<String> response = call6.execute();
                        if (response.isSuccessful()) {
                            if (response.code() == 200) {
                                String htmlResponse = response.body();
                                //Response - END ----------------------------------------------------------------------------------
                                if (htmlResponse.contains("Logout.aspx")) {
                                    Call<String> call7 = callClient.getUrl("https://aplikace.skolaonline.cz/SOL/App/Hodnoceni/KZH001_HodnVypisStud.aspx");
                                    try {
                                        response = call7.execute();
                                        if (response.isSuccessful()) {
                                            if (response.code() == 200) {
                                                htmlResponse = response.body();
                                                //Response - END ----------------------------------------------------------------------------------
                                                if(htmlResponse.contains("<table id='G")) {
                                                    final ArrayList<String> items = new ArrayList<>();
                                                    final ArrayList<String> responses = new ArrayList<>();

                                                    String table = stringBetweenSubstrings(htmlResponse, "<table id='G", "</table>");
                                                    String newContent = "<hodnoceni>";
                                                    while (table.contains("<tr id='")) {
                                                        String tableRow = stringBetweenSubstrings(table, "<tr id='", "</tr>");
                                                        table = table.substring(table.indexOf(tableRow));

                                                        String subject = stringBetweenSubstrings(tableRow, "height_transp.gif'>", "</td>");
                                                        String shortSubject;
                                                        if (subject.contains("(")) {
                                                            shortSubject = stringBetweenSubstrings(subject, "(", "").trim();
                                                            subject = stringBetweenSubstrings(subject, "(", ")").trim();
                                                        } else {
                                                            shortSubject = subject;
                                                        }
                                                        String average = stripHTML(stringBetweenSubstrings(tableRow, "VHCelkPrumer\">", "</td>"));
                                                        String finish = stripHTML(stringBetweenSubstrings(tableRow, "VHUzaverka\">", "</td>"));
                                                        if (finish.contains("<span")) {
                                                            finish = stripHTML(stringBetweenSubstrings(tableRow, "'>", "</span>"));
                                                        }

                                                        if (!subject.equals("")) {
                                                            String item = "<item><subject>" + shortSubject + "</subject><subjectlong>" + subject + "</subjectlong><average>" + average + "</average><half1></half1><half2>" + finish + "</half2></item>";
                                                            newContent += item;
                                                            items.add(item);
                                                        }
                                                    }
                                                    newContent += "</hodnoceni>";
                                                    responses.add(newContent);

                                                    Call<String> call77 = callClient.getUrl("https://aplikace.skolaonline.cz/SOL/App/Hodnoceni/KZH003_PrubezneHodnoceni.aspx");
                                                    try {
                                                        response = call77.execute();
                                                        if (response.isSuccessful()) {
                                                            if (response.code() == 200) {
                                                                htmlResponse = response.body();
                                                                //Response - END ----------------------------------------------------------------------------------
                                                                if(htmlResponse.contains("<table id=")) {
                                                                    String obdobi = stringBetweenSubstrings(htmlResponse, "<select name=\"ctl00$main$ddlObdobi\"", "</select>");
                                                                    obdobi = stringBetweenSubstrings(obdobi, "<option selected=\"selected\"", "option>");
                                                                    obdobi = fromHTML(stringBetweenSubstrings(obdobi, "\">", "</"));
                                                                    System.out.println(obdobi);

                                                                    table = stringBetweenSubstrings(htmlResponse, "<table id=", "</table>");

                                                                    newContent = "<datum>";
                                                                    while (table.contains("<tr id='")) {
                                                                        String tableRow = stringBetweenSubstrings(table, "<tr id='", "</tr>");
                                                                        table = table.substring(table.indexOf(tableRow));

                                                                        String date = stringBetweenSubstrings(tableRow, "_3\"><nobr>", "</nobr>");
                                                                        String subject = stringBetweenSubstrings(tableRow, "_4\"><nobr>", "</nobr>");
                                                                        String description = stringBetweenSubstrings(tableRow, "_5\"><nobr>", "</nobr>");
                                                                        String weight = stringBetweenSubstrings(tableRow, "_6\"><nobr>", "</nobr>");
                                                                        String mark = stringBetweenSubstrings(tableRow, "_7\"><nobr>", "</nobr>");
                                                                        String subjectShort = "";

                                                                        for (int i = 0; i < items.size(); i++) {
                                                                            String item = items.get(i);
                                                                            if(item.contains(subject)){
                                                                                subjectShort = stringBetweenSubstrings(item,"<subject>","</subject>");
                                                                                i = items.size();
                                                                            }
                                                                        }

                                                                        newContent += "<item><date>" + date + "</date><subject>" + subjectShort + "</subject><mark>" + mark + "</mark><value></value><weight>" + weight + "</weight><description>" + description + "</description></item>";

                                                                    }
                                                                    newContent += "</datum>\n";

                                                                    newContent += "<predmety>";
                                                                    String subjects = responses.get(0);
                                                                    String marks = stringBetweenSubstrings(newContent,"<datum>","</datum>");
                                                                    while (subjects.contains("<item>")) {
                                                                        String subjectRow = stringBetweenSubstrings(subjects, "<item>", "</item>");
                                                                        subjects = subjects.substring(subjects.indexOf(subjectRow) + subjectRow.length());

                                                                        String subjectshort = stringBetweenSubstrings(subjectRow,"<subject>","</subject>");
                                                                        String average = stringBetweenSubstrings(subjectRow,"<average>","</average>");

                                                                        if(containsNumber(average)) {
                                                                            String tempMarks = marks;
                                                                            while (tempMarks.contains("<item>")) {
                                                                                String markRow = stringBetweenSubstrings(tempMarks, "<item>", "</item>");
                                                                                tempMarks = tempMarks.substring(tempMarks.indexOf(markRow) + markRow.length());

                                                                                if (markRow.contains(subjectshort)) {
                                                                                    newContent += "<item>" + markRow + "</item>";
                                                                                }
                                                                            }
                                                                            newContent += "<item2><subject>" + subjectshort + "</subject><average>" + average + "</average></item2>";
                                                                        }
                                                                    }
                                                                    newContent += "</predmety>\n";

                                                                    newContent += responses.get(0);

                                                                    try {
                                                                        FileOutputStream stream = MainActivity.appContext.openFileOutput("marks.dat", 0);
                                                                        stream.write(newContent.getBytes());
                                                                        stream.close();
                                                                    } catch (IOException e) {
                                                                        Log.e("file", "WRITING FILE: " + e.toString());
                                                                    }
                                                                    System.out.println("DONE");
                                                                } else {
                                                                    returnString = appContext.getResources().getString(R.string.request_error_parsing);
                                                                }
                                                                //Response - END ----------------------------------------------------------------------------------
                                                            } else {
                                                                returnString = appContext.getResources().getString(R.string.request_error_code, response.code());
                                                            }
                                                        } else {
                                                            returnString = appContext.getResources().getString(R.string.request_error, response.errorBody().toString());
                                                        }
                                                    } catch (UnknownHostException e) {
                                                        returnString = appContext.getResources().getString(R.string.request_error_unknown_host);
                                                    } catch (SocketTimeoutException e) {
                                                        returnString = appContext.getResources().getString(R.string.request_error_timeout);
                                                    } catch (Exception e) {
                                                        returnString = appContext.getResources().getString(R.string.request_error_unknown);
                                                    }
                                                } else {
                                                    returnString = appContext.getResources().getString(R.string.request_error_parsing);
                                                }
                                                //Response - END ----------------------------------------------------------------------------------
                                            } else {
                                                returnString = appContext.getResources().getString(R.string.request_error_code, response.code());
                                            }
                                        } else {
                                            returnString = appContext.getResources().getString(R.string.request_error, response.errorBody().toString());
                                        }
                                    } catch (UnknownHostException e) {
                                        returnString = appContext.getResources().getString(R.string.request_error_unknown_host);
                                    } catch (SocketTimeoutException e) {
                                        returnString = appContext.getResources().getString(R.string.request_error_timeout);
                                    } catch (Exception e) {
                                        returnString = appContext.getResources().getString(R.string.request_error_unknown);
                                    }
                                } else {
                                    returnString = appContext.getResources().getString(R.string.dialog_login_unsuccessful);
                                }
                                //Response - END ----------------------------------------------------------------------------------
                            } else {
                                returnString = appContext.getResources().getString(R.string.request_error_code, response.code());
                            }
                        } else {
                            returnString = appContext.getResources().getString(R.string.request_error, response.errorBody().toString());
                        }
                    } catch (UnknownHostException e) {
                        returnString = appContext.getResources().getString(R.string.request_error_unknown_host);
                    } catch (SocketTimeoutException e) {
                        returnString = appContext.getResources().getString(R.string.request_error_timeout);
                    } catch (Exception e) {
                        returnString = appContext.getResources().getString(R.string.request_error_unknown);
                    }
                } else {
                    returnString = appContext.getResources().getString(R.string.aktuality_nologin);
                }
            } else {
                returnString = appContext.getResources().getString(R.string.aktuality_nologin);
            }

            if(returnString.equals("")) {
                updatedCount++;
            } else {
                resultString += "Známky: " + returnString + ";\n";
            }
            returnString = "";

            okHttpClient.dispatcher().cancelAll();

            publishProgress("Obědy...");

            accountsPath = MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "accounts.cfg";
            file = new File(accountsPath);
            if (file.exists()) {
                int file_length = (int) file.length();
                byte[] file_bytes = new byte[file_length];
                try {
                    FileInputStream file_stream = new FileInputStream(file);
                    int readResult = file_stream.read(file_bytes);
                    file_stream.close();
                    if (file_length != readResult) {
                        Log.e("file", "READING FILE: " + readResult + "/" + file_length);
                    }
                } catch (IOException e) {
                    Log.e("file", "READING FILE: " + e.toString());
                }
                String file_contents = new String(file_bytes);

                if (file_contents.contains("<icanteen_username>") && file_contents.contains("</icanteen_username>")) {
                    String username = file_contents.substring(file_contents.indexOf("<icanteen_username>") + "<icanteen_username>".length(), file_contents.indexOf("</icanteen_username>"));
                    String password = file_contents.substring(file_contents.indexOf("<icanteen_password>") + "<icanteen_password>".length(), file_contents.indexOf("</icanteen_password>"));

                    API_BASE_URL = "http://jidelna.gvid.cz/";
                    cookieManager = new CookieManager();
                    cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
                    okHttpClient = new OkHttpClient();
                    httpClient = okHttpClient.newBuilder();
                    httpClient.cookieJar(new JavaNetCookieJar(cookieManager));
                    httpClient.addInterceptor(new Interceptor() {
                        @Override
                        public okhttp3.Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();
                            Request request = original.newBuilder()
                                    .header("Accept-Language", "cs")//Locale.getDefault().getDisplayLanguage()
                                    .method(original.method(), original.body())
                                    .build();
                            return chain.proceed(request);
                        }
                    });
                    retrofitBuilder = new Retrofit.Builder().baseUrl(API_BASE_URL).addConverterFactory(ScalarsConverterFactory.create());
                    retrofit = retrofitBuilder.client(httpClient.build()).build();
                    callClient = retrofit.create(HttpClient.class);

                    Call<String> call88 = callClient.getUrl("http://jidelna.gvid.cz");
                    try {
                        Response<String> response = call88.execute();
                        if (response.isSuccessful()) {
                            if (response.code() == 200) {
                                String htmlResponse = response.body();
                                //Response - END ----------------------------------------------------------------------------------
                                String hash = stringBetweenSubstrings(htmlResponse,"<input type=\"hidden\" name=\"_csrf\" value=\"","\"/>");
                                Call<String> call8 = callClient.postiCanteenLogin(username, password, "false", "web", hash);
                                try {
                                    response = call8.execute();
                                    if (response.isSuccessful()) {
                                        if (response.code() == 200) {
                                            htmlResponse = response.body();
                                            //Response - END ----------------------------------------------------------------------------------
                                            if (htmlResponse.contains("j_spring_security_logout")) {
                                                String firstName = MainActivity.stringBetweenSubstrings(htmlResponse, "<span id=\"top:status:firstName\">", "</span>");
                                                String lastName = MainActivity.stringBetweenSubstrings(htmlResponse, "<span id=\"top:status:lastName\">", "</span>");
                                                String credit = MainActivity.stringBetweenSubstrings(htmlResponse, "<span id=\"Kredit\" style=\"font-weight: bold; color: #99cc00;\">", " K");
                                                String consumption = MainActivity.stringBetweenSubstrings(htmlResponse, "eba</span>: <strong>", " K");
                                                String time = MainActivity.stringBetweenSubstrings(htmlResponse, "<span id=\"Clock\" style=\"font-weight: bold;\" >", "</span>");

                                                Calendar calendar = Calendar.getInstance();
                                                if (calendar.get(Calendar.HOUR_OF_DAY) > 12) {
                                                    time = ((Integer.valueOf(time.substring(0, time.indexOf(":")))) + 12) + time.substring(time.indexOf(":"));
                                                }
                                                String day_name = "neznámý";
                                                int dayWeek = calendar.get(Calendar.DAY_OF_WEEK);
                                                if(dayWeek == Calendar.MONDAY) {
                                                    day_name = "Pondělí";
                                                } else if(dayWeek == Calendar.TUESDAY) {
                                                    day_name = "Úterý";
                                                } else if(dayWeek == Calendar.WEDNESDAY) {
                                                    day_name = "Středa";
                                                } else if(dayWeek == Calendar.THURSDAY) {
                                                    day_name = "Čtvrtek";
                                                } else if(dayWeek == Calendar.FRIDAY) {
                                                    day_name = "Pátek";
                                                } else if(dayWeek == Calendar.SATURDAY) {
                                                    day_name = "Sobota";
                                                } else if(dayWeek == Calendar.SUNDAY) {
                                                    day_name = "Neděle";
                                                }
                                                String date = calendar.get(Calendar.DAY_OF_MONTH)+"."+(calendar.get(Calendar.MONTH)+1)+"."+calendar.get(Calendar.YEAR);

                                                final String lunchNewContent = "<lunch_info><firstName>" + firstName + "</firstName><lastName>" + lastName + "</lastName><credit>" + credit + "</credit><consumption>" + consumption + "</consumption><day>" + day_name + "</day><time>" + time + "</time><date>" + date + "</date></lunch_info>\n";
                                                String thisMonth = htmlResponse.substring(MainActivity.indexOfFromEnd(htmlResponse.substring(0, htmlResponse.indexOf("<div>1</div>")), "<td"));
                                                if (thisMonth.contains("<div>" + Integer.valueOf(date.substring(0, date.indexOf("."))) + "</div>")) {
                                                    int year = Integer.valueOf(date.substring(MainActivity.indexOfFromEnd(date, ".") + 1));
                                                    int month = Integer.valueOf(MainActivity.stringBetweenSubstrings(date, ".", "."));
                                                    int day = Integer.valueOf(date.substring(0, date.indexOf(".")));
                                                    final ArrayList<String> weekDates = new ArrayList<>();
                                                    int KalDnesPos = thisMonth.indexOf("<div>" + Integer.valueOf(date.substring(0, date.indexOf("."))) + "</div>") + htmlResponse.indexOf(thisMonth);
                                                    String stringWeek = htmlResponse.substring(MainActivity.indexOfFromEnd(htmlResponse.substring(0, KalDnesPos), "<tr>"), htmlResponse.substring(KalDnesPos).indexOf("</tr>") + KalDnesPos);
                                                    if (day > Integer.valueOf(stringWeek.substring(MainActivity.indexOfFromEnd(stringWeek, "<div>") + "<div>".length(), MainActivity.indexOfFromEnd(stringWeek, "</div>")))) {
                                                        month++;
                                                        if (month == 13) {
                                                            month = 1;
                                                            year++;
                                                        }
                                                    }
                                                    while (stringWeek.contains("</td>")) {
                                                        String stringDay = stringWeek.substring(MainActivity.indexOfFromEnd(stringWeek, "<td"));
                                                        stringWeek = stringWeek.substring(0, MainActivity.indexOfFromEnd(stringWeek, "<td"));
                                                        day = Integer.valueOf(MainActivity.stringBetweenSubstrings(stringDay, "<div>", "</div>"));
                                                        weekDates.add(year + "-" + month + "-" + day);
                                                        if (day == 1) {
                                                            month--;
                                                            if (month == 0) {
                                                                year--;
                                                                month = 12;
                                                            }
                                                        }
                                                    }
                                                    Collections.reverse(weekDates);
                                                    final Map<String, String> weekResponsesCount = new HashMap<>();
                                                    for (int i = 0; i < weekDates.size(); i++) {
                                                        Call<String> call9 = callClient.getUrl("http://jidelna.gvid.cz/faces/secured/db/dbJidelnicekOnDay.jsp?day=" + weekDates.get(i));
                                                        try {
                                                            response = call9.execute();
                                                            if (response.isSuccessful()) {
                                                                if (response.code() == 200) {
                                                                    htmlResponse = response.body();
                                                                    //Response - END ----------------------------------------------------------------------------------
                                                                    date = MainActivity.stringBetweenSubstrings(htmlResponse, "<span class=\"important\">", "</span>").trim();
                                                                    day_name = "";
                                                                    if (htmlResponse.contains("Pondělí")) {
                                                                        day_name = "Pondělí";
                                                                    } else if (htmlResponse.contains("Úterý")) {
                                                                        day_name = "Úterý";
                                                                    } else if (htmlResponse.contains("Středa")) {
                                                                        day_name = "Středu";
                                                                    } else if (htmlResponse.contains("Čtvrtek")) {
                                                                        day_name = "Čtvrtek";
                                                                    } else if (htmlResponse.contains("Pátek")) {
                                                                        day_name = "Pátek";
                                                                    } else if (htmlResponse.contains("Sobota")) {
                                                                        day_name = "Sobotu";
                                                                    } else if (htmlResponse.contains("Neděle")) {
                                                                        day_name = "Neděli";
                                                                    }
                                                                    String weekDay = "<day><date>" + date + "</date><day_name>" + day_name + "</day_name><items>";
                                                                    if (!htmlResponse.contains("Litujeme, ale na vybraný den nejsou zadána v jídelníčku žádná jídla.")) {
                                                                        while (htmlResponse.contains("jidelnicekItem ")) {
                                                                            String oneItem;
                                                                            if (htmlResponse.substring(htmlResponse.indexOf("jidelnicekItem ") + "jidelnicekItem ".length()).contains("jidelnicekItem ")) {
                                                                                oneItem = MainActivity.stringBetweenSubstrings(htmlResponse, "<div class=\"jidelnicekItem ", "<div class=\"jidelnicekItem ");
                                                                                htmlResponse = htmlResponse.substring(0, htmlResponse.indexOf("jidelnicekItem ")) + htmlResponse.substring(htmlResponse.substring(htmlResponse.indexOf("jidelnicekItem ") + "jidelnicekItem ".length()).indexOf("<div class=\"jidelnicekItem "));
                                                                            } else {
                                                                                oneItem = htmlResponse.substring(htmlResponse.indexOf("jidelnicekItem "));
                                                                                htmlResponse = " ";
                                                                            }
                                                                            String foodName = MainActivity.stringBetweenSubstrings(oneItem, "<span style=\"min-width: 250px; display: inline-block; word-wrap: break-word; vertical-align: middle;\">", "<br>").trim();
                                                                            String foodType = MainActivity.stringBetweenSubstrings(oneItem, "<span class=\"smallBoldTitle button-link-align\">", "<");
                                                                            String foodStatus = MainActivity.stringBetweenSubstrings(oneItem, "<span class=\"button-link-align\">", "<");
                                                                            String foodUrl = MainActivity.stringBetweenSubstrings(oneItem, " ajaxOrder(this, '", "', '");
                                                                            if(!foodName.equals("")) {
                                                                                weekDay = weekDay + "<item><foodType>" + foodType + "</foodType><foodName>" + foodName + "</foodName><foodStatus>" + foodStatus + "</foodStatus><foodUrl>" + foodUrl + "</foodUrl></item>";
                                                                            }
                                                                        }
                                                                    } else {
                                                                        weekDay = weekDay + "nothing";
                                                                    }
                                                                    weekDay = weekDay + "</items></day>";
                                                                    String sortDate = date.substring(MainActivity.indexOfFromEnd(date, ".") + 1) + MainActivity.stringBetweenSubstrings(date, ".", ".") + date.substring(0, date.indexOf("."));
                                                                    weekResponsesCount.put(sortDate, weekDay);
                                                                    if (weekDates.size() == weekResponsesCount.size()) {
                                                                        ArrayList<String> sortedKeys = new ArrayList<>(weekResponsesCount.keySet());
                                                                        Collections.sort(sortedKeys);
                                                                        String lunchNewContent2 = lunchNewContent;
                                                                        for (i = 0; i < sortedKeys.size(); i++) {
                                                                            lunchNewContent2 = lunchNewContent2 + weekResponsesCount.get(sortedKeys.get(i)) + "\n";
                                                                        }
                                                                        try {
                                                                            FileOutputStream stream = MainActivity.appContext.openFileOutput("lunch_info.dat", 0);
                                                                            stream.write(lunchNewContent2.getBytes());
                                                                            stream.close();
                                                                        } catch (IOException e) {
                                                                            Log.e("file", "WRITING FILE: " + e.toString());
                                                                        }
                                                                    }
                                                                    //Response - END ----------------------------------------------------------------------------------
                                                                } else {
                                                                    returnString = appContext.getResources().getString(R.string.request_error_code, response.code());
                                                                }
                                                            } else {
                                                                returnString = appContext.getResources().getString(R.string.request_error, response.errorBody().toString());
                                                            }
                                                        } catch (UnknownHostException e) {
                                                            returnString = appContext.getResources().getString(R.string.request_error_unknown_host);
                                                        } catch (SocketTimeoutException e) {
                                                            returnString = appContext.getResources().getString(R.string.request_error_timeout);
                                                        } catch (Exception e) {
                                                            returnString = appContext.getResources().getString(R.string.request_error_unknown);
                                                        }
                                                    }
                                                } else {
                                                    returnString = appContext.getResources().getString(R.string.request_error, "1");
                                                }
                                            } else {
                                                returnString = appContext.getResources().getString(R.string.dialog_login_unsuccessful);
                                            }
                                            //Response - END ----------------------------------------------------------------------------------
                                        } else {
                                            returnString = appContext.getResources().getString(R.string.request_error_code, response.code());
                                        }
                                    } else {
                                        returnString = appContext.getResources().getString(R.string.request_error, response.errorBody().toString());
                                    }
                                } catch (UnknownHostException e) {
                                    returnString = appContext.getResources().getString(R.string.request_error_unknown_host);
                                } catch (SocketTimeoutException e) {
                                    returnString = appContext.getResources().getString(R.string.request_error_timeout);
                                } catch (Exception e) {
                                    returnString = appContext.getResources().getString(R.string.request_error_unknown);
                                }
                                //Response - END ----------------------------------------------------------------------------------
                            } else {
                                returnString = appContext.getResources().getString(R.string.request_error_code, response.code());
                            }
                        } else {
                            returnString = appContext.getResources().getString(R.string.request_error, response.errorBody().toString());
                        }
                    } catch (UnknownHostException e) {
                        returnString = appContext.getResources().getString(R.string.request_error_unknown_host);
                    } catch (SocketTimeoutException e) {
                        returnString = appContext.getResources().getString(R.string.request_error_timeout);
                    } catch (Exception e) {
                        returnString = appContext.getResources().getString(R.string.request_error_unknown);
                    }

                } else {
                    returnString = appContext.getResources().getString(R.string.obedy_nologin);

                    Call<String> call10 = callClient.getUrl("http://jidelna.gvid.cz/faces/login.jsp");
                    try {
                        final Response<String> response = call10.execute();
                        if (response.isSuccessful()) {
                            if (response.code() == 200) {
                                String htmlResponse = response.body();
                                //Response - END ----------------------------------------------------------------------------------
                                String info = htmlResponse.substring(htmlResponse.indexOf("<div align=\"center\" class=\"topMenu textGrey noPrint\" style=\"position: relative; clear: both; z-index:1; text-align: center; margin-top: 10px\">")+"<div align=\"center\" class=\"topMenu textGrey noPrint\" style=\"position: relative; clear: both; z-index:1; text-align: center; margin-top: 10px\">".length());
                                info = stringBetweenSubstrings(info," | "," | ");
                                String info_date = info.substring(0,info.indexOf(" ")).replaceAll("-",".");
                                info_date = Integer.valueOf(info_date.substring(indexOfFromEnd(info_date,".")+1))+"."+Integer.valueOf(stringBetweenSubstrings(info_date,".","."))+"."+info_date.substring(0,info_date.indexOf("."));
                                String time = (info.substring(info.indexOf(" "),indexOfFromEnd(info,":"))+".").trim();
                                Calendar calendar = Calendar.getInstance();
                                if (calendar.get(Calendar.HOUR_OF_DAY) > 12) {
                                    time = ((Integer.valueOf(time.substring(0, time.indexOf(":")))) + 12) + time.substring(time.indexOf(":"));
                                }
                                String day_name = "neznámý";
                                int dayWeek = calendar.get(Calendar.DAY_OF_WEEK);
                                if(dayWeek == Calendar.MONDAY) {
                                    day_name = "Pondělí";
                                } else if(dayWeek == Calendar.TUESDAY) {
                                    day_name = "Úterý";
                                } else if(dayWeek == Calendar.WEDNESDAY) {
                                    day_name = "Středa";
                                } else if(dayWeek == Calendar.THURSDAY) {
                                    day_name = "Čtvrtek";
                                } else if(dayWeek == Calendar.FRIDAY) {
                                    day_name = "Pátek";
                                } else if(dayWeek == Calendar.SATURDAY) {
                                    day_name = "Sobota";
                                } else if(dayWeek == Calendar.SUNDAY) {
                                    day_name = "Neděle";
                                }

                                String fileContent = "<lunch_info><day>" + day_name + "</day><time>" + time + "</time><date>" + info_date + "</date></lunch_info>\n";
                                while(htmlResponse.contains("<div class=\"jidelnicekDen\">")) {
                                    String lunchDay = stringBetweenSubstrings(htmlResponse,"<div class=\"jidelnicekDen\">","</div>\n    </div>");
                                    htmlResponse = htmlResponse.substring(htmlResponse.indexOf(lunchDay)+lunchDay.length());

                                    String date = stringBetweenSubstrings(lunchDay,"<span class=\"important\">","</span> - ").trim();
                                    String day = stringBetweenSubstrings(lunchDay,"<span>","</span>").trim();
                                    if (day.contains("Pondělí")) {
                                        day = "Pondělí";
                                    } else if (day.contains("Úterý")) {
                                        day = "Úterý";
                                    } else if (day.contains("Středa")) {
                                        day = "Středu";
                                    } else if (day.contains("Čtvrtek")) {
                                        day = "Čtvrtek";
                                    } else if (day.contains("Pátek")) {
                                        day = "Pátek";
                                    } else if (day.contains("Sobota")) {
                                        day = "Sobotu";
                                    } else if (day.contains("Neděle")) {
                                        day = "Neděli";
                                    }

                                    fileContent += "<day><date>" + date + "</date><day_name>" + day + "</day_name><items>";
                                    while(lunchDay.contains("<span class=\"smallBoldTitle\" style=\"color: #1b75bb;\">")) {
                                        String lunchType = stringBetweenSubstrings(lunchDay,"<span class=\"smallBoldTitle\" style=\"color: #1b75bb;\">","</span>");
                                        lunchDay = lunchDay.substring(lunchDay.indexOf(lunchType)+lunchType.length());
                                        String lunchName = stringBetweenSubstrings(lunchDay,"</span> -- ","</div>").trim();
                                        if(!lunchName.equals("")) {
                                            fileContent = fileContent + "<item><foodType>" + lunchType + "</foodType><foodName>" + lunchName + "</foodName></item>";
                                        }
                                    }
                                    fileContent += "</items></day>\n";
                                }

                                try {
                                    FileOutputStream stream = MainActivity.appContext.openFileOutput("lunch_info.dat", 0);
                                    stream.write(fileContent.getBytes());
                                    stream.close();
                                } catch (IOException e) {
                                    Log.e("file", "WRITING FILE: " + e.toString());
                                }
                                //Response - END ----------------------------------------------------------------------------------
                            } else {
                                returnString = appContext.getResources().getString(R.string.request_error_code, response.code());
                            }
                        } else {
                            returnString = appContext.getResources().getString(R.string.request_error, response.errorBody().toString());
                        }
                    } catch (UnknownHostException e) {
                        returnString = appContext.getResources().getString(R.string.request_error_unknown_host);
                    } catch (SocketTimeoutException e) {
                        returnString = appContext.getResources().getString(R.string.request_error_timeout);
                    } catch (Exception e) {
                        returnString = appContext.getResources().getString(R.string.request_error_unknown);
                    }
                }
            } else {
                returnString = appContext.getResources().getString(R.string.obedy_nologin);

                Call<String> call11 = callClient.getUrl("http://jidelna.gvid.cz/faces/login.jsp");
                try {
                    final Response<String> response = call11.execute();
                    if (response.isSuccessful()) {
                        if (response.code() == 200) {
                            String htmlResponse = response.body();
                            //Response - END ----------------------------------------------------------------------------------
                            String info = htmlResponse.substring(htmlResponse.indexOf("<div align=\"center\" class=\"topMenu textGrey noPrint\" style=\"position: relative; clear: both; z-index:1; text-align: center; margin-top: 10px\">")+"<div align=\"center\" class=\"topMenu textGrey noPrint\" style=\"position: relative; clear: both; z-index:1; text-align: center; margin-top: 10px\">".length());
                            info = stringBetweenSubstrings(info," | "," | ");
                            String info_date = info.substring(0,info.indexOf(" ")).replaceAll("-",".");
                            info_date = Integer.valueOf(info_date.substring(indexOfFromEnd(info_date,".")+1))+"."+Integer.valueOf(stringBetweenSubstrings(info_date,".","."))+"."+info_date.substring(0,info_date.indexOf("."));
                            String time = (info.substring(info.indexOf(" "),indexOfFromEnd(info,":"))+".").trim();
                            Calendar calendar = Calendar.getInstance();
                            if (calendar.get(Calendar.HOUR_OF_DAY) > 12) {
                                time = ((Integer.valueOf(time.substring(0, time.indexOf(":")))) + 12) + time.substring(time.indexOf(":"));
                            }
                            String day_name = "neznámý";
                            int dayWeek = calendar.get(Calendar.DAY_OF_WEEK);
                            if(dayWeek == Calendar.MONDAY) {
                                day_name = "Pondělí";
                            } else if(dayWeek == Calendar.TUESDAY) {
                                day_name = "Úterý";
                            } else if(dayWeek == Calendar.WEDNESDAY) {
                                day_name = "Středa";
                            } else if(dayWeek == Calendar.THURSDAY) {
                                day_name = "Čtvrtek";
                            } else if(dayWeek == Calendar.FRIDAY) {
                                day_name = "Pátek";
                            } else if(dayWeek == Calendar.SATURDAY) {
                                day_name = "Sobota";
                            } else if(dayWeek == Calendar.SUNDAY) {
                                day_name = "Neděle";
                            }

                            String fileContent = "<lunch_info><day>" + day_name + "</day><time>" + time + "</time><date>" + info_date + "</date></lunch_info>\n";
                            while(htmlResponse.contains("<div class=\"jidelnicekDen\">")) {
                                String lunchDay = stringBetweenSubstrings(htmlResponse,"<div class=\"jidelnicekDen\">","</div>\n    </div>");
                                htmlResponse = htmlResponse.substring(htmlResponse.indexOf(lunchDay)+lunchDay.length());
                                String day = stringBetweenSubstrings(lunchDay,"<font color=\"#AF1014\">","</font>").trim();
                                lunchDay = lunchDay.substring(lunchDay.indexOf(day)+day.length()+"</font>".length());
                                String date = stringBetweenSubstrings(lunchDay,"<font color=\"#AF1014\">","</font>").trim();
                                if (day.contains("Pondělí")) {
                                    day = "Pondělí";
                                } else if (day.contains("Úterý")) {
                                    day = "Úterý";
                                } else if (day.contains("Středa")) {
                                    day = "Středu";
                                } else if (day.contains("Čtvrtek")) {
                                    day = "Čtvrtek";
                                } else if (day.contains("Pátek")) {
                                    day = "Pátek";
                                } else if (day.contains("Sobota")) {
                                    day = "Sobotu";
                                } else if (day.contains("Neděle")) {
                                    day = "Neděli";
                                }
                                fileContent += "<day><date>" + date + "</date><day_name>" + day + "</day_name><items>";
                                while(lunchDay.contains("<span class=\"smallBoldTitle\" style=\"color: #1b75bb;\">")) {
                                    String lunchType = stringBetweenSubstrings(lunchDay,"<span class=\"smallBoldTitle\" style=\"color: #1b75bb;\">","</span>");
                                    lunchDay = lunchDay.substring(lunchDay.indexOf(lunchType)+lunchType.length());
                                    String lunchName = stringBetweenSubstrings(lunchDay,"</span> -- ","</div>").trim();
                                    if(!lunchName.equals("")) {
                                        fileContent = fileContent + "<item><foodType>" + lunchType + "</foodType><foodName>" + lunchName + "</foodName></item>";
                                    }
                                }
                                fileContent += "</items></day>\n";
                            }

                            try {
                                FileOutputStream stream = MainActivity.appContext.openFileOutput("lunch_info.dat", 0);
                                stream.write(fileContent.getBytes());
                                stream.close();
                            } catch (IOException e) {
                                Log.e("file", "WRITING FILE: " + e.toString());
                            }
                            //Response - END ----------------------------------------------------------------------------------
                        } else {
                            returnString = appContext.getResources().getString(R.string.request_error_code, response.code());
                        }
                    } else {
                        returnString = appContext.getResources().getString(R.string.request_error, response.errorBody().toString());
                    }
                } catch (UnknownHostException e) {
                    returnString = appContext.getResources().getString(R.string.request_error_unknown_host);
                } catch (SocketTimeoutException e) {
                    returnString = appContext.getResources().getString(R.string.request_error_timeout);
                } catch (Exception e) {
                    returnString = appContext.getResources().getString(R.string.request_error_unknown);
                }
            }

            if(returnString.equals("") || returnString.equals(appContext.getResources().getString(R.string.obedy_nologin))) {
                updatedCount++;
            } else {
                resultString += "Obědy: " + returnString + ";\n";
            }
            returnString = "";

            okHttpClient.dispatcher().cancelAll();

            return updatedCount;
        }

        @Override
        protected void onProgressUpdate(String... message) {
            progressdialog.setMessage(message[0]);
        }

        @Override
        protected void onCancelled() {
            if (progressdialog != null && progressdialog.isShowing()) {
                progressdialog.dismiss();
            }
            super.onCancelled();
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);

            if (progressdialog != null && progressdialog.isShowing()) {
                progressdialog.dismiss();
            }
            setScreenOrientationUnlock();
            fragmentReloader.reloadFragment("current");

            if(!resultString.equals("")) {
                resultString = resultString.trim();
                Toast.makeText(MainActivity.appContext, resultString, Toast.LENGTH_LONG).show();
            }
            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.updateall_updated, result), Toast.LENGTH_LONG).show();
        }
    }

    //comment-todo - FUNCTIONS

    private static class DownloadSuply extends AsyncTask<Void, Void, String> {

        private int responsesErrorCount = 0;
        private int responsesCount = 0;
        private String latestFiles = "";
        private Map<String,String> suplyFiles;
        private String returnString = "";

        private DownloadSuply(Map<String,String> suplyFiles) {
            this.suplyFiles = suplyFiles;
        }

        @Override
        protected String doInBackground(Void... ignore) {
            responsesErrorCount = 0;
            responsesCount = 0;

            for (final String key : suplyFiles.keySet()) {
                final String value = suplyFiles.get(key);
                latestFiles += key + ";";

                File pdfFile = new File(Environment.getExternalStorageDirectory() + File.separator + "GvidApp" + File.separator + key);
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.appContext);
                if (!pdfFile.exists() || sharedPref.getBoolean("download_all_button", true)) {
                    responsesErrorCount++;
                    responsesCount++;
                    Call<ResponseBody> call2 = callClient.download(value);
                    try {
                        final Response<ResponseBody> response = call2.execute();
                        if (response.isSuccessful()) {
                            if (response.code() == 200) {
                                //Response - END ----------------------------------------------------------------------------------
                                String sdPath = Environment.getExternalStorageDirectory() + File.separator;
                                sdPath = sdPath + "GvidApp" + File.separator;
                                File Dir = new File(sdPath);
                                if (!Dir.isDirectory()) {
                                    if (!Dir.mkdirs()) {
                                        Log.e("dir", "Can not create dir");
                                    }
                                }

                                sdPath = sdPath + key;
                                File file = new File(sdPath);
                                try {
                                    OutputStream output = new FileOutputStream(file);
                                    InputStream input = response.body().byteStream();
                                    byte[] buffer = new byte[1024];
                                    int read;

                                    while ((read = input.read(buffer)) != -1) {
                                        output.write(buffer, 0, read);
                                    }
                                    output.flush();
                                    output.close();
                                    input.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                responsesErrorCount--;
                                //Response - END ----------------------------------------------------------------------------------
                            } else {
                                returnString = appContext.getResources().getString(R.string.request_error_code, response.code());
                                suplovaniUpdating = false;
                            }
                        } else {
                            returnString = appContext.getResources().getString(R.string.request_error, response.errorBody().toString());
                            suplovaniUpdating = false;
                        }
                    } catch (UnknownHostException e) {
                        returnString = appContext.getResources().getString(R.string.request_error_unknown_host);
                    } catch (SocketTimeoutException e) {
                        returnString = appContext.getResources().getString(R.string.request_error_timeout);
                    } catch (Exception e) {
                        returnString = appContext.getResources().getString(R.string.request_error_unknown);
                    }
                    suplovaniUpdating = false;
                }
            }
            String sdPath = Environment.getExternalStorageDirectory() + File.separator;
            sdPath = sdPath + "GvidApp" + File.separator;
            File Dir = new File(sdPath);
            String sdPathOld = Environment.getExternalStorageDirectory() + File.separator;
            sdPathOld = sdPathOld + "GvidApp" + File.separator + "old" + File.separator;
            File oldDir = new File(sdPathOld);
            if (!oldDir.isDirectory()) {
                if (!oldDir.mkdirs()) {
                    Log.e("dir", "Can not create dir");
                }
            }

            if (Dir.isDirectory()) {
                File[] dirFiles = Dir.listFiles();
                Arrays.sort(dirFiles);
                for (File oldFile : dirFiles) {
                    String fileName = oldFile.getName();
                    if (!fileName.equals("old") && !latestFiles.contains(fileName)) {
                        moveFile(sdPath, fileName, sdPathOld);
                    }
                }
            }

            fragmentReloader.reloadFragment("FragmentSuplovani");
            returnString = appContext.getResources().getString(R.string.suplovani_updated, (responsesCount - responsesErrorCount), suplyFiles.size());
            return returnString;
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(MainActivity.appContext, returnString, Toast.LENGTH_LONG).show();
            suplovaniUpdating = false;
        }
    }

    public void setScreenOrientationLock() {
        Display display = getWindowManager().getDefaultDisplay();
        int rotation = display.getRotation();

        Point size = new Point();
        display.getSize(size);

        int lock;

        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
            // if rotation is 0 or 180 and width is greater than height, we have
            // a tablet
            if (size.x > size.y) {
                if (rotation == Surface.ROTATION_0) {
                    lock = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else {
                    lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                }
            } else {
                // we have a phone
                if (rotation == Surface.ROTATION_0) {
                    lock = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                } else {
                    lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                }
            }
        } else {
            // if rotation is 90 or 270 and width is greater than height, we
            // have a phone
            if (size.x > size.y) {
                if (rotation == Surface.ROTATION_90) {
                    lock = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else {
                    lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                }
            } else {
                // we have a tablet
                if (rotation == Surface.ROTATION_90) {
                    lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                } else {
                    lock = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                }
            }
        }
        setRequestedOrientation(lock);
    }

    public void setScreenOrientationUnlock() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    public static void moveFile(String inputPath, String inputFile, String outputPath) {

        InputStream in;
        OutputStream out;
        try {
            //create output directory if it doesn't exist
            File dir = new File (outputPath);
            if (!dir.exists())
            {
                if (!dir.mkdirs()) {
                    Log.e("dir", "Can not create dir");
                }
            }


            in = new FileInputStream(inputPath + inputFile);
            out = new FileOutputStream(outputPath + inputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();

            // write the output file
            out.flush();
            out.close();

            // delete the original file
            if(!new File(inputPath + inputFile).delete()) {
                Log.e("dir", "Can not delete old file");
            }
        } catch (Exception e) {
            Log.e("tag", e.getMessage());
        }
    }

    public static String stringBetweenSubstrings(String content, String substring1, String substring2) {
        String returnString = "";
        if(content.contains(substring1) && substring2.equals("")){
            returnString = content.substring(0, content.indexOf(substring1));
        } else if(content.contains(substring1) && content.contains(substring2)) {
            returnString = content.substring(content.indexOf(substring1) + substring1.length());
            if(returnString.contains(substring2)) {
                if(returnString.indexOf(substring2) > 0) {
                    returnString = returnString.substring(0, returnString.indexOf(substring2));
                } else {
                    returnString = "";
                }
            }
        }
        return returnString;
    }

    public static int indexOfFromEnd(String string, String substring) {
        StringBuilder buffer = new StringBuilder(string);
        buffer.reverse();
        String stringReversed = buffer.toString();

        buffer = new StringBuilder(substring);
        buffer.reverse();
        String substringReversed = buffer.toString();

        //stringReversed = stringReversed.substring(string.length()-index-1);
        if(!stringReversed.contains(substringReversed)) { return -1;}
        stringReversed = stringReversed.substring(stringReversed.indexOf(substringReversed));

        return stringReversed.length()-substring.length();
    }

    public static String fromHTML(String htmlString) {
        if (Build.VERSION.SDK_INT >= 24) {
            htmlString = Html.fromHtml(htmlString , Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            htmlString = Html.fromHtml(htmlString).toString();
        }
        return htmlString;
    }

    public static String stripHTML(String htmlString) {
        return fromHTML(htmlString).replaceAll("\n", "").trim();
    }

    public static boolean containsNumber(String str) {
        if(str == null || str.isEmpty()) return false;

        for(char c : str.toCharArray()){
            if(Character.isDigit(c)){
                return true;
            }
        }

        return false;
    }

    public static void startNewActivity(Context context, String packageName) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent == null) {
            // Bring user to the market or let them choose an app?
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + packageName));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
