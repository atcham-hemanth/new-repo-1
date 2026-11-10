pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        AWS_REGION   = 'ap-south-1'
        ECR_REGISTRY = '230623977815.dkr.ecr.ap-south-1.amazonaws.com'
        ECR_URI      = '230623977815.dkr.ecr.ap-south-1.amazonaws.com/hemanth/ecr'

        DEPLOY_HOST  = '172.31.9.81'
        DEPLOY_USER  = 'ubuntu'
        SSH_KEY      = '/var/jenkins_home/.ssh/deploy_to_vm_ed25519'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                echo 'Source code checked out successfully'
            }
        }

        stage('Maven Build') {
            steps {
                sh '''
                    set -e
                    echo "Building Java application"
                    mvn -B clean verify
                '''
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube-Server') {
                    sh '''
                        set -e
                        mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:3.11.0.3922:sonar \
                          -Dsonar.projectKey=ecr-java-app \
                          -Dsonar.projectName=ecr-java-app \
                          -Dsonar.token="$SONAR_AUTH_TOKEN"
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    script {
                        def qualityGate = waitForQualityGate()

                        if (qualityGate.status != 'OK') {
                            error "SonarQube Quality Gate failed: ${qualityGate.status}"
                        }

                        echo 'SonarQube Quality Gate passed'
                    }
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    set -e

                    docker build \
                      -t "$ECR_URI:$BUILD_NUMBER" \
                      -t "$ECR_URI:latest" .
                '''
            }
        }

        stage('Push to AWS ECR') {
            steps {
                sh '''
                    set -e

                    aws ecr get-login-password --region "$AWS_REGION" |
                    docker login --username AWS --password-stdin "$ECR_REGISTRY"

                    docker push "$ECR_URI:$BUILD_NUMBER"
                    docker push "$ECR_URI:latest"
                '''
            }
        }

        stage('Deploy to Deployment VM') {
            steps {
                sh '''
                    set -e

                    ssh -i "$SSH_KEY" \
                      -o BatchMode=yes \
                      -o StrictHostKeyChecking=accept-new \
                      "$DEPLOY_USER@$DEPLOY_HOST" "

                        set -e

                        aws ecr get-login-password --region $AWS_REGION |
                        sudo docker login --username AWS --password-stdin $ECR_REGISTRY

                        sudo docker pull $ECR_URI:$BUILD_NUMBER

                        sudo docker rm -f hemanth-app >/dev/null 2>&1 || true

                        sudo docker run -d \
                          --restart unless-stopped \
                          --name hemanth-app \
                          -p 8085:8085 \
                          $ECR_URI:$BUILD_NUMBER

                        sleep 3

                        echo 'Application response:'
                        curl -fsS http://localhost:8085
                    "
                '''
            }
        }
    }

    post {
        success {
            echo 'CI/CD pipeline completed successfully!'
            echo 'Application: http://13.203.199.119:8085'
        }

        failure {
            echo 'CI/CD pipeline failed. Check the failed stage in the console output.'
        }

        always {
            archiveArtifacts artifacts: 'target/*.jar',
                             fingerprint: true,
                             allowEmptyArchive: true
        }
    }
}
