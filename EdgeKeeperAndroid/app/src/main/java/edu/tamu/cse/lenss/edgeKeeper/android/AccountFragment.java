package edu.tamu.cse.lenss.edgeKeeper.android;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.widget.Toast;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.sql.SQLOutput;
import java.util.HashMap;
import java.util.Map;

import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;

public class AccountFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {
    Logger logger = Logger.getLogger(this.getClass());
    boolean serviceRestart = false;
    Map<String, String> tempPref = new HashMap<>();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.account_pref);

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        PreferenceScreen prefScreen = getPreferenceScreen();

        int count = prefScreen.getPreferenceCount();

        // Go through all of the preferences, and set up their preference summary.
        for (int i = 0; i < count; i++) {

            Preference p = prefScreen.getPreference(i);
            // You don't need to set up preference summaries for checkbox preferences because
            // they are already set up in xml using summaryOff and summary On
            if (!((p instanceof CheckBoxPreference) || (p.getKey().equals(EKProperties.p12pass)))) {
                String value = sharedPreferences.getString(p.getKey(), "");
                p.setSummary(value);
                tempPref.put(p.getKey(), value.toString());
                logger.log(Level.ALL,"Preference screen setting summary for "+p.getKey()+" "+value);
            }else{
                String value = sharedPreferences.getString(p.getKey(), "");
                tempPref.put(p.getKey(), value.toString());
            }
            p.setOnPreferenceChangeListener(this);
        }

        Preference dialogPreference = findPreference(EKProperties.p12Path);
        dialogPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                logger.info("User is trying to choose p12 file");
                fileChooser();
                return true;
            }
        });

    }

    ///when user presses on a Account item and changes the values, this function is called
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        logger.debug("In onPreferenceChange "+preference.getKey());

        if(EKProperties.validateField( preference.getKey(), newValue ))
            return true;
        else{
            logger.warn("Attempted to enter invalid entries. Please try again.");
            Toast error = Toast.makeText(getContext(), "Please select a valid input.", Toast.LENGTH_SHORT);
            error.show();
            return false;
        }
    }

    //when user presses on a Account button, change the previous value, and presses OK, this function is called.
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Figure out which preference was changed
        Preference preference = findPreference(key);
        if (null != preference) {
            // Updates the summary for the preference
            if (!(preference instanceof CheckBoxPreference)) {

                String value = sharedPreferences.getString(preference.getKey(), "");

                //setPreferenceSummary(preference, value);
                preference.setSummary(value);

                //check if the old value and new value same or diff
                if(tempPref.get(preference.getKey())!= null) {
                    //if new value is different, restart needed
                    if (!tempPref.get(preference.getKey()).equals(value)) {
                        serviceRestart = true;
                    }else{
                        //new value is not different, no restart needed
                        serviceRestart = false;
                    }
                }

            }

            logger.info("Preference change "+preference.getKey()+" " + preference.getClass().getName());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);

        //decide whether require service restart or nah
        if(serviceRestart){
            //Value has been changed for one or many items in Settings
            ValueStore.restart.set(true);
        }else{
            //Value has not been changed for any items in Settings
            ValueStore.restart.set(false);
        }

    }

    private static final int READ_REQUEST_CODE = 42;
    public void fileChooser() {
        logger.info("Starting the file chooser");
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("*/*");

        startActivityForResult(intent, READ_REQUEST_CODE);

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        logger.info("Got the URI"+requestCode+resultCode);

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();

                String path = uri.getPath();
                logger.info(uri.getPath());

                // Now check if this filepath is from internal memory or not
                if (path.toLowerCase().startsWith("/document/primary:") && path.toLowerCase().endsWith(".p12"))
                {
                    String p12FilePath = path.replaceFirst("/document/primary:", Environment.getExternalStorageDirectory().toString()+ "/");
                    logger.info("Chosen P12 file path: "+p12FilePath);

                    //todo: match here if the file is same
                    if(p12FilePath.equals(tempPref.get("P12_FILE_PATH"))){
                        //selected p12 file is same as old one so no restart
                        serviceRestart = false;
                    }else{
                        serviceRestart = true;
                    }

                    // Now move on to login with password
                    //changep12Password(p12FilePath);
                    SharedPreferences.Editor prefEditor = getPreferenceScreen().getSharedPreferences().edit();
                    prefEditor.putString(EKProperties.p12Path, p12FilePath);
                    prefEditor.apply();



                }
                else{
                    Toast.makeText(getContext(), "Please select a valid certificate", Toast.LENGTH_SHORT).show();
                    logger.warn("p12 file retrieval error");
                }
            }
        }
    }


}

