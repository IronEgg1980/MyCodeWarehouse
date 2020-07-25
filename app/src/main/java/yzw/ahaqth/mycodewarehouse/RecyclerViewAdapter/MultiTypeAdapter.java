package yzw.ahaqth.mycodewarehouse.RecyclerViewAdapter;

import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import java.util.List;

public abstract class MultiTypeAdapter<E extends ItemViewTypeLayoutConvert> extends BaseAdapter<E> {
    private SparseIntArray layoutIds;

    public MultiTypeAdapter(List<E> list) {
        super(-1,list);
        this.layoutIds = new SparseIntArray();
    }

    @Override
    public int getItemViewType(int position) {
        E e = mList.get(position);
        layoutIds.put(e.getItemType(),e.getLayoutId());
        return e.getItemType();
    }

    @NonNull
    @Override
    public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = layoutIds.get(viewType);
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId,parent,false);
        return new BaseViewHolder(view);
    }
}
