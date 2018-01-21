package com.pacmac.trackr;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pacmac on 2017-08-27.
 */


public final class HelpExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<String> headers;
    private List<String> children;

    private boolean isPermissionEnabled = false;
    private boolean isGooglePlayAvailable = false;
    private boolean isGooglePlayVersionHigher = false;
    private boolean isLocationingEnabled = false;
    private Activity activity;
    private int[] googleVersion;

    /**
     * This flag only indicates that troubleshooting menu was just removed
     */
    private boolean isTroubleshootingDisabled = false;

    public HelpExpandableListAdapter(Context context) {
        this.context = context;
        this.headers = loadHeaders();
        this.children = loadChildern();
    }

    private List<String> loadHeaders() {
        List<String> headers = new ArrayList<>();
        TypedArray headersArray = context.getResources().obtainTypedArray(R.array.helpHeaders);

        for (int i = 0; i < headersArray.length(); i++) {
            headers.add(headersArray.getString(i));
        }
        return headers;
    }

    private List<String> loadChildern() {
        List<String> children = new ArrayList<>();
        TypedArray headersArray = context.getResources().obtainTypedArray(R.array.helpChildren);

        for (int i = 0; i < headersArray.length(); i++) {
            children.add(headersArray.getString(i));
        }
        return children;
    }

    protected void updateErrors(Activity activity, boolean isPermissionEnabled,
                                boolean isGooglePlayAvailable, boolean isGooglePlayVersionHigher,
                                boolean isLocationingEnabled, int[] googleVersion) {
        this.googleVersion = googleVersion;
        this.activity = activity;
        this.isGooglePlayAvailable = isGooglePlayAvailable;
        this.isGooglePlayVersionHigher = isGooglePlayVersionHigher;
        this.isPermissionEnabled = isPermissionEnabled;
        this.isLocationingEnabled = isLocationingEnabled;
        notifyDataSetInvalidated();
    }

    protected void addTroubleshootingRow() {
        headers.add(context.getString(R.string.help_header_troubleshooting));
        notifyDataSetChanged();
    }

    protected void removeTroubleshootingRow() {
        headers.remove(headers.size() - 1);
        isTroubleshootingDisabled = true;
        notifyDataSetChanged();
        isLocationingEnabled = false;
        isGooglePlayVersionHigher = false;
        isPermissionEnabled = false;
        isGooglePlayAvailable = false;
        googleVersion = new int[]{-1, -1};
    }

    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        return children.get(groupPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        if (groupPosition == HelpActivity.HEADERS_COUNT-1) {

            LayoutInflater infalInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.help_troubleshooting_item, null);

            displayResolutions(convertView);
        } else {

            String version = "";
            if (groupPosition == 0) {
                version = " " + Utility.getCurrentAppVersion(context);
            }
            final String childText = getChild(groupPosition, childPosition) + version;


            if (convertView == null || getGroupCount() == HelpActivity.HEADERS_COUNT || isTroubleshootingDisabled) {
                isTroubleshootingDisabled = false;
                LayoutInflater infalInflater = (LayoutInflater) this.context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = infalInflater.inflate(R.layout.help_list_item, null);
            }
            TextView txtListChild = convertView.findViewById(R.id.lblListItem);
            txtListChild.setText(childText);
        }
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 1;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return headers.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return headers.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.help_list_header, null);
        }

        TextView lblListHeader = convertView
                .findViewById(R.id.helpListHeader);
//        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }


    private void displayResolutions(View view) {
        if (!isPermissionEnabled) {
            view.findViewById(R.id.locPermission).setVisibility(View.VISIBLE);
            view.findViewById(R.id.resolvePermission).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Utility.requestPermissions(activity, Constants.LOCATION_PERMISSION);
                }
            });

        } else {
            view.findViewById(R.id.locPermission).setVisibility(View.GONE);
        }
        if (!isGooglePlayVersionHigher) {
            view.findViewById(R.id.locGpsVersion).setVisibility(View.VISIBLE);
            if (googleVersion[0] != -1) {
                ((TextView) view.findViewById(R.id.currentGpsVersion)).setText(String.format("Current version: %d.%d", googleVersion[0], googleVersion[1]));
            }
            view.findViewById(R.id.resolveGooglePlayVersion).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Utility.checkPlayServices(activity);
                }
            });
        } else {
            view.findViewById(R.id.locGpsVersion).setVisibility(View.GONE);
        }
        if (!isGooglePlayAvailable) {
            view.findViewById(R.id.locGpsUnavailable).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.locGpsUnavailable).setVisibility(View.GONE);
        }
        if (!isLocationingEnabled) {
            view.findViewById(R.id.locationDisabled).setVisibility(View.VISIBLE);
            view.findViewById(R.id.resolveLocationDisabled).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showGoToSettingsDialog();
                }
            });
        } else {
            view.findViewById(R.id.locationDisabled).setVisibility(View.GONE);

        }
    }

    private void showGoToSettingsDialog() {

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_default);
        dialog.setCancelable(false);

        TextView title = dialog.findViewById(R.id.title);
        title.setText(context.getString(R.string.dialog_title_location_settings));
        TextView content = dialog.findViewById(R.id.content);
        content.setText(context.getString(R.string.dialog_content_location_settings));
        Button yesButton = dialog.findViewById(R.id.dialogYes);
        yesButton.setText(context.getString(R.string.fixit));
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                try {
                    context.startActivity(myIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Utility.showToast(context, "Navigate to Device Settings and enable Location feature.", 0);
                }
                dialog.dismiss();
            }
        });

        Button noButton = dialog.findViewById(R.id.dialogCancel);
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }


}








