

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

![image-20240830194307731](https://github.com/user-attachments/assets/313e6922-763d-4bab-bb0f-3804beb951a6)

2、注解使用

![image](https://github.com/user-attachments/assets/1251bea5-2178-4cf8-8b23-bcebeb7efac4)

3、简单日志打印策略

![image-20240830194201520](https://github.com/user-attachments/assets/f3b89e26-240e-47eb-8871-816a63e0cd50)

4、详细日志打印策略

![image-20240830194552335](https://github.com/user-attachments/assets/4aef1efc-a482-436f-b65b-d71cc22adc98)

5、自定义增强日志处理器

![image-20240830194615779](https://github.com/user-attachments/assets/5210e32d-67d7-487d-93b5-7de72d848ca5)

