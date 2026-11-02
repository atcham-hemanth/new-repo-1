
pipeline {
    agent any

    stages {

        stage('Checkout') {
            steps {
                echo 'Checking out source code'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Starting build process'
                sh 'chmod +x hello.sh'
                sh './hello.sh'
            }
        }

        stage('Test') {
            steps {
                echo 'Testing the application'
                sh 'test -f hello.sh'
                echo 'Test passed!'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    def scannerHome = tool 'SonarScanner'

                    withSonarQubeEnv('SonarQube-Server') {
                        sh """
                            ${scannerHome}/bin/sonar-scanner \
                            -Dsonar.projectKey=hello-project \
                            -Dsonar.projectName=hello-project \
                            -Dsonar.sources=. \
                            -Dsonar.sourceEncoding=UTF-8
                        """
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                echo 'Waiting for SonarQube Quality Gate result'

                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        def qualityGate = waitForQualityGate()

                        if (qualityGate.status != 'OK') {
                            error "Quality Gate FAILED: ${qualityGate.status}"
                        }

                        echo 'Quality Gate PASSED!'
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                echo 'Quality Gate passed. Ready for deployment!'
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully!'
        }

        failure {
            echo 'Pipeline failed!'
        }

        always {
            echo 'Pipeline execution finished.'
        }
    }
}

