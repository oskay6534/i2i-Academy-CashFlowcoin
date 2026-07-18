pipeline {
    agent any

    parameters {
        booleanParam(name: 'START_DEPENDENCIES', defaultValue: false, description: 'Start PostgreSQL and Redis only on an isolated CI agent. Leave disabled when local Docker services are already running.')
        booleanParam(name: 'RUN_SELENIUM', defaultValue: false, description: 'Run browser tests against a running application.')
        string(name: 'APP_URL', defaultValue: 'http://localhost:5173', description: 'Frontend URL for Selenium tests.')
        string(name: 'SELENIUM_GRID_URL', defaultValue: 'http://localhost:4444/wd/hub', description: 'Selenium Grid URL.')
    }
    // environment
    environment {
        POSTGRES_HOST = 'localhost'
        POSTGRES_PORT = '55432'
        POSTGRES_DB = 'cryptopal'
        POSTGRES_USER = 'cryptopal'
        POSTGRES_PASSWORD = 'cryptopal123'
        REDIS_HOST = 'localhost'
        REDIS_PORT = '6379'
    }
    // stages
    stages {
        stage('Validate Docker Compose') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'docker compose config -q'
                    } else {
                        bat 'docker compose config -q'
                    }
                }
            }
        }

        stage('Start dependencies') {
            when { expression { return params.START_DEPENDENCIES } }
            steps {
                script {
                    if (isUnix()) {
                        sh 'docker compose up -d --wait'
                    } else {
                        bat 'docker compose up -d'
                    }
                }
            }
        }

        stage('Backend tests') {
            steps {
                dir('backend') {
                    script {
                        if (isUnix()) {
                            sh './mvnw test'
                        } else {
                            bat 'mvnw.cmd test'
                        }
                    }
                }
            }
        }

        stage('Frontend build') {
            steps {
                dir('frontend') {
                    script {
                        if (isUnix()) {
                            sh 'npm ci && npm run build'
                        } else {
                            bat 'npm ci && npm run build'
                        }
                    }
                }
            }
        }

        stage('Selenium E2E') {
            when { expression { return params.RUN_SELENIUM } }
            steps {
                script {
                    if (isUnix()) {
                        sh 'mvn -f tests/e2e/pom.xml test'
                    } else {
                        bat 'mvn -f tests/e2e/pom.xml test'
                    }
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
            archiveArtifacts artifacts: 'frontend/dist/**', allowEmptyArchive: true
            script {
                if (params.START_DEPENDENCIES) {
                    if (isUnix()) {
                        sh 'docker compose down'
                    } else {
                        bat 'docker compose down'
                    }
                }
            }
        }
    }
}
