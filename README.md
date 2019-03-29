# Spring cloud config 

## 코드와 설정의 분리
모든 Application에서 떼려야 뗄 수 없는 것이 바로 Configuration이다. Database, Cache 등과 관련된 정보  
연동할 Server의 주소 등이 대부분인데 일반적으로는 하드코딩 된 설정파일을 가지고 주석 처리를 하는 형태로 관리한다.
  
Project의 규모가 커짐에 따라 Configuration 값들에 대한 중앙 관리 및 손쉬운 재배포 등의 필요가 커진다.  
특히 dev, staging, service 등 여러 profile을 가진 Web application이라면 더욱 필요로 할 것이다.   
 
 ```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">

<properties>
    <entry key="stage.mode">development</entry>
    <entry key="domain.name">App-DEV</entry>
    <entry key="domain.url">https://192.168.11.56:8463</entry>
	<entry key="batch.domain.url">https://192.168.11.56:8503</entry>
    <entry key="domain.mail.prefix">DEV Server</entry>
    <entry key="jndi.dataSource">jboss/datasources/dev</entry>
    <!--<entry key="jndi.dataSource">jboss/datasources/stage</entry>-->
    <!--<entry key="jndi.dataSource">jboss/datasources/beta</entry>-->
    <!--<entry key="jndi.dataSource">jboss/datasources/real</entry>-->
    <entry key="jndi.mail">jboss/mail/Default</entry>
    <entry key="jndi.nativeCacheManager">jboss/infinispan/container/application</entry>
    <entry key="schedule.dataSyncYn">N</entry>
    <entry key="schedule.sendDailyMailYn">N</entry>
    <entry key="schedule.sendWeeklyMailYn">N</entry>
    <entry key="schedule.sendMonthlyMailYn">N</entry>
</properties>
```
위와 같은 설정파일을 통해 관리하는 Application의 경우엔 dev, staging, real 등의 서버마다 개별적인 property xml 파일을  
지정된 위치에 두어 관리를 할 것이다.  

여기서 만약 새로운 설정값이 추가되거나 기존의 설정값이 변경되는 경우엔 모든 서버를 돌아다니며 새로이 값을 추가하거나  
변경된 값을 복사 붙여넣기 하는 자신을 발견할 수 있다.  
(복사 붙여넣기면 다행이다. 서버마다 연동하는 주소가 다르다면 double check을 넘어서 triple check을 해도 불안하다.)  

사실 Spring에는 코드와 설정을 분리하자는 패러다임을 위해 application.properties(yml)를 통해 profile 마다 다른 설정파일을  
지정할 수 있다. 
```yaml
# application-real.yml
taesu:
  profile: I'm real
  said:
    first: Hello
    second: Real world!

```

```yaml
# application-dev.yml
taesu:
  profile: I'm dev
  said:
    first: Hello
    second: Dev world! (changed for refresh bus in server)

```

Profile 별 설정파일을 분리하고 Application이 실행 될 시점에 spring.profile.active를 전달하여 주면 딱 한번의 빌드이지만  
설정값은 제 각각으로 적용되어 동작하는 형태로 실행할 수 있다.     
  
하지만 이 방법에도 단점은 있다. 설정 파일이 변경되면 다시 빌드 및 배포를 진행해야 한다는 것이다.  
어찌보면 매우 당연한 것이나 만약 하나의 application에서 배포해야 할 서버가 100대라고 가정해보면  
당연하지 않고 싶을 것이다.    
 
 
## Configuration 공통 관리
<img src="https://i0.wp.com/blog.leekyoungil.com/wp-content/uploads/2017/04/1.png" width="1000" />
<참조 http://blog.leekyoungil.com/?p=352>  
  
Spring cloud config에선 위의 그림처럼 여러 Application의 설정을 중앙에서 관리할 수 있다. 또한 설정 파일을  
Github과 같은 Git repositroy를 통해 관리할 수 있도록 한다.


먼저 Github과 같은 Repository 하나를 생성하여 아래와 같은 설정파일을 생성하여 commit 및 push한다.  
파일 이름 규칙은 {application}-{property}.properties(yml)이다.  
 
```yaml
# config-client-real.yml
taesu:
  profile: I'm real
  said:
    first: Hello
    second: Real world!

```

```yaml
# config-client-dev.yml
taesu:
  profile: I'm dev
  said:
    first: Hello
    second: Dev world! (changed for refresh bus in server)

```

