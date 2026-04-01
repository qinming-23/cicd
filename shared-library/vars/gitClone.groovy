#!/usr/bin/env groovy

/**
 * Git 克隆
 * @param config Git 配置
 */
def call(Map config = [:]) {
    def url = config.url ?: env.GIT_URL
    def branch = config.branch ?: 'master'
    def credentialsId = config.credentialsId ?: 'github-credentials'
    def depth = config.depth ?: 1
    
    if (!url) {
        error "Git URL is required"
    }
    
    container('jenkins-ssh-client') {
        dir(config.workDir ?: env.WORKSPACE) {
            git credentialsId: credentialsId, 
                url: url, 
                branch: branch,
                depth: depth
        }
    }
}

/**
 * Git 克隆 (多仓库)
 */
def cloneMultiple(Map config = [:]) {
    def repos = config.repos  // List of [url, dir, branch]
    def credentialsId = config.credentialsId ?: 'github-credentials'
    
    container('jenkins-ssh-client') {
        repos.each { repo ->
            def (gitUrl, workDir, branch) = repo
            dir(workDir) {
                git credentialsId: credentialsId, 
                    url: gitUrl, 
                    branch: branch ?: 'master',
                    depth: 1
            }
        }
    }
}