package edu.tamu.cse.lenss.edgeKeeper.android;

import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

import org.apache.log4j.Logger;

import edu.tamu.cse.lenss.edgeKeeper.android.R;

/**
 * https://github.com/bhavyakaria/Android.git
 */
public class SettingsActivity extends AppCompatActivity {
    Logger logger = Logger.getLogger(this.getClass());


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        logger.info("On the SettingsActivity, onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActionBar actionBar = this.getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        logger.debug("Exiting from Setting view. Restarting the EK Service");
        Autostart.restartEKService(this.getApplication().getApplicationContext());
    }
}
