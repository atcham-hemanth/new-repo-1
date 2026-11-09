
pipeline {
    agent any

    tools {
        maven 'Maven3'
    }

    environment {
        SONAR_PROJECT_KEY = 'hello-java-project'
        DEPLOY_HOST = '172.31.9.81'
        DEPLOY_DIR = '/home/ubuntu/hello-java-deploy'
    }

    stages {

        stage('Checkout') {
            steps {
                echo 'Checking out GitHub repository'

                git branch: 'master',
                    url: 'https://github.com/atcham-hemanth/new-repo-1.git'
            }
        }

        stage('Check Tools') {
            steps {
                sh 'java -version'
                sh 'mvn -version'
            }
        }

        stage('Build') {
            steps {
                echo 'Building Hemanth Java application'
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                echo 'Running unit tests'
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

        stage('Prepare Deployment Files') {
            steps {
                sh '''
                    set -e

                    echo "Locating generated JAR..."

                    JAR_FILE=$(find target \
                        -maxdepth 1 \
                        -type f \
                        -name '*.jar' \
                        ! -name '*-sources.jar' \
                        ! -name '*-javadoc.jar' \
                        | head -n 1)

                    if [ -z "$JAR_FILE" ]; then
                        echo "ERROR: JAR file not found!"
                        exit 1
                    fi

                    mkdir -p target/deployment

                    cp "$JAR_FILE" target/deployment/hello-java.jar

                    cat > target/deployment/Dockerfile <<'EOF'
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY hello-java.jar /app/hello-java.jar
EXPOSE 8080
CMD ["java", "-cp", "hello-java.jar", "com.example.HelloWorld"]
EOF

                    echo "Deployment files prepared:"
                    ls -lh target/deployment/
                '''
            }
        }

        stage('Deploy to Deployment VM') {
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: 'deployment-ssh-key',
                        keyFileVariable: 'SSH_KEY',
                        usernameVariable: 'SSH_USER'
                    )
                ]) {
                    sh '''
                        set -e

                        echo "Preparing deployment directory..."

                        ssh -i "$SSH_KEY" \
                            -o StrictHostKeyChecking=accept-new \
                            "$SSH_USER@$DEPLOY_HOST" \
                            "mkdir -p '$DEPLOY_DIR'"

                        echo "Transferring JAR and Dockerfile..."

                        scp -i "$SSH_KEY" \
                            -o StrictHostKeyChecking=accept-new \
                            target/deployment/hello-java.jar \
                            target/deployment/Dockerfile \
                            "$SSH_USER@$DEPLOY_HOST:$DEPLOY_DIR/"

                        echo "Building and starting application..."

                        ssh -i "$SSH_KEY" \
                            -o StrictHostKeyChecking=accept-new \
                            "$SSH_USER@$DEPLOY_HOST" 'bash -s' <<'REMOTE'
set -e

cd /home/ubuntu/hello-java-deploy

echo "Removing old application container..."

sudo docker rm -f hello-java >/dev/null 2>&1 || true

echo "Building Docker image..."

sudo docker build -t hello-java:latest .

echo "Starting application container..."

sudo docker run -d \
    --name hello-java \
    --restart unless-stopped \
    -p 8080:8080 \
    hello-java:latest

echo "Waiting for application startup..."

for i in $(seq 1 15)
do
    HTTP_CODE=$(curl -s \
        -o /tmp/hello-response.txt \
        -w '%{http_code}' \
        http://127.0.0.1:8080/ || true)

    if [ "$HTTP_CODE" = "200" ]; then
        echo "Application response:"
        cat /tmp/hello-response.txt
        echo

        if grep -Fxq 'Hello from Hemanth Application!' /tmp/hello-response.txt; then
            echo "HTTP verification successful!"
            exit 0
        fi

        echo "Unexpected application response!"
        exit 1
    fi

    sleep 2
done

echo "Application did not respond!"
sudo docker logs hello-java
exit 1
REMOTE

                        echo "Deployment completed successfully!"
                    '''
                }
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