그 후 Spring initializer를 통해 Spring Config Server dependency를 설정한 Spring boot application을 생성한다.  
그 후 아래와 같은 application.properties를 설정한다.   
Config server는 9090 포트에 띄울 것이며  git uri에는 우리가 생성한 repository를 명시하면 된다.  

```properties
server.port=9090
spring.cloud.config.server.git.uri=https://github.com/dlxotn216/spring-cloud-config.git
        
```

spring.profile.active를 dev로 주고 application을 실행한 후 설정값이 잘 연동되었는지 URL을 호출하여 확인할 수 있다.   
<img src="https://raw.githubusercontent.com/dlxotn216/spring-cloud-config/master/images/config-client_dev.png" width="1000" />


다음으로 Spring initializer를 통해 Spring config client, Actuactor dependency를 설정한 Spring boot application을 생성한다.    
그 후 아래와 같은 bootstrap.properties를 선언한다. bootstrap.properties 파일은 application.properties 보다 먼저 로드된다.    
Config client는 8081 포트에 띄울 것이며 application name은 config-client이고 config server 주소를 지정하고 있다.  
```properties
server.port=8081

spring.application.name=config-client
spring.cloud.config.uri=http://localhost:9090

management.endpoints.web.exposure.include=refresh
```

spring.profile.active를 dev로 주고 Client appliation을 구동하면 아래와 같은 INFO log들을 볼 수 있다.   
<img src="https://raw.githubusercontent.com/dlxotn216/spring-cloud-config/master/images/first_info_log.png" width="1000" />

제대로 Config server를 통해 Client application이 떴는지 확인을 하기 위해 아래와 같은 controller 및 service를 정의한다.  

```java
@Service
@RefreshScope
public class DynamicConfigService {

    @Value("${taesu.profile}")
    private String profile;

    @Value("${taesu.said.first}")
    private String first;

    @Value("${taesu.said.second}")
    private String second;

    public Map<String, String> getPropertyMap() {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put("profile", this.profile);
        propertyMap.put("first", this.first);
        propertyMap.put("second", this.second);
        return propertyMap;
    }
}
```

```java
@Service
public class StaticConfigService {

    @Value("${taesu.profile}")
    private String profile;

    @Value("${taesu.said.first}")
    private String first;

    @Value("${taesu.said.second}")
    private String second;

    public Map<String, String> getPropertyMap() {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put("profile", this.profile);
        propertyMap.put("first", this.first);
        propertyMap.put("second", this.second);
        return propertyMap;
    }
}
```

```java
@RestController
@RequiredArgsConstructor
public class ConfigController {

    private final @NotNull StaticConfigService staticConfigService;
    private final @NotNull DynamicConfigService dynamicConfigService;

    @GetMapping("config/static")
    public ResponseEntity<Map<String, String>> getStaticConfigPropertyMap() {
        return ResponseEntity.ok(staticConfigService.getPropertyMap());
    }

    @GetMapping("config/dynamic")
    public ResponseEntity<Map<String, String>> getDynamicConfigPropertyMap() {
        return ResponseEntity.ok(dynamicConfigService.getPropertyMap());
    }
}
```

현재 설장파일을 조회하는 간단한 코드이다. 그런데 DynamicConfigService를 보면 @RefreshScope라는 어노테이션이 있다.  
 이 어노테이션은 설정파일을 다시 읽어들일 수 있도록 하는 개념이라고 생각하면 된다.  
 
 http://localhost:8081/config/dynamic으로 조회하면 아래와 같은 response가 나타난다 
 ```json
{"profile":"I'm dev","first":"Hello","second":"Dev world! (changed for refresh bus in server)"}
```

여기서 Git repository의 설정파일을 변경 후 push 해보자

```yaml
# config-client-dev.yml
taesu:
  profile: I'm dev
  said:
    first: Hello
    second: Dev world!

```

push가 완료된 후 http://localhost:9090/config-client/dev를 조회하면 변경된 설정파일의 값들이 조회된다.   
```json
{"name":"config-client","profiles":["dev"],
"label":null,"version":"a575e458e22764f3fe7b99fb38c93800bfefae89","state":null,
"propertySources":[{"name":"https://github.com/dlxotn216/spring-cloud-config.git/config-client-dev.yml",
"source":{"taesu.profile":"I'm dev","taesu.said.first":"Hello","taesu.said.second":"Dev world!"}}]}
```

