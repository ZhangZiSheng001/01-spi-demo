package cn.zzs.spi;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.junit.Test;


/**
 * <p>测试使用spi获得UserService实现类</p>
 * @author: zzs
 * @date: 2019年12月29日 上午9:56:16
 */
public class UserServiceTest {

	@Test
	public void test() {
        //读取META-INF/services/cn.zzs.spi.UserService文件的类全路径名。
        ServiceLoader<UserService> userServiceLoader = ServiceLoader.load(UserService.class);
        Iterator<UserService> userServiceIterator = userServiceLoader.iterator();
        //加载并初始化类
        while(userServiceIterator.hasNext()) {
        	UserService userService = userServiceIterator.next();
        	userService.save();
        	userService.update();
        	userService.delete();
        	userService.find();
        	System.out.println("==================");
        }
	}
}
