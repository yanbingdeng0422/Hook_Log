package com.example.demo.hook;

import android.content.Context;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ServiceFetcherHandler implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return new CustomLayoutInflater((Context)args[0]);
    }
}
