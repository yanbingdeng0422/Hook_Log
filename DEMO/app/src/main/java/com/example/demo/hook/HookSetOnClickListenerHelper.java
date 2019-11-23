package com.example.demo.hook;

import android.content.Context;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class HookSetOnClickListenerHelper {

    private static final String TAG = HookSetOnClickListenerHelper.class.getSimpleName();


    public static void hook(Context context,final View view){
        try {
            // 反射执行View类的getListenerInfo()方法，拿到v的mListenerInfo对象，这个对象就是点击事件的持有者
            Method method = View.class.getDeclaredMethod("getListenerInfo");
            method.setAccessible(true);//由于getListenerInfo()方法并不是public的，所以要加这个代码来保证访问权限
            Object mListenerInfo = method.invoke(view);//这里拿到的就是mListenerInfo对象，也就是点击事件的持有者

            //要从这里面拿到当前的点击事件对象
            Class<?> listenerInfoClz = Class.forName("android.view.View$ListenerInfo");// 这是内部类的表示方法
            Field field = listenerInfoClz.getDeclaredField("mOnClickListener");
            final View.OnClickListener onClickListenerInstance = (View.OnClickListener) field.get(mListenerInfo);//取得真实的mOnClickListener对象

            //2. 创建我们自己的点击事件代理类
            //   方式1：自己创建代理类
            //   ProxyOnClickListener proxyOnClickListener = new ProxyOnClickListener(onClickListenerInstance);
            //   方式2：由于View.OnClickListener是一个接口，所以可以直接用动态代理模式
            Object proxyOnClickListener = Proxy.newProxyInstance(context.getClass().getClassLoader(), new Class[]{View.OnClickListener.class}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    Log.d("HookSetOnClickListener", "点击事件被hook到了1");//加入自己的逻辑
                    return method.invoke(onClickListenerInstance, args);//执行被代理的对象的逻辑
                }
            });
            //3. 用我们自己的点击事件代理类，设置到"持有者"中
            field.set(mListenerInfo, proxyOnClickListener);
            //完成
        }catch (Exception e){
            Log.d(TAG,"Exception:"+e.toString());
        }
    }

    // 还真是这样,自定义代理类
    static class ProxyOnClickListener implements View.OnClickListener {
        View.OnClickListener oriLis;

        public ProxyOnClickListener(View.OnClickListener oriLis) {
            this.oriLis = oriLis;
        }

        @Override
        public void onClick(View v) {
            Log.d("HookSetOnClickListener", "点击事件被hook到了2");
            if (oriLis != null) {
                oriLis.onClick(v);
            }
        }
    }


    public static void hookLayoutInflater()throws Exception{

        //获取ServiceFetcher的实例ServiceFetcherImpl
        Class<?> ServiceFetcher = Class.forName("android.app.SystemServiceRegistry$ServiceFetcher");

        //Proxy.newProxyInstance返回的对象会实现指定的接口
        Object ServiceFetcherImpl = Proxy.newProxyInstance(HookSetOnClickListenerHelper.class.getClassLoader(),new Class[]{ServiceFetcher},new ServiceFetcherHandler());

        Class<?> SystemServiceRegistry = Class.forName("android.app.SystemServiceRegistry");

        Method registerService = SystemServiceRegistry.getDeclaredMethod("registerService",String.class,CustomLayoutInflater.class.getClass(),ServiceFetcher);

        registerService.setAccessible(true);
        registerService.invoke(SystemServiceRegistry,new Object[]{Context.LAYOUT_INFLATER_SERVICE,CustomLayoutInflater.class,ServiceFetcherImpl});
    }
}