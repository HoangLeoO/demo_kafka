// ============================================================
//  JENKINSFILE - Pipeline CI/CD cho Demo Kafka
// ============================================================
//
//  Pipeline gồm 5 bước:
//  1. Checkout  → lấy code từ Git
//  2. Build     → compile + đóng gói thành file .jar
//  3. Test      → chạy unit test
//  4. Docker    → build image & push lên Docker Hub (tuỳ chọn)
//  5. Deploy    → chạy ứng dụng trên server
//
// ============================================================

pipeline {

    // Chạy pipeline trên bất kỳ agent nào có sẵn
    agent any

    // ── Biến môi trường dùng chung trong pipeline ──────────────
    environment {
        APP_NAME    = 'demo-kafka'
        JAR_NAME    = 'demo_kafka-0.0.1-SNAPSHOT.jar'
        DEPLOY_PORT = '8081'

        // Nếu dùng Docker Hub: thay bằng username của bạn
        DOCKER_IMAGE = "your-dockerhub-username/${APP_NAME}"
        DOCKER_TAG   = "${BUILD_NUMBER}"   // Tag = số build (1, 2, 3,...)
    }

    // ── Tuỳ chọn pipeline ──────────────────────────────────────
    options {
        // Giữ lại tối đa 5 build gần nhất
        buildDiscarder(logRotator(numToKeepStr: '5'))
        // Timeout toàn bộ pipeline: 20 phút
        timeout(time: 20, unit: 'MINUTES')
    }

    stages {

        // ──────────────────────────────────────────────────────
        // STAGE 1: Lấy code từ Git
        // ──────────────────────────────────────────────────────
        stage('① Checkout') {
            steps {
                echo '📥 Đang lấy code từ Git...'
                // Jenkins tự động checkout code từ repo đã cấu hình
                checkout scm
            }
        }

        // ──────────────────────────────────────────────────────
        // STAGE 2: Build - Compile & đóng gói thành .jar
        // ──────────────────────────────────────────────────────
        stage('② Build') {
            steps {
                echo '🔨 Đang build dự án...'
                // Windows dùng gradlew.bat, Linux/Mac dùng ./gradlew
                bat 'gradlew.bat clean build -x test'
                // -x test: bỏ qua test ở bước này (test riêng ở stage sau)
            }
            post {
                success {
                    echo "✅ Build thành công! File jar: build/libs/${JAR_NAME}"
                    // Lưu file .jar như artifact của build này
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
                    // Hiển thị kết quả test trên Jenkins UI
                    junit 'build/test-results/test/*.xml'
                }
                failure {
                    echo '❌ Test thất bại! Dừng pipeline.'
                }
            }
        }

        // ──────────────────────────────────────────────────────
        // STAGE 4: Docker - Build image & Push lên Docker Hub
        // (Bỏ qua stage này nếu không dùng Docker)
        // ──────────────────────────────────────────────────────
        stage('④ Docker Build & Push') {
            steps {
                echo '🐳 Đang build Docker image...'
                // Dùng credentials đã lưu trong Jenkins (ID: dockerhub-credentials)
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    bat "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
                    bat "docker login -u %DOCKER_USER% -p %DOCKER_PASS%"
                    bat "docker push ${DOCKER_IMAGE}:${DOCKER_TAG}"
                    // Cũng tag là latest
                    bat "docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest"
                    bat "docker push ${DOCKER_IMAGE}:latest"
                }
            }
        }

        // ──────────────────────────────────────────────────────
        // STAGE 5: Deploy - Chạy ứng dụng
        // ──────────────────────────────────────────────────────
        stage('⑤ Deploy') {
            steps {
                echo '🚀 Đang deploy ứng dụng...'

                // Dừng process cũ đang chạy ở port 8081 (nếu có)
                bat """
                    FOR /F "tokens=5" %%a IN ('netstat -aon ^| findstr :${DEPLOY_PORT}') DO (
                        taskkill /F /PID %%a 2>nul
                    )
                """

                // Chạy file .jar mới ở background
                bat "start /B java -jar build/libs/${JAR_NAME} --server.port=${DEPLOY_PORT}"

                echo "✅ Deploy xong! Ứng dụng chạy tại: http://localhost:${DEPLOY_PORT}"
            }
        }
    }

    // ── Thông báo sau khi pipeline chạy xong ──────────────────
    post {
        success {
            echo """
            ============================================
            ✅ PIPELINE THÀNH CÔNG!
            App: ${APP_NAME}
            Build: #${BUILD_NUMBER}
            URL: http://localhost:${DEPLOY_PORT}/api/kafka/send?msg=Test
            ============================================
            """
        }
        failure {
            echo '❌ PIPELINE THẤT BẠI! Kiểm tra log bên trên.'
        }
        always {
            // Dọn dẹp workspace sau mỗi build
            cleanWs()
        }
    }
}
