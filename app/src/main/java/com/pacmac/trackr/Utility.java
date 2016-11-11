package com.pacmac.trackr;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by pacmac on 2016-10-27.
 */


public class Utility {


    public final static int MY_PERMISSIONS_REQUEST = 8;

    /**
     * This method will check if permission is granted at runtime
     */
    public static boolean checkPermission(Context context, String permission) {

        int status = PermissionChecker.checkSelfPermission(context, permission);
        if (status == PermissionChecker.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    public static void requestPermissions(Activity activity, String permission) {
        // No explanation needed, we can request the permission.
        ActivityCompat.requestPermissions(activity, new String[]{permission}, MY_PERMISSIONS_REQUEST);
    }

    public static void displayExplanationForPermission(Activity act, final String permission) {

        final Activity mActivity = act;
        AlertDialog.Builder builder = new AlertDialog.Builder(act, 0)
                .setCancelable(true).setMessage(act.getString(R.string.location_message)).setTitle("Missing Permission")
                .setPositiveButton((act.getResources().getString(R.string.request_perm)), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermissions(mActivity, permission);
                    }
                })
                .setNegativeButton(act.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public static String parseDate(long timestamp) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);

        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        return String.format("%02d", hour) + ":"
                + String.format("%02d", minute) + " - " + day + " " + month;
    }

    public static String checkAndReplaceForbiddenChars(String id) {
        // Firebase paths must not contain '.', '#', '$', '[', or ']'
        // We have to make sure these chars will be replaced
        // '.' = ','  '#' = '@' '$' = '%'  '[' = '('   ']' = ')'
        if (id == null)
            return "ID_ERROR_TRY_AGAIN";
        return id.replace(".", ",").replace("#", "@").replace("$", "%").replace("[", "(").replace("[", "(");
    }


    // This is util method to delete unused ID from Firebase DB
    protected static void deleteUnusedIdFromFb() {
        final long timeThreshold = System.currentTimeMillis() - Constants.OLD_ID_THRESHOLD; // 7 days
        final Firebase firebase = new Firebase("https://trackr1.firebaseio.com");
        firebase.keepSynced(true);
        firebase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    long timestamp = (long) snapshot.child("timestamp").getValue();
                    String id = snapshot.getKey();
                    if (timestamp < timeThreshold) {
                        Log.d(Constants.TAG, id + " ID was not updated in last 7 days - likely not in use anymore");
                        firebase.child(id).removeValue();
                    }
                }
                firebase.removeEventListener(this);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
            }
        });
    }


    public static Intent createShareIntent(StringBuilder sb) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "TrackR - Device Location");
        return shareIntent;
    }


    public static StringBuilder updateShareIntent(Context context, double latitude, double longitude, long timestamp, String address, double batteryLevel) {
        StringBuilder sb = new StringBuilder();
        sb.append(context.getResources().getString(R.string.share_header));
        sb.append("\n");
        sb.append("TrackR Export");
        sb.append("\n");
        sb.append(context.getResources().getString(R.string.share_header));
        sb.append("\n\n");
        //body
        // TODO add ID & NAME
        sb.append("Coordinates");
        sb.append("\n");
        sb.append("Latitude: " + latitude);
        sb.append("\n");
        sb.append("Longitude: " + longitude);
        sb.append("\n\n");
        sb.append("Last Seen At: " + new Date(timestamp).toString());
        sb.append("\n\n");
        sb.append("Address:");
        sb.append("\n");
        sb.append("" + address);
        sb.append("\n\n");
        sb.append("Battery Level: " + String.format("%.2f", batteryLevel) + " %");
        sb.append("\n");
        sb.append(context.getResources().getString(R.string.share_header));
        sb.append("\n\n");
        sb.append("Thank you for using TrackR. If you like TrackR please give it a rating and submit a feedback at Google Play Store:\n");
        sb.append("https://play.google.com/store/apps/details?id=com.pacmac.trackr");
        return sb;
    }


    public static void createAlertDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(context.getResources().getString(R.string.new_update_title) + Utility.getCurrentAppVersion(context));

        StringBuilder sb = new StringBuilder();

        sb.append(context.getString(R.string.updateMsg1));
        sb.append("\n");
        sb.append(context.getString(R.string.updateMsg2));
        sb.append("\n");
        sb.append(context.getString(R.string.updateMsg3));
        sb.append("\n");
        sb.append(context.getString(R.string.updateMsg4));
        sb.append("\n");
        sb.append(context.getString(R.string.updateMsg5));
        sb.append("\n");
        sb.append(context.getString(R.string.updateMsg6));
        sb.append("\n");
        sb.append(context.getString(R.string.updateMsg7));
        builder.setMessage(sb.toString());
        sb = null;
        builder.setCancelable(true);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static String getCurrentAppVersion(Context context) {
        String appVersion = "N/A";
        try {
            appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return appVersion;
    }


    public static void showRateMyAppDialog(final Context context, final SharedPreferences preferences) {

        if (preferences.getBoolean(Constants.RATING_POPUP_ENABLED, true)) {

            final Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.rate_dialog);
            dialog.setCancelable(false);

            Button yesButton = (Button) dialog.findViewById(R.id.yesExit);
            yesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // if user clicks on Rate Now then don't show again this dialog
                    preferences.edit().putBoolean(Constants.RATING_POPUP_ENABLED, false).commit();

                    String appPackage = context.getPackageName();
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackage));
                    if (intent.resolveActivity(context.getPackageManager()) != null) {
                        context.startActivity(intent);
                    } else {
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackage));
                        context.startActivity(intent);
                    }
                    dialog.dismiss();
                }
            });

            Button noButton = (Button) dialog.findViewById(R.id.noExit);
            noButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });

            AppCompatCheckBox checkBox = (AppCompatCheckBox) dialog.findViewById(R.id.neverAgain);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    preferences.edit().putBoolean(Constants.RATING_POPUP_ENABLED, !isChecked).commit();
                }
            });

            dialog.show();
        }
    }

}
