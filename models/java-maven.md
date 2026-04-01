# Java Maven 流水线模板

> 通用 Java Maven 项目流水线，支持构建、镜像构建、ECS/K8S 部署

## 模板参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `language` | 语言类型 | `java-maven` |
| `build_command` | Maven 构建命令 | `./mvnw clean package -Dmaven.test.skip=true` |
| `build_container` | 构建容器名称 | `jenkins-maven` |
| `maven_settings_id` | Maven settings 凭据 ID | `maven-settings` |
| `jar_name` | JAR 包名 | `app.jar` |
| `package_path` | 打包后 JAR 路径 | `target/` |

## 镜像参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `base_image` | 基础镜像 | `openjdk:17-slim` |
| `registry` | 镜像仓库 | `harbor.example.com` |
| `image_name` | 镜像名称 | 项目名 |
| `jvm_opts` | JVM 参数 | `-Xmx1g -Xms512m` |
| `health_check_path` | 健康检查路径 | `/actuator/health` |
| `health_check_port` | 健康检查端口 | `8080` |

## 部署参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `deploy_target` | 部署目标 | `k8s` (或 `ecs`) |
| `namespace` | K8S 命名空间 | `default` |
| `replicas` | 副本数 | `2` |
| `cpu_limit` | CPU 限制 | `500m` |
| `memory_limit` | 内存限制 | `1Gi` |
| `hosts` | ECS 主机列表 | - |
| `deploy_path` | ECS 部署路径 | `/data/apps` |

## 使用示例

### YAML 元数据示例

```yaml
name: my-java-service
language: java-maven

build:
  build_command: "./mvnw clean package -Dmaven.test.skip=true"
  build_container: "jenkins-maven"
  jar_name: "my-service.jar"
  package_path: "target/"

image:
  base_image: "openjdk:17-slim"
  registry: "harbor.example.com"
  image_name: "my-java-service"
  jvm_opts: "-Xmx2g -Xms1g -XX:+UseG1GC"
  dockerfile: |
    FROM openjdk:17-slim
    COPY target/my-service.jar /app/my-service.jar
    ENV JAVA_OPTS="-Xmx2g -Xms1g"
    ENTRYPOINT ["java", "-jar", "/app/my-service.jar"]

deploy:
  target: k8s
  namespace: production
  replicas: 2
  resources:
    cpu: "1000m"
    memory: "2Gi"
  health_check:
    path: "/actuator/health"
    port: 8080

env:
  SPRING_PROFILES_ACTIVE: "prod"
  JAVA_OPTS: "-Xmx2g -Xms1g"
```

### 生成后的 Jenkinsfile 片段

```groovy
@Library('jenkins-cicd-shared')_

pipeline {
    agent {
        kubernetes {
            yamlFile 'resources/agent/k8s-agent.yaml'
        }
    }
    
    environment {
        IMAGE_NAME = 'my-java-service'
        HARBOR_REGISTRY = 'harbor.example.com'
    }
    
    stages {
        stage('Build') {
            steps {
                script {
                    buildProject(language: 'java-maven', 
                                buildCommand: './mvnw clean package -Dmaven.test.skip=true')
                }
            }
        }
        
        stage('Build Image') {
            steps {
                script {
                    def image = buildImage(imageName: 'my-java-service',
                                          baseImage: 'openjdk:17-slim',
                                          language: 'java-maven')
                    env.IMAGE_FULL_NAME = image
                }
            }
        }
        
        stage('Deploy to K8S') {
            steps {
                script {
                    deployToK8s(imageName: 'my-java-service',
                               imageTag: env.BUILD_NUMBER,
                               namespace: 'production')
                }
            }
        }
    }
}
```