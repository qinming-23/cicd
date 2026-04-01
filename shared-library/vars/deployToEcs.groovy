#!/usr/bin/env groovy

/**
 * 部署到 ECS 虚拟机
 * @param config 部署配置
 */
def call(Map config = [:]) {
    def hosts = config.hosts ?: env.ECS_HOSTS?.split(',')
    def deployPath = config.deployPath ?: '/data/apps'
    def packagePath = config.packagePath ?: "${env.WORKSPACE}/dist.tar"
    def serviceName = config.serviceName ?: env.IMAGE_NAME
    def user = config.user ?: 'www-data'
    def backupPath = config.backupPath ?: '/data/backup/apps'
    def restartCommand = config.restartCommand ?: "systemctl restart ${serviceName}"
    
    if (!hosts) {
        error "ECS hosts is required"
    }
    
    container('jenkins-ssh-client') {
        // 获取 SSH 密钥
        withCredentials([sshUserPrivateKey(keyFileVariable: 'SSH_KEY', 
            credentialsId: config.sshCredentialsId ?: 'ecs-ssh-key')]) {
            
            def timestamp = sh(script: "date +%Y-%m-%d-%H-%M-%S", returnStdout: true).trim()
            
            hosts.each { host ->
                sh """
                    # 复制密钥
                    cp \${SSH_KEY} /tmp/service-key.pem
                    chmod 400 /tmp/service-key.pem
                    
                    # 备份旧版本
                    ssh -i /tmp/service-key.pem -o StrictHostKeyChecking=no ${user}@${host} \
                        "mkdir -p ${backupPath}/${serviceName} && \
                         cp ${deployPath}/${serviceName}/${serviceName}.* ${backupPath}/${serviceName}/${timestamp}-${serviceName}.tar || true"
                    
                    # 上传新包
                    scp -i /tmp/service-key.pem -o StrictHostKeyChecking=no ${packagePath} \
                        ${user}@${host}:${deployPath}/${serviceName}/
                    
                    # 解压
                    ssh -i /tmp/service-key.pem -o StrictHostKeyChecking=no ${user}@${host} \
                        "cd ${deployPath}/${serviceName} && tar xvf dist.tar"
                    
                    # 设置权限
                    ssh -i /tmp/service-key.pem -o StrictHostKeyChecking=no ${user}@${host} \
                        "chown -R ${user}:${user} ${deployPath}/${serviceName}"
                    
                    # 重启服务
                    ssh -i /tmp/service-key.pem -o StrictHostKeyChecking=no ${user}@${host} \
                        "${restartCommand}"
                """
            }
        }
    }
    
    return true
}