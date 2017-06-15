package com.pacmac.trackr;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

/**
 * Created by pacmac on 2016-10-27.
 */


public class Utility {

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
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
        String month = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        return month + " " + day + ", " + String.format("%02d", hour) + ":"
                + String.format("%02d", minute);
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
        firebase.goOnline();
        Log.d(Constants.TAG, "Firebase goes online");
        firebase.keepSynced(true);
        firebase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    long timestamp = timeThreshold - 1;
                    try {
                        //Some stored keys might be in weird format likely because people use //\\
                        timestamp = (long) snapshot.child("timestamp").getValue();
                    } catch (Exception ex) {
                        Log.d(Constants.TAG, "Error while deleting old records: " + ex.getMessage() + " " + snapshot.toString());
                    }
                    String id = snapshot.getKey();
                    if (timestamp < timeThreshold) {
                        Log.d(Constants.TAG, id + " ID was not updated in last 7 days - likely not in use anymore");
                        firebase.child(id).removeValue();
                    }
                }
                firebase.removeEventListener(this);
                Log.d(Constants.TAG, "Firebase goes offline");
                firebase.goOffline();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.d(Constants.TAG, "DELETING UNUSED IDs WAS CANCELED");
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


    public static StringBuilder updateShareIntent(Context context, String deviceName, double latitude, double longitude, long timestamp, String address, double batteryLevel) {
        StringBuilder sb = new StringBuilder();
        sb.append(context.getResources().getString(R.string.share_header));
        sb.append("\n");
        sb.append("TrackR Export");
        sb.append("\n");
        sb.append(context.getResources().getString(R.string.share_header));
        sb.append("\n\n");
        //body
        // TODO add ID & NAME

        sb.append("Device: " + deviceName);
        sb.append("\n");
        sb.append("Coordinates");
        sb.append("\n");
        sb.append("Latitude: " + latitude);
        sb.append("\n");
        sb.append("Longitude: " + longitude);
        sb.append("\n");
        sb.append("Last Seen At: " + new Date(timestamp).toString());
        sb.append("\n");
        sb.append("Address:");
        sb.append("\n");
        sb.append("" + address);
        sb.append("\n");
        sb.append("Battery Level: " + String.format("%.0f", batteryLevel) + " %");
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

//   sb.append(context.getString(R.string.updateMsg1));
//        sb.append("\n");
//        sb.append(context.getString(R.string.updateMsg2));
//        sb.append("\n");
//        sb.append(context.getString(R.string.updateMsg3));
//        sb.append("\n");
//        sb.append(context.getString(R.string.updateMsg4));
//        sb.append("\n");
//        sb.append(context.getString(R.string.updateMsg5));
//        sb.append("\n");
//        sb.append(context.getString(R.string.updateMsg6));
//        sb.append("\n");
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

    public static void showToast(Context context, CharSequence text) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.trackr_notification_top, null);
        TextView toastText = (TextView) view.findViewById(R.id.toastText);
        toastText.setText(text);

        Toast toast = new Toast(context);
        toast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.TOP, 0, 0);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(view);
        toast.show();
    }


    public static boolean saveJsonStringToFile(String path, String data) {

        File file = new File(path);
        if (file.exists()) {
            file.delete();
//            try {
//                file.createNewFile();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
        BufferedWriter writer = null;
        boolean isSuccesful = false;

        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(data);
            isSuccesful = true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(Constants.TAG, "Failed to write rec ids in file: " + e.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return isSuccesful;
    }

    public static String loadJsonStringFromFile(String path) {

        File file = new File(path);
        if (!file.exists()) {
            return "";
        }

        BufferedReader reader = null;
        StringBuilder readOutput = new StringBuilder();

        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            readOutput.append("");
            while ((line = reader.readLine()) != null) {
                readOutput.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(Constants.TAG, "Failed to read rec ids from file: " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return readOutput.toString();
    }


    public static String generateUniqueID() {
        return UUID.randomUUID().toString();
    }

    public static SettingsObject createSettingsObjectFromJson(JSONObject object) {
        try {
            return new SettingsObject(Constants.TYPE_NORMAL, object.getString("alias"),
                    object.getString("id"), object.getString("safeId"));
        } catch (JSONException e) {
            Log.d(Constants.TAG, "#3# Error parsing json from file. " + e.getMessage());
        }
        return null;
    }

    public static LocationRecord createLocationRecordFromJson(JSONObject object) {
        try {
            return new LocationRecord(object.getInt("id"), object.getDouble("latitude"), object.getDouble("longitude"), object.getLong("timestamp"), object.getDouble("batteryLevel"), object.getString("address"));
        } catch (JSONException e) {
            Log.d(Constants.TAG, "#6# Error parsing locationRecord json from file. " + e.getMessage());
        }
        return null;
    }

    public static int isGooglePlayAvailable(Context context) {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        return googleAPI.isGooglePlayServicesAvailable(context);
    }

    public static int[] getGooglePlayVersion(Context context) {
        try {
            String versionName = context.getPackageManager().getPackageInfo(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE,0).versionName;
            String[] versionComponents = versionName.split("\\.");
            return new int[]{Integer.parseInt(versionComponents[0]), Integer.parseInt(versionComponents[1])};
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new int[]{-1,-1};
    }

    public static boolean checkPlayServices(Activity activity) {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(activity.getApplicationContext());
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(activity, result,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
                showToast(activity.getApplicationContext(), activity.getString(R.string.google_play_services_outdated));
            }
            return false;
        }
        return true;
    }


    public static boolean checkIfLocationIsEnabled(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (!gps_enabled && !network_enabled) {
            return false;
        }
        return true;
    }



    public static HashMap<Integer, LocationRecord> convertJsonStringToLocList(String filePath) {
        String jsonString = Utility.loadJsonStringFromFile(filePath);
        if (jsonString.equals("")) {
            return null;
        }
        HashMap<Integer, LocationRecord> locationRecList = new HashMap<>();
        try {
            JSONObject jsnobject = new JSONObject(jsonString);
            JSONArray jsonArray = jsnobject.getJSONArray("locrecords");

            for (int i = 0; i < jsonArray.length(); i++) {
                LocationRecord locationRecord = Utility.createLocationRecordFromJson((JSONObject) jsonArray.get(i));
                if (locationRecord != null) {
                    locationRecList.put(locationRecord.getId(), locationRecord);
                }
            }
            return locationRecList;
        } catch (JSONException e) {
            Log.d(Constants.TAG, "#7# Error getting LocRecord JSON obj or array. " + e.getMessage());
        }
        return null;
    }

    public static String createFinalJsonString(ArrayList<SettingsObject> recIdObjList) {
        StringBuilder sb = new StringBuilder();


        sb.append("{\"receiverids\":[");
        //if passed list has only length == 1 then we are upgrading from v1.7
        // TODO if statement can be removed later on
        if (recIdObjList.size() == 1) {
            sb.append(recIdObjList.get(0).convertToJSONString(0));
        } else {
            // We have to exclude first 4 items related to track mode and last item which is footer
            for (int i = 5; i < recIdObjList.size() - 2; i++) {
                sb.append(recIdObjList.get(i).convertToJSONString(i) + ",");
            }
            sb.append(recIdObjList.get(recIdObjList.size() - 2).convertToJSONString(recIdObjList.size() - 2));
        }
        sb.append("]}");
        return sb.toString();
    }


}
