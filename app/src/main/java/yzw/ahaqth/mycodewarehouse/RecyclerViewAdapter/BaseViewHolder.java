package yzw.ahaqth.mycodewarehouse.RecyclerViewAdapter;

import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class BaseViewHolder extends RecyclerView.ViewHolder {
    private SparseArray<View> views;

    BaseViewHolder(@NonNull View itemView) {
        super(itemView);
        this.views = new SparseArray<>();
        this.itemView.setClickable(true);
        this.itemView.setLongClickable(true);
    }

    private  <T extends View> T getView(int id){
        View view = views.get(id);
        if(view == null){
            view = itemView.findViewById(id);
            views.put(id,view);
        }
        return (T) view;
    }

    public BaseViewHolder setText(int id,String text){
        TextView textView = getView(id);
        textView.setText(text);
        return this;
    }

    public BaseViewHolder setImage(int id,int resoursId){
        ImageView imageView = getView(id);
        imageView.setImageResource(resoursId);
        return this;
    }

    public BaseViewHolder setImage(int id, Drawable drawable){
        ImageView imageView = getView(id);
        imageView.setImageDrawable(drawable);
        return this;
    }
}
