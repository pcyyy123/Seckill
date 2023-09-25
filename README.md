# Java高并发秒杀系统

**为了解决秒杀场景下的高并发问题，引入了redis作为缓存中间件，主要作用是缓存预热、预减库存等。针对高并发场景进行了页面优化，缓存页面至浏览器，加快用户访问速度。在安全性问题上，使用双重MD5密码校验，隐藏了秒杀接口地址，设置了接口限流防刷。最后还使用数学公式验证码不仅可以防恶意刷访问，还起到了削峰的作用。通过JMeter压力测试，系统的QPS从1100/s提升到2600/s。**

### 一、用户登录功能

#### 1.两次MD5加密的时机及原因

MD5(MD5(明文 + salt1) + salt2)

**第一次：**客户端输入的密码传入后端之前

原因：客户端输入的是明文密码，直接在网络中传输容易被截获，因此要防止密码在网络中明文传输。

**第二次：**后端接收到第一次加密后的密码之后，传入到数据库之前

原因：万一数据库被盗，盗用者可以根据数据库中的盐（salt）字段，反推出明文。因此，数据库中存的密码是两次加密后的密文。

```java
public class MD5Util {

    public static String md5(String str) {
        return DigestUtils.md5Hex(str);
    }

    private static final String salt = "1a2b3c4d";

    /**
     * 第一次加密
     **/
    public static String inputPassToFromPass(String inputPass) {
        String str = "" +  salt.charAt(0) + salt.charAt(2) + inputPass + salt.charAt(5) + salt.charAt(4);
        return md5(str);
    }

    /**
     * 第二次加密
     **/
    public static String formPassToDBPass(String formPass, String salt) {
        String str = "" +  salt.charAt(0) + salt.charAt(2) + formPass + salt.charAt(5) + salt.charAt(4);
        return md5(str);
    }

    // 用于后端调用
    public static String inputPassToDBPass(String inputPass, String salt) {
        String fromPass = inputPassToFromPass(inputPass);
        String dbPass = formPassToDBPass(fromPass, salt);
        return dbPass;
    }
}

```



#### 2.用自定义注解进行参数校验

每个类都写大量的健壮性判断过于麻烦，我们可以使用 validation 简化我们的代码。比如可以自定义一个`@IsMobile`注解来判断登录功能中手机号码的合法性。

```java
public @interface IsMobile {

    boolean required() default true;

    java.lang.String message() default "手机号码格式错误";

    java.lang.Class<?>[] groups() default {};

    java.lang.Class<? extends jakarta.validation.Payload>[] payload() default {};
}
```

#### 3.异常处理

如何将异常展现在前端？使用SpringBoot全局异常处理

系统中异常包括：编译时异常和运行时异常 RuntimeException ，前者通过捕获异常从而获取异常信息，后者主要通过规范代码开发、测试通过手段减少运行时异常的发生。在开发中，不管是dao层、service层还是controller层，都有可能抛出异常，在Springmvc中，能将所有类型的异常处理从各处理过程解耦出来，既保证了相关处理过程的功能较单一，也实现了异常信息的统一处理和维护。
SpringBoot全局异常处理方式中我们使用`@RestControllerAdvice`和 `@ExceptionHandler` 注解的组合。

`@RestControllerAdvice` 是 Spring 框架中的一个注解，用于全局处理异常和统一处理响应的注解。它通常结合 `@ExceptionHandler` 和 `@ResponseBody` 注解一起使用，用于处理控制器（Controller）层中抛出的异常，并将异常信息包装成统一格式的响应返回给客户端。

