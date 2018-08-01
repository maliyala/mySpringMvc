package com.mituan.servlet;


import com.mituan.annotation.MyController;
import com.mituan.annotation.MyRequestMapping;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MyDispatcherServlet extends HttpServlet{
    private Properties properties = new Properties();

    private List<String> classNames = new ArrayList<>();

    private Map<String,Object> iocMap = new HashMap<>();

    private Map<String,Method> handlerMapping = new HashMap<>();

    private Map<String,Object> controllerMap = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println(111111);
        //1.加载配置文件。
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //2.初始化相关的类，即获取配置的扫描包下的类。
        doScanner(properties.getProperty("scanPackage"));
        //3.拿到扫描的类，通过reflect.instance(反射机制的实例化），放到iocMap中，beanNmae，参见日常使用时类名首字母小写。
        doInstance();
        //4.初始化HandlerMapping，即让url与method相对应。
        initHandlerMapping();

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //处理请求
        try {
            doDispatch(req,resp);
        } catch (Exception e) {
            resp.getWriter().write("500,Server Exception");
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        //判断有没有url值
        if(handlerMapping.isEmpty()){
            return;
        }
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        //获取地址
        url = url.replace(contextPath,"").replaceAll("/+","/");
        //没有该地址报错404，
        if(!handlerMapping.containsKey(url)){
            resp.getWriter().write("404! NOT FOUND");
            return;
        }

        Method method = this.handlerMapping.get(url);
        //获取方法的参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();
        //获取请求的参数
        Map<String,String[]> paramterMap = req.getParameterMap();
        //保存参数值
        Object[] paramValues = new Object[parameterTypes.length];
        //获取方法的参数列表
        for(int i = 0;i<parameterTypes.length;i++){
            //根据参数名称，做出处理。
            String requestParam = parameterTypes[i].getSimpleName();
            if(requestParam.equals("HttpServletRequest")){
                paramValues[i] = req;
                continue;
            }
            if(requestParam.equals("HttpServletResponse")){
                paramValues[i] = resp;
                continue;
            }
            if(requestParam.equals("String")){
                for(Entry<String,String[]> param :paramterMap.entrySet()){
                    String value =Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "")
                            .replaceAll(",\\s", ",");
                    paramValues[i] = value;
                }
            }
            try {
                method.invoke(this.controllerMap.get(url),paramValues);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }




    }

    private void doLoadConfig(String contextConfigLocation) {
        //1.把web.xml中的contextConfigLocation属性所对应的文件读取到数据流中
        InputStream resourceInputStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        //2.用Properties类加载上述流中数据
        try {
            properties.load(resourceInputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            //3.关闭数据流
            if(null!=resourceInputStream){
                try {
                    resourceInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doScanner(String scanPackage) {
        //1、将.转换成/,记得用转义字符
        URL url = this.getClass().getClassLoader().getResource(scanPackage.replaceAll("\\.","/"));
        //2、获取文件
        File dir = new File(url.getFile());
        //3、获取所有的类放入list中
        for(File file:dir.listFiles()){
            if(file.isDirectory()){
                //递归获取所有
                doScanner(scanPackage+"."+file.getName());
            }else{
                String classname = scanPackage+"."+file.getName().replace(".class","");
                classNames.add(classname);
            }
        }

    }

    private void doInstance() {
        //1、判断classNames是否为空
        if(classNames.isEmpty()){
            return ;
        }
        //2、循环拿出className，判断是否注解为MyController
        for(String className : classNames) {

            try {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(MyController.class)) {
                    //3、判断是的放入实例化后并放入iocMap中
                    iocMap.put(toLowerFirstWord(clazz.getSimpleName()), clazz.newInstance());
                } else {
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    private void initHandlerMapping() {
        //1、判断iocMap是否为空
        if(iocMap.isEmpty()){
            return;
        }
        //2、循环进入拿到的类中
        try {
            for(Entry<String,Object> entry: iocMap.entrySet()){
                Class<? extends Object> clazz = entry.getValue().getClass();
                if(!clazz.isAnnotationPresent(MyController.class)){
                    continue;
                }
                //3、判断出MyRequestMapping注解的类，获取baseUrl
                String baseUrl = "";
                String url = "";
                if(clazz.isAnnotationPresent(MyRequestMapping.class)){
                    MyRequestMapping annotation = clazz.getAnnotation(MyRequestMapping.class);
                    baseUrl = annotation.value();
                }
                //4、判断MyRequestMapping的方法获取到完整的url，并完成初始化放入controllerMap中
                Method[] methods = clazz.getMethods();
                for(Method method : methods){
                    if(!method.isAnnotationPresent(MyRequestMapping.class)){
                        continue;
                    }
                    MyRequestMapping annotation = method.getAnnotation(MyRequestMapping.class);
                    url = annotation.value();
                    url = (baseUrl+"/"+url).replaceAll("/+","/");
                    handlerMapping.put(url,method);
                    System.out.println(url+","+method);
                }
                controllerMap.put(url,clazz.newInstance());
                url = "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //
        //
    }
    /**
     * 把首字母大写转成小写
     * @param simpleName
     * @return
     */
    private String toLowerFirstWord(String simpleName) {
        char[] charArray = simpleName.toCharArray();
        charArray[0]+= 32;
        return String.valueOf(charArray);
    }
}
