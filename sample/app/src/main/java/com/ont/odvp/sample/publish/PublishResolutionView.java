package com.ont.odvp.sample.publish;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.ont.odvp.sample.R;
import com.ont.media.odvp.model.Resolution;
import com.ont.odvp.sample.def.IResolutonViewEventListener;

import java.util.ArrayList;

/**
 * Created by betali on 2/3/15.
 */
public class PublishResolutionView extends PopupWindow{

    private ListView mCameraResolutionsListView;
    private ResolutoinViewAdapter mResolutionAdapter;
    private Context mContext;
    private IResolutonViewEventListener mResolutionViewEventListener;
    private int mSelectedSizeWidth;
    private int mSelectedSizeHeight;

    public PublishResolutionView(Context context) {
        super(context);
        init(context);
    }

    public PublishResolutionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PublishResolutionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void setResolutionViewEventListener(IResolutonViewEventListener resolutionViewEventListener) {
        this.mResolutionViewEventListener = resolutionViewEventListener;
    }

    public void setSelectedResolution(Resolution selectedSize){
        mSelectedSizeWidth = selectedSize.width;
        mSelectedSizeHeight = selectedSize.height;
    }

    private void init(Context context) {

        mContext = context;
        View view = LayoutInflater.from(mContext).inflate(R.layout.layout_camera_resolutions, null);
        setContentView(view);

        mCameraResolutionsListView = view.findViewById(R.id.camera_resolutions_listview);
        mCameraResolutionsListView.setVerticalScrollBarEnabled(false); //隐藏侧滑栏
        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);

        //设置Item监听
        mCameraResolutionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // 刷新listview
                mResolutionAdapter.notifyDataSetChanged();//调用getView
                mResolutionViewEventListener.onSelectResolutionUpdate(mResolutionAdapter.getItem(position));

                if (isShowing()) {
                    dismiss();
                }
            }
        });

        setOutsideTouchable(true);
        setBackgroundDrawable(new ColorDrawable());
        setFocusable(true);

        mResolutionAdapter = new ResolutoinViewAdapter();
        mCameraResolutionsListView.setAdapter(mResolutionAdapter);
    }

    public void updateCameraResolutions(ArrayList<Resolution> cameraResolutions) {

        mResolutionAdapter.updateCameraResolutions(cameraResolutions);
    }

    @Override
    public void showAsDropDown(View anchor, int x, int y) {

        setAnimationStyle(R.style.PopupWindowAnimStyle);
        super.showAsDropDown(anchor, x, y);
    }

    class ResolutoinViewAdapter extends BaseAdapter {

        ArrayList<Resolution> mCameraResolutions;

        private void updateCameraResolutions(ArrayList<Resolution> cameraResolutions) {
            this.mCameraResolutions = cameraResolutions;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {

            if (mCameraResolutions == null) {
                return 0;
            } else {
                return mCameraResolutions.size();
            }
        }

        @Override
        public Resolution getItem(int i) {

            if (mCameraResolutions == null) {
                return null;
            } else {
                return mCameraResolutions.get(i);
            }
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {

            ViewHolder holder;
            if (convertView == null) {

                convertView = LayoutInflater.from(mContext).inflate(R.layout.popup_item, null);
                holder = new ViewHolder();
                holder.resolutionText = convertView.findViewById(R.id.tv_pop);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Resolution size =  getItem(i);

            if(size.width == mSelectedSizeWidth && size.height == mSelectedSizeHeight){
                holder.resolutionText.setBackgroundColor(mContext.getResources().getColor(R.color.colorBackground0));
            } else {
                holder.resolutionText.setBackgroundColor(Color.WHITE);
            }

            String resolutionText = size.width + " x " + size.height;
            holder.resolutionText.setText(resolutionText);
            return convertView;
        }

        public class ViewHolder {
            public TextView resolutionText;
        }
    }
}
