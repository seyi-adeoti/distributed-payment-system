#!/bin/bash

echo "🚀 Starting Distributed Payment Platform Infrastructure..."
docker-compose up -d

echo "⏳ Waiting 20 seconds for Databases, Zookeeper, and Kafka to initialize..."
sleep 20

echo "🔨 Compiling all microservices..."
# Using the local Maven installation found in .m2 wrapper since mvnw was deleted from the root
MVN_CMD="/Users/macbook/.m2/wrapper/dists/apache-maven-3.9.15/9925cc1d/bin/mvn"
$MVN_CMD clean install -DskipTests

mkdir -p logs

echo "🌟 Starting Eureka Server..."
nohup java -jar eureka-server/target/eureka-server-0.0.1-SNAPSHOT.jar > logs/eureka-server.log 2>&1 &
EUREKA_PID=$!
echo $EUREKA_PID > .eureka.pid
echo "⏳ Waiting 15 seconds for Eureka to start..."
sleep 15

echo "🌟 Starting API Gateway..."
nohup java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar > logs/api-gateway.log 2>&1 &
echo $! > .gateway.pid

echo "🌟 Starting Core Services..."
nohup java -jar user-service/target/user-service-0.0.1-SNAPSHOT.jar > logs/user-service.log 2>&1 &
echo $! > .user.pid

nohup java -jar wallet-service/target/wallet-service-0.0.1-SNAPSHOT.jar > logs/wallet-service.log 2>&1 &
echo $! > .wallet.pid

nohup java -jar payment-service/target/payment-service-0.0.1-SNAPSHOT.jar > logs/payment-service.log 2>&1 &
echo $! > .payment.pid

nohup java -jar ledger-service/target/ledger-service-0.0.1-SNAPSHOT.jar > logs/ledger-service.log 2>&1 &
echo $! > .ledger.pid

nohup java -jar notification-service/target/notification-service-0.0.1-SNAPSHOT.jar > logs/notification-service.log 2>&1 &
echo $! > .notification.pid

echo "✅ All services started in the background!"
echo "📄 Logs are available in the 'logs/' directory (e.g., 'tail -f logs/api-gateway.log')."
echo "🛑 To stop all services, run: kill \$(cat .*.pid) && rm .*.pid"
