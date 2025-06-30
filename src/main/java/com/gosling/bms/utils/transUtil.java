package com.gosling.bms.utils;

import org.w3c.dom.ls.LSInput;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.hutool.core.map.MapUtil.removeNullValue;

/**
 * 将对象映射成Map
 *
 * @author hhhhlkf
 * @date 2023-01-04 - 9:23
 */
public class transUtil {
    public static Map<String, Object> transBean2Map(Object obj) {
        if (obj == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor property : propertyDescriptors) {
                String key = property.getName();
                // 过滤class属性
                if (!key.equals("class")) {
                    // 得到property对应的getter方法
                    Method getter = property.getReadMethod();
                    Object value = getter.invoke(obj);

                    map.put(key, value);
                }

            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return map;
    }

    public static <T> List<Map<String, Object>> removeListNullValue(List<T> list) {
        ArrayList<Map<String, Object>> arrayList = new ArrayList<>();
        for (T item : list) {
            Map<String, Object> map = transBean2Map(item);
            removeNullValue(map);
            arrayList.add(map);
        }
        return arrayList;
    }
}