`@ExceptionHandler(Exception.class)` 是 Spring 框架中的一个注解，用于标记方法，指示该方法用于处理特定类型的异常，其中 `Exception.class` 表示处理所有类型的异常。这意味着当控制器（Controller）层中抛出任何类型的异常时，被标记的方法将被触发来处理异常。

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public RespBean ExceptionHandler(Exception e) {
        if (e instanceof GlobalException) {
            GlobalException exception = (GlobalException) e;
            return RespBean.error(exception.getRespBeanEnum());
        } else if (e instanceof BindException) {
            BindException bindException = (BindException) e;
            RespBean respBean = RespBean.error(RespBeanEnum.BIND_ERROR);
            respBean.setMessage("参数校验异常：" + bindException.getBindingResult().getAllErrors().get(0).getDefaultMessage());
            return respBean;
        }
        return RespBean.error(RespBeanEnum.ERROR);
    }
}
```

> 注：将异常使用RespBean返回给前端



#### 4.使用公共返回对象和前端交互

定义RespBean类

```java
public class RespBean {

    private long code;
    private String message;
    private Object obj;

    // 成功返回结果
    public static RespBean success(){
        return new RespBean(RespBeanEnum.SUCCESS.getCode(), RespBeanEnum.SUCCESS.getMessage(), null);
    }
    // 成功返回结果带参数
    public static RespBean success(Object obj){
        return new RespBean(RespBeanEnum.SUCCESS.getCode(), RespBeanEnum.SUCCESS.getMessage(), obj);
    }
    // 失败返回结果
    public static RespBean error(RespBeanEnum respBeanEnum){
        return new RespBean(respBeanEnum.getCode(), respBeanEnum.getMessage(), null);
    }
}
```

定义RespBeanEnum枚举类

```java
public enum RespBeanEnum {
    // 通用
    SUCCESS(200, "SUCCESS"),
    ERROR(500, "服务端异常"),
    // 登录模块
    LOGIN_ERROR(500210, "用户名或密码不正确"),
    MOBILE_ERROR(500211, "手机号码格式不正确"),
    BIND_ERROR(500212, "参数校验异常"),
    MOBILE_NOT_EXIST(500213, "手机号码不存在"),
    PASSWORD_UPDATE_FAIL(500214, "更新密码失败"),
    SESSION_ERROR(500215, "用户SESSION不存在"),
    //秒杀模块
    EMPTY_STOCK(500500, "库存不足"),
    REPEATE_ERROR(500501, "该商品每人限购一件"),
    REQUEST_ILLEGAL(500502, "请求非法，请重新尝试"),
    ERROR_CAPTCHA(500503, "验证码错误，请重新输入"),
    ACCESS_LIMIT_REACHED(500504, "访问过于频繁，请稍后重试"),
    //订单模块
    ORDER_NOT_EXIST(500300, "订单不存在");

    private final Integer code;
    private final String message;

}
```

#### 5.分布式Session问题

由于 Nginx 使用默认负载均衡策略（轮询），请求将会按照时间顺序逐一分发到后端应用上。也就是说刚开始我们在 Tomcat1 登录之后，用户信息放在 Tomcat1 的 Session 里。过了一会，请求又被 Nginx 分发到了 Tomcat2 上，这时 Tomcat2 上 Session 里还没有用户信息，于是又要登录。
<img src="HCS_img\1.png" alt="images" style="zoom:30%;" />

基本解决方案：

1.Session复制
	优点: 无需修改代码，只需要修改Tomcat配置
	缺点: Session同步传输占用内网带宽；多台Tomcat同步性能指数级下降；Session占用内存，无法有效水平扩展

2.前端存储（Cookie）
	优点: 不占用服务端内存
	缺点: 存在安全风险；数据大小受cookie限制；占用外网带宽

3.Session粘滞
	优点: 无需修改代码；服务端可以水平扩展
	缺点: 增加新机器，会重新Hash，导致重新登录；应用重启，需要重新登录

4.后端集中存储

​	优点: 安全；容易水平扩展
​	缺点: 增加复杂度；需要修改代码

**用Redis实现分布式Session**

**有两个方法：使用SpringSession工具包和登录时直接将用户信息存入Redis。二者的共同原理都是将用户信息存在第三方的一个Redis中**。我们采用后者。

### 二、系统压测

**QPS（Queries Per Second）：一台服务器每秒能够响应的查询次数**

#### 1. 缓存

##### 1.1 页面缓存

缓存页面数据，避免每次页面请求都查询数据库，提升系统的QPS。

```java
public String toList(Model model,User user) {
        // 尝试从缓存中获取页面
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        String cachedHtml = valueOperations.get("goodsList");

        if (cachedHtml != null) {
            // 如果缓存中存在，直接返回缓存的HTML页面
            return cachedHtml;
        }

        // 创建Thymeleaf上下文
        Context thymeleafContext = new Context();

        // 添加要在模板中使用的变量
        thymeleafContext.setVariable("user", user);
        thymeleafContext.setVariable("goodsList", goodsService.findGoodsVO()); // 替换为你的商品数据

        // 渲染HTML页面
        String renderedHtml = templateEngine.process("goodsList", thymeleafContext);

        // 将渲染后的HTML页面存储到Redis缓存中，设置过期时间（例如60分钟）
        valueOperations.set("goodsList", renderedHtml, 60, TimeUnit.MINUTES);

        // 返回渲染后的HTML页面
        return renderedHtml;
    }	