하지만 config client의 static, dynamic은 둘다 이전 값들을 내보내고 있다.  
여기서 http://localhost:8081/actuator/refresh [POST] (Content-Type: application/json)으로 요청을 보내면 아래의 응답이 나온다  
<img src="https://raw.githubusercontent.com/dlxotn216/spring-cloud-config/master/images/8081_actuator_refresh.png" width="1000" />

그 후 http://localhost:8081/config/dynamic으로 조회하면 업데이트 된 설정 값을 볼 수 있다.
```json
{"profile":"I'm dev","first":"Hello","second":"Dev world!"}
```


## Spring cloud bus 
앞선 방법을 통해서는 Config server가 Client에 publishing 할 수 없어 Config client에 일일히 /actuator/refresh 요청을 날려  
매뉴얼적으로 설정값을 업데이트 해야한다. 서버가 100대라면 매우 비 효율적일 것이다.  

Spring cloud bus는 Message queue를 통해 일괄 갱신하는 방법을 제공하며 설정은 아래와 같다. 

#### Config Server config
먼저 Config Server에 아래와 같은 설정을 적용한다.  
bus-refresh endpoints를 포함하여 새로운 endpoints를 적용한 것을 볼 수 있다.  
 ```properties
# application.properties
server.port=9090
spring.cloud.config.server.git.uri=https://github.com/dlxotn216/spring-cloud-config.git

management.endpoints.web.exposure.include=refresh, bus-refresh, bus-env

spring.rabbitmq.host=192.168.99.100
spring.rabbitmq.port=5672
spring.rabbitmq.username=test
spring.rabbitmq.password=test
```
그 후 spring-cloud-config-monitor, spring-cloud-starter-bus-amqp 의존성을 추가한다.  

#### Config Client config  
마찬가지로 Config client에도 아래와 같은 의존을 추가한다. (bootstrap.properties와 다름)  
```properties
# application.properties
spring.rabbitmq.host=192.168.99.100
spring.rabbitmq.port=5672
spring.rabbitmq.username=test
spring.rabbitmq.password=test
```

Message queue는 RabbitMQ를 사용하므로 적절히 설치하거나 준비하자.  
본문에서는 Docker에서 아래 명령어를 통해 설정했다. (Window 환경이라 IP가 192.168.99.100에 매핑 되어있다.)  
docker run -it -d -p 5672:5672 -p 15672:15672 -e RABBITMQ_DEFAULT_USER=test -e RABBITMQ_DEFAULT_PASS=test rabbitmq:3.7.2-management-alpine
<img src="https://raw.githubusercontent.com/dlxotn216/spring-cloud-config/master/images/docker.png" width="1000" />


다시 Config Server, Config Client를 띄워보자. 정상적으로 설정이 완료되면 아래와 같은 로그가 뜬다.  
**Config Server**
<img src="https://raw.githubusercontent.com/dlxotn216/spring-cloud-config/master/images/docker_config_server_log.png" width="1000" />
**Config Client**
<img src="https://raw.githubusercontent.com/dlxotn216/spring-cloud-config/master/images/docker_config_config_log.png" width="1000" />

기본적으로 설정된 값들이 정상적으로 조회되는 지 확인 후 Config 파일을 수정하여 commit 및 push한다.  
```yaml
taesu:
  profile: I'm dev
  said:
    first: Hello
    second: Dev world! for check bus-refresh
```

그 후 Config client의 주소가 아닌 Config Server로 아래와 같은 요청을 보내보자.  
http://localhost:9090/actuator/bus-refresh [POST] (Content-Type: application/json)  
<img src="https://raw.githubusercontent.com/dlxotn216/spring-cloud-config/master/images/9090_bus_refresh.png" width="1000" />

정상적인 response가 온 후 Config client에서 업데이트 된 것을 확인 해보자.  
MQ에서 받은 메시지에 의해 Spring actuator가 설정을 다시 로드하는 로그가 보인다. 
<img src="https://raw.githubusercontent.com/dlxotn216/spring-cloud-config/master/images/refresh_bus_reload_log.png" width="1000" />

실제 조회해보면 정상적으로 변경 된 설정값이 노출되는 것을 볼 수 있다.
<img src="https://raw.githubusercontent.com/dlxotn216/spring-cloud-config/master/images/refresh_bus_dynamic.png" width="1000" />


