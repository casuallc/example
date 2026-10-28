# admq 客户端使用说明

本文档指导开发者如何使用 admq JMS SDK 进行消息收发。

## 环境要求

- JDK 11+
- Maven 3.6+
- admq Broker（默认地址 tcp://localhost:61616）

> 本示例基于 jakarta 命名空间版本客户端（`admq-jms-client-jakarta`，Jakarta EE 9+）。
> 如需 javax 命名空间版本（Java 8），请改用 `com.apusic.admq:admq-jms-client:1.0.0` +
> `javax.jms:javax.jms-api:2.0.1` + slf4j 1.7.x / logback 1.2.x，示例代码仅需将
> `jakarta.jms.*` import 换回 `javax.jms.*`。两个版本的 jar 不能同时放在 classpath 上。

## 1. 安装 admq 依赖到本地 Maven 仓库

由于 admq 客户端 jar 包无法从远程 Maven 仓库获取，需要先将 jar 包安装到本地仓库。

### 1.1 获取 jar 包

从 admq 安装目录或官方发布包中提取以下 jar 文件，放到项目本地目录（例如 `lib/`）：

| jar 文件 | Maven 坐标 |
|---------|-----------|
| `admq-jms-client-jakarta-1.0.0.jar` | `com.apusic.admq:admq-jms-client-jakarta:1.0.0` |
| `jakarta.jms-api-3.1.0.jar` | `jakarta.jms:jakarta.jms-api:3.1.0` |
| `slf4j-api-2.0.17.jar` | `org.slf4j:slf4j-api:2.0.17` |
| `logback-classic-1.4.14.jar` | `ch.qos.logback:logback-classic:1.4.14` |
| `logback-core-1.4.14.jar` | `ch.qos.logback:logback-core:1.4.14` |

> `admq-jms-client-jakarta` 为 shade fat jar，ActiveMQ 客户端、连接池、国密 Kona
> 等依赖已内嵌，无需再单独安装 geronimo / hawtbuf 等传递依赖。

### 1.2 执行 mvn install:install-file 安装

逐条执行以下命令（请根据 jar 包实际路径修改 `-Dfile` 参数）：

```bash
# admq 核心客户端（jakarta 版）
mvn install:install-file \
  -Dfile=lib/admq-jms-client-jakarta-1.0.0.jar \
  -DgroupId=com.apusic.admq \
  -DartifactId=admq-jms-client-jakarta \
  -Dversion=1.0.0 \
  -Dpackaging=jar

# JMS API（Jakarta EE 9+）
mvn install:install-file \
  -Dfile=lib/jakarta.jms-api-3.1.0.jar \
  -DgroupId=jakarta.jms \
  -DartifactId=jakarta.jms-api \
  -Dversion=3.1.0 \
  -Dpackaging=jar

# SLF4J 日志门面
mvn install:install-file \
  -Dfile=lib/slf4j-api-2.0.17.jar \
  -DgroupId=org.slf4j \
  -DartifactId=slf4j-api \
  -Dversion=2.0.17 \
  -Dpackaging=jar

# Logback 日志实现
mvn install:install-file \
  -Dfile=lib/logback-classic-1.4.14.jar \
  -DgroupId=ch.qos.logback \
  -DartifactId=logback-classic \
  -Dversion=1.4.14 \
  -Dpackaging=jar

mvn install:install-file \
  -Dfile=lib/logback-core-1.4.14.jar \
  -DgroupId=ch.qos.logback \
  -DartifactId=logback-core \
  -Dversion=1.4.14 \
  -Dpackaging=jar
```

> **说明**：所有 jar 安装完成后，即可在项目的 `pom.xml` 中正常引用这些依赖，Maven 会从本地仓库解析。

### 1.3 在 pom.xml 中引用

```xml
<project>
    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <admq.version>1.0.0</admq.version>
    </properties>

    <dependencies>
        <!-- admq 客户端（jakarta 版） -->
        <dependency>
            <groupId>com.apusic.admq</groupId>
            <artifactId>admq-jms-client-jakarta</artifactId>
            <version>${admq.version}</version>
        </dependency>

        <!-- JMS API -->
        <dependency>
            <groupId>jakarta.jms</groupId>
            <artifactId>jakarta.jms-api</artifactId>
            <version>3.1.0</version>
        </dependency>

        <!-- 日志 -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>2.0.17</version>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.4.14</version>
        </dependency>
    </dependencies>
</project>
```

## 2. 发送消息（Producer）

