package com.yang.myapplication.Tools;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yang.myapplication.entity.NeighborInfo;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class JsonUtils {

    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
    	objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    }
    /**
     * 对象转换成json字符串
     * @param obj
     * @return
     */
    public static String toJson(Object obj) {
        Gson gson = new Gson();
        return gson.toJson(obj);
    }
    /**
     * json字符串转成对象
     * @param str
     * @param type
     * @return
     */
    public static <T> T fromJson(String str, Class<T> type) {
        Gson gson = new Gson();
        return gson.fromJson(str, type);
    }


    /**
     * @param json map的序列化结果
     * @param <K>   k类型
     * @param <V>   v类型
     * @return Map<K       ,       V>
     */
    public <K, V> Map<K, V> toMap(String json, Class<K> kClazz, Class<V> vClazz) {
        Gson gson = new Gson();
        return gson.fromJson(json, TypeToken.getParameterized(Map.class,kClazz,vClazz).getType());
    }
    public static <T> List<T> getList(List<T> mList, Class<T[]> aClass){
        Gson gson = new Gson();
        String toJson = gson.toJson(mList);
        T[] array = gson.fromJson(toJson , (Type) aClass);
        List<T> newList = Arrays.asList(array);
        return newList;
    }
    /**
     * List<T> 转 json字符串
     */

    public static <T> String listToJson(List<T> ts) {
        return JSON.toJSONString(ts);
    }

    public static HashMap<String, Object> objectToMap(Object obj) {
        if (obj == null) {
            return null;
        }
        HashMap<String, Object> map = new HashMap<String, Object>();
        try {
            Field[] declaredFields = obj.getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                field.setAccessible(true);
                map.put(field.getName(), field.get(obj));
            }
        } catch (Exception e) {

        }

        return map;
    }

    /**
     * 将对象转换成json字符串。
     * <p>Title: pojoToJson</p>
     * <p>Description: </p>
     * @param data
     * @return
     */
    public static String objectToJson(Object data) {
    	try {
			String string = objectMapper.writeValueAsString(data);
			return string;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
    	return null;
    }

    public static <T> T jsonToPojo(String jsonData, Class<T> beanType) {
        try {
            T t = objectMapper.readValue(jsonData, beanType);
            return t;
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return null;
    }

    // 定义jackson对象
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static JsonNode stringToJsonNode(String data) {
        try {
            JsonNode jsonNode = MAPPER.readTree(data);
            return jsonNode;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将json数据转换成pojo对象list
     * <p>Title: jsonToList</p>
     * <p>Description: </p>
     * @param jsonData
     * @param beanType
     * @return
     */
    public static <T>List<T> jsonToList(String jsonData, Class<T> beanType) {
    	JavaType javaType = objectMapper.getTypeFactory().constructParametricType(List.class, beanType);
    	try {
    		List<T> list = objectMapper.readValue(jsonData, javaType);
    		return list;
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	return null;
    }


    public static <T> T mapper(JsonNode node , Class<T> valueType) throws Exception{
        if (node == null||node.size()==0)
            return null;
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(node), valueType);
        } catch (Exception e) {
            throw e;
        }
    }

    public static String entittyToString(NeighborInfo info, String lastName) {
        String nei_name = info.getNeighborName();
        String nei_mac = info.getNeighborMac();
        int hop = info.getHop()+1;
        return nei_name +","+nei_mac+","+hop+","+ lastName;
    }

}
