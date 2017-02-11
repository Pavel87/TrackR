package com.pacmac.trackr;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by pacmac on 2016-11-19.
 */


public class AdapterReceivingIds extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<SettingsObject> mDataset;
    private SettingsInteractionListener listener = null;
    private Context context;


    public void add(int position, SettingsObject item) {
        mDataset.add(position, item);
        notifyItemInserted(position);
    }

//    public void update(ArrayList<SettingsObject> mDataset) {
//        this.mDataset = mDataset;
//        notifyDataSetChanged();
//    }
//    public void remove(String item) {
//        int position = mDataset.indexOf(item);
//        mDataset.remove(position);
//        notifyItemRemoved(position);
//    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public AdapterReceivingIds(ArrayList<SettingsObject> myDataset, SettingsInteractionListener listener, Context context) {
        mDataset = myDataset;
        this.listener = listener;
        this.context = context;
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }


    @Override
    public int getItemViewType(int position) {
        return mDataset.get(position).getRowType();
    }


    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == Constants.TYPE_HEADER) {
            return new VHHeader(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_header_settings, parent, false));
        } else if (viewType == Constants.TYPE_NORMAL) {
            return new VHItem(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_rec_id, parent, false));
        } else if (viewType == Constants.TYPE_TRACK_SWITCH) {
            return new VHTrackingSwitch(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_track_switch, parent, false));
        } else if (viewType == Constants.TYPE_TRACKID) {
            return new VHTrackId(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_track_id, parent, false));
        } else if (viewType == Constants.TYPE_FOOTER) {
            return new VHFooter(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_footer_settings, parent, false));
        } else {
            return new VHError(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_header_settings, parent, false));
        }

    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        final int type = holder.getItemViewType();

        holder.itemView.setEnabled(mDataset.get(position).isEnabled());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (type == Constants.TYPE_FOOTER | type == Constants.TYPE_TRACK_SWITCH) {
                    return;
                }
                if (listener != null) {
                    listener.settingsInteractionRequest(position, mDataset.get(position));
                }
            }
        });

        if (type == Constants.TYPE_NORMAL) {
            ((VHItem) holder).alias.setText(mDataset.get(position).getAlias());
            ((VHItem) holder).recID.setText(mDataset.get(position).getId());

        } else if (type == Constants.TYPE_HEADER) {
            ((VHHeader) holder).header.setText(mDataset.get(position).getId());
            if(position != 0) {
                ((VHHeader) holder).headerImg.setImageDrawable(context.getResources().getDrawable(R.drawable.signs));
            }
        } else if (type == Constants.TYPE_TRACKID) {
            ((VHTrackId) holder).trackingID.setText(mDataset.get(position).getId());
        } else if (type == Constants.TYPE_TRACK_SWITCH) {
            ((VHTrackingSwitch) holder).switchTracking.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        boolean isChecked = ((VHTrackingSwitch) holder).switchTracking.isChecked();
                    //    ((VHTrackingSwitch) holder).switchTracking.setChecked(!isChecked);
                        mDataset.set(position, new SettingsObject(Constants.TYPE_TRACK_SWITCH, isChecked));
                        listener.settingsInteractionRequest(position, mDataset.get(position));
                    }
                }
            });
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        boolean isChecked = ((VHTrackingSwitch) holder).switchTracking.isChecked();
                        ((VHTrackingSwitch) holder).switchTracking.setChecked(!isChecked);
                        mDataset.set(position, new SettingsObject(Constants.TYPE_TRACK_SWITCH, !isChecked));
                        listener.settingsInteractionRequest(position, mDataset.get(position));
                    }
                }
            });
            ((VHTrackingSwitch) holder).switchTracking.setChecked(mDataset.get(position).isTrackingEnabled());
            ((VHTrackingSwitch) holder).switchTracking.setEnabled(mDataset.get(position).isEnabled());
        } else if (type == Constants.TYPE_FOOTER) {
            ((VHFooter) holder).footerBtn.setEnabled(mDataset.get(position).isEnabled());
            ((VHFooter) holder).footerBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.settingsInteractionRequest(position, mDataset.get(position));
                    }
                }
            });
        } else if (holder instanceof VHError) {
            ((VHError) holder).error.setText("#1#ERROR");
        }
    }





    public class VHItem extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView recID;
        public TextView alias;

        public VHItem(View itemView) {
            super(itemView);
            recID = (TextView) itemView.findViewById(R.id.receivingID);
            alias = (TextView) itemView.findViewById(R.id.aliasId);
        }
    }

    class VHHeader extends RecyclerView.ViewHolder {
        public TextView header;
        public ImageView headerImg;

        public VHHeader(View headerView) {
            super(headerView);
            header = (TextView) headerView.findViewById(R.id.header);
            headerImg = (ImageView) headerView.findViewById(R.id.settingsImg);
        }
    }

    class VHTrackingSwitch extends RecyclerView.ViewHolder {
        private SwitchCompat switchTracking = null;

        public VHTrackingSwitch(View trackSwitchView) {
            super(trackSwitchView);
            switchTracking = (SwitchCompat) trackSwitchView.findViewById(R.id.switchTracking);
        }
    }

    class VHTrackId extends RecyclerView.ViewHolder {
        public TextView trackingID;

        public VHTrackId(View trackIDView) {
            super(trackIDView);
            trackingID = (TextView) trackIDView.findViewById(R.id.trackingID);
        }
    }

    class VHFooter extends RecyclerView.ViewHolder {
        public ImageButton footerBtn;

        public VHFooter(View footerView) {
            super(footerView);
            footerBtn = (ImageButton) footerView.findViewById(R.id.footerBtn);
        }
    }

    class VHError extends RecyclerView.ViewHolder {
        public TextView error;

        public VHError(View errorView) {
            super(errorView);
            error = (TextView) errorView.findViewById(R.id.header);
        }
    }


}
