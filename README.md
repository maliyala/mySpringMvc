# MySpringMVC
**自己手动写一个简化版通过注释扫描的SpringMVC框架**

|&emsp;[简介](#简介)&emsp; || &emsp;[运行流程及九大组件](# 运行流程及九大组件)&emsp; |
|&emsp;[设计思路](# 设计思路)&emsp;||&emsp;[结构](#结构)&emsp;|
|&emsp;[目的](#目的)&emsp; | 


## 简介

> SpringMVC以DispatcherServlet为核心，负责协调和组织不同组件以完成请求处理并返回响应的工作，实现了MVC模式。想要实现自己的SpringMVC框架，需要从以下几点入手：
  
&emsp;&emsp;一、了解SpringMVC运行流程及九大组件
  
&emsp;&emsp;二、梳理自己的SpringMVC的设计思路
  
&emsp;&emsp;三、实现自己的SpringMVC框架




## 运行流程及九大组件
### 1.1、SpringMVC的运行流程
![运行流程](https://static.oschina.net/uploads/space/2018/0222/091846_FTTR_3577599.png)

⑴ 用户发送请求至前端控制器DispatcherServlet

⑵ DispatcherServlet收到请求调用HandlerMapping处理器映射器。

⑶ 处理器映射器根据请求url找到具体的处理器，生成处理器对象及处理器
拦截器(如果有则生成)一并返回给DispatcherServlet。

⑷ DispatcherServlet通过HandlerAdapter处理器适配器调用处理器

⑸ 执行处理器(Controller，也叫后端控制器)。

⑹ Controller执行完成返回ModelAndView

⑺ HandlerAdapter将controller执行结果ModelAndView返回给DispatcherServlet

⑻ DispatcherServlet将ModelAndView传给ViewReslover视图解析器

⑼ ViewReslover解析后返回具体View

⑽ DispatcherServlet对View进行渲染视图（即将模型数据填充至视图中）。

⑾ DispatcherServlet响应用户。

从上面可以看出，DispatcherServlet有接收请求，响应结果，转发等作用。
有了DispatcherServlet之后，可以减少组件之间的耦合度。
### 1.2、SpringMVC的九大组件
【1. HandlerMapping】

        是用来查找Handler的。在SpringMVC中会有很多请求，每个请求都需要一个Handler处理，
    具体接收到一个请求之后使用哪个Handler进行处理呢？这就是HandlerMapping需要做的事。
【2. HandlerAdapter】

        从名字上看，它就是一个适配器。因为SpringMVC中的Handler可以是任意的形式，只要能
    处理请求就ok，但是Servlet需要的处理方法的结构却是固定的，都是以request和response为
    参数的方法。如何让固定的Servlet处理方法调用灵活的Handler来进行处理呢？这就是
    HandlerAdapter要做的事情。
        小结：Handler是用来干活的工具；HandlerMapping用于根据需要干的活找到相应的工具；
    HandlerAdapter是使用工具干活的人。
【3. HandlerExceptionResolver】

    其它组件都是用来干活的。在干活的过程中难免会出现问题，出问题后怎么办呢？这就需要有一个
    专门的角色对异常情况进行处理，在SpringMVC中就是HandlerExceptionResolver。具体来说，
    此组件的作用是根据异常设置ModelAndView，之后再交给render方法进行渲染。
【4. ViewResolver】

    ViewResolver用来将String类型的视图名和Locale解析为View类型的视图。View是用来渲染页
    面的，也就是将程序返回的参数填入模板里，生成html（也可能是其它类型）文件。这里就有两个
    关键问题：使用哪个模板？用什么技术（规则）填入参数？这其实是ViewResolver主要要做的工作，
    ViewResolver需要找到渲染所用的模板和所用的技术（也就是视图的类型）进行渲染，具体的渲染
    过程则交由不同的视图自己完成。
【5. RequestToViewNameTranslator】

    ViewName是根据ViewName查找View，但有的Handler处理完后并没有设置View也没有设置
    ViewName，这时就需要从request获取ViewName了，如何从request中获取ViewName就是
    RequestToViewNameTranslator要做的事情了。RequestToViewNameTranslator在
    Spring MVC容器里只可以配置一个，所以所有request到ViewName的转换规则都要在一个
    Translator里面全部
    实现。
【6. LocaleResolver】

    解析视图需要两个参数：一是视图名，另一个是Locale。视图名是处理器返回的，Locale是从哪里来
    的？这就是LocaleResolver要做的事情。LocaleResolver用于从request解析出Locale，Locale
    就是zh-cn之类，表示一个区域，有了这个就可以对不同区域的用户显示不同的结果。SpringMVC主要有
    两个地方用到了Locale：一是ViewResolver视图解析的时候；二是用到国际化资源或者主题的时候。
【7. ThemeResolver】

    用于解析主题。SpringMVC中一个主题对应一个properties文件，里面存放着跟当前主题相关的所有
    资源、如图片、css样式等。SpringMVC的主题也支持国际化，同一个主题不同区域也可以显示不同的
    风格。SpringMVC中跟主题相关的类有 ThemeResolver、ThemeSource和Theme。主题是通过一系
    列资源来具体体现的，要得到一个主题的资源，首先要得到资源的名称，这是ThemeResolver的工作。
    然后通过主题名称找到对应的主题（可以理解为一个配置）文件，这是ThemeSource的工作。最后从
    主题中获取资源就可以了。
【8. MultipartResolver】

    用于处理上传请求。处理方法是将普通的request包装成MultipartHttpServletRequest，后者可
    以直接调用getFile方法获取File，如果上传多个文件，还可以调用getFileMap得到FileName->File
    结构的Map。此组件中一共有三个方法，作用分别是判断是不是上传请求，将request包装成
    MultipartHttpServletRequest、处理完后清理上传过程中产生的临时资源。
【9. FlashMapManager】

    用来管理FlashMap的，FlashMap主要用在redirect中传递参数
## 设计思路
本文只实现注解读取，实现了@MyController，@MyRequestMapping，@MyRequestParam的注释。
简化流程为：
### 2.1、加载配置
- 初始化加载配置文件，从web.xml中获取到加载类地址，和配置文件地址。
### 2.2、初始化
DispatcherServlet的初始化会加载上述的9大组件，但是本文只以最简单实现：
- 加载配置文件。
- 初始化相关的类，即获取配置的扫描包下的类。
- 拿到扫描的类，通过reflect.instance(反射机制的实例化），放到iocMap中，beanNmae，参见日常使用时类名首字母小写。
- 初始化HandlerMapping，即让url与method相对应。
### 2.3、运行
- 加拦截
- 获取请求传入的参数并处理参数
- 通过初始化好的handlerMapping中拿出url对应的方法名，反射调用



## 结构

```
|-src
   |-main
      |-java
      |    |-com.mituan
      |              |-annotation     // 
      |              |     |-MyController           // 自定义@Controller注解
      |              |     |-MyRequestMapping       // 自定义@RequestMappin注解
      |              |     |-MyRequestParam         // 自定义@RequestParam注解
      |              |
      |              |-core.controller              // controller层
      |              |-servlet                      
      |              |     |-MyDispatcherServlet    // 自己实现的DispatcherServlet 
      |              
      |-resources
            |-application.properties                // 配置文件

```


## 目的
   
&emsp;&emsp;是为了更好的学习spring框架，后续还会自己实现Spring IOC，Spring AOP。


