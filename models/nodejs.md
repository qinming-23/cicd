# Node.js 流水线模板

> 通用 Node.js 项目流水线，支持 npm/pnpm/yarn，支持构建、镜像构建、ECS/K8S 部署

## 模板参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `language` | 语言类型 | `nodejs` |
| `package_manager` | 包管理器 | `npm` (可选: pnpm, yarn) |
| `install_command` | 安装命令 | `${packageManager} install` |
| `build_command` | 构建命令 | `${packageManager} run build` |
| `build_container` | 构建容器名称 | `jenkins-nodejs` |
| `registry` | npm 镜像 | `https://registry.npmmirror.com` |
| `dist_path` | 静态资源路径 | `dist` |

## 镜像参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `base_image` | 基础镜像 | `node:18-slim` |
| `registry` | 镜像仓库 | `harbor.example.com` |
| `image_name` | 镜像名称 | 项目名 |
| `node_env` | Node 环境 | `production` |
| `health_check_path` | 健康检查路径 | `/` |
| `health_check_port` | 健康检查端口 | `3000` |

## 部署参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `deploy_target` | 部署目标 | `k8s` (或 `ecs`) |
| `namespace` | K8S 命名空间 | `default` |
| `replicas` | 副本数 | `2` |
| `cpu_limit` | CPU 限制 | `500m` |
| `memory_limit` | 内存限制 | `512Mi` |

## 使用示例

### YAML 元数据示例 (pnpm)

```yaml
name: my-frontend
language: nodejs

build:
  package_manager: pnpm
  install_command: "pnpm install"
  build_command: "pnpm run build"
  build_container: "jenkins-nodejs"
  dist_path: "dist"

image:
  base_image: "node:18-slim"
  registry: "harbor.example.com"
  image_name: "my-frontend"
  dockerfile: |
    FROM node:18-slim
    WORKDIR /app
    COPY dist /app/dist
    COPY nginx.conf /etc/nginx/nginx.conf
    EXPOSE 80
    CMD ["nginx", "-g", "daemon off;"]

deploy:
  target: k8s
  namespace: production
  replicas: 2
  resources:
    cpu: "500m"
    memory: "512Mi"
  health_check:
    path: "/"
    port: 80
```

### YAML 元数据示例 (yarn)

```yaml
name: my-react-app
language: nodejs

build:
  package_manager: yarn
  build_command: "yarn build"
  dist_path: "build"

image:
  base_image: "nginx:alpine"
  registry: "harbor.example.com"
  image_name: "my-react-app"
  dockerfile: |
    FROM nginx:alpine
    COPY build /usr/share/nginx/html
    COPY nginx.conf /etc/nginx/conf.d/default.conf
    EXPOSE 80
    CMD ["nginx", "-g", "daemon off;"]

deploy:
  target: k8s
  namespace: production
  ingress:
    host: "my-app.example.com"
    path: "/"
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
        IMAGE_NAME = 'my-frontend'
        HARBOR_REGISTRY = 'harbor.example.com'
    }
    
    stages {
        stage('Install & Build') {
            steps {
                script {
                    buildProject(language: 'nodejs', 
                                packageManager: 'pnpm',
                                buildCommand: 'pnpm run build')
                }
            }
        }
        
        stage('Build Image') {
            steps {
                script {
                    def image = buildImage(imageName: 'my-frontend',
                                          baseImage: 'nginx:alpine',
                                          language: 'nodejs')
                    env.IMAGE_FULL_NAME = image
                }
            }
        }
        
        stage('Deploy to K8S') {
            steps {
                script {
                    deployToK8s(imageName: 'my-frontend',
                               imageTag: env.BUILD_NUMBER,
                               namespace: 'production')
                }
            }
        }
    }
}
```