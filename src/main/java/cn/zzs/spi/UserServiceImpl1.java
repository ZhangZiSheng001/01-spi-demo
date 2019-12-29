package cn.zzs.spi;

/**
 * <p>用户服务接口实现类</p>
 * @author: zzs
 * @date: 2019年12月29日 上午9:52:06
 */
public class UserServiceImpl1 implements UserService {

	@Override
	public void save() {
		System.err.println("执行服务1的save方法");
	}

	@Override
	public void update() {
		System.err.println("执行服务1的update方法");
	}

	@Override
	public void delete() {
		System.err.println("执行服务1的delete方法");
	}

	@Override
	public void find() {
		System.err.println("执行服务1的find方法");
	}
}
