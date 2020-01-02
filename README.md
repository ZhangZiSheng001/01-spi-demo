# 目录

* [什么是SPI机制](#什么是spi机制)
* [怎么使用SPI](#怎么使用spi)
  * [需求](#需求)
  * [工程环境](#工程环境)
  * [主要步骤](#主要步骤)
  * [创建项目](#创建项目)
  * [引入依赖](#引入依赖)
  * [编写用户服务类接口](#编写用户服务类接口)
  * [编写接口实现类](#编写接口实现类)
  * [配置接口文件](#配置接口文件)
  * [编写测试方法](#编写测试方法)
  * [测试结果](#测试结果)
* [SPI在JDBC中的应用](#spi在jdbc中的应用)
* [源码分析](#源码分析)
  * [创建一个ServiceLoader](#创建一个serviceloader)
  * [创建一个迭代器](#创建一个迭代器)
  * [加载配置文件](#加载配置文件)
  * [实例化接口实现类](#实例化接口实现类)
* [参考资料](#参考资料)


# 什么是SPI机制

最近我建了另一个文章分类，用于扩展`JDK`中一些重要但不常用的功能。

`SPI`，全名`Service Provider Interface`，是一种服务发现机制。它可以看成是一种针对接口实现类的解耦方案。我们只需要采用配置文件方式配置好接口的实现类，就可以利用`SPI`机制去加载到它们了，当我们需要修改实现类时，改改配置文件就可以了，而不需要去改代码。

当然，有的同学可能会问，`spring`也可以做接口实现类的解耦，是不是`SPI`就没用了呢？虽然两者都可以达到相同的目的，但是不一定所有应用都可以引入`spring`框架，例如`JDBC`自动发现驱动并注册，它就是采用`SPI`机制，它就不大可能引入`spring`来解耦接口实现类。另外，`druid`、`dubbo`等都采用了`SPI`机制。

# 怎么使用SPI

## 需求

利用`SPI`机制加载用户服务接口的实现类并测试。

## 工程环境

`JDK`：1.8.0_201

`maven`：3.6.1

`IDE`：eclipse 4.12

## 主要步骤

1. 编写用户服务类接口和实现类；
2. 在`classpath`路径下的`META-INF/services`文件夹下配置好接口的实现类；
3. 利用`SPI`机制加载接口实现类并测试。

## 创建项目

项目类型Maven Project，打包方式`jar`

## 引入依赖

```xml
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.12</version>
            <scope>test</scope>
        </dependency>
```

## 编写用户服务类接口

路径：`cn.zzs.spi`

```java
public interface UserService {
	void save();
}
```

## 编写接口实现类

路径：`cn.zzs.spi`。这里就简单实现就好了。

```java
public class UserServiceImpl1 implements UserService {

	@Override
	public void save() {
		System.err.println("执行服务1的save方法");
	}
}
// ------------------------
public class UserServiceImpl2 implements UserService {

	@Override
	public void save() {
		System.err.println("执行服务2的save方法");
	}
}
```

## 配置接口文件

在`resources`路径下创建`META-INF/services`文件夹，并以`UserService`的全限定类名为文件名，创建一个文件。如图所示。

![UserService接口实现类配置文件](https://img2018.cnblogs.com/blog/1731892/201912/1731892-20191229131119499-1656437745.png)

文件中写入接口实现类的全限定类名，多个用换行符隔开。

```
cn.zzs.spi.UserServiceImpl1
cn.zzs.spi.UserServiceImpl2
```

## 编写测试方法

路径：test下的`cn.zzs.spi`。如果实际项目中配置了比较多的接口文件，可以考虑抽取工具类。

```java
public class UserServiceTest {

	@Test
	public void test() {
        // 1. 创建一个ServiceLoader对象
        ServiceLoader<UserService> userServiceLoader = ServiceLoader.load(UserService.class);
        // 2. 创建一个迭代器
        Iterator<UserService> userServiceIterator = userServiceLoader.iterator();
        // 3. 加载配置文件并实例化接口实现类
        while(userServiceIterator.hasNext()) {
        	UserService userService = userServiceIterator.next();
        	userService.save();
        	System.out.println("==================");
        }
	}
}
```

## 测试结果

```
执行服务1的save方法
==================
执行服务2的save方法
==================
```

# SPI在JDBC中的应用

本文以`mysql` 8.0.15版本的驱动来说明。首先，当我们调用`Class.forName("com.mysql.cj.jdbc.Driver")`时，会去执行这个类的静态代码块，在静态代码块中就会完成驱动注册。

```java
    static {
        try {
            //静态代码块中注册当前驱动
            java.sql.DriverManager.registerDriver(new Driver());
        } catch (SQLException E) {
            throw new RuntimeException("Can't register driver!");
        }
    }
```

`JDK`6后不再需要`Class.forName(driver)`也能注册驱动。因为从`JDK6`开始，`DriverManager`增加了以下静态代码块，当类被加载时会执行static代码块的`loadInitialDrivers`方法。

而这个方法会通过查询系统参数（`jdbc.drivers`）和`SPI`机制两种方式去加载数据库驱动。

注意：考虑篇幅，以下代码经过修改，仅保留所需部分。

```java
    static {
        loadInitialDrivers();
    }
    //这个方法通过两个渠道加载所有数据库驱动：
    //1. 查询系统参数jdbc.drivers获得数据驱动类名
    //2. SPI机制
    private static void loadInitialDrivers() {
        //通过系统参数jdbc.drivers读取数据库驱动的全路径名。该参数可以通过启动参数来设置，其实引入SPI机制后这一步好像没什么意义了。
        String drivers;
        try {
            drivers = AccessController.doPrivileged(new PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty("jdbc.drivers");
                }
            });
        } catch (Exception ex) {
            drivers = null;
        }
        //使用SPI机制加载驱动
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                //读取META-INF/services/java.sql.Driver文件的类全路径名。
                ServiceLoader<Driver> loadedDrivers = ServiceLoader.load(Driver.class);
                Iterator<Driver> driversIterator = loadedDrivers.iterator();
                //加载并初始化类
                try{
                    while(driversIterator.hasNext()) {
                    	// 这里才会去实例化驱动
                        driversIterator.next();
                    }
                } catch(Throwable t) {
                // Do nothing
                }
                return null;
            }
        });

        if (drivers == null || drivers.equals("")) {
            return;
        }
        //加载jdbc.drivers参数配置的实现类
        String[] driversList = drivers.split(":");
        for (String aDriver : driversList) {
            try {
                Class.forName(aDriver, true,
                        ClassLoader.getSystemClassLoader());
            } catch (Exception ex) {
                println("DriverManager.Initialize: load failed: " + ex);
            }
        }
    }
```

在`mysql`的驱动包中，我们可以看到`SPI`的配置文件。

![Driver接口实现类配置文件](https://img2018.cnblogs.com/blog/1731892/201912/1731892-20191229131145669-1276828026.png)

# 源码分析

本文将根据测试例子中方法的调用顺序来分析。

```java
	@Test
	public void test() {
        // 1. 创建一个ServiceLoader对象
        ServiceLoader<UserService> userServiceLoader = ServiceLoader.load(UserService.class);
        // 2. 创建一个迭代器
        Iterator<UserService> userServiceIterator = userServiceLoader.iterator();
        // 3. 加载配置文件并实例化接口实现类
        while(userServiceIterator.hasNext()) {
        	UserService userService = userServiceIterator.next();
        	userService.save();
        	System.out.println("==================");
        }
	}
```

注意：考虑篇幅，以下代码经过修改，仅保留所需部分。

## 创建一个ServiceLoader

我们从`load(Class service)`方法开始分析，可以看到，调用这个方法时还不会去加载配置文件和初始化接口实现类。因为`SPI`采用延迟加载的方式，只有去调用`hasNext()`才会去加载配置文件，调用`next()`才会去实例化对象。

```java
    public static <S> ServiceLoader<S> load(Class<S> service) {
    	// 获得当前线程上下文的类加载器
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return ServiceLoader.load(service, cl);
    }
    public static <S> ServiceLoader<S> load(Class<S> service,
                                            ClassLoader loader)
    {	
    	// 创建一个ServiceLoader对象
        return new ServiceLoader<>(service, loader);
    }
    private ServiceLoader(Class<S> svc, ClassLoader cl) {
    	// 校验接口类型和类加载器是否为空
        service = Objects.requireNonNull(svc, "Service interface cannot be null");
        loader = (cl == null) ? ClassLoader.getSystemClassLoader() : cl;
        // 初始化访问控制器
        acc = (System.getSecurityManager() != null) ? AccessController.getContext() : null;
        reload();
    }
    // 存放接口实现类对象。形式为全限定类名=实例对象
    private LinkedHashMap<String,S> providers = new LinkedHashMap<>();
    // 迭代器，有加载和实例化接口实现类的方法
    private LazyIterator lookupIterator;
    public void reload() {
    	// 清空存放的接口实现类对象
        providers.clear();
        // 创建一个LazyIterator
        lookupIterator = new LazyIterator(service, loader);
    }
    // LazyIterator是ServiceLoader的内部类
    private class LazyIterator implements Iterator<S> {
        private LazyIterator(Class<S> service, ClassLoader loader) {
            this.service = service;
            this.loader = loader;
        }
	}
```

## 创建一个迭代器

因为`SPI`机制采用了延迟加载的方式，所以在没有调用`next()`之前，`providers`会是一个空的`Map`，也就是说以下的`knownProviders`也会是一个空的迭代器，所以，这个时候都必须去调用`lookupIterator`的方法，本文讨论的正是这种情况。

```java
    public Iterator<S> iterator() {
        return new Iterator<S>() {
			// providers的迭代器，一般为空
            Iterator<Map.Entry<String,S>> knownProviders
                = providers.entrySet().iterator();
			
            public boolean hasNext() {
                if (knownProviders.hasNext())
                    return true;
                return lookupIterator.hasNext();
            }

            public S next() {
                if (knownProviders.hasNext())
                    return knownProviders.next().getValue();
                return lookupIterator.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }
```

## 加载配置文件

前面已经提到，当调用`hasNext()`时才会去加载配置文件。那么，我们直接看`LazyIterator`的`hasNext()`方法

```java
	// 接口类型
  	Class<S> service;
	// 类加载器
	ClassLoader loader;
	// 配置文件列表，一般只有一个
	Enumeration<URL> configs = null;
	// 所有实现类全限定类名的迭代器
	Iterator<String> pending = null;
	// 下一个实现类全限定类名
	String nextName = null;
	public boolean hasNext() {
		return hasNextService();
	}
	private boolean hasNextService() {
		// 判断是否有下一个实现类全限定类名，有的话直接返回true
		// 第一次调用这个方法nextName肯定是null的
		if(nextName != null) {
			return true;
		}
		// 下面就是加载配置文件了
		if(configs == null) {
			// 本文例子中：fullName = META-INF/services/cn.zzs.spi.UserService
			String fullName = PREFIX + service.getName();
			if(loader == null)
				configs = ClassLoader.getSystemResources(fullName);
			else
				configs = loader.getResources(fullName);
		}
		// pending是所有实现类全限定类名的迭代器，此时是空
		while((pending == null) || !pending.hasNext()) {
			// 如果文件中没有配置实现类，直接返回false
			if(!configs.hasMoreElements()) {
				return false;
			}
			// 解析配置文件，并初始化pending迭代器
			pending = parse(service, configs.nextElement());
		}
		// 将第一个实现类的全限定类名赋值给nextName
		nextName = pending.next();
		return true;
	}
```

解析的过程就是简单的IO操作，这里就不再扩展了。

## 实例化接口实现类

前面已经提到，当调用`next()`时才会去实例化接口实现类。那么，我们直接看`LazyIterator`的`next()`方法。

```java
    public S next() {
    	return nextService();
    }

    private S nextService() {
    	// 判断是否有下一个接口实现类。因为前面已经有nextName，所以直接返回true
        if (!hasNextService())
            throw new NoSuchElementException();
        // 获得下一个接口实现类的全限定类名
        String cn = nextName;
        // 将nextName置空，这样下次调用hasNext()就会重新赋值nextName
        nextName = null;
        Class<?> c = null;
        // 加载接口实现类
        c = Class.forName(cn, false, loader);
        // 判断是否是指定接口的实现类
        if (!service.isAssignableFrom(c)) {
            fail(service,"Provider " + cn  + " not a subtype");
        }
        // 转化为指定类型
        S p = service.cast(c.newInstance());
        // 放入providers的Map中
        // 前面提到过，只有调用了next()方法，这个Map才会放入元素
        providers.put(cn, p);
        return p;
    }

```

以上，`SPI`的源码基本分析完。

# 参考资料

-[深入理解SPI机制](https://www.jianshu.com/p/3a3edbcd8f24)

>本文为原创文章，转载请附上原文出处链接：https://github.com/ZhangZiSheng001/01-spi-demo
