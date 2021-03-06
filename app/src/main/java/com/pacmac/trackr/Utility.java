package com.pacmac.trackr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatCheckBox;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by pacmac on 2016-10-27.
 */


public class Utility {

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public final static int MY_PERMISSIONS_REQUEST = 8;

    public static void requestPermissions(Activity activity, String permission) {
        // No explanation needed, we can request the permission.
        ActivityCompat.requestPermissions(activity, new String[]{permission}, MY_PERMISSIONS_REQUEST);
    }

    /**
     * Utility method returning whether the given permission is granted or not.
     *
     * @param context
     * @param permission
     * @return true if permission is granted.
     */
    protected static boolean checkSelfPermission(Context context, String permission) {

        if (Build.VERSION.SDK_INT >= 23) {
            return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static void displayExplanationForPermission(Activity act, final String permission) {

        final Activity mActivity = act;
        AlertDialog.Builder builder = new AlertDialog.Builder(act, 0)
                .setCancelable(true).setMessage(act.getString(R.string.location_message))
                .setTitle(act.getApplicationContext().getResources().getString(R.string.dialog_title_missing_permission))
                .setPositiveButton((act.getResources().getString(R.string.request_perm)), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermissions(mActivity, permission);
                    }
                });
//                .setNegativeButton(act.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                    }
//                });
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
        return String.format("%s %02d, %02d:%02d", month, day, hour, minute);
    }

    public static int getDayOfMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public static String checkAndReplaceForbiddenChars(String id) {
        // Firebase paths must not contain '.', '#', '$', '[', or ']'
        // We have to make sure these chars will be replaced
        // '.' = ','  '#' = '@' '$' = '%'  '[' = '('   ']' = ')'
        if (id == null)
            return "ID_ERROR_TRY_AGAIN";
        return id.replace(".", ",").replace("#", "@").replace("$", "%").replace("[", "(").replace("[", "(");
    }


//TODO re-add this
//    public static Intent createShareIntent(StringBuilder sb) {
//        Intent shareIntent = new Intent(Intent.ACTION_SEND);
//        shareIntent.setType("text/plain");
//        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
//        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "TrackR - Device Location");
//        return shareIntent;
//    }


//    public static StringBuilder updateShareIntent(Context context, String deviceName, double latitude, double longitude, long timestamp, String address, double batteryLevel) {
//        StringBuilder sb = new StringBuilder();
//        sb.append(context.getResources().getString(R.string.share_header));
//        sb.append("\n");
//        sb.append("TrackR Export");
//        sb.append("\n");
//        sb.append(context.getResources().getString(R.string.share_header));
//        sb.append("\n\n");
//        //body
//        // TODO add ID & NAME
//
//        sb.append("Device: " + deviceName);
//        sb.append("\n");
//        sb.append("Coordinates");
//        sb.append("\n");
//        sb.append("Latitude: " + latitude);
//        sb.append("\n");
//        sb.append("Longitude: " + longitude);
//        sb.append("\n");
//        sb.append("Last Seen At: " + new Date(timestamp).toString());
//        sb.append("\n");
//        sb.append("Address:");
//        sb.append("\n");
//        sb.append("" + address);
//        sb.append("\n");
//        sb.append("Battery Level: " + String.format("%.0f", batteryLevel) + " %");
//        sb.append("\n");
//        sb.append(context.getResources().getString(R.string.share_header));
//        sb.append("\n\n");
//        sb.append("Thank you for using TrackR. If you like TrackR please give it a rating and submit a feedback at Google Play Store:\n");
//        sb.append("https://play.google.com/store/apps/details?id=com.pacmac.trackr");
//        return sb;
//    }


    public static void createAlertDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(context.getResources().getString(R.string.new_update_title) + Utility.getCurrentAppVersion(context));

        StringBuilder sb = new StringBuilder();

        sb.append(context.getString(R.string.updateMsg1));
//        sb.append("\n");
//        sb.append(context.getString(R.string.updateMsg2));
        sb.append("\n");
        sb.append(context.getString(R.string.updateMsg3));
//        sb.append("\n");
//        sb.append(context.getString(R.string.updateMsg4));
//        sb.append("\n");
//        sb.append(context.getString(R.string.updateMsg5));
//        sb.append("\n");
//        sb.append(context.getString(R.string.updateMsg6));
//        sb.append("\n");
//        sb.append(context.getString(R.string.updateMsg7));
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
            appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(Constants.TAG, "Exception while retrieving app packageInfo#1" + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(Constants.TAG, "Exception while retrieving app packageInfo#2" + e.getMessage());
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

    public static void showToast(Context context, CharSequence text, int yOffset, boolean changeColor) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.trackr_notification_top, null);
        TextView toastText = view.findViewById(R.id.toastText);
        toastText.setText(text);
        if (changeColor) {
            toastText.setTextColor(Color.WHITE);
            view.findViewById(R.id.toastLayout).setBackgroundColor(context.getResources().getColor(R.color.colorAccent));
        }


