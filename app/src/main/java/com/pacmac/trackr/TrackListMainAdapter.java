package com.pacmac.trackr;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by pacmac on 2017-08-05.
 */


public class TrackListMainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<LocationRecord> mDataset;
    private Context context;
    private TrackListItemSelectedListener listener = null;

    protected void add(int position, LocationRecord record) {
        mDataset.add(record);
        notifyItemInserted(position);
    }

    protected void setItemSelectedListener(TrackListItemSelectedListener listener) {
        this.listener = listener;
    }

    protected void updateViews(List<LocationRecord> newDataSet){
        this.mDataset = newDataSet;
        notifyDataSetChanged();
    }


    // Provide a suitable constructor (depends on the kind of dataset)
    protected TrackListMainAdapter(List<LocationRecord> myDataset, Context context) {
        mDataset = myDataset;
        this.context = context;
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if(mDataset == null) {
            return 0;
        }
        return mDataset.size();
    }

    // Load Row View
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new TrackListMainAdapter.ViewHolderForRow(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_tracked_device, parent, false));
    }

    private final static String TAG = "TrackListMainAdapter";
    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(listener != null){
                    listener.OnItemSelected(holder.getLayoutPosition());
                    return;
                }
                Log.e(TAG, "Position: " + holder.getLayoutPosition() + " Error#listener is NULL.");
            }
        });

        ((ViewHolderForRow) holder).alias.setText(mDataset.get(position).getAlias());
        ((ViewHolderForRow) holder).lastUpdateTime.setText(Utility.getLastUpdateString(mDataset.get(position).getTimestamp()));
        ((ViewHolderForRow) holder).address.setText(mDataset.get(position).getAddress());

        // set battery % and indicator
        double batteryLevel = mDataset.get(position).getBatteryLevel();
        if(batteryLevel == -1){
            ((ViewHolderForRow) holder).batteryLevel.setText("NA");
        } else {
            ((ViewHolderForRow) holder).batteryLevel.setText(String.valueOf(batteryLevel) +"%");
        }
        setBatteryIndicatorDrawable(((ViewHolderForRow) holder).batIndicator, batteryLevel);

    }

    protected class ViewHolderForRow extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView alias;
        public TextView lastUpdateTime;
        public TextView address;
        public TextView batteryLevel;
        public ImageView batIndicator;

        public ViewHolderForRow(View rowView) {
            super(rowView);
            alias = rowView.findViewById(R.id.alias);
            lastUpdateTime = rowView.findViewById(R.id.updateTime);
            address = rowView.findViewById(R.id.address);
            batteryLevel = rowView.findViewById(R.id.batteryLevel);
            batIndicator = rowView.findViewById(R.id.batIndicator);
        }
    }

    private void setBatteryIndicatorDrawable(ImageView batteryIndicatorView, double batteryLevel) {
        if (context != null) {
            if(batteryLevel > 75) {
                batteryIndicatorView.setImageDrawable(context.getDrawable(R.drawable.bat1));
            } else if(batteryLevel > 20) {
                batteryIndicatorView.setImageDrawable(context.getDrawable(R.drawable.bat2));

            } else {
                batteryIndicatorView.setImageDrawable(context.getDrawable(R.drawable.bat3));
            }
        }
    }

    protected interface TrackListItemSelectedListener {
        void OnItemSelected(int position);
    }


}