### 2.1 Queue（点对点）发送示例

```java
import com.apusic.admq.jms.AdmqConnectionFactory;
import jakarta.jms.*;

public class AdmqQueueSender {

    public static void main(String[] args) {
        // Broker 地址
        String brokerUrl = "tcp://localhost:61616";
        // 目标队列名称
        String queueName = "test.queue";

        Connection connection = null;
        try {
            // 1. 创建连接工厂
            AdmqConnectionFactory factory = new AdmqConnectionFactory(brokerUrl);

            // 如需认证
            // factory.setUserName("admin");
            // factory.setPassword("admin");

            // 2. 创建连接
            connection = factory.createConnection();
            connection.start();

            // 3. 创建 Session（false=非事务，AUTO_ACKNOWLEDGE=自动确认）
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // 4. 创建目标队列
            Queue queue = session.createQueue(queueName);

            // 5. 创建消息生产者
            MessageProducer producer = session.createProducer(queue);

            // 6. 创建并发送文本消息
            String text = "Hello admq!";
            TextMessage message = session.createTextMessage(text);
            producer.send(message);
            System.out.println("Message sent: " + text);

            // 7. 关闭资源
            producer.close();
            session.close();

        } catch (JMSException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
```

### 2.2 Topic（发布/订阅）发送示例

```java
import com.apusic.admq.jms.AdmqConnectionFactory;
import jakarta.jms.*;

public class AdmqTopicPublisher {

    public static void main(String[] args) {
        String brokerUrl = "tcp://localhost:61616";
        String topicName = "test.topic";

        Connection connection = null;
        try {
            AdmqConnectionFactory factory = new AdmqConnectionFactory(brokerUrl);
            connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // 创建 Topic（与 Queue 的区别仅在于这里）
            Topic topic = session.createTopic(topicName);

            MessageProducer producer = session.createProducer(topic);

            TextMessage message = session.createTextMessage("Hello Topic!");
            producer.send(message);
            System.out.println("Topic message published");

            producer.close();
            session.close();

        } catch (JMSException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try { connection.close(); } catch (JMSException ignored) {}
            }
        }
    }
}
```

### 2.3 发送 BytesMessage 示例

```java
// 发送字节消息
BytesMessage bytesMessage = session.createBytesMessage();
bytesMessage.writeBytes("Hello".getBytes());
producer.send(bytesMessage);

// 发送 ObjectMessage（对象需实现 Serializable）
ObjectMessage objectMessage = session.createObjectMessage(new MySerializableObject());
producer.send(objectMessage);
```

### 2.4 设置消息属性

```java
TextMessage message = session.createTextMessage("priority message");
// 消息持久化（默认 PERSISTENT）
producer.setDeliveryMode(DeliveryMode.PERSISTENT);
// 消息优先级（0-9，默认 4）
producer.setPriority(9);
// 消息过期时间（毫秒）
producer.setTimeToLive(60000);
producer.send(message);
```

## 3. 接收消息（Consumer）

### 3.1 同步接收（receive）

```java
import com.apusic.admq.jms.AdmqConnectionFactory;
import jakarta.jms.*;

public class AdmqQueueReceiver {

    public static void main(String[] args) {
        String brokerUrl = "tcp://localhost:61616";
        String queueName = "test.queue";

        Connection connection = null;
        try {
            AdmqConnectionFactory factory = new AdmqConnectionFactory(brokerUrl);
            connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(queueName);

            // 创建消费者
            MessageConsumer consumer = session.createConsumer(queue);

            System.out.println("Waiting for message...");

            // 阻塞等待接收消息，超时 5 秒
            Message message = consumer.receive(5000);

            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                System.out.println("Received: " + textMessage.getText());
            } else if (message != null) {
                System.out.println("Received non-text message");
            } else {
                System.out.println("No message received within timeout");
            }

            consumer.close();
            session.close();

        } catch (JMSException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try { connection.close(); } catch (JMSException ignored) {}
            }
        }
    }
}
```

### 3.2 异步接收（MessageListener）

```java
import com.apusic.admq.jms.AdmqConnectionFactory;
import jakarta.jms.*;

public class AdmqAsyncConsumer {

    public static void main(String[] args) {
        String brokerUrl = "tcp://localhost:61616";
        String queueName = "test.queue";

        Connection connection = null;
        try {
            AdmqConnectionFactory factory = new AdmqConnectionFactory(brokerUrl);
            connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(queueName);

            MessageConsumer consumer = session.createConsumer(queue);

            // 注册异步监听器
            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        if (message instanceof TextMessage) {
                            TextMessage textMessage = (TextMessage) message;
                            System.out.println("Async received: " + textMessage.getText());
                        }
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });

            System.out.println("Listener registered, waiting for messages...");

            // 保持主线程不退出
            Thread.sleep(60000);

            consumer.close();
            session.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try { connection.close(); } catch (JMSException ignored) {}
            }
        }
    }
}
```

