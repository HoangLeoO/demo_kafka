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
        // STAGE 4: Docker Build & Run LOCAL
        // Không cần credentials, không push lên Docker Hub.
        // Build image xong → dừng container cũ → chạy container mới.
        // ──────────────────────────────────────────────────────
        stage('④ Docker Build & Run') {
            steps {
                echo '🐳 Đang build Docker image (local)...'

                // Build image từ Dockerfile trong repo
                // Tag: demo-kafka:1, demo-kafka:2,... theo số build
                bat "docker build -t demo-kafka:${BUILD_NUMBER} ."
                // Cũng tag là latest để luôn có bản mới nhất
                bat "docker tag demo-kafka:${BUILD_NUMBER} demo-kafka:latest"

                echo '🛑 Dừng container cũ (nếu đang chạy)...'
                // Dừng và xóa container cũ - dùng "|| exit 0" để không báo lỗi nếu chưa có
                bat "docker stop demo-kafka-app 2>nul || exit 0"
                bat "docker rm   demo-kafka-app 2>nul || exit 0"

                echo '🚀 Khởi động container mới...'
                // Giải thích network:
                //   Container Spring Boot cần nói chuyện với container Kafka.
                //   Kafka đang chạy trong network "demo_kafka_default" (do docker-compose tạo).
                //   → Cho Spring Boot join vào CÙNG network → dùng hostname "kafka:9092" thay vì localhost
                //
                //   -p 8081:8081              → vẫn map port ra máy thật để test bằng browser
                //   --network demo_kafka_default → join cùng network với Kafka
                //   -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092 → kết nối đến Kafka bằng port nội bộ
                bat "docker run -d --name demo-kafka-app -p ${DEPLOY_PORT}:${DEPLOY_PORT} --network demo_kafka_default -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092 demo-kafka:latest"

                echo "✅ Container đang chạy! Truy cập: http://localhost:${DEPLOY_PORT}/api/kafka/send?msg=Test"
            }
            post {
                failure {
                    echo '❌ Docker build/run thất bại! Kiểm tra Docker Engine có đang chạy không.'
                }
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