```

执行流程：

<img src="HCS_img\2.png" alt="images" style="zoom:40%;" />

##### 1.2 对象缓存

分布式Session中将User类（用户信息）存入Redis就属于对象缓存。

**缓存和数据库数据一致性如何保证？**

每次更新数据库的数据时，一定要处理Redis里的相应数据。我们选择直接删除Redis，然后下一次查缓存找不到数据后会先从数据库查找，然后同步到缓存。

#### 2. 秒杀功能

```java
public RespBean doSeckill(Model model, User user, Long goodsId){

        if(user==null){
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
        GoodsVo goods = goodsService.findGoodsVOByGoodsId(goodsId);
        //判断库存
        if (goods.getStockCount()<1){
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }
        //判断是否重复抢购
        //SeckillOrder seckillOrder = seckillOrderService.getOne(new QueryWrapper<SeckillOrder>().eq("user_id", user.getId()).eq("goods_id", goodsId));
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
        if (seckillOrder!=null){
            return RespBean.error(RespBeanEnum.REPEATE_ERROR);
        }
        //能够成功秒杀
        Order order = orderService.seckill(user,goods);
        return RespBean.success(order);
    }
```

执行流程：

<img src="HCS_img\3.png" alt="images" style="zoom:30%;" />

**减库存成功了，但是生成订单失败了，该怎么办？**

当减库存成功但生成订单失败时，通常需要采取一些措施来保证数据的一致性和完整性。

**事务处理：**使用数据库的事务机制可以确保减库存和生成订单两个操作要么同时成功，要么同时失败。你可以使用 Spring 的 `@Transactional` 注解来管理事务，将减库存和生成订单的操作放在同一个事务中。如果其中一个操作失败，整个事务将回滚，从而保持数据一致性。

**消息队列：**可以使用消息队列来解耦减库存和生成订单的操作。当减库存成功后，将一个消息发送到消息队列中，然后由订单服务消费消息并生成订单。如果生成订单失败，可以重新尝试或记录错误消息供后续处理。

> 事务处理：

```java
		@Transactional
    @Override
    public Order seckill(User user, GoodsVo goods) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //秒杀商品表减库存
        SeckillGoods seckillGoods = seckillGoodsService.getOne(new QueryWrapper<SeckillGoods>().eq("goods_id", goods.getId()));
        seckillGoods.setStockCount(seckillGoods.getStockCount()-1);
        if (seckillGoods.getStockCount()<1){
            // 判断是否还有库存
            valueOperations.set("isStockEmpty:"+goods.getId(),"0");
            return null;
        }
        //生成订单
        Order order = new Order();
        order.setUserId(user.getId());
        order.setGoodsId(goods.getId());
        order.setDeliveryAddrId(0L);
        order.setGoodsName(goods.getGoodsName());
        order.setGoodsCount(1);
        order.setGoodsPrice(seckillGoods.getSeckillPrice());
        order.setOrderChannel(1);
        order.setStatus(0);
        order.setCreateDate(new Date());
        orderMapper.insert(order);
        //生成秒杀订单
        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setUserId(user.getId());
        seckillOrder.setOrderId(order.getId());
        seckillOrder.setGoodsId(goods.getId());
        seckillOrderService.save(seckillOrder);
        redisTemplate.opsForValue().set("order:"+user.getId()+":"+goods.getId(),seckillOrder);
        return order;
    }
