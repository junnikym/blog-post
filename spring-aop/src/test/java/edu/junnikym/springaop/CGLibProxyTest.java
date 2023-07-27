package edu.junnikym.springaop;

import edu.junnikym.springaop.unit.EngineControlUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.FixedValue;
import org.springframework.cglib.proxy.MethodInterceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class CGLibProxyTest {

	@Autowired
	EngineControlUnit engineControlUnit;

	@Test
	@DisplayName("CGLib Proxy 생성 테스트 - 단순 데이터 변경")
	void CGLib_Proxy_생성_테스트__단순_데이터_변경() {
		/* given */
		final String value = "Hello CGLib!";
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(EngineControlUnit.class);
		enhancer.setCallback((FixedValue) ()-> value);
		EngineControlUnit proxy = (EngineControlUnit) enhancer.create();

		/* when */
		final String name = proxy.getName();
		final String desc = proxy.getDescription();

		/* then */
		assertEquals(name, value);
		assertEquals(desc, value);
	}

	@Test
	@DisplayName("CGLib Proxy 생성 테스트 - Method Interceptor")
	void CGLib_Proxy_생성_테스트__Method_Interceptor() {
		/* given */
		final String value = "Hello CGLib!";
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(EngineControlUnit.class);
		enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy)-> {
			if(method.getName().equals("getDescription"))
				return value;

			return proxy.invokeSuper(obj, args);
		});
		EngineControlUnit proxy = (EngineControlUnit) enhancer.create();

		/* when */
		final String name = proxy.getName();
		final String desc = proxy.getDescription();

		/* then */
		assertEquals(name, "engine control unit");
		assertEquals(desc, value);
	}

	@Test
	@DisplayName("CGLib Proxy 생성 테스트 - Bean 등록")
	void CGLib_Proxy_빈_등록_테스트() {
		/* given */

		/* when */
		final String name = engineControlUnit.getName();

		/* then */
		assertEquals(name, "ENGINE CONTROL UNIT");
	}

}
