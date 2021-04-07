package edu.tamu.cse.lenss.edgeKeeper.android;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;

/**
 * Created by Parzival on 18-03-2018.
 */

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {
    Logger logger = Logger.getLogger(this.getClass());
    boolean serviceRestart = false;
    Map<String, String> tempPref = new HashMap<>();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        addPreferencesFromResource(R.xml.settings_pref);

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        PreferenceScreen prefScreen = getPreferenceScreen();

        int count = prefScreen.getPreferenceCount();

        // Go through all of the preferences, and set up their preference summary.
        for (int i = 0; i < count; i++) {
            Preference p = prefScreen.getPreference(i);

            if (! ( (p instanceof CheckBoxPreference) || (p.getKey().equals(EKProperties.p12pass))   ) ) {
                //handle non-check box items
                String value = sharedPreferences.getString(p.getKey(), "");
                p.setSummary(value);
                tempPref.put(p.getKey(), value.toString());
                logger.log(Level.ALL,"Preference screen setting summary for "+p.getKey()+" "+value);
            }else{
                //handle check box items
                CheckBoxPreference mCheckBoxPreference = (CheckBoxPreference) getPreferenceScreen()
                        .findPreference(p.getKey());
                if (mCheckBoxPreference.isChecked()) {
                    tempPref.put(p.getKey(), "true");
                }else{
                    tempPref.put(p.getKey(), "false");
                }
            }
            p.setOnPreferenceChangeListener(this);
        }
    }


    ///when user presses on a setting item and presses OK (regardless of changing value), this function is called
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

    //when user presses on a setting button, change the previous value, and presses OK, this function is called.
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        // Figure out which preference was changed
        Preference preference = findPreference(key);
        if (null != preference) {

            // Updates the summary for the preference
            if (!(preference instanceof CheckBoxPreference)) {
                String value = sharedPreferences.getString(preference.getKey(), "");
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
            }else{
                CheckBoxPreference mCheckBoxPreference = (CheckBoxPreference) getPreferenceScreen()
                        .findPreference(preference.getKey());
                if (key.equals("ENABLE_MASTER")  || key.equals("ENABLE_REAL_IP_REPORTING" )){

                    //checkbox has been checked/unchecked
                    boolean checked = false;
                    if(mCheckBoxPreference.isChecked()) {
                        checked = true;
                    }

                    //at this point boolean "checked" denotes to current status of the checkbox
                    //check if checkbox changed since last time
                    if(tempPref.get(preference.getKey()).equals("true") && checked){
                        serviceRestart = false;
                    }else if(tempPref.get(preference.getKey()).equals("false") && !checked){
                        serviceRestart = false;
                    }else{
                        serviceRestart = true;
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