```



#### 3.解决库存超卖

##### 3.1 解决同一用户同时秒杀多件商品的步骤

1.对数据库建立唯一索引

<img src="HCS_img\4.png" alt="images" style="zoom:80%;" />

2.将秒杀订单信息存储在Redis中，以便在检查是否存在重复抢购时进行查询。如果在Redis中已经存在相同的订单信息，将触发重复下单错误。

##### 3.2 解决超卖问题（避免零库存时继续减库存）

SQL的`update`会自动加行级别排他锁。在减库存时判断商品库存是否为负，为负不再继续，解决超卖。（注意是在`update`语句加`where stock>0`而不仅仅是查库存的`select`语句）
但单纯加这种锁会影响并发量，因此需要进行[接口优化](https://blog.csdn.net/Teachmepatiently/article/details/132125673#t14)。

```java
//秒杀商品表减库存
        SeckillGoods seckillGoods = seckillGoodsService.getOne(new QueryWrapper<SeckillGoods>().eq("goods_id", goods.getId()));
        seckillGoods.setStockCount(seckillGoods.getStockCount()-1);
        boolean result = seckillGoodsService.update(new UpdateWrapper<SeckillGoods>().setSql("stock_count = " +
                "stock_count-1").eq("goods_id", goods.getId()).gt("stock_count", 0));
        if (seckillGoods.getStockCount()<1){
            // 判断是否还有库存
            valueOperations.set("isStockEmpty:"+goods.getId(),"0");
            return null;
        }
```

### 三、接口优化

#### 1. RabbitMQ

RabbitMQ 是一个开源的消息代理（message broker）软件，用于实现高效的消息传递和事件驱动架构。它允许不同的应用程序或服务之间通过消息进行异步通信，以实现解耦、高可用性、扩展性和可靠性的目标。

**RabbitMQ和一般的消息传递使用了一些术语**

- 生产者：生产只意味着发送。发送消息的程序就是生产者。

- 队列：尽管消息流经RabbitMQ和您的应用程序，但它们只能存储在队列中。队列只受主机的内存和磁盘限制，它本质上是一个大的消息缓冲区。许多生产者可以向一个队列发送消息，许多消费者可以尝试从一个队列接收数据。

- 消费者：消费和接受有着相似的含义。消费者是一个主要等待接收消息的程序。

<img src="HCS_img\5.png" alt="images" style="zoom:80%;" />

##### 交换机（Exchanges）

RabbitMQ 中的交换机（Exchange）是消息传递的核心组件之一，它负责接收来自生产者的消息，并将这些消息路由到一个或多个消息队列，从而实现消息的分发和路由。交换机定义了消息如何被发送到队列的规则和逻辑。

RabbitMQ 中消息传递模型的核心思想是生产者从不直接向队列发送任何消息。实际上，通常情况下，生产者甚至根本不知道消息是否会被传递到任何队列。相反，生产者只能向交换机发送消息。交换是一件非常简单的事情。它一方面接收来自生产者的消息，另一方面将它们推送到队列中。交换机必须确切地知道如何处理收到的消息。是否应将其附加到特定队列？是否应该将它附加到许多队列中？或者应该丢弃它。其规则由交换类型定义。

**Fanout模式：**

- 不处理路由键，只需要简单的将队列绑定到交换机上
- 发送到交换机的消息都会被转发到与该交换机绑定的所有队列上
- Fanout交换机转发消息是最快的

1. 创建多个队列Queue和一个交换机FanoutExchange

2. 把这些队列绑定(Binding)到交换机上

3. 生产者发送消息到交换机

4. 每个消费者从一个队列接收消息

**Direct模式:**

- 所有发送到Direct Exchange的消息被转发到RouteKey中指定的Queue
- 注意：Direct模式可以使用RabbitMQ自带的Exchange：default Exchange,所以不需要将Exchange进行任何绑定(Binding)操作，消息传递时，RouteKey必须完全匹配才会被队列接收，否则该消息会被抛弃。

- 重点：routing key与队列queues 的key保持一致，即可以路由到对应的queue中。

<img src="HCS_img\6.png" alt="images" style="zoom:70%;" />

1. 创建多个队列Queue和一个交换机DirectExchange
2. 把这些队列绑定(Binding)到交换机上，并为每个队列设置路由键
3. 生产者发送带有一个路由键的消息到交换机，用于和队列的路由键匹配
4. 每个消费者从一个队列接收消息（消费者不知道路由键）

**Topic模式：**

- 所有发送到Topic Exchange的消息被转发到所有管线RouteKey中指定Topic的Queue上
- Exchange将RouteKey和某Topic进行模糊匹配,此时队列需要绑定一个Topic

对于routing key匹配模式定义规则举例如下:

- routing key为一个句点号 . 分隔的字符串（我们将被句点号 . 分隔开的每一段独立的字符串称为一
  个单词），如“stock.usd.nyse”、“nyse.vmw”、“quick.orange.rabbit”
- routing key中可以存在两种特殊字符 * 与 # ，用于做模糊匹配，其中 * 用于匹配一个单词， # 用于匹配多个单词（可以是零个）
- 发送到主题交换机的消息不能有任意的routing_key，它必须是用句点分隔的单词列表。

<img src="HCS_img\7.png" alt="images" style="zoom:70%;" />

#### 2. 接口优化

**核心目的：** 减少数据库访问

##### 优化后的秒杀功能

> 1. 首先，系统初始化时，需要把商品库存数量从数据库加载到Redis。

```java
// 系统初始化(程序启动时执行该方法)，把商品库存数量加载到redis
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> list = goodsService.findGoodsVO();
        if (CollectionUtils.isEmpty(list)){
            return;
        }
        list.forEach(goodsVo -> {
                    redisTemplate.opsForValue().set("seckillGoods:"+goodsVo.getId(),goodsVo.getStockCount());
                    emptyStockMap.put(goodsVo.getId(),false);
                });
    }
