package com.example.demo.hook;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import java.math.BigInteger;
import java.security.MessageDigest;

public class CustomLayoutInflater extends LayoutInflater {

    private static final String[] sClassPrefixList = {
            "android.widger.",
            "android.webkit."
    };

    protected CustomLayoutInflater(Context context) {
        super(context);
    }

    protected CustomLayoutInflater(LayoutInflater layoutInflater,Context newcontext){
        super(layoutInflater,newcontext);
    }

    @Override
    public LayoutInflater cloneInContext(Context newContext) {
        return new CustomLayoutInflater(this,newContext);
    }

    @Override
    protected View onCreateView(String name, AttributeSet attrs) throws ClassNotFoundException {
        for(String prefix : sClassPrefixList){
            try{
                View view = createView(name,prefix,attrs);
                if(view !=null){
                    return view;
                }
            }catch (ClassNotFoundException e){
                e.printStackTrace();
            }
        }

        return super.onCreateView(name, attrs);
    }


    @Override
    public View inflate(int resource, @Nullable ViewGroup root, boolean attachToRoot) {

        View viewGroup = super.inflate(resource,root,attachToRoot);
        View rootView = viewGroup;
        View tempView = viewGroup;

        //得到根View
        while (tempView !=null){
            rootView = viewGroup;
            tempView = (ViewGroup)tempView.getParent();
        }

        //遍历根View的所有子View
        traversalViewGroup(rootView);

        return viewGroup;

       // return super.inflate(resource, root, attachToRoot);
    }

    private void traversalViewGroup(View rootView){
        if(rootView !=null && rootView instanceof ViewGroup){
            if(rootView.getTag() ==null){
                //如果Tag的值已经存在，就不用再赋值
                rootView.setTag(getViewTag());
            }

            ViewGroup viewGroup =(ViewGroup) rootView;

            int childCount = viewGroup.getChildCount();
            for(int i=0;i<childCount;i++){
                View childView = viewGroup.getChildAt(i);
                if(childView.getTag() ==null){
                    childView.setTag(combineTag(getViewTag(),viewGroup.getTag().toString()));
                }

                Log.d(CustomLayoutInflater.class.getSimpleName(),"childView name="+childView.getClass().getName()+" id="+childView.getTag().toString());
                if(childView instanceof ViewGroup){
                    traversalViewGroup(childView);
                }
            }
        }

    }

    private String combineTag(String tag1, String tag2){
        return getMD5(getMD5(tag1)+getMD5(tag2));
    }

    public static String getMD5(String str){
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            return new BigInteger(1,md.digest()).toString(16);
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    private static int VIEW_TAG = 0x10000000;

    private static String getViewTag(){
        return String.valueOf(VIEW_TAG++);
    }
}
