// ============================================================
//  JENKINSFILE - Pipeline CI/CD cho Demo Kafka
// ============================================================
//
//  Pipeline gồm 4 bước:
//  1. Checkout → lấy code từ Git
//  2. Build    → compile + đóng gói thành file .jar
//  3. Test     → chạy unit test
//  4. Deploy   → chạy ứng dụng trên server
//
//  Stage Docker bị TẮT (khi cần thì bật lại - xem bên dưới)
// ============================================================

pipeline {

    agent any

    environment {
        APP_NAME    = 'demo-kafka'
        JAR_NAME    = 'demo_kafka-0.0.1-SNAPSHOT.jar'
        DEPLOY_PORT = '8081'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
        timeout(time: 20, unit: 'MINUTES')
    }

    stages {

        // ──────────────────────────────────────────────────────
        // STAGE 1: Lấy code từ Git
        // ──────────────────────────────────────────────────────
        stage('① Checkout') {
            steps {
                echo '📥 Đang lấy code từ Git...'
                checkout scm
            }
        }

        // ──────────────────────────────────────────────────────
        // STAGE 2: Build - Compile & đóng gói thành .jar
        // ──────────────────────────────────────────────────────
        stage('② Build') {
            steps {
                echo '🔨 Đang build dự án...'
                bat 'gradlew.bat clean build -x test'
            }
            post {
                success {
                    echo "✅ Build thành công! File jar: build/libs/${JAR_NAME}"
                    archiveArtifacts artifacts: "build/libs/${JAR_NAME}"
                }
            }
        }

        // ──────────────────────────────────────────────────────
        // STAGE 3: Test - Chạy unit test
        // ──────────────────────────────────────────────────────
        stage('③ Test') {
            steps {
                echo '🧪 Đang chạy unit test...'
                bat 'gradlew.bat test'
            }
            post {
                always {
                    junit 'build/test-results/test/*.xml'
                }
                failure {
                    echo '❌ Test thất bại! Dừng pipeline.'
                }
            }
        }

        // ──────────────────────────────────────────────────────
        // STAGE 4 (TẮT): Docker Build & Push
        //
        // ĐỂ BẬT LẠI:
        //   Bước 1 - Thêm credentials vào Jenkins:
        //     Manage Jenkins → Credentials → Global → Add Credentials
        //     Kind     : Username with password
        //     Username : <docker hub username>
        //     Password : <docker hub password>
        //     ID       : dockerhub-credentials
        //
        //   Bước 2 - Đổi dòng bên dưới:
        //     expression { false }  →  expression { true }
        // ──────────────────────────────────────────────────────
        stage('④ Docker Build & Push') {
            when {
                expression { true }    // ← đang BẬT
            }
            steps {
                echo '🐳 Đang build Docker image...'
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    bat "docker build -t your-dockerhub-username/demo-kafka:${BUILD_NUMBER} ."
                    bat "docker login -u %DOCKER_USER% -p %DOCKER_PASS%"
                    bat "docker push your-dockerhub-username/demo-kafka:${BUILD_NUMBER}"
                }
            }
        }

        // ──────────────────────────────────────────────────────
        // STAGE 5: Deploy - Chạy file .jar trên server
        // ──────────────────────────────────────────────────────
        stage('⑤ Deploy') {
            steps {
                echo '🚀 Đang deploy ứng dụng...'

                // Dừng process cũ ở port 8081 (nếu có)
                bat """
                    FOR /F "tokens=5" %%a IN ('netstat -aon ^| findstr :${DEPLOY_PORT}') DO (
                        taskkill /F /PID %%a 2>nul
                    )
                """

                // Chạy .jar mới ở background
                bat "start /B java -jar build/libs/${JAR_NAME} --server.port=${DEPLOY_PORT}"

                echo "✅ Deploy xong! Truy cập: http://localhost:${DEPLOY_PORT}/api/kafka/send?msg=Test"
            }
        }
    }

    post {
        success {
            echo """
            ============================================
            ✅ PIPELINE THÀNH CÔNG!
            Build : #${BUILD_NUMBER}
            URL   : http://localhost:${DEPLOY_PORT}/api/kafka/send?msg=Test
            ============================================
            """
        }
        failure {
            echo '❌ PIPELINE THẤT BẠI! Kiểm tra log bên trên.'
        }
        always {
            cleanWs()
        }
    }
}
