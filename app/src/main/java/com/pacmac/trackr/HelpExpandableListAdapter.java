package com.pacmac.trackr;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pacmac on 2017-08-27.
 */


public final class HelpExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<String> headers; // header titles
    // child data in format of header title, child title
    private List<String> childern;

    public HelpExpandableListAdapter(Context context) {
        this.context = context;
        this.headers = loadHeaders();
        this.childern = loadChildern();
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

    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        return childern.get(groupPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        final String childText = (String) getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.help_list_item, null);
        }

        TextView txtListChild = convertView
                .findViewById(R.id.lblListItem);

        txtListChild.setText(childText);
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
        lblListHeader.setTypeface(null, Typeface.BOLD);
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

    @Override
    public void onGroupCollapsed(int groupPosition) {
        super.onGroupCollapsed(groupPosition);
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
        super.onGroupExpanded(groupPosition);
    }
}








