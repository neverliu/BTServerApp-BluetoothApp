package com.mega.deviceapp.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mega.deviceapp.model.SmsInfo;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class JsonUitl {
 
    private static Gson mGson = new Gson();
 
    /**
     * 将json字符串转化成实体对象
     * @param json
     * @param classOfT
     * @return
     */
    public static Object stringToObject( String json , Class classOfT){
        return  mGson.fromJson( json , classOfT ) ;
    }
 
    /**
     * 将对象准换为json字符串 或者 把list 转化成json
     * @param object
     * @param <T>
     * @return
     */
    public static <T> String objectToString(T object) {
        return mGson.toJson(object);
    }
 
    /**
     * 把json 字符串转化成list
     * @param json
     * @param cls
     * @param <T>
     * @return
     */
    public static <T> List<T> stringToList(String json , Class<T> cls  ){
        Gson gson = new Gson();
        List<T> list = new ArrayList<T>();
       // JsonArray array = new JsonParser().parse(json).getAsJsonArray();
        //Json的解析类对象
        JsonParser parser = new JsonParser();
        //将JSON的String 转成一个JsonArray对象
        JsonArray jsonArray = parser.parse(json).getAsJsonArray();
        for(final JsonElement elem : jsonArray){
            list.add(gson.fromJson(elem, cls));
        }
        return list ;
    }

    public static  List<SmsInfo> jsonSmsStringToList(String json){
       // final Object clsType = obj;
       // Type type = new TypeToken<obj>(){}.getType();
        Type type = new TypeToken<List<SmsInfo>>(){}.getType();
        Gson gson = new Gson();
        List<SmsInfo> list = gson.fromJson(json, type);
        return list;
    }
 
}