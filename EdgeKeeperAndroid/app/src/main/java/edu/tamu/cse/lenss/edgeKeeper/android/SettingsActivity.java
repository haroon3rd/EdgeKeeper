package edu.tamu.cse.lenss.edgeKeeper.android;

import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

import org.apache.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

import edu.tamu.cse.lenss.edgeKeeper.android.R;


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

        //decie whether service requires restart or nah
        if(ValueStore.restart.get()) {
            Autostart.restartEKService(this.getApplication().getApplicationContext());
        }
    }
}
