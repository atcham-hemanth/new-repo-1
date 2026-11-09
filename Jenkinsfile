
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
                echo 'Building Java application'
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

                        # Use a fresh local Maven repository
                        # so we retrieve the artifact from Nexus.
                        rm -rf "$WORKSPACE/.m2-nexus-download"

                        mvn -U \
                            -s settings-nexus.xml \
                            -Dmaven.repo.local="$WORKSPACE/.m2-nexus-download" \
                            org.apache.maven.plugins:maven-dependency-plugin:3.6.1:get \
                            -Dartifact=com.example:hello-java:1.0-SNAPSHOT \
                            -Dtransitive=false \
                            -DremoteRepositories=nexus-snapshots::default::${NEXUS_URL}

                        mkdir -p target/deployment

                        cp "$WORKSPACE/.m2-nexus-download/com/example/hello-java/1.0-SNAPSHOT/hello-java-1.0-SNAPSHOT.jar" \
                           target/deployment/hello-java.jar

                        echo "Artifact downloaded from Nexus:"
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
CMD ["java", "-cp", "hello-java.jar", "com.example.HelloWorld"]
EOF

                        echo "Transferring JAR and Dockerfile..."

                        scp -i "$SSH_KEY" \
                            -o StrictHostKeyChecking=accept-new \
                            target/deployment/hello-java.jar \
                            target/deployment/Dockerfile \
                            "$SSH_USER@$DEPLOY_HOST:$DEPLOY_DIR/"

                        echo "Building and running Docker container..."

                        ssh -i "$SSH_KEY" \
                            -o StrictHostKeyChecking=accept-new \
                            "$SSH_USER@$DEPLOY_HOST" \
                            "set -e
                             cd '$DEPLOY_DIR'

                             sudo docker rm -f hello-java >/dev/null 2>&1 || true

                             sudo docker build -t hello-java:latest .

                             sudo docker run -d --name hello-java hello-java:latest

                             exit_code=\$(sudo docker wait hello-java)

                             sudo docker logs hello-java

                             echo 'Application exit code:' \$exit_code

                             test \"\$exit_code\" -eq 0
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

