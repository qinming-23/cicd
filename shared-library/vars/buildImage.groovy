#!/usr/bin/env groovy

/**
 * 构建 Docker 镜像
 * @param config 镜像构建配置
 */
def call(Map config = [:]) {
    def imageName = config.imageName ?: env.IMAGE_NAME
    def imageTag = config.imageTag ?: env.BUILD_NUMBER
    def baseImage = config.baseImage ?: 'openjdk:17-slim'
    def registry = config.registry ?: env.HARBOR_REGISTRY ?: 'harbor.example.com'
    def contextPath = config.contextPath ?: env.WORKSPACE
    def dockerfileContent = config.dockerfile
    
    // 如果没有提供 dockerfile，生成默认的
    if (!dockerfileContent) {
        dockerfileContent = generateDefaultDockerfile(config)
    }
    
    container('jenkins-docker') {
        dir(contextPath) {
            // 写入 Dockerfile
            writeFile file: 'Dockerfile', text: dockerfileContent
            
            // 登录 Harbor
            withCredentials([[$class: 'UsernamePasswordMultiBinding', 
                credentialsId: config.credentialsId ?: 'harbor-credentials',
                usernameVariable: 'HARBOR_USER', 
                passwordVariable: 'HARBOR_PASS']]) {
                sh "docker login ${registry} -u ${HARBOR_USER} -p ${HARBOR_PASS}"
                
                // 构建镜像
                sh "docker build -t ${registry}/${imageName}:${imageTag} ."
                
                // 同时打 latest 标签
                sh "docker tag ${registry}/${imageName}:${imageTag} ${registry}/${imageName}:latest"
            }
        }
    }
    
    // 返回镜像完整地址
    return "${registry}/${imageName}:${imageTag}"
}

/**
 * 生成默认 Dockerfile
 */
def generateDefaultDockerfile(Map config) {
    def language = config.language
    def packagePath = config.packagePath ?: 'target'
    def jarName = config.jarName
    def entrypoint = config.entrypoint
    
    switch(language) {
        case 'java-maven':
        case 'java-gradle':
            return """\
FROM ${config.baseImage ?: 'openjdk:17-slim'}
COPY ${packagePath}/${jarName ?: 'app.jar'} /app/app.jar
WORKDIR /app
${entrypoint ?: 'ENTRYPOINT ["java", "-jar", "/app/app.jar"]'}
"""
        case 'nodejs':
        case 'node':
            return """\
FROM node:18-slim
WORKDIR /app
COPY package*.json ./
RUN npm install --registry=https://registry.npmmirror.com
COPY . .
RUN npm run build
${entrypoint ?: ''}
"""
        case 'go':
            return """\
FROM golang:1.22-alpine AS builder
WORKDIR /app
COPY . .
RUN CGO_ENABLED=0 go build -o app .

FROM alpine:latest
WORKDIR /app
COPY --from=builder /app/app .
${entrypoint ?: 'ENTRYPOINT ["/app/app"]'}
"""
        default:
            return """\
FROM ${config.baseImage ?: 'alpine:latest'}
COPY . /app
WORKDIR /app
${entrypoint ?: ''}
"""
    }
}