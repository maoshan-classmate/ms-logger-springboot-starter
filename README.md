

#  ms-logger-springboot-starter【自定义日志组件】

使用Java开发的自定义日志组件，开箱即用！其中涉及到简单工厂模式、策略模式、自定义注解、Spring Event等技术点，让我们一起共勉！

## 功能

1、自带通用的日志打印模板。

2、提供两种不同的日志打印模板可供选择。

3、提供自定义增强日志处理器，可支持数据持久化操作。

## 配置属性（按需配置）

### application.yml

​	ms.logger.enable=true（默认开启模板日志打印） 可选（true/false）

​	ms.logger.strategy=simple (默认简单简单日志打印策略)   可选(simple/detail)

## 注解

### @MsLogger

注解可添加在类或方法上，类和方法上都添加以方法为准。

desc(日志描述) 默认为“ ”

handler() 默认为未实现增强日志的处理器接口，可通过自定义类实现该接口，增强日志处理。

## 效果展示

1、配置属性

![image-20240830194307731](C:\Users\WINDOWS\AppData\Roaming\Typora\typora-user-images\image-20240830194307731.png)

2、注解使用

![image-20240830194403751](C:\Users\WINDOWS\AppData\Roaming\Typora\typora-user-images\image-20240830194403751.png)

![image-20240830194422259](C:\Users\WINDOWS\AppData\Roaming\Typora\typora-user-images\image-20240830194422259.png)

3、简单日志打印策略

![image-20240830194201520](C:\Users\WINDOWS\AppData\Roaming\Typora\typora-user-images\image-20240830194201520.png)

4、详细日志打印策略

![image-20240830194552335](C:\Users\WINDOWS\AppData\Roaming\Typora\typora-user-images\image-20240830194552335.png)

5、自定义增强日志处理器

![image-20240830194615779](C:\Users\WINDOWS\AppData\Roaming\Typora\typora-user-images\image-20240830194615779.png)
