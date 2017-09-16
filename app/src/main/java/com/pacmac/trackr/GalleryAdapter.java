package com.pacmac.trackr;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by pacmac on 2017-08-14.
 */


public class GalleryAdapter extends ArrayAdapter {

    private Context context;
    private int layoutResourceId;
    private ArrayList data = new ArrayList();
    int selectedItem = 0;

    public GalleryAdapter(Context context, int layoutResourceId, ArrayList data, int selectedItem) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
        this.selectedItem = selectedItem;
    }

    protected void selectView(int selectedItem) {
        this.selectedItem = selectedItem;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewHolder holder;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new ViewHolder();
            holder.image = row.findViewById(R.id.image);
            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }

        ImageItem item = (ImageItem) data.get(position);
       // holder.image.setImageBitmap(item.getImage());
        loadImageIntoGridItem(holder, position);

        if (selectedItem == item.getId()) {
            holder.image.setBackground(context.getResources().getDrawable(R.drawable.profile_img_selected));
        } else {
            holder.image.setBackground(context.getResources().getDrawable(R.drawable.image_button_bg));
        }
        return row;
    }

    static class ViewHolder {
        ImageView image;
    }

    // Using an AsyncTask to load the slow images in a background thread

    private void loadImageIntoGridItem(ViewHolder viewHolder, final int position) {
        new AsyncTask<ViewHolder, Void, Bitmap>() {
            private ViewHolder v;

            @Override
            protected Bitmap doInBackground(ViewHolder... params) {
                v = params[0];
                return ((ImageItem) data.get(position)).getImage();
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                super.onPostExecute(result);

                v.image.setImageBitmap(result);
//                if (v.position == position) {
//                    // If this item hasn't been recycled already, hide the
//                    // progress and set and show the image
//                    v.progress.setVisibility(View.GONE);
//                    v.icon.setVisibility(View.VISIBLE);
//                    v.icon.setImageBitmap(result);
//                }
            }
        }.execute(viewHolder);

    }

}