```

> 2. 通过redis判断是否重复抢购

```java 
// 判断是否重复抢购
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
        if (seckillOrder!=null){
            return RespBean.error(RespBeanEnum.REPEATE_ERROR);
        }
```

> 3. 通过一个Map对象判断剩余库存（通过**内存标记**，减少Redis的访问）

```java
// 通过初始化库存数量，并标记货物库存为false（即不为空），当emptyStockMap.get(goodsId)为true，则向前端返回空库存异常
        if (emptyStockMap.get(goodsId)){
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }
```

> 4. 通过redis预减库存
>
>    进行预减库存操作的主要目的是避免超卖（Over-Selling）问题，确保系统的稳定性和库存的一致性。
>
>    **为什么要进行预减操作：**
>
>    1. **避免超卖问题：** 超卖是指在瞬间内接收到大量请求时，由于并发处理不当，多个用户可能同时购买了同一件商品，导致库存数量减少超过了实际库存，从而出现负库存或卖出的商品数量大于实际库存的情况。预减库存操作可以避免这种情况发生，确保库存不会被减到负数。
>    2. **提高系统响应速度：** 预减库存可以在用户提交秒杀请求之前先检查库存是否足够，如果库存不足，可以立即返回错误响应，而不需要进行后续的订单生成和数据库操作。这样可以大大提高系统的响应速度，减轻服务器负载。
>    3. **保护数据库：** 直接在数据库中进行库存减少操作可能会导致数据库的性能问题，特别是在高并发情况下。通过预减库存，可以将一部分库存操作转移到缓存层（如Redis），减轻数据库的压力，提高系统的吞吐量和性能。
>    4. **实现库存控制：** 预减库存操作允许系统在库存不足时进行有序的错误处理，例如返回错误消息，阻止进一步的秒杀请求。这有助于控制用户的访问频率，避免用户不断尝试秒杀导致服务器过载。
>
>    总之，通过进行预减库存操作，秒杀系统可以更好地处理高并发请求，防止超卖现象，提高系统的性能和稳定性，同时保护数据库不受过大的负载影响。这是设计一个健壮的秒杀系统的重要步骤之一。

```java
// 预减库存
        Long stock = valueOperations.decrement("seckillGoods:" + goodsId);
        if (stock < 0){
            emptyStockMap.put(goodsId,true);
            valueOperations.increment("seckillGoods:"+goodsId);
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }
```

> > 库存数量 < 0:
> >
> > ​	将库存为空属性置为true，向前端返回一个错误响应。
> >
> > 库存数量 > 0:
> >
> > ​	将User和Goods对象封装到秒杀信息对象SeckillMessage中，并用JsonUtil将秒杀信息转换成消息字符串。
> >
> > ```java
> > SeckillMessage seckillMessage = new SeckillMessage(user, goodsId);
> > ```

> 5. 通过RabbitMQ请求入队，进入排队状态
>
>    ```java
>    mqSender.sendSeckillMessage(JsonUtil.object2JsonStr(seckillMessage));
>       
>    public void sendSeckillMessage(String message) {
>            log.info("发送消息" + message);
>            rabbitTemplate.convertAndSend("seckillExchange", "seckill.message", message);
>        }
>    ```

> 6. MQReceiver接收消息并处理
>
>    ```java
>    @RabbitListener(queues = "seckillQueue")
>    public void receive(String message) {
>        log.info("接收消息：" + message);
>        SeckillMessage seckillMessage = JsonUtil.jsonStr2Object(message, SeckillMessage.class);
>        Long goodsId = seckillMessage.getGoodsId();
>        User user = seckillMessage.getUser();
>        GoodsVo goodsVo = goodsService.findGoodsVOByGoodsId(goodsId);
>        if (goodsVo.getStockCount() < 1) {
>            return;
>        }
>        //判断是否重复抢购
>        SeckillOrder tSeckillOrder = (SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
>        if (tSeckillOrder != null) {
>            return;
>        }
>        //下单操作
>        orderService.seckill(user, goodsVo);
>    }
>    ```
>
>    生成订单
>
>    > 如果下单后库存数为0，将为“0”的商品库存标识设置为true。

**如何增减Redis中的库存数？**
decrement方法减库存，increment方法回增库存。以上的指令都是单线程原子性的。

**为什么要用RabbitMQ优化？**
变成了异步操作，请求的返回更快，缓解了数据库的并发压力，起到了流量削峰的效果。

### 四、安全优化

#### 1. 秒杀接口地址隐藏

秒杀开始时，如果有用户提前知道了、或者能快速获取秒杀接口的URL地址，那么他就可以利用这个地址通过脚本不断地刷新秒杀。

核心思想是通过将接口地址存储在 Redis 中，并使用随机生成的哈希值来作为地址的一部分，从而实现接口地址的隐藏。用户在秒杀前需要先获取这个地址，然后才能访问秒杀接口。这样可以增加秒杀的安全性，降低恶意攻击的风险。同时，通过设置地址的过期时间，可以确保地址的有效期有限，一定程度上限制了攻击者的时间窗口。

```java
public String createPath(User user, Long goodsId) {
        String str = MD5Util.md5(UUIDUtil.uuid() + "123456");
        redisTemplate.opsForValue().set("seckillPath:"+user.getId()+":"+goodsId,str,60, TimeUnit.SECONDS);
        return str;
    }
```

#### 2. 数学公式验证码

用户仍然能通过脚本快速获取随机的UUID和字符串拼接规则，实现快速不断刷新秒杀。因此每次点击秒杀开始前，先让用户输入验证码。此外，验证码还能起到分散用户的请求的作用，防止大量的用户请求集中在刚开始的几秒中，对服务器造成压力。

```java
public void verifyCode(User user, Long goodsId, HttpServletResponse response) {
        if (user == null || goodsId < 0) {
            throw new GlobalException(RespBeanEnum.REQUEST_ILLEGAL);
        }
        //设置请求头为输出图片的类型
        response.setContentType("image/png");
        response.setHeader("Pargam", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        //生成验证码
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 32,3);
        // 设置字体
        captcha.setFont(new Font("Verdana", Font.PLAIN, 32));  // 有默认字体，可以不用设置
        // 设置类型，纯数字、纯字母、字母数字混合
        captcha.setCharType(Captcha.TYPE_ONLY_NUMBER);
        System.out.println("captcha："+captcha.text().toLowerCase());
        redisTemplate.opsForValue().set("captcha:" + user.getId() + ":" + goodsId, captcha.text().toLowerCase(), 300, TimeUnit.SECONDS);
        try {
            captcha.out(response.getOutputStream());
        } catch (IOException e) {
            log.error("验证码生成失败", e.getMessage());
        }
    }
```

#### 3. 接口限流

目的同样是为了减轻服务器的压力。

##### 3.1 计数器算法（采用这个算法）

一定时间周期内的请求数量有上限。
缺点：周期时间内的用户到达数量是随机的，因此可能会出现周期内实际到达数量少而导致资源的浪费、以及相邻周期的临界时间段内的到达数量过多导致请求数超过服务器处理容量。

```java
// 自定义@AccessLimit注解实现计数器算法

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AccessLimit {
    int second();
    int maxCount();
    boolean needLogin() default true;
}

public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            User user = getUser(request, response);
            UserContext.setUser(user);
            HandlerMethod hm = (HandlerMethod) handler;
            AccessLimit accessLimit = hm.getMethodAnnotation(AccessLimit.class);
            if (accessLimit == null) {
                return true;
            }
            int second = accessLimit.second();
            int maxCount = accessLimit.maxCount();
            boolean needLogin = accessLimit.needLogin();

            String key = request.getRequestURI();
            if (needLogin) {
                if (user == null) {
                    render(response, RespBeanEnum.SESSION_ERROR);
                }
                key += ":" + user.getId();
            }
            ValueOperations valueOperations = redisTemplate.opsForValue();
            Integer count = (Integer) valueOperations.get(key);
            if (count == null) {
                valueOperations.set(key, 1, second, TimeUnit.SECONDS);
            } else if (count < maxCount) {
                valueOperations.increment(key);
            } else {
                render(response, RespBeanEnum.ACCESS_LIMIT_REACHED);
                return false;
            }
        }
        return true;
    }
