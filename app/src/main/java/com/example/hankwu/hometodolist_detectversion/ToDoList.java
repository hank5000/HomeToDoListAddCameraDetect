package com.example.hankwu.hometodolist_detectversion;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;

import java.util.ArrayList;

/**
 * Created by HankWu on 16/10/17.
 */
public class ToDoList {

    ToDoListMenuCreator creator = null;

    private int dp2px(Activity act,int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                act.getResources().getDisplayMetrics());
    }

    public SwipeMenuCreator getCreator() {
        return creator;
    }

    public static class ToDoItem {
        final static String UNKNOWN = "未知";

        String mTitle;
        String mDeadline;
        String mGroup;
        String mStatus;
        int mSheetPosition;

        public ToDoItem() {
            mTitle = UNKNOWN;
            mDeadline = UNKNOWN;
            mGroup = UNKNOWN;
            mStatus = UNKNOWN;
            mSheetPosition = -1;
        }
    }



    public ToDoList(final Activity act) {
        // Setting SwipeMenu UI
        creator = new ToDoListMenuCreator(act);
    }

    class ToDoListMenuCreator implements SwipeMenuCreator {
        Activity mAct = null;

        public ToDoListMenuCreator(Activity act) {
            mAct = act;
        }

        @Override
        public void create(SwipeMenu menu) {
            // create "open" item
            SwipeMenuItem openItem = new SwipeMenuItem(
                    mAct.getApplicationContext());
            // set item background
            openItem.setBackground(new ColorDrawable(Color.rgb(0xC9, 0xC9,
                    0xCE)));
            // set item width
            openItem.setWidth(dp2px(mAct,90));
            // set item title
            openItem.setTitle("Open");
            // set item title fontsize
            openItem.setTitleSize(18);
            // set item title font color
            openItem.setTitleColor(Color.WHITE);
            // add to menu
            menu.addMenuItem(openItem);

            // create "delete" item
            SwipeMenuItem deleteItem = new SwipeMenuItem(
                    mAct.getApplicationContext());
            // set item background
            deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
                    0x3F, 0x25)));
            // set item width
            deleteItem.setWidth(dp2px(mAct,90));
            // set a icon
            deleteItem.setIcon(R.drawable.ic_delete);
            // add to menu
            menu.addMenuItem(deleteItem);
        }
    }



    static class Adapter extends BaseAdapter {

        ArrayList<ToDoItem> items = null;
        Activity mAct = null;

        public Adapter(Activity act, ArrayList<ToDoList.ToDoItem> is) {
            items = is;
            mAct = act;
        }

        @Override
        public int getCount() {
            return items.size();
        }
        @Override
        public ToDoList.ToDoItem getItem(int position) {
            return items.get(position);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(mAct.getApplicationContext(),
                        R.layout.item_list_app, null);
                new ViewHolder(convertView);
            }

            ViewHolder holder = (ViewHolder) convertView.getTag();
            ToDoList.ToDoItem item = getItem(position);

            holder.iv_icon.setImageResource(R.drawable.ic_delete);
            holder.tv_name.setText(item.mTitle);

            holder.iv_icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // control items
                    Toast.makeText(mAct, "iv_icon_click", Toast.LENGTH_SHORT).show();
                }
            });

            holder.tv_name.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // control items
                    Toast.makeText(mAct,"iv_icon_click", Toast.LENGTH_SHORT).show();
                }
            });

            return convertView;
        }

        class ViewHolder {
            ImageView iv_icon;
            TextView tv_name;

            public ViewHolder(View view) {
                iv_icon = (ImageView) view.findViewById(R.id.iv_icon);
                tv_name = (TextView) view.findViewById(R.id.tv_name);
                view.setTag(this);
            }
        }

        public boolean getSwipEnableByPosition(int position) {
            if(position % 2 == 0){
                return false;
            }
            return true;
        }
    }
}
