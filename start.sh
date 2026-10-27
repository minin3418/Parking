#!/bin/bash
killall node 2>/dev/null
killall ssh 2>/dev/null
termux-wake-lock

echo "1. Запуск сервера..."
node server.js &
sleep 2

echo "2. Проверка сервера..."
curl -s http://localhost:3000/api/parking > /dev/null && echo "-> Сервер работает отлично!" || echo "-> Ошибка запуска сервера"

echo "3. Запуск туннеля для соседей..."
echo "--------------------------------------------------"
ssh -o ServerAliveInterval=15 -R 80:127.0.0.1:3000 nokey@localhost.run
