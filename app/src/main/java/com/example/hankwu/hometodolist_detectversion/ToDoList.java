package com.example.hankwu.hometodolist_detectversion;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
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
//            SwipeMenuItem openItem = new SwipeMenuItem(
//                    mAct.getApplicationContext());
//            // set item background
//
//            TextDrawable drawable = TextDrawable.builder()
//                    .beginConfig()
//                    .withBorder(5)
//                    .endConfig()
//                    .buildRect("完",mAct.getResources().getColor(R.color.Done));
//
//            openItem.setBackground(drawable);
////            // set item width
//            openItem.setWidth(dp2px(mAct,100));
////            // set item title
////            openItem.setTitle("Done");
////            // set item title fontsize
////            openItem.setTitleSize(18);
////            // set item title font color
////            openItem.setTitleColor(Color.WHITE);
//            // add to menu
//
//            menu.addMenuItem(openItem);
//
//            // create "delete" item
//            SwipeMenuItem laterItem = new SwipeMenuItem(
//                    mAct.getApplicationContext());
//
//            TextDrawable drawable2 = TextDrawable.builder()
//                    .beginConfig()
//                    .withBorder(5)
//                    .endConfig()
//                    .buildRect("遲",mAct.getResources().getColor(R.color.Later));
//
//            // set item background
//            laterItem.setBackground(drawable2);
//            laterItem.setWidth(dp2px(mAct,100));
////            laterItem.setTitle("Later");
////            laterItem.setTitleSize(18);
////            laterItem.setTitleColor(Color.WHITE);
//
//            // set item width// add to menu
//            menu.addMenuItem(laterItem);
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

            MaluProgressText drawable = MaluProgressText.builder()
                    .beginConfig()
                        .withBorder(5)
                        .progress(28)// 28%
                    .endConfig()
                    .buildRound(item.mGroup.substring(0,1),getGroupColor(item.mGroup));

            holder.group_icon.setImageDrawable(drawable);
            holder.title.setText(item.mTitle);
            holder.deadline.setText(item.mDeadline);
            holder.group_icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // control items
                    Toast.makeText(mAct, "iv_icon_click", Toast.LENGTH_SHORT).show();
                }
            });

            holder.title.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // control items
                    Toast.makeText(mAct,"iv_icon_click", Toast.LENGTH_SHORT).show();
                }
            });

            return convertView;
        }

        class ViewHolder {
            ImageView group_icon;
            TextView title;
            TextView deadline;

            public ViewHolder(View view) {
                group_icon = (ImageView) view.findViewById(R.id.group_icon);
                title = (TextView) view.findViewById(R.id.title);
                deadline = (TextView) view.findViewById(R.id.deadline);
                view.setTag(this);
            }
        }

        public boolean getSwipEnableByPosition(int position) {
            if(position % 2 == 0){
                return false;
            }
            return true;
        }

        public int getGroupColor(String group) {
            int c = Color.rgb(0x28,0x66,0x61);
            switch (group.substring(0,1)) {
                case "家":
                case "F":
                case "H":
                    c = Color.rgb(0x03,0x3f,0x63);
                    break;
                case "工":
                case "W":
                    c = Color.rgb(0xA3,0x00,0x36);
                    break;
                case "私":
                case "P":
                    c = Color.rgb(0xA8,0xC2,0x56);
                    break;
            }
            return c;
        }

    }
}