```

##### 3.2 漏斗算法

将请求放入一个队列，控制请求放行速度。

##### 3.3 令牌桶算法

以恒定的速度发放令牌（token）到令牌桶，请求从桶里取token。令牌桶满时直接丢弃token。
优点：减轻突发流量带来的压力。

##### 3.4 其他优化方案
**网关过滤：**

- 设置预约按钮，提前预约的用户提前获得token，秒杀开始时网关直接过滤掉没有token的。

- 黑名单中的IP地址（单个IP访问频率和次数多了之后就进行拉黑，解决客户的恶意下单问题）同一个IP地址发起的重复请求。

- 设置每个服务器发放的总token数量。

#### 加了缓存之后的缓存三大问题及解决方法：

- 穿透：查询一个数据库一定不存在的数据。
  - 解决方法：布隆过滤器。

- 击穿：缓存中没有但数据库中有的数据，并且某一个key非常热点，在不停的扛着大并发，大并发集中对这一个点进行访问，当这个key在失效的瞬间（一般是缓存时间到期），持续的大并发就穿破缓存，直接请求数据库，就像在一个屏障上凿开了一个洞。
  - 解决方法：设置热点数据永远不过期。

- 雪崩：缓存中大批量数据到过期时间，而查询数据量巨大，引起数据库压力过大甚至down机。
  - 解决方法：
    （1）随机设置缓存有效期，比如，热门类目的商品缓存时间长一些，冷门类目的商品缓存时间短一些，也能节省缓存服务的资源；
    （2）如果缓存数据库是分布式部署，将热点数据均匀分布在不同的缓存数据库中；
    （3）设置热点数据永远不过期。

