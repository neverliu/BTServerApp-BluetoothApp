package com.mega.deviceapp.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mega.deviceapp.R;
import com.mega.deviceapp.model.SmsInfo;
import com.mega.deviceapp.util.TimeUtil;

import java.util.List;

/**
 * @author liuhao
 * @time 2017/10/25 11:43
 * @des ${TODO}
 * @email liuhao_nevermore@163.com
 */

public class SmsAdapter extends RecyclerView.Adapter<SmsAdapter.ViewHolder> implements View.OnClickListener  {
    private Context context;
    private List<SmsInfo> list;
    private static final String TAG = "SmsAdapter";
    private OnRecyclerItemClickListener listener;
    private RecyclerView recyclerView;
    private boolean isDeleteAble = true;

    public void setOnRecyclerItemClickListener(OnRecyclerItemClickListener listener) {
        this.listener = listener;
    }

    public SmsAdapter(Context context, List<SmsInfo> list) {
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
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.person.setText(list.get(position).getPerson().equals("0") ? list.get(position).getAddress() : list.get(position).getPerson()
        +"("+list.get(position).getAddress()+")");
//        if(list.get(position).getRead() == 1){
//            holder.type.setVisibility(View.GONE);
//        }else{
//            holder.type.setVisibility(View.VISIBLE);
//        }
        holder.body.setText(list.get(position).getBody());
        holder.date.setText(TimeUtil.formatDisplayTime(list.get(position).getDate(), "yyyy-MM-dd HH:mm:ss", context));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_list_sms,parent,false);
        view.setOnClickListener(this);
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }
    @Override
    public void onClick(View v) {
        if (recyclerView != null && listener != null &&list != null){
            int position = recyclerView.getChildAdapterPosition(v);
            listener.OnRecyclerItemClick(recyclerView,v,position,list.get(position));
        }
    }
    /**
     * 删除指定数据
     * @param position 数据位置
     */
    public void remove(int position){
        if(isDeleteAble){
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
     * @param position 插入位置
     * @param data 插入的数据
     */
    public void insert(int position,SmsInfo data){
        list.add(position,data);
        notifyItemInserted(position);

    }
    public void flashList(List<SmsInfo> data){
        Log.d(TAG,"liuhao flashList");
        list = data;

    }
    public void insertList(List<SmsInfo> data){
        if(data == null)
            return;

        if(list == null){
            list = data;
        }else{
            list.addAll(data);
        }
    }
    public static class ViewHolder extends RecyclerView.ViewHolder{

        private final TextView person;
        private final ImageView type;
        private final TextView body;
        private final TextView date;

        public ViewHolder(View itemView) {
            super(itemView);
            person = (TextView) itemView.findViewById(R.id.person);
            type = (ImageView) itemView.findViewById(R.id.read);
            body = (TextView) itemView.findViewById(R.id.body);
            date = (TextView) itemView.findViewById(R.id.date);
        }
    }
    /**
     * 自定义RecyclerView的点击事件
     */
    public interface OnRecyclerItemClickListener{
        void OnRecyclerItemClick(RecyclerView parent, View view, int position, SmsInfo data);
    }
}
