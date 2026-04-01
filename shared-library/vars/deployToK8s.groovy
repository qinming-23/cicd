#!/usr/bin/env groovy

/**
 * 部署到 Kubernetes (kubectl 方式)
 * @param config 部署配置
 */
def call(Map config = [:]) {
    def deploymentName = config.deploymentName ?: env.IMAGE_NAME
    def imageName = config.imageName ?: env.IMAGE_NAME
    def imageTag = config.imageTag ?: env.BUILD_NUMBER
    def registry = config.registry ?: env.HARBOR_REGISTRY ?: 'harbor.example.com'
    def namespace = config.namespace ?: 'default'
    def containerName = config.containerName ?: deploymentName
    def kubeconfigId = config.kubeconfigId ?: 'k8s-config'
    def replicas = config.replicas ?: 1
    
    def fullImageName = "${registry}/${imageName}:${imageTag}"
    
    container('jenkins-kubectl') {
        withKubeConfig(credentialsId: kubeconfigId) {
            // 更新 Deployment 镜像
            sh """
                kubectl set image deployment/${deploymentName} \
                    ${containerName}=${fullImageName} \
                    -n ${namespace}
            """
            
            // 记录变更原因
            sh """
                kubectl annotate deployment/${deploymentName} \
                    kubernetes.io/change-cause="Build ${imageTag}" \
                    -n ${namespace} --overwrite
            """
            
            // 等待部署完成
            sh """
                kubectl rollout status deployment/${deploymentName} \
                    -n ${namespace} --timeout=300s
            """
        }
    }
    
    return fullImageName
}

/**
 * 使用 YAML 部署到 K8S
 */
def deployWithYaml(Map config = [:]) {
    def yamlFile = config.yamlFile ?: 'deploy.yaml'
    def kubeconfigId = config.kubeconfigId ?: 'k8s-config'
    def namespace = config.namespace ?: 'default'
    
    container('jenkins-kubectl') {
        withKubeConfig(credentialsId: kubeconfigId) {
            sh "kubectl apply -f ${yamlFile} -n ${namespace}"
        }
    }
}

/**
 * 部署到 K8S (Helm 方式)
 */
def helmDeploy(Map config = [:]) {
    def releaseName = config.releaseName ?: env.IMAGE_NAME
    def chartPath = config.chartPath ?: './chart'
    def namespace = config.namespace ?: 'default'
    def valuesFile = config.valuesFile ?: 'values.yaml'
    def kubeconfigId = config.kubeconfigId ?: 'k8s-config'
    def setValues = config.setValues ?: []
    
    container('jenkins-helm') {
        withKubeConfig(credentialsId: kubeconfigId) {
            def setArgs = setValues.collect { "${it.key}=${it.value}" }.join(' ')
            
            sh """
                helm upgrade --install ${releaseName} ${chartPath} \
                    -n ${namespace} \
                    -f ${valuesFile} \
                    ${setArgs ? "--set ${setArgs}" : ""}
            """
        }
    }
}