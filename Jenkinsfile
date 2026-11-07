cat > /var/jenkins_home/workspace/test3/Jenkinsfile <<'EOF'
pipeline {
    agent any
           stage('Check Tools') {
    steps {
        sh 'java -version'
        sh 'mvn -version'
    }
}
    environment {
        SONAR_PROJECT_KEY = 'hello-java-project'
    }

    stages {

        stage('Checkout') {
            steps {
                echo 'Checking out source code from GitHub'

                git branch: 'master',
                    url: 'https://github.com/atcham-hemanth/new-repo-1.git'
            }
        }

        stage('Build') {
            steps {
                echo 'Building Java application with Maven'
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                echo 'Running Maven tests'
                sh 'mvn test'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    def scannerHome = tool 'SonarScanner'

                    withSonarQubeEnv('SonarQube-Server') {
                        sh """
                            ${scannerHome}/bin/sonar-scanner \
                            -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                            -Dsonar.projectName=${SONAR_PROJECT_KEY} \
                            -Dsonar.sources=src/main/java \
                            -Dsonar.tests=src/test/java \
                            -Dsonar.java.binaries=target/classes \
                            -Dsonar.java.test.binaries=target/test-classes \
                            -Dsonar.sourceEncoding=UTF-8
                        """
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        def qg = waitForQualityGate()

                        if (qg.status != 'OK') {
                            error "Quality Gate failed: ${qg.status}"
                        }

                        echo 'Quality Gate passed!'
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                echo 'Demo Java application deployment successful'
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


