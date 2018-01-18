package com.mega.deviceapp.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mega.deviceapp.R;
import com.mega.deviceapp.model.SmsInfo;
import com.mega.deviceapp.util.TimeUtil;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * @author liuhao
 * @time 2017/10/25 11:43
 * @des ${TODO}
 * @email liuhao_nevermore@163.com
 */

public class SmsChatAdapter extends RecyclerView.Adapter<SmsChatAdapter.ViewHolder> implements View.OnClickListener {
    private Context context;
    private List<SmsInfo> list;
    private static final String TAG = "SmsAdapter";
    private OnRecyclerItemClickListener listener;
    private RecyclerView recyclerView;
    private boolean isDeleteAble = true;

    public void setOnRecyclerItemClickListener(OnRecyclerItemClickListener listener) {
        this.listener = listener;
    }

    public SmsChatAdapter(Context context, List<SmsInfo> list) {
        this.context = context;
        this.list = list;
    }

    //在为RecyclerView提供数据的时候调用
    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView = null;
    }

    @Override
    public void onBindViewHolder(SmsChatAdapter.ViewHolder holder, int position) {
        SmsInfo info = list.get(position);
        holder.body.setText(info.getBody());
        holder.time.setText(TimeUtil.formatDisplayTime(info.getDate(), "yyyy-MM-dd HH:mm:ss", context));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        if (info.getType().equals("发送")) {
            holder.body.setBackgroundResource(R.mipmap.chat_right);
            //该布局在LinearLayout下
            lp.gravity = Gravity.RIGHT;
            lp.setMargins(10, 0, 10, 30);
            holder.body.setLayoutParams(lp);
//            holder.rightView.setVisibility(View.GONE);
            holder.leftView.setVisibility(View.VISIBLE);
            holder.relativeLayoutLeft.setVisibility(View.VISIBLE);
        } else if (info.getType().equals("接收")) {
            lp.gravity = Gravity.LEFT;
            lp.setMargins(10, 0, 10, 30);
            holder.body.setLayoutParams(lp);
//            holder.rightView.setVisibility(View.VISIBLE);
            holder.leftView.setVisibility(View.GONE);
            holder.body.setBackgroundResource(R.mipmap.chat_left);
            holder.relativeLayoutLeft.setVisibility(View.GONE);
        }
    }
/*
        private final TextView time;

        private final RelativeLayout relativeLayoutRight;
        private final ProgressBar progressBarRight;

        private final RelativeLayout relativeLayoutLeft;
        private final ProgressBar progressBarLeft;
 */
    private String getTime(String time) {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd    hh:mm:ss");
        String date = sDateFormat.format(new java.util.Date());
        if(date.substring(0,4) == time.substring(0,4)){
            if(date.substring(5,7) == time.substring(5,7)){
                if(date.substring(8,10) == time.substring(8,10)){
                    return time.substring(11,19);
                }else{
                    return time.substring(8,19);
                }
            }else{
                return time.substring(5,13);
            }
        }else{
            return time.substring(0,13);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.fragment_item_sms_chat, parent, false);
        view.setOnClickListener(this);
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    @Override
    public void onClick(View v) {
        if (recyclerView != null && listener != null) {
            int position = recyclerView.getChildAdapterPosition(v);
            listener.OnRecyclerItemClick(recyclerView, v, position, list.get(position));
        }
    }

    /**
     * 删除指定数据
     *
     * @param position 数据位置
     */
    public void remove(int position) {
        if (isDeleteAble) {
            isDeleteAble = false;

            list.remove(position);
            notifyItemRemoved(position);//这样就只会删除这一条数据，而不会一直刷
            notifyItemRangeChanged(position, getItemCount());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                        isDeleteAble = true;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

    }

    /**
     * 插入数据
     *
     * @param position 插入位置
     * @param data     插入的数据
     */
    public void insert(int position, SmsInfo data) {
        list.add(position, data);
        notifyItemInserted(position);

    }

    public void flashList(List<SmsInfo> data) {
        list = data;

    }

    public void insertList(List<SmsInfo> data) {
        if (data == null)
            return;

        if (list == null) {
            list = data;
        } else {
            list.addAll(data);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView body;
        private final TextView time;

        private final RelativeLayout relativeLayoutLeft;
        private final ProgressBar progressBarLeft;
        private final View leftView;

        public ViewHolder(View itemView) {
            super(itemView);
            body = (TextView) itemView.findViewById(R.id.client_chat);
            time = (TextView) itemView.findViewById(R.id.time);
            relativeLayoutLeft = (RelativeLayout) itemView.findViewById(R.id.relativeLayout_left);
            progressBarLeft = (ProgressBar) itemView.findViewById(R.id.progressBar_left);

            leftView = itemView.findViewById(R.id.left_view);
//            rightView = itemView.findViewById(R.id.right_view);
        }
    }

    /**
     * 自定义RecyclerView的点击事件
     */
    public interface OnRecyclerItemClickListener {
        void OnRecyclerItemClick(RecyclerView parent, View view, int position, SmsInfo data);
    }
}
