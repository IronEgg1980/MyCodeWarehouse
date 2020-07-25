package yzw.ahaqth.mycodewarehouse;

import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public abstract class MyAdapter<T> extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    protected int parentWidth;
    private List<T> mList;

    public MyAdapter(List<T> list) {
        this.mList = list;
    }

    @Override
    public int getItemViewType(int position) {
        return getLayoutId(position);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        bindData(holder, mList.get(position));
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.parentWidth = parent.getMeasuredWidth();
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public abstract void bindData(MyViewHolder myViewHolder, T data);

    public abstract int getLayoutId(int position);

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private SparseArray<View> views;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            this.views = new SparseArray<>();
            this.itemView.setClickable(true);
            this.itemView.setLongClickable(true);
        }

        public <T extends View> T getView(int id) {
            View view = views.get(id);
            if (view == null) {
                view = itemView.findViewById(id);
                views.put(id, view);
            }
            return (T) view;
        }

        public MyViewHolder setText(int id, String text) {
            TextView textView = getView(id);
            textView.setText(text);
            return this;
        }

        public MyViewHolder setImage(int id, int resoursId) {
            ImageView imageView = getView(id);
            imageView.setImageResource(resoursId);
            return this;
        }

        public MyViewHolder setImage(int id, Drawable drawable) {
            ImageView imageView = getView(id);
            imageView.setImageDrawable(drawable);
            return this;
        }
    }
}
