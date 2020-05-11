package edu.tamu.cse.lenss.edgeKeeper.android;

import android.content.SharedPreferences;


import java.lang.reflect.Field;
import java.util.Map;

import edu.tamu.cse.lenss.edgeKeeper.utils.EKProperties;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;

public class EKPropertiesAndroid extends EKProperties {

    /**
     * This class loads the properties from the shared preference.
     * @param sharedPreferences
     */
    public void load(SharedPreferences sharedPreferences){
        EKUtils.logger.error("Loading properties from Shared preference");

        Map<String, ?> map =  sharedPreferences.getAll();

        for(Field f:  EKProperties.class.getDeclaredFields()) {
            String key;
            try {
                key = (String) f.get(this);
                Object value = map.get(key);
                this.setProperty(key, value);
                EKUtils.logger.error("Loading properties key: "+key+" value: "+value);
            } catch (IllegalAccessException e) {
                EKUtils.logger.error("Problem in loading shared preference", e);
            }
        }

    }

}
