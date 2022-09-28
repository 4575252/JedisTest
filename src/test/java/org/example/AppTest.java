package org.example;

import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/***
 * @description: 测试Jedis操作api，应用于String、Hash、List、Set、ZSet的五种常见应用
 * @return:
 * @author: eric 4575252@gmail.com
 * @date: 2022-09-28, 周三, 9:23
 */
@Log
public class AppTest {
    private Jedis jedis;

    @Before
    public void setUp() throws Exception {
        jedis = new Jedis("192.168.20.147", 6379);
        jedis.auth("123456");
    }

    @After
    public void tearDown() throws Exception {
        //3 关闭连接
        jedis.close();
        jedis = null;
    }

    /**
     * @description: 测试jedis操作string对象的常见操作，主要有存取、删除、过期设置
     * @return: void
     * @author: eric 4575252@gmail.com
     * @date: 2022-09-28, 周三, 9:16
     */
    @Test
    public void testRedisStringAPI() {
        //1、set&get
        jedis.set("username", "zhangsan");
        assertEquals(jedis.get("username"),"zhangsan");

        //2、set超时验证
        /* 第三个参数nxxx，nx为不存在才set，xx为存在才set
         * 第四个参数expx，ex为秒，px为毫秒
         */
        jedis.set("timeout", "yes", "nx", "ex", 1);
        for (int i = 0; i < 10; i++) {
            //瞬时取10次，现有cpu设置应该能在1秒内跑完
            assertEquals(jedis.get("timeout"),"yes");
        }
        try {
            Thread.sleep(1000);     //休眠一秒
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertNull(jedis.get("timeout"));   //过期应为null

        //3、删除字段后验证是否
        jedis.del("username");
        assertNull(jedis.get("username"));
    }


    /***
     * @description: hash对象的常见操作，主要有存取、删除、keys、values验证
     * @return: void
     * @author: eric 4575252@gmail.com
     * @date: 2022-09-28, 周三, 9:25
     */
    @Test
    public void testRedisHashAPI() {

        // 1、Hset、Hget测试
        String userKey="user12";
        jedis.hset(userKey, "name", "jim");
        jedis.hset(userKey, "age", "12");
        jedis.hset(userKey, "phone", "12345678901");
        assertEquals(jedis.hget(userKey,"name"),"jim"); //hset&hget 验证

//        jedis.del(userKey);

        // 2、验证key是否一致
        assertTrue(SetUtils.isEqualSet(jedis.hkeys(userKey), Stream.of("name","age","phone").collect(Collectors.toSet())));

        // 3、删除key测试
        log.info("hkeys删除前 "+jedis.hkeys(userKey));
        jedis.hdel(userKey,"name");
        assertTrue(SetUtils.isEqualSet(jedis.hkeys(userKey), Stream.of("phone","age").collect(Collectors.toSet())));
        log.info("hkeys删除后 "+jedis.hkeys(userKey));

        // 4、验证values是否一致
        log.info("hvals "+jedis.hvals(userKey));
        assertTrue(ListUtils.isEqualList(jedis.hvals(userKey), Stream.of("12","12345678901").collect(Collectors.toList())));
    }

    @Test
    public void testRedisListAPI() {
        // 数据准备
        jedis.flushDB();
        jedis.rpush("names", "唐僧");
        jedis.rpush("names", "悟空");
        jedis.rpush("names", "八戒");
        jedis.rpush("names", "悟净");

        // 1、测试list的数据顺序是否一致
        assertTrue(ListUtils.isEqualList(jedis.lrange("names", 0, -1), Stream.of("唐僧", "悟空", "八戒", "悟净").collect(Collectors.toList())));

        // 2、测试list长度
        assertTrue(jedis.llen("names") == 4l);

        // 数据准备
        jedis.flushDB();
        jedis.lpush("scores", "99");    //lpush是从左侧、头部压入
        jedis.lpush("scores", "100");
        jedis.lpush("scores", "55");

        // 3、排序校验
        assertTrue(ListUtils.isEqualList(jedis.sort("scores"), Stream.of("55", "99", "100").collect(Collectors.toList())));

        // 4、校验移除动作，移除头部一个元素
        jedis.lpop("scores");
        assertTrue(ListUtils.isEqualList(jedis.lrange("scores",0,-1), Stream.of("100", "99").collect(Collectors.toList())));

        // 5、修改列表中单个值
        jedis.lset("scores", 0, "66");
        assertTrue(ListUtils.isEqualList(jedis.lrange("scores",0,-1), Stream.of("66", "99").collect(Collectors.toList())));


        // 6、获取列表指定下标的值
        System.out.println(jedis.lindex("scores", 1));
        assertEquals(jedis.lindex("scores", 0), "66");


        // 删除列表指定下标的值
        System.out.println(jedis.lrange("scores",0,-1));
        System.out.println(jedis.lrem("scores", 2, "99"));  //count为0则全部删除匹配的值，大于0从左往右比对，小于零从右往左，绝对值就是删除的数量。
        System.out.println(jedis.lrange("scores",0,-1));
    }

    @Test
    public void testRedisSetAPI() {
    }

    @Test
    public void testRedisZsetAPI() {
    }
}
