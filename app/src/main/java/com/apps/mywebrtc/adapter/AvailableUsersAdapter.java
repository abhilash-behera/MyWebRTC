package com.apps.mywebrtc.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.apps.mywebrtc.R;
import com.apps.mywebrtc.Utils;
import com.apps.mywebrtc.activity.VideoChatActivity;
import com.apps.mywebrtc.retrofit.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Abhilash on 09-11-2017
 */

public class AvailableUsersAdapter extends RecyclerView.Adapter<AvailableUsersAdapter.MyViewHolder>{
    private Context context;
    private ArrayList<User> users;

    public AvailableUsersAdapter(Context context, ArrayList<User> users){
        this.users=users;
        this.context=context;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{
        private TextView txtEmail;
        private ImageView imgStatus;
        private CardView cardView;
        private MyViewHolder(View itemView){
            super(itemView);
            txtEmail=itemView.findViewById(R.id.txtEmail);
            imgStatus=itemView.findViewById(R.id.imgStatus);
            cardView=itemView.findViewById(R.id.cardView);
        }
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.user_row_view,null);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        try{
            final User user=users.get(position);
            holder.txtEmail.setText(user.getEmail());
            String status=user.getStatus();
            if(status.equalsIgnoreCase("offline")){
                holder.imgStatus.setImageResource(R.drawable.ic_user_offline);
            }else if(status.equalsIgnoreCase("online")){
                holder.imgStatus.setImageResource(R.drawable.ic_user_online);
            }else if(status.equalsIgnoreCase("busy")){
                holder.imgStatus.setImageResource(R.drawable.ic_user_busy);
            }

            holder.cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try{
                        if(user.getStatus().equalsIgnoreCase("online")){
                            Intent intent=new Intent(context, VideoChatActivity.class);
                            intent.putExtra(Utils.USER_EMAIL,user.getEmail());
                            intent.putExtra(Utils.CALL_TYPE,Utils.CALL_TYPE_OUTGOING);
                            context.startActivity(intent);
                        }else if(user.getStatus().equalsIgnoreCase("offline")){
                            Toast.makeText(context, "User is not online", Toast.LENGTH_SHORT).show();
                        }else if(user.getStatus().equalsIgnoreCase("busy")){
                            Toast.makeText(context,"User is busy on another call.",Toast.LENGTH_SHORT).show();
                        }
                    }catch (Exception e){
                        Log.d("mwr","Exception in making call: "+e.toString());
                        Toast.makeText(context, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
                    }

                }
            });
        }catch (Exception e){
            Log.d("mwr","Exception in creating view: "+e.toString());
            Toast.makeText(context, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }
}
