# Android 无痕埋点

标签： 无痕埋点 全埋点

---
##无痕埋点定义
无痕埋点 = 全埋点
俩着定义一样：都市通过基础代码在所有页面及页面路径上的可交互事件元素上放置监听器来实现数据采集；

##无痕埋点实例

###**问题**
在开发过程中，软件需求是对事件进行统计，比如，对某个界面启动次数的统计，或者对某个按钮点击次数的统计；

###**解决方案**

**方案一：Hook LayoutInflater**
Hook LayoutInflater 是通过调用context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)来返回自定义的CustomLayoutInflater的，然后覆写inflate方法就可以得到对应的View Tree;

由源码可知，getSystemService方法最终会得到SystemServiceRegistry.SYSTEM_SERVICE_FETCHERS代码如下：
SystemServiceRegistry.java
```Java
    private static final HashMap<String, ServiceFetcher<?>> SYSTEM_SERVICE_FETCHERS =new HashMap<String, ServiceFetcher<?>>();

    public static Object getSystemService(ContextImpl ctx, String name) {
        ServiceFetcher<?> fetcher = SYSTEM_SERVICE_FETCHERS.get(name);
        return fetcher != null ? fetcher.getService(ctx) : null;
    }
```
SYSTEM_SERVICE_FETCHERS的赋值在SystemServiceRegistry.registerService中；
SystemServiceRegistry.java
```Java
    private static <T> void registerService(String serviceName, Class<T> serviceClass,
            ServiceFetcher<T> serviceFetcher) {
        SYSTEM_SERVICE_NAMES.put(serviceClass, serviceName);
        SYSTEM_SERVICE_FETCHERS.put(serviceName, serviceFetcher);
    }
```
而调用registerService方法是在static中：
SystemServiceRegistry.java
```
    static {
    ...
        registerService(Context.LAYOUT_INFLATER_SERVICE, LayoutInflater.class,
                new CachedServiceFetcher<LayoutInflater>() {
            @Override
            public LayoutInflater createService(ContextImpl ctx) {
                return new PhoneLayoutInflater(ctx.getOuterContext());
            }});
    ...        
    }
```
由以上代码可知，可以反射调用registerService,将自定义的CustomLayoutInflater注册进去，替换调原本的LayoutInflater，这样当系统获取LayoutInflater的时候，得到的是CustomLayoutInflater；

具体代码实践如下：

```Java
public class HookSetOnClickListenerHelper {

    private static final String TAG = HookSetOnClickListenerHelper.class.getSimpleName();

    public static void hookLayoutInflater()throws Exception{

        //获取ServiceFetcher的实例ServiceFetcherImpl
        Class<?> ServiceFetcher = Class.forName("android.app.SystemServiceRegistry$ServiceFetcher");

        //Proxy.newProxyInstance返回的对象会实现指定的接口
        Object ServiceFetcherImpl = Proxy.newProxyInstance(HookSetOnClickListenerHelper.class.getClassLoader(),new Class[]{ServiceFetcher},new ServiceFetcherHandler());

        //获取到SystemServiceRegistry的registerService方法
        Class<?> SystemServiceRegistry = Class.forName("android.app.SystemServiceRegistry");

        Method registerService = SystemServiceRegistry.getDeclaredMethod("registerService",String.class,CustomLayoutInflater.class.getClass(),ServiceFetcher);

        registerService.setAccessible(true);
        //调用registerService方法，将自定义的CustomLayoutInflater设置到SystemServiceRegistry
        registerService.invoke(SystemServiceRegistry,new Object[]{Context.LAYOUT_INFLATER_SERVICE,CustomLayoutInflater.class,ServiceFetcherImpl});
    }
}
```

```Java
ServiceFetcherHandler.java
public class ServiceFetcherHandler implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //当调用ServiceFetcherImpl的getService的时候，会返回自定义的LayoutInflater
        return new CustomLayoutInflater((Context)args[0]);
    }
}
```
CustomLayoutInflater参考了系统自带的PhoneLayoutInflater,加上自己生成的View ID;
```Java
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
```
最后在Activity onCreate方法中调用 HookSetOnClickListenerHelper.hookLayoutInflater()即可；

具体log:
```
1970-01-23 01:49:50.312 4633-4633/com.example.demo D/HookSetOnClickListener: 点击事件被hook到了1
1970-01-23 01:49:50.327 4633-4633/com.example.demo D/CustomLayoutInflater: childView name=androidx.appcompat.widget.AppCompatTextView id=463236ab8d10e1eb78d613ea53af76aa
```
