#!/usr/bin/env groovy

/**
 * 推送 Docker 镜像到 Harbor
 * @param config 推送配置
 */
def call(Map config = [:]) {
    def imageName = config.imageName ?: env.IMAGE_NAME
    def imageTag = config.imageTag ?: env.BUILD_NUMBER
    def registry = config.registry ?: env.HARBOR_REGISTRY ?: 'harbor.example.com'
    def credentialsId = config.credentialsId ?: 'harbor-credentials'
    
    container('jenkins-docker') {
        // 登录 Harbor
        withCredentials([[$class: 'UsernamePasswordMultiBinding', 
            credentialsId: credentialsId,
            usernameVariable: 'HARBOR_USER', 
            passwordVariable: 'HARBOR_PASS']]) {
            sh "docker login ${registry} -u ${HARBOR_USER} -p ${HARBOR_PASS}"
            
            // 推送镜像
            sh "docker push ${registry}/${imageName}:${imageTag}"
            sh "docker push ${registry}/${imageName}:latest"
        }
    }
    
    return "${registry}/${imageName}:${imageTag}"
}

/**
 * 镜像打标签并推送
 * @param imageName 镜像名
 * @param tagName 标签名
 * @param registry 镜像仓库
 */
def retagAndPush(String imageName, String tagName, String registry = null) {
    registry = registry ?: env.HARBOR_REGISTRY ?: 'harbor.example.com'
    def credentialsId = 'harbor-credentials'
    
    container('jenkins-docker') {
        withCredentials([[$class: 'UsernamePasswordMultiBinding', 
            credentialsId: credentialsId,
            usernameVariable: 'HARBOR_USER', 
            passwordVariable: 'HARBOR_PASS']]) {
            sh "docker login ${registry} -u ${HARBOR_USER} -p ${HARBOR_PASS}"
            sh "docker tag ${registry}/${imageName} ${registry}/${imageName}:${tagName}"
            sh "docker push ${registry}/${imageName}:${tagName}"
        }
    }
}