### 3.3 Topic 订阅者（持久化订阅）

```java
import com.apusic.admq.jms.AdmqConnectionFactory;
import jakarta.jms.*;

public class AdmqDurableSubscriber {

    public static void main(String[] args) {
        String brokerUrl = "tcp://localhost:61616";
        String topicName = "test.topic";
        String clientId = "subscriber-1";
        String subscriptionName = "my-sub";

        Connection connection = null;
        try {
            AdmqConnectionFactory factory = new AdmqConnectionFactory(brokerUrl);
            connection = factory.createConnection();

            // 设置客户端 ID（持久化订阅必须）
            connection.setClientID(clientId);
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(topicName);

            // 创建持久化订阅者
            TopicSubscriber subscriber = session.createDurableSubscriber(topic, subscriptionName);

            Message message = subscriber.receive(5000);
            if (message instanceof TextMessage) {
                System.out.println("Received: " + ((TextMessage) message).getText());
            }

            subscriber.close();
            session.close();

        } catch (JMSException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try { connection.close(); } catch (JMSException ignored) {}
            }
        }
    }
}
```

## 4. 事务消息

```java
// 创建事务性 Session（第一个参数 true）
Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

MessageProducer producer = session.createProducer(queue);
TextMessage message = session.createTextMessage("transactional message");
producer.send(message);

// 提交事务
session.commit();

// 如需回滚：
// session.rollback();
```

## 5. 顺序消息（Message Group）

通过 `JMSXGroupID` 保证同一组的消息按顺序被同一个消费者处理：

```java
TextMessage message = session.createTextMessage("ordered message 1");
message.setStringProperty("JMSXGroupID", "group-1");
producer.send(message);

TextMessage message2 = session.createTextMessage("ordered message 2");
message2.setStringProperty("JMSXGroupID", "group-1");
producer.send(message2);
```

## 6. 编译与运行

### 6.1 编译

```bash
mvn clean compile
```

### 6.2 运行

```bash
# 运行生产者
mvn exec:java -Dexec.mainClass="com.example.AdmqQueueSender"

# 或直接使用 java 命令
java -cp "target/classes:$(mvn dependency:build-classpath -q -DincludeScope=compile -Dmdep.outputFile=/dev/stdout)" com.example.AdmqQueueSender
```

## 7. 关键 API 速查

| 操作 | API |
|------|-----|
| 创建连接工厂 | `new AdmqConnectionFactory(brokerUrl)` |
| 创建连接 | `factory.createConnection()` |
| 创建 Session | `connection.createSession(transacted, acknowledgeMode)` |
| 创建 Queue | `session.createQueue(name)` |
| 创建 Topic | `session.createTopic(name)` |
| 创建生产者 | `session.createProducer(destination)` |
| 创建消费者 | `session.createConsumer(destination)` |
| 创建文本消息 | `session.createTextMessage(text)` |
| 创建字节消息 | `session.createBytesMessage()` |
| 创建对象消息 | `session.createObjectMessage(serializable)` |
| 同步接收 | `consumer.receive(timeoutMs)` |
| 异步接收 | `consumer.setMessageListener(listener)` |
| 事务提交 | `session.commit()` |
| 事务回滚 | `session.rollback()` |

## 8. 常见问题

**Q: 连接失败，提示 Connection refused？**
A: 检查 admq Broker 是否已启动，以及 `brokerUrl` 地址和端口是否正确。

**Q: Topic 消息发送后，订阅者收不到？**
A: Topic 是发布/订阅模式，订阅者必须在消息发布前已启动并连接。后启动的订阅者收不到历史消息（除非使用持久化订阅）。

**Q: 如何确认消息已送达？**
A: 使用 `producer.send(message, DeliveryMode.PERSISTENT, priority, timeToLive)` 并确保 Broker 配置了持久化存储（如 KahaDB）。

**Q: 多个消费者时消息如何分配？**
A: Queue 模式下多个消费者竞争消费，每条消息只被一个消费者接收。Topic 模式下每个订阅者都会收到全部消息。
