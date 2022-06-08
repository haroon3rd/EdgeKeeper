package edu.tamu.cse.lenss.edgeKeeper.android;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import edu.tamu.cse.lenss.edgeKeeper.client.EdgeKeeperAPI;

public class GVAdapter extends ArrayAdapter<GVItem> {
    public GVAdapter(@NonNull Context context, ArrayList<GVItem> courseModelArrayList) {
        super(context, 0, courseModelArrayList);
    }
    public static EdgeKeeperAPI mEKClient;

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View listitemView = convertView;

        if (listitemView == null) {

            // Layout Inflater inflates each item to be displayed in GridView.
            listitemView = LayoutInflater.from(getContext()).inflate(R.layout.card_item, parent, false);
        }

        //get this item
        GVItem input = getItem(position);

        //get resources
        ImageView image = (ImageView) listitemView.findViewById(R.id.idIV);
        ImageView pin = (ImageView) listitemView.findViewById(R.id.pin);
        ImageView indicator = (ImageView) listitemView.findViewById(R.id.indicator);
        TextView text = listitemView.findViewById(R.id.idTV);

        //set item image
        if(input.getNAME().equals("Cloud")){
            image.setImageResource(R.drawable.cloud);
        }else{
            image.setImageResource(R.drawable.person);
        }

        //set item pin
        if(input.isPinned()){
            pin.setVisibility(View.VISIBLE);
        }else{
            pin.setVisibility(View.GONE);
        }

        //set item indicator
        if(input.isCONNECTED()){
            indicator.setImageResource(R.drawable.green);
        }else{
            indicator.setImageResource(R.drawable.red);
        }

        //set item text
        text.setText(input.getNAME());

        //image long click
        image.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                onLongClickLogic(input, v, image, pin, indicator, text);

                return true;
            }
        });

        //image onclick
        image.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onClickLogic(input, v, image, pin, indicator, text);
            }
        });

        //indicator long click
        indicator.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                onLongClickLogic(input, v, image, pin, indicator, text);

                return true;
            }
        });

        //indicator onclick
        indicator.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onClickLogic(input, v, image, pin, indicator, text);
            }
        });

        //text long click
        text.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                onLongClickLogic(input, v, image, pin, indicator, text );

                return true;
            }
        });

        //text onclick
        text.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onClickLogic(input, v, image, pin, indicator, text);
            }
        });


        return listitemView;

    }

    public void onLongClickLogic(GVItem input, View view, ImageView image, ImageView pin, ImageView indicator, TextView text){
        if(!input.getNAME().equals("Cloud")) {

            vibrator(10, input.getContext());

            if (ValueStore.pinnedItems.contains(input.getNAME())) {

                //unpin this item from pinnedItems list
                ValueStore.pinnedItems.remove(input.getNAME());

                //update visual
                pin.setVisibility(View.GONE);


            } else {

                //pin this item into pinnedIntems list
                ValueStore.pinnedItems.add(input.getNAME());

                //update visual
                pin.setVisibility(View.VISIBLE);

            }
        }
    }

    public void onClickLogic(GVItem input, View view, ImageView image, ImageView pin, ImageView indicator, TextView text){
        if(!input.getNAME().equals("Cloud")) {
            String guid = null;
            List<String> ips = null;
            try {
                //get guid for this name
                guid = mEKClient.getGUIDbyAccountName(input.getNAME() + ".distressnet.org");

                //get ips by guid
                ips = mEKClient.getIPbyGUID(guid);

            } catch (Exception e) {
                e.printStackTrace();
            }finally{
                String message = "";
                if(guid!=null){
                    message = message + "GUID: " + guid;
                }
                if(ips!=null && ips.size()>0){
                    message = message + "\n" + "IP: " + ips;
                }
                //show as snackbar
                snackbar(message, input.getView());
            }
        }
    }


    public static void vibrator(int millisec, Context context){
        Vibrator v = (Vibrator) context.getSystemService(context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(millisec, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(millisec);
        }
    }

    //shoes a snackbar on android activity main
    //public static void snackbar(String message, View view){
    //    Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    //}


    public static void snackbar(String message, View view){
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();
        TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setMaxLines(3);
        snackbar.show();

    }

}
