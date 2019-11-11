package cz.gvid.app;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Calendar;

import okhttp3.Interceptor;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

@SuppressWarnings("StringConcatenationInLoop")
public class SettingsActivity extends AppCompatActivity
{

    private void setupActionBar()
    {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //up button close activity
                finish();
                return(true);
        }

        return(super.onOptionsItemSelected(item));
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupActionBar();

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(R.id.content_frame_settings, new SettingsPreferenceFragment()).commit();
        }
    }

    public static class SettingsPreferenceFragment extends PreferenceFragment
    {
        static Preference moodleLogin;
        static Preference sasLogin;
        static Preference icanteenLogin;

        static boolean account_moodle_logedin = false;
        static boolean account_sas_logedin = false;
        static boolean account_icanteen_logedin = false;

        static boolean moodleLoggingIn = false;
        static boolean sasLoggingIn = false;
        static boolean canteenLoggingIn = false;

        static OkHttpClient okHttpClient = null;
        static HttpClient callClient;

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getActivity().setTitle("Nastavení");
        }

        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings_fragment);

            moodleLogin = findPreference("moodle_login");
            sasLogin = findPreference("sas_login");
            icanteenLogin = findPreference("icanteen_login");
            final Preference memorySuply = findPreference("memory_suply");
            final SwitchPreference floatingButtonUpdate = (SwitchPreference) findPreference("floating_button_update");
            final CheckBoxPreference updateAllButton = (CheckBoxPreference) findPreference("update_all_button");
            if(!floatingButtonUpdate.isChecked()){
                updateAllButton.setChecked(true);
            }
            floatingButtonUpdate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference p) {
                    if(!floatingButtonUpdate.isChecked()){
                        updateAllButton.setChecked(true);
                    }
                    return false;
                }
            });

            reCreateView();

            moodleLogin.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference p) {
                    if(moodleLoggingIn) {
                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.string_already_loggingin), Toast.LENGTH_LONG).show();
                    } else {
                        if (account_moodle_logedin) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle(MainActivity.appContext.getString(R.string.dialog_logout_title, MainActivity.appContext.getString(R.string.settings_ucty_moodle)));
                            builder.setMessage(MainActivity.appContext.getString(R.string.dialog_logout_message, MainActivity.appContext.getString(R.string.settings_ucty_moodle)));
                            builder.setPositiveButton(R.string.string_logout, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    String accountsNewContent = "";
                                    String accountsPath = MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "accounts.cfg";
                                    File file = new File(accountsPath);
                                    if (file.exists()) {
                                        try {
                                            FileInputStream inputStream = MainActivity.appContext.openFileInput("accounts.cfg");

                                            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                                            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                                            String receivedString;
                                            while ((receivedString = bufferedReader.readLine()) != null) {
                                                if (!(receivedString.contains("<moodle>") && receivedString.contains("</moodle>"))) {
                                                    accountsNewContent += receivedString + "\n";
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
                                        FileOutputStream outputstream = MainActivity.appContext.openFileOutput("accounts.cfg", Context.MODE_PRIVATE);
                                        outputstream.write(accountsNewContent.getBytes());
                                        outputstream.close();
                                    } catch (IOException e) {
                                        Log.e("file", "WRITING FILE: " + e.toString());
                                    }

                                    reCreateView();
                                }
                            });
                            builder.setNegativeButton(R.string.string_cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {/* User cancelled the dialog */}
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setView(R.layout.dialog_login_moodle);
                            builder.setPositiveButton(R.string.string_login, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Dialog dialogView = (Dialog) dialog;
                                    final String dialog_username = ((EditText) dialogView.findViewById(R.id.username)).getText().toString();
                                    final String dialog_password = ((EditText) dialogView.findViewById(R.id.password)).getText().toString();
                                    if (dialog_username.equals("") || dialog_password.equals("")) {
                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_nothing), Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_login_loggigin), Toast.LENGTH_LONG).show();
                                        moodleLoggingIn = true;

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

                                        Call<String> call = callClient.postMoodleLogin(dialog_username, dialog_password);
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
                                                                            final String htmlResponse = response.body();
                                                                            //Response - END ----------------------------------------------------------------------------------
                                                                            String moodleLogoutUrl = htmlResponse.substring(htmlResponse.indexOf("</a> (<a href=\""));
                                                                            moodleLogoutUrl = moodleLogoutUrl.substring(0, moodleLogoutUrl.indexOf("\">"));

                                                                            if (htmlResponse.contains("<input name=\"id\" type=\"hidden\" value=\"3\" />") && htmlResponse.contains("<input name=\"sesskey\" type=\"hidden\" value=")) {
                                                                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                                                                builder.setTitle(R.string.dialog_login_moodle_registration_title);
                                                                                builder.setMessage(R.string.dialog_login_moodle_registration_message);
                                                                                builder.setPositiveButton(R.string.string_register, new DialogInterface.OnClickListener() {
                                                                                    public void onClick(DialogInterface dialog, int id) {
                                                                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_login_registering), Toast.LENGTH_LONG).show();

                                                                                        String moodleLoginSesskey = htmlResponse.substring(htmlResponse.indexOf("<input name=\"sesskey\" type=\"hidden\" value=\"") + "<input name=\"sesskey\" type=\"hidden\" value=\"".length());
                                                                                        moodleLoginSesskey = moodleLoginSesskey.substring(0, moodleLoginSesskey.indexOf("\" />"));

                                                                                        Call<String> call3 = callClient.postMoodleCourseRegistration("3", "7", moodleLoginSesskey, "1", "1");
                                                                                        call3.enqueue(new Callback<String>() {
                                                                                            @Override
                                                                                            public void onResponse(Call<String> call, Response<String> response) {
                                                                                                if (response.isSuccessful()) {
                                                                                                    if (response.code() == 200) {
                                                                                                        String htmlResponse = response.body();
                                                                                                        //Response - END ----------------------------------------------------------------------------------
                                                                                                        String moodleLogoutUrl = htmlResponse.substring(htmlResponse.indexOf("</a> (<a href=\""));
                                                                                                        moodleLogoutUrl = moodleLogoutUrl.substring(0, moodleLogoutUrl.indexOf("\">"));
                                                                                                        Call<String> callOut = callClient.getUrl(moodleLogoutUrl);
                                                                                                        callOut.enqueue(new Callback<String>() {
                                                                                                            @Override
                                                                                                            public void onResponse(Call<String> call, Response<String> response) {
                                                                                                            }

                                                                                                            @Override
                                                                                                            public void onFailure(Call<String> call, Throwable t) {
                                                                                                            }
                                                                                                        });
                                                                                                        if (htmlResponse.contains("course-content")) {
                                                                                                            String accountsNewContent = "";
                                                                                                            String accountsPath = MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "accounts.cfg";
                                                                                                            File file = new File(accountsPath);
                                                                                                            if (file.exists()) {
                                                                                                                boolean tagExists = false;
                                                                                                                try {
                                                                                                                    InputStream inputStream = MainActivity.appContext.openFileInput("accounts.cfg");

                                                                                                                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                                                                                                                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                                                                                                                    String receivedString;

                                                                                                                    while ((receivedString = bufferedReader.readLine()) != null) {
                                                                                                                        if (receivedString.contains("<moodle>") && receivedString.contains("</moodle>")) {
                                                                                                                            accountsNewContent += "<moodle><moodle_username>" + dialog_username + "</moodle_username><moodle_password>" + dialog_password + "</moodle_password></moodle>\n";
                                                                                                                            tagExists = true;
                                                                                                                        } else {
                                                                                                                            accountsNewContent += receivedString + "\n";
                                                                                                                        }
                                                                                                                    }

                                                                                                                    inputStream.close();
                                                                                                                } catch (FileNotFoundException e) {
                                                                                                                    Log.e("file", "File not found: " + e.toString());
                                                                                                                } catch (IOException e) {
                                                                                                                    Log.e("file", "Can not read file: " + e.toString());
                                                                                                                }
                                                                                                                if (!tagExists) {
                                                                                                                    accountsNewContent += "<moodle><moodle_username>" + dialog_username + "</moodle_username><moodle_password>" + dialog_password + "</moodle_password></moodle>\n";
                                                                                                                }
                                                                                                            } else {
                                                                                                                accountsNewContent += "<moodle><moodle_username>" + dialog_username + "</moodle_username><moodle_password>" + dialog_password + "</moodle_password></moodle>\n";
                                                                                                            }

                                                                                                            try {
                                                                                                                FileOutputStream stream = MainActivity.appContext.openFileOutput("accounts.cfg", Context.MODE_PRIVATE);
                                                                                                                stream.write(accountsNewContent.getBytes());
                                                                                                                stream.close();
                                                                                                            } catch (IOException e) {
                                                                                                                Log.e("file", "WRITING FILE: " + e.toString());
                                                                                                            }
                                                                                                            reCreateView();

                                                                                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_login_successful), Toast.LENGTH_LONG).show();
                                                                                                            moodleLoggingIn = false;
                                                                                                        } else {
                                                                                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_login_unsuccessful), Toast.LENGTH_LONG).show();
                                                                                                            moodleLoggingIn = false;
                                                                                                        }
                                                                                                        //Response - END ----------------------------------------------------------------------------------
                                                                                                    } else {
                                                                                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_code, response.code()), Toast.LENGTH_LONG).show();
                                                                                                        moodleLoggingIn = false;
                                                                                                    }
                                                                                                } else {
                                                                                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, response.errorBody().toString()), Toast.LENGTH_LONG).show();
                                                                                                    moodleLoggingIn = false;
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
                                                                                                moodleLoggingIn = false;
                                                                                            }
                                                                                        });
                                                                                    }
                                                                                });
                                                                                builder.setNegativeButton(R.string.string_cancel, new DialogInterface.OnClickListener() {
                                                                                    public void onClick(DialogInterface dialog, int id) {
                                                                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_login_unsuccessful), Toast.LENGTH_LONG).show();
                                                                                        moodleLoggingIn = false;
                                                                                    }
                                                                                });
                                                                                AlertDialog dialog = builder.create();
                                                                                dialog.show();
                                                                            } else if (htmlResponse.contains("course-content")) {
                                                                                String accountsNewContent = "";
                                                                                String accountsPath = MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "accounts.cfg";
                                                                                File file = new File(accountsPath);
                                                                                if (file.exists()) {
                                                                                    boolean tagExists = false;
                                                                                    try {
                                                                                        InputStream inputStream = MainActivity.appContext.openFileInput("accounts.cfg");

                                                                                        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                                                                                        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                                                                                        String receivedString;

                                                                                        while ((receivedString = bufferedReader.readLine()) != null) {
                                                                                            if (receivedString.contains("<moodle>") && receivedString.contains("</moodle>")) {
                                                                                                accountsNewContent += "<moodle><moodle_username>" + dialog_username + "</moodle_username><moodle_password>" + dialog_password + "</moodle_password></moodle>\n";
                                                                                                tagExists = true;
                                                                                            } else {
                                                                                                accountsNewContent += receivedString + "\n";
                                                                                            }
                                                                                        }

                                                                                        inputStream.close();
                                                                                    } catch (FileNotFoundException e) {
                                                                                        Log.e("file", "File not found: " + e.toString());
                                                                                    } catch (IOException e) {
                                                                                        Log.e("file", "Can not read file: " + e.toString());
                                                                                    }
                                                                                    if (!tagExists) {
                                                                                        accountsNewContent += "<moodle><moodle_username>" + dialog_username + "</moodle_username><moodle_password>" + dialog_password + "</moodle_password></moodle>\n";
                                                                                    }
                                                                                } else {
                                                                                    accountsNewContent += "<moodle><moodle_username>" + dialog_username + "</moodle_username><moodle_password>" + dialog_password + "</moodle_password></moodle>\n";
                                                                                }

                                                                                try {
                                                                                    FileOutputStream stream = MainActivity.appContext.openFileOutput("accounts.cfg", Context.MODE_PRIVATE);
                                                                                    stream.write(accountsNewContent.getBytes());
                                                                                    stream.close();
                                                                                } catch (IOException e) {
                                                                                    Log.e("file", "WRITING FILE: " + e.toString());
                                                                                }
                                                                                reCreateView();

                                                                                Call<String> callOut = callClient.getUrl(moodleLogoutUrl);
                                                                                callOut.enqueue(new Callback<String>() {
                                                                                    @Override
                                                                                    public void onResponse(Call<String> call, Response<String> response) {
                                                                                    }

                                                                                    @Override
                                                                                    public void onFailure(Call<String> call, Throwable t) {
                                                                                    }
                                                                                });
                                                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_login_successful), Toast.LENGTH_LONG).show();
                                                                                moodleLoggingIn = false;
                                                                            } else {
                                                                                Call<String> callOut = callClient.getUrl(moodleLogoutUrl);
                                                                                callOut.enqueue(new Callback<String>() {
                                                                                    @Override
                                                                                    public void onResponse(Call<String> call, Response<String> response) {
                                                                                    }

                                                                                    @Override
                                                                                    public void onFailure(Call<String> call, Throwable t) {
                                                                                    }
                                                                                });
                                                                                Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_login_unsuccessful), Toast.LENGTH_LONG).show();
                                                                                moodleLoggingIn = false;
                                                                            }
                                                                            //Response - END ----------------------------------------------------------------------------------
                                                                        } else {
                                                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_code, response.code()), Toast.LENGTH_LONG).show();
                                                                            moodleLoggingIn = false;
                                                                        }
                                                                    } else {
                                                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, response.errorBody().toString()), Toast.LENGTH_LONG).show();
                                                                        moodleLoggingIn = false;
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
                                                                    moodleLoggingIn = false;
                                                                }
                                                            });
                                                        } else {
                                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_login_bad_data), Toast.LENGTH_LONG).show();
                                                            moodleLoggingIn = false;
                                                        }
                                                        //Response - END ----------------------------------------------------------------------------------
                                                    } else {
                                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_code, response.code()), Toast.LENGTH_LONG).show();
                                                        moodleLoggingIn = false;
                                                    }
                                                } else {
                                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, response.errorBody().toString()), Toast.LENGTH_LONG).show();
                                                    moodleLoggingIn = false;
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
                                                moodleLoggingIn = false;
                                            }
                                        });
                                    }
                                }
                            });
                            builder.setNegativeButton(R.string.string_cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {/* User cancelled the dialog */}
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    }
                    return false;
                }
            });

            sasLogin.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference p) {
                    if(sasLoggingIn) {
                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.string_already_loggingin), Toast.LENGTH_LONG).show();
                    } else {
                        if (account_sas_logedin) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle(MainActivity.appContext.getString(R.string.dialog_logout_title, MainActivity.appContext.getString(R.string.settings_ucty_sas)));
                            builder.setMessage(MainActivity.appContext.getString(R.string.dialog_logout_message, MainActivity.appContext.getString(R.string.settings_ucty_sas)));
                            builder.setPositiveButton(R.string.string_logout, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    String accountsNewContent = "";
                                    String accountsPath = MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "accounts.cfg";
                                    File file = new File(accountsPath);
                                    if (file.exists()) {
                                        try {
                                            InputStream inputStream = MainActivity.appContext.openFileInput("accounts.cfg");

                                            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                                            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                                            String receivedString;

                                            while ((receivedString = bufferedReader.readLine()) != null) {
                                                if (!(receivedString.contains("<sas>") && receivedString.contains("</sas>"))) {
                                                    accountsNewContent += receivedString + "\n";
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
                                        FileOutputStream stream = MainActivity.appContext.openFileOutput("accounts.cfg", Context.MODE_PRIVATE);
                                        stream.write(accountsNewContent.getBytes());
                                        stream.close();
                                    } catch (IOException e) {
                                        Log.e("file", "WRITING FILE: " + e.toString());
                                    }
                                    reCreateView();
                                }
                            });
                            builder.setNegativeButton(R.string.string_cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {/* User cancelled the dialog */}
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setView(R.layout.dialog_login_skolaonline);
                            builder.setPositiveButton(R.string.string_login, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Dialog dialogView = (Dialog) dialog;
                                    final String dialog_username = ((EditText) dialogView.findViewById(R.id.username)).getText().toString();
                                    final String dialog_password = ((EditText) dialogView.findViewById(R.id.password)).getText().toString();
                                    if (dialog_username.equals("") || dialog_password.equals("")) {
                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_nothing), Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_login_loggigin), Toast.LENGTH_LONG).show();
                                        sasLoggingIn = true;

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

                                        Call<String> call = callClient.postSkolaOnlineLogin("dnn$ctr994$SOLLogin$btnODeslat","","","","","","","",dialog_username, dialog_password,"","","");
                                        call.enqueue(new Callback<String>() {
                                            @Override
                                            public void onResponse(Call<String> call, Response<String> response) {
                                                if (response.isSuccessful()) {
                                                    if (response.code() == 200) {
                                                        String htmlResponse = response.body();
                                                        //Response - END ----------------------------------------------------------------------------------
                                                        if (htmlResponse.contains("Logout.aspx")) {
                                                            String accountsNewContent = "";
                                                            String accountsPath = MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "accounts.cfg";
                                                            File file = new File(accountsPath);
                                                            if (file.exists()) {
                                                                boolean tagExists = false;
                                                                try {
                                                                    InputStream inputStream = MainActivity.appContext.openFileInput("accounts.cfg");

                                                                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                                                                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                                                                    String receivedString;

                                                                    while ((receivedString = bufferedReader.readLine()) != null) {
                                                                        if (receivedString.contains("<sas>") && receivedString.contains("</sas>")) {
                                                                            accountsNewContent += "<sas><sas_username>" + dialog_username + "</sas_username><sas_password>" + dialog_password + "</sas_password></sas>\n";
                                                                            tagExists = true;
                                                                        } else {
                                                                            accountsNewContent += receivedString + "\n";
                                                                        }
                                                                    }

                                                                    inputStream.close();
                                                                } catch (FileNotFoundException e) {
                                                                    Log.e("file", "File not found: " + e.toString());
                                                                } catch (IOException e) {
                                                                    Log.e("file", "Can not read file: " + e.toString());
                                                                }
                                                                if (!tagExists) {
                                                                    accountsNewContent += "<sas><sas_username>" + dialog_username + "</sas_username><sas_password>" + dialog_password + "</sas_password></sas>\n";
                                                                }
                                                            } else {
                                                                accountsNewContent += "<sas><sas_username>" + dialog_username + "</sas_username><sas_password>" + dialog_password + "</sas_password></sas>\n";
                                                            }

                                                            try {
                                                                FileOutputStream stream = MainActivity.appContext.openFileOutput("accounts.cfg", Context.MODE_PRIVATE);
                                                                stream.write(accountsNewContent.getBytes());
                                                                stream.close();
                                                            } catch (IOException e) {
                                                                Log.e("file", "WRITING FILE: " + e.toString());
                                                            }
                                                            reCreateView();

                                                            Call<String> callOut = callClient.getUrl("https://aplikace.skolaonline.cz/SOL/App/Logout.aspx");
                                                            callOut.enqueue(new Callback<String>() {
                                                                @Override
                                                                public void onResponse(Call<String> call, Response<String> response) {
                                                                }

                                                                @Override
                                                                public void onFailure(Call<String> call, Throwable t) {
                                                                }
                                                            });
                                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_login_successful), Toast.LENGTH_LONG).show();
                                                            sasLoggingIn = false;
                                                        } else {
                                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_login_bad_data), Toast.LENGTH_LONG).show();
                                                            sasLoggingIn = false;
                                                        }
                                                        //Response - END ----------------------------------------------------------------------------------
                                                    } else {
                                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_code, response.code()), Toast.LENGTH_LONG).show();
                                                        sasLoggingIn = false;
                                                    }
                                                } else {
                                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, response.errorBody().toString()), Toast.LENGTH_LONG).show();
                                                    sasLoggingIn = false;
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
                                                sasLoggingIn = false;
                                            }
                                        });
                                    }
                                }
                            });
                            builder.setNegativeButton(R.string.string_cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {/* User cancelled the dialog */}
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    }
                    return false;
                }
            });

            icanteenLogin.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference p) {
                    if(canteenLoggingIn) {
                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.string_already_loggingin), Toast.LENGTH_LONG).show();
                    } else {
                        if (account_icanteen_logedin) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle(MainActivity.appContext.getString(R.string.dialog_logout_title, MainActivity.appContext.getString(R.string.settings_ucty_icanteen)));
                            builder.setMessage(MainActivity.appContext.getString(R.string.dialog_logout_message, MainActivity.appContext.getString(R.string.settings_ucty_icanteen)));
                            builder.setPositiveButton(R.string.string_logout, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    File file_lunch_info = new File(MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "lunch_info.dat");
                                    if (file_lunch_info.exists()) {
                                        deleteFileOrDirectoryRecursive(file_lunch_info);
                                    }

                                    String accountsNewContent = "";
                                    String accountsPath = MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "accounts.cfg";
                                    File file = new File(accountsPath);
                                    if (file.exists()) {
                                        try {
                                            InputStream inputStream = MainActivity.appContext.openFileInput("accounts.cfg");

                                            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                                            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                                            String receivedString;

                                            while ((receivedString = bufferedReader.readLine()) != null) {
                                                if (!(receivedString.contains("<icanteen>") && receivedString.contains("</icanteen>"))) {
                                                    accountsNewContent += receivedString + "\n";
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
                                        FileOutputStream stream = MainActivity.appContext.openFileOutput("accounts.cfg", Context.MODE_PRIVATE);
                                        stream.write(accountsNewContent.getBytes());
                                        stream.close();
                                    } catch (IOException e) {
                                        Log.e("file", "WRITING FILE: " + e.toString());
                                    }

                                    reCreateView();
                                }
                            });
                            builder.setNegativeButton(R.string.string_cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {/* User cancelled the dialog */}
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setView(R.layout.dialog_login_icanteen);
                            builder.setPositiveButton(R.string.string_login, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Dialog dialogView = (Dialog) dialog;
                                    final String dialog_username = ((EditText) dialogView.findViewById(R.id.username)).getText().toString();
                                    final String dialog_password = ((EditText) dialogView.findViewById(R.id.password)).getText().toString();
                                    if (dialog_username.equals("") || dialog_password.equals("")) {
                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_nothing), Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_login_loggigin), Toast.LENGTH_LONG).show();
                                        canteenLoggingIn = true;

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
                                                        String hash = MainActivity.stringBetweenSubstrings(htmlResponse,"<input type=\"hidden\" name=\"_csrf\" value=\"","\"/>");

                                                        Call<String> call2 = callClient.postiCanteenLogin(dialog_username, dialog_password, "false", "web", hash);
                                                        call2.enqueue(new Callback<String>() {
                                                            @Override
                                                            public void onResponse(Call<String> call, Response<String> response) {
                                                                if (response.isSuccessful()) {
                                                                    if (response.code() == 200) {
                                                                        String htmlResponse = response.body();
                                                                        //System.out.println(response.raw().request().url());
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
                                                                            String lunchNewContent = "<lunch_info><firstName>" + firstName + "</firstName><lastName>" + lastName + "</lastName><credit>" + credit + "</credit><consumption>" + consumption + "</consumption><day>" + day_name + "</day><time>" + time + "</time><date>" + date + "</date></lunch_info>\n";
                                                                            try {
                                                                                FileOutputStream stream = MainActivity.appContext.openFileOutput("lunch_info.dat", Context.MODE_PRIVATE);
                                                                                stream.write(lunchNewContent.getBytes());
                                                                                stream.close();
                                                                            } catch (IOException e) {
                                                                                Log.e("file", "WRITING FILE: " + e.toString());
                                                                            }

                                                                            String accountsNewContent = "";
                                                                            String accountsPath = MainActivity.appContext.getFilesDir().getAbsolutePath() + File.separator + "accounts.cfg";
                                                                            File file = new File(accountsPath);
                                                                            if (file.exists()) {
                                                                                boolean tagExists = false;
                                                                                try {
                                                                                    InputStream inputStream = MainActivity.appContext.openFileInput("accounts.cfg");

                                                                                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                                                                                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                                                                                    String receivedString;

                                                                                    while ((receivedString = bufferedReader.readLine()) != null) {
                                                                                        if (receivedString.contains("<icanteen>") && receivedString.contains("</icanteen>")) {
                                                                                            accountsNewContent += "<icanteen><icanteen_username>" + dialog_username + "</icanteen_username><icanteen_password>" + dialog_password + "</icanteen_password></icanteen>\n";
                                                                                            tagExists = true;
                                                                                        } else {
                                                                                            accountsNewContent += receivedString + "\n";
                                                                                        }
                                                                                    }

                                                                                    inputStream.close();
                                                                                } catch (FileNotFoundException e) {
                                                                                    Log.e("file", "File not found: " + e.toString());
                                                                                } catch (IOException e) {
                                                                                    Log.e("file", "Can not read file: " + e.toString());
                                                                                }
                                                                                if (!tagExists) {
                                                                                    accountsNewContent += "<icanteen><icanteen_username>" + dialog_username + "</icanteen_username><icanteen_password>" + dialog_password + "</icanteen_password></icanteen>\n";
                                                                                }
                                                                            } else {
                                                                                accountsNewContent += "<icanteen><icanteen_username>" + dialog_username + "</icanteen_username><icanteen_password>" + dialog_password + "</icanteen_password></icanteen>\n";
                                                                            }

                                                                            try {
                                                                                FileOutputStream stream = MainActivity.appContext.openFileOutput("accounts.cfg", Context.MODE_PRIVATE);
                                                                                stream.write(accountsNewContent.getBytes());
                                                                                stream.close();
                                                                            } catch (IOException e) {
                                                                                Log.e("file", "WRITING FILE: " + e.toString());
                                                                            }
                                                                            reCreateView();

                                                                            Call<String> callOut = callClient.getUrl("https://jidelna.gvid.cz/faces/secured/j_spring_security_logout?terminal=false&keyboard=false&printer=false");
                                                                            callOut.enqueue(new Callback<String>() {
                                                                                @Override
                                                                                public void onResponse(Call<String> call, Response<String> response) {
                                                                                }

                                                                                @Override
                                                                                public void onFailure(Call<String> call, Throwable t) {
                                                                                }
                                                                            });
                                                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_login_successful), Toast.LENGTH_LONG).show();
                                                                            canteenLoggingIn = false;
                                                                        } else {
                                                                            Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.dialog_login_bad_data), Toast.LENGTH_LONG).show();
                                                                            canteenLoggingIn = false;
                                                                        }
                                                                        //Response - END ----------------------------------------------------------------------------------
                                                                    } else {
                                                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_code, response.code()), Toast.LENGTH_LONG).show();
                                                                        canteenLoggingIn = false;
                                                                    }
                                                                } else {
                                                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, response.errorBody().toString()), Toast.LENGTH_LONG).show();
                                                                    canteenLoggingIn = false;
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
                                                                canteenLoggingIn = false;
                                                            }
                                                        });
                                                        //Response - END ----------------------------------------------------------------------------------
                                                    } else {
                                                        Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error_code, response.code()), Toast.LENGTH_LONG).show();
                                                        canteenLoggingIn = false;
                                                    }
                                                } else {
                                                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.request_error, response.errorBody().toString()), Toast.LENGTH_LONG).show();
                                                    canteenLoggingIn = false;
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
                                                canteenLoggingIn = false;
                                            }
                                        });
                                    }
                                }
                            });
                            builder.setNegativeButton(R.string.string_cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {/* User cancelled the dialog */}
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    }
                    return false;
                }
            });

            memorySuply.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference p) {
                    String sdPath = Environment.getExternalStorageDirectory() + File.separator;
                    sdPath = sdPath + "GvidApp" + File.separator + "old" + File.separator;
                    File oldDir = new File(sdPath);
                    if(oldDir.isDirectory()) {
                        deleteFileOrDirectoryRecursive(oldDir);
                        if(!oldDir.mkdirs()) {
                            Log.e("dir", "Can not create dir");
                        }
                    } else {
                        if(!oldDir.mkdirs()) {
                            Log.e("dir", "Can not create dir");
                        }
                    }
                    Toast.makeText(MainActivity.appContext, MainActivity.appContext.getString(R.string.settings_mezipamet_suply_string), Toast.LENGTH_LONG).show();
                    return false;
                }
            });
        }

        boolean deleteFileOrDirectoryRecursive(File fileOrDirectory) {
            if (fileOrDirectory.isDirectory())
                for (File child : fileOrDirectory.listFiles())
                    deleteFileOrDirectoryRecursive(child);
            return fileOrDirectory.delete();
        }

        public void reCreateView() {
            String account_moodle = MainActivity.appContext.getString(R.string.string_no_login);
            String account_sas = MainActivity.appContext.getString(R.string.string_no_login);
            String account_icanteen = MainActivity.appContext.getString(R.string.string_no_login);

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
                    account_moodle = file_contents.substring(file_contents.indexOf("<moodle_username>") + "<moodle_username>".length(), file_contents.indexOf("</moodle_username>"));
                }
                if (file_contents.contains("<sas_username>") && file_contents.contains("</sas_username>")) {
                    account_sas = file_contents.substring(file_contents.indexOf("<sas_username>") + "<sas_username>".length(), file_contents.indexOf("</sas_username>"));
                }
                if (file_contents.contains("<icanteen_username>") && file_contents.contains("</icanteen_username>")) {
                    account_icanteen = file_contents.substring(file_contents.indexOf("<icanteen_username>") + "<icanteen_username>".length(), file_contents.indexOf("</icanteen_username>"));
                }
            }

            account_moodle_logedin = !account_moodle.contains(MainActivity.appContext.getString(R.string.string_no_login));
            account_sas_logedin = !account_sas.contains(MainActivity.appContext.getString(R.string.string_no_login));
            account_icanteen_logedin = !account_icanteen.contains(MainActivity.appContext.getString(R.string.string_no_login));

            moodleLogin.setSummary(MainActivity.appContext.getString(R.string.settings_ucty_moodle_summary, account_moodle));
            sasLogin.setSummary(MainActivity.appContext.getString(R.string.settings_ucty_sas_summary, account_sas));
            icanteenLogin.setSummary(MainActivity.appContext.getString(R.string.settings_ucty_icanteen_summary, account_icanteen));
        }
    }
}
