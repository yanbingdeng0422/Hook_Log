package com.example.demo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.demo.hook.HookSetOnClickListenerHelper;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG,"onCreate");
        Button startActivity = findViewById(R.id.startActivy);
        startActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "别点啦，再点我咬你了...", Toast.LENGTH_SHORT).show();
//                Intent intent = new Intent(getApplicationContext(),Activity_B.class);
//                startActivityForResult(intent,-1);
            }
        });

        HookSetOnClickListenerHelper.hook(this, startActivity);//这个hook的作用，是 用我们自己创建的点击事件代理对象，替换掉之前的点击事件。
        //所以，这个hook动作，必须在setOnClickListener之后，不然就不起作用
        try {
            HookSetOnClickListenerHelper.hookLayoutInflater();
        } catch (Exception e) {
            e.printStackTrace();
        }

        PackageManager manager = getPackageManager();
        Intent intent =new Intent(Intent.ACTION_MAIN,null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> appList = manager.queryIntentActivities(intent,0);

        for(int i=0;i<appList.size();i++){
            ResolveInfo info = appList.get(i);
            Log.d(TAG,"packageName="+info.activityInfo.packageName+
                    " info.toString="+info.activityInfo.toString()
            +" appName="+info.activityInfo.loadLabel(manager).toString());
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG,"onActi<vityResult requestCode="+requestCode +" resultCode="+resultCode);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            Thread.sleep(12000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