        Toast toast = new Toast(context);
        toast.setGravity(Gravity.FILL_HORIZONTAL | Gravity.BOTTOM, 0, yOffset);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(view);
        toast.show();
    }


    public static boolean saveJsonStringToFile(String path, String data) {

        File file = new File(path);
        if (file.exists()) {
            file.delete();

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

    public static String loadCSVFile(String path) {

        File file = new File(path);
        if (!file.exists()) {
            return null;
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
            Log.e(Constants.TAG, "Failed to read CSV: " + e.getMessage());
            return null;
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
            int batteryLevel = (int) object.getDouble("batteryLevel");
            return new LocationRecord(object.getInt("id"), object.getDouble("latitude"), object.getDouble("longitude"),
                    object.getLong("timestamp"), batteryLevel, object.getString("address"),
                    object.getString("alias"), object.getString("recId"), object.getString("safeId"),
                    object.getInt("profileImageId"), object.getInt("cellQuality"));
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
            String versionName = context.getPackageManager().getPackageInfo(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE,
                    PackageManager.GET_META_DATA).versionName;
            String[] versionComponents = versionName.split("\\.");
            return new int[]{Integer.parseInt(versionComponents[0]), Integer.parseInt(versionComponents[1])};
        } catch (Exception e) {
            Log.e(Constants.TAG, "Exception while retrieving app packageInfo#3" + e.getMessage());
            e.printStackTrace();
        }
        return new int[]{-1, -1};
    }

    public static boolean checkPlayServices(Activity activity) {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(activity.getApplicationContext());
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(activity, result,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
                showToast(activity.getApplicationContext(), activity.getString(R.string.google_play_services_outdated), 0, false);
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


    public static List<LocationRecord> convertJsonStringToUserRecords(String filePath) {
        List<LocationRecord> userRecords = new ArrayList<>();
        String jsonString = Utility.loadJsonStringFromFile(filePath);
        if (jsonString.equals("")) {
            return userRecords;
        }
        try {
            JSONObject jsnobject = new JSONObject(jsonString);
            JSONArray jsonArray = jsnobject.getJSONArray("locrecords");

            for (int i = 0; i < jsonArray.length(); i++) {
                LocationRecord locationRecord = Utility.createLocationRecordFromJson((JSONObject) jsonArray.get(i));
                if (locationRecord != null) {
                    userRecords.add(locationRecord);
                }
            }
            return userRecords;
        } catch (JSONException e) {
            Log.e(Constants.TAG, "#7# Error getting LocRecord JSON obj or array. " + e.getMessage());
        }
        return new ArrayList<>();
    }


    // TODO this might be deprecated was ensuring backward compatibility
    public static String createFinalJsonString(LocationRecord userRecord) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"receiverids\":[");
        sb.append(userRecord.convertToJSONForSettings(0));
        sb.append("]}");
        return sb.toString();
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

    protected static void startTrackingService(Context context, final SharedPreferences preferences) {
        boolean isTrackingOn = preferences.getBoolean(Constants.TRACKING_STATE, false);
        long updateFreq = preferences.getInt(Constants.TRACKING_FREQ, Constants.TIME_BATTERY_OK) * 60 * 1000;


//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1 && updateFreq < 15 * 60 * 1000L) {
//            updateFreq = 15 * 60 * 1000L;
//        }
        if (isTrackingOn && (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1 || !isMyServiceRunning(context, LocationService.class))) {

            Intent intentService = new Intent(context, LocationService.class);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                context.startService(intentService);
            } else {
                context.startService(intentService);
//                JobSchedulerHelper.scheduleLocationUpdateJOB(context, updateFreq);
            }
            TrackingForegroundServiceController.startForegroundServiceWithNotification(context, 0);
        }
    }

    protected static void startFetchingService(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                && !isMyServiceRunning(context, FetchFirebaseData.class)) {
            Intent intentService = new Intent(context, FetchFirebaseData.class);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                context.startService(intentService);
            } else {
                context.startService(intentService);
                // TODO add job for this.
                // JobSchedulerHelper.scheduleLocationUpdateJOB(context, updateFreq);
            }
        }
    }

    protected static void stopFetchingService(Context context) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                    && isMyServiceRunning(context, FetchFirebaseData.class)) {
                Intent intentService = new Intent(context, FetchFirebaseData.class);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    context.stopService(intentService);
                } else {
                    context.stopService(intentService);
                    // TODO add job for this.
                    // JobSchedulerHelper.scheduleLocationUpdateJOB(context, updateFreq);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    protected static void openSettings(Context context, Activity activity) {
        if (Utility.checkPlayServices(activity)) {
            Intent intent = new Intent(context, SettingsActivityV2.class);

            activity.startActivityForResult(intent, Constants.SETTINGS_REQUESTCODE);
        }
    }

    protected static void openUserEditActivity(Activity activity, int position, List<LocationRecord> mDataset) {
        Intent intent = new Intent(activity.getApplicationContext(), AddDeviceActivity.class);
        intent.putExtra(Constants.EDIT_USER_POSITION, position);
        if (position != -1) {
            intent.putExtra(Constants.EDIT_USER_ALIAS, mDataset.get(position).getAlias());
            intent.putExtra(Constants.EDIT_USER_ID, mDataset.get(position).getRecId());
            intent.putExtra(Constants.EDIT_USER_IMG, mDataset.get(position).getProfileImageId());
            intent.putExtra(Constants.EDIT_USER_TYPE, mDataset.get(position).getId());
        }
        activity.startActivityForResult(intent, Constants.EDIT_RESULT_REQUEST_CODE);
    }

    /**
     * check the given service is running
     *
     * @param serviceClass class eg MyService.class
     * @return boolean
     */
    private static boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        //TODO try catch random errors
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager
                    .getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected static String createJsonArrayStringFromUserRecords(List<LocationRecord> userRecords) {
        StringBuilder sb = new StringBuilder();

        sb.append("{\"locrecords\":[");

        if (userRecords != null) {
            for (int i = 0; i < userRecords.size(); i++) {
                if (i == userRecords.size() - 1) {
                    sb.append(userRecords.get(i).getJSONString());
                    break;
                }
                sb.append(userRecords.get(i).getJSONString() + ",");
            }
        }
        sb.append("]}");
        return sb.toString();
    }


    private static int DAY_LENGTH = 24 * 60 * 60;
    private static int HOUR_LENGTH = 60 * 60;
    private static int MINUTE_LENGTH = 60;

    protected static String getLastUpdateString(Context context, long lastSeen) {

        if (lastSeen == 0) {
            return "Pending location update.";
        }

        long currentTime = System.currentTimeMillis();

        long diff = currentTime - lastSeen;

        if (diff <= 0) {
            return "Error #TS1";
        }

        long diffSec = diff / 1000;

        long days = diffSec / DAY_LENGTH;
        long hours = diffSec % DAY_LENGTH / HOUR_LENGTH;
        long mins = diffSec % DAY_LENGTH % HOUR_LENGTH / MINUTE_LENGTH;

        if (days < 1 && hours < 1 && mins < 1) {
            return context.getResources().getString(R.string.just_now);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(context.getResources().getString(R.string.last_seen) + " ");
        if (days > 0) {
            sb.append(String.valueOf(days) + "d ");
        }
        if (hours > 0) {
            sb.append(String.valueOf(hours) + "hr ");
        }
        if (mins > 0) {
            sb.append(String.valueOf(mins) + "min ");
        }
        sb.append(context.getResources().getString(R.string.ago));
        return sb.toString();
    }

    protected static boolean checkConnectivity(Context context) {
        try {
            ConnectivityManager conn = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = conn.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    @SuppressLint("MissingPermission")
    public static int getCellSignalQuality(Context context, boolean isPermissionEnabled) {
        int cellQuality = -1;
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                return cellQuality;
            }

            TelephonyManager telephonyManager = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager == null) {
                return cellQuality;
            }

            if (!isPermissionEnabled) {
                return cellQuality;
            }

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                SignalStrength signalStrength = telephonyManager.getSignalStrength();
                if (signalStrength != null) {
                    return signalStrength.getLevel();
                }
            }

            List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
            if (cellInfoList == null) {
                return cellQuality;
            }
            for (CellInfo cell : cellInfoList) {
                if (cell.isRegistered()) {
                    if (cell instanceof CellInfoLte) {
                        cellQuality = ((CellInfoLte) cell).getCellSignalStrength().getLevel();
                    } else if (cell instanceof CellInfoWcdma) {
                        cellQuality = ((CellInfoWcdma) cell).getCellSignalStrength().getLevel();
                    } else if (cell instanceof CellInfoGsm) {
                        cellQuality = ((CellInfoGsm) cell).getCellSignalStrength().getLevel();
                    } else if (cell instanceof CellInfoCdma) {
                        cellQuality = ((CellInfoCdma) cell).getCellSignalStrength().getLevel();
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cellQuality;
    }


    public static int getBatteryLevel(Context context) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent == null) {
            return -1;
        }
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Error checking that probably isn't needed but I added just in case.
        if (level == -1 || scale == -1) {
            return -1;
        }
        float rawLevel = ((float) level / (float) scale) * 100.0f;
        return (int) (Math.round(rawLevel * 100.0) / 100.0);
    }

    public static String getLastFBPullTime(long timestamp) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
//        int day = calendar.get(Calendar.DAY_OF_MONTH);
//        String month = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        return String.format("%02d:%02d", hour, minute);
    }

}
