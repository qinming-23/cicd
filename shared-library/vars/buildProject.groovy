#!/usr/bin/env groovy

/**
 * 通用构建方法 - 根据语言自动选择构建方式
 * @param config 构建配置
 */
def call(Map config = [:]) {
    def language = config.language ?: 'java-maven'
    
    switch(language) {
        case 'java-maven':
            buildMaven(config)
            break
        case 'java-gradle':
            buildGradle(config)
            break
        case 'nodejs':
        case 'node':
            buildNodejs(config)
            break
        case 'go':
            buildGo(config)
            break
        case 'python':
            buildPython(config)
            break
        default:
            error "Unsupported language: ${language}"
    }
}

/**
 * Maven 构建
 */
def buildMaven(Map config = [:]) {
    def buildCommand = config.buildCommand ?: './mvnw clean package -Dmaven.test.skip=true'
    def buildContainer = config.buildContainer ?: 'jenkins-maven'
    def settingsId = config.mavenSettingsId ?: 'maven-settings'
    
    container(buildContainer) {
        dir(config.workDir ?: env.WORKSPACE) {
            withMaven(globalMavenSettingsConfig: settingsId) {
                sh buildCommand
            }
        }
    }
}

/**
 * Gradle 构建
 */
def buildGradle(Map config = [:]) {
    def buildCommand = config.buildCommand ?: './gradlew clean build -x test'
    def buildContainer = config.buildContainer ?: 'jenkins-maven'
    
    container(buildContainer) {
        dir(config.workDir ?: env.WORKSPACE) {
            sh buildCommand
        }
    }
}

/**
 * Node.js 构建
 */
def buildNodejs(Map config = [:]) {
    def packageManager = config.packageManager ?: 'npm'  // npm, pnpm, yarn
    def installCommand = config.installCommand ?: "${packageManager} install"
    def buildCommand = config.buildCommand ?: "${packageManager} run build"
    def buildContainer = config.buildContainer ?: 'jenkins-nodejs'
    def registry = config.registry ?: 'https://registry.npmmirror.com'
    
    container(buildContainer) {
        dir(config.workDir ?: env.WORKSPACE) {
            sh "npm config set registry ${registry}"
            
            if (packageManager == 'pnpm') {
                sh "npm install -g pnpm"
            } else if (packageManager == 'yarn') {
                sh "yarn config set registry ${registry}"
            }
            
            sh installCommand
            sh buildCommand
            
            // 打包 dist 目录
            sh "tar cvf dist.tar dist"
        }
    }
}

/**
 * Go 构建
 */
def buildGo(Map config = [:]) {
    def buildCommand = config.buildCommand ?: 'go build -o app .'
    def buildContainer = config.buildContainer ?: 'jenkins-golang'
    def goVersion = config.goVersion ?: '1.22'
    def moduleProxy = config.moduleProxy ?: 'https://goproxy.cn'
    
    container(buildContainer) {
        dir(config.workDir ?: env.WORKSPACE) {
            sh """
                export GO111MODULE=on
                export GOPROXY=${moduleProxy}
                go mod tidy
                ${buildCommand}
            """
        }
    }
}

/**
 * Python 构建
 */
def buildPython(Map config = [:]) {
    def pythonVersion = config.pythonVersion ?: '3.10'
    def venvName = config.venvName ?: 'venv'
    def buildCommand = config.buildCommand ?: 'pip install -r requirements.txt'
    def buildContainer = config.buildContainer ?: 'jenkins-python'
    
    container(buildContainer) {
        dir(config.workDir ?: env.WORKSPACE) {
            sh """
                python${pythonVersion} -m venv ${venvName}
                source ${venvName}/bin/activate
                ${buildCommand}
            """
        }
    }
}