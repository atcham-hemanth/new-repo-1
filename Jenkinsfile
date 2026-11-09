
pipeline {
    agent any

    tools {
        maven 'Maven3'
    }

    environment {
        SONAR_PROJECT_KEY = 'hello-java-project'
        NEXUS_URL = 'http://172.31.45.96:8081/repository/maven-snapshots-atcham/'
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
                echo 'Building Hemanth Java web application'
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

        stage('Deploy to Nexus') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-credentials',
                        usernameVariable: 'NEXUS_USERNAME',
                        passwordVariable: 'NEXUS_PASSWORD'
                    )
                ]) {
                    sh '''
                        set +x

                        trap 'rm -f settings-nexus.xml' EXIT

                        cat > settings-nexus.xml <<EOF
<settings>
    <servers>
        <server>
            <id>nexus-snapshots</id>
            <username>${NEXUS_USERNAME}</username>
            <password>${NEXUS_PASSWORD}</password>
        </server>
    </servers>
</settings>
EOF

                        chmod 600 settings-nexus.xml

                        mvn -s settings-nexus.xml deploy -DskipTests
                    '''
                }
            }
        }

        stage('Download from Nexus') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-credentials',
                        usernameVariable: 'NEXUS_USERNAME',
                        passwordVariable: 'NEXUS_PASSWORD'
                    )
                ]) {
                    sh '''
                        set -e
                        set +x

                        ARTIFACT_BASE="${NEXUS_URL}com/example/hello-java/1.0-SNAPSHOT"

                        echo "Reading snapshot metadata from Nexus..."

                        METADATA=$(curl -fsS \
                            --user "$NEXUS_USERNAME:$NEXUS_PASSWORD" \
                            "${ARTIFACT_BASE}/maven-metadata.xml")

                        SNAPSHOT_VERSION=$(printf '%s\\n' "$METADATA" | awk '
                            /<snapshotVersion>/ {
                                in_block=1
                                jar=0
                                value=""
                            }

                            in_block && /<extension>jar<\\/extension>/ {
                                jar=1
                            }

                            in_block && jar && /<value>/ {
                                line=$0
                                sub(/^.*<value>/, "", line)
                                sub(/<\\/value>.*$/, "", line)
                                value=line
                            }

                            /<\\/snapshotVersion>/ {
                                if (jar && value != "") print value
                                in_block=0
                            }
                        ' | tail -n 1)

                        if [ -z "$SNAPSHOT_VERSION" ]; then
                            echo "ERROR: Could not find JAR snapshot version"
                            exit 1
                        fi

                        echo "Snapshot version: $SNAPSHOT_VERSION"

                        mkdir -p target/deployment

                        curl -fSs \
                            --user "$NEXUS_USERNAME:$NEXUS_PASSWORD" \
                            "${ARTIFACT_BASE}/hello-java-${SNAPSHOT_VERSION}.jar" \
                            -o target/deployment/hello-java.jar

                        echo "Artifact downloaded successfully:"
                        ls -lh target/deployment/hello-java.jar
                    '''
                }
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

                        cat > target/deployment/Dockerfile <<'EOF'
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY hello-java.jar /app/hello-java.jar
EXPOSE 8080
CMD ["java", "-cp", "hello-java.jar", "com.example.HelloWorld"]
EOF

                        echo "Transferring JAR and Dockerfile..."

                        scp -i "$SSH_KEY" \
                            -o StrictHostKeyChecking=accept-new \
                            target/deployment/hello-java.jar \
                            target/deployment/Dockerfile \
                            "$SSH_USER@$DEPLOY_HOST:$DEPLOY_DIR/"

                        echo "Building and starting application..."

                        ssh -i "$SSH_KEY" \
                            -o StrictHostKeyChecking=accept-new \
                            "$SSH_USER@$DEPLOY_HOST" \
                            "set -e
                             cd '$DEPLOY_DIR'

                             sudo docker rm -f hello-java >/dev/null 2>&1 || true

                             sudo docker build -t hello-java:latest .

                             sudo docker run -d \
                                 --name hello-java \
                                 --restart unless-stopped \
                                 -p 8080:8080 \
                                 hello-java:latest

                             echo 'Waiting for application startup...'

                             for i in \$(seq 1 15)
                             do
                                 HTTP_CODE=\$(curl -s \
                                     -o /tmp/hello-response.txt \
                                     -w '%{http_code}' \
                                     http://127.0.0.1:8080/ || true)

                                 if [ \"\$HTTP_CODE\" = '200' ]; then
                                     echo 'Application response:'
                                     cat /tmp/hello-response.txt
                                     echo

                                     if grep -Fxq 'Hello from Hemanth Application!' /tmp/hello-response.txt; then
                                         echo 'HTTP verification successful!'
                                         exit 0
                                     fi

                                     echo 'Unexpected application response!'
                                     exit 1
                                 fi

                                 sleep 2
                             done

                             echo 'Application did not respond!'
                             sudo docker logs hello-java
                             exit 1
                            "

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


