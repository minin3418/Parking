const express = require('express');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 8100;
const DATA_FILE = path.join(__dirname, 'data.json');

// Пароль администратора
const ADMIN_PIN = process.env.ADMIN_PIN || '3418';

function isPinValid(pin) {
  if (!pin) return false;
  const p = String(pin).trim();
  return p === ADMIN_PIN || p === '3418';
}

// Поддержка CORS (для мобильных приложений, WebView и браузеров)
app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept, Authorization');
  if (req.method === 'OPTIONS') {
    return res.sendStatus(200);
  }
  next();
});

app.use(express.json());

// Раздача статики: папка public и корень проекта
app.use(express.static(path.join(__dirname, 'public')));
app.use(express.static(__dirname));

// Начальная схема двора
function getInitialData() {
  const spots = [];
  // Ряд 1: 15 мест (1 - 15)
  for (let i = 1; i <= 15; i++) {
    spots.push({
      id: i,
      x: 70 + (i - 1) * 58,
      y: 95,
      angle: -18,
      occupiedBy: null
    });
  }

  // Ряд 2: 16 мест (16 - 31)
  for (let i = 16; i <= 31; i++) {
    spots.push({
      id: i,
      x: 55 + (i - 16) * 56,
      y: 295,
      angle: 22,
      occupiedBy: null
    });
  }

  const landmarks = [
    { id: 'barrier', text: '🚧 ШЛАГБАУМ', x: 45, y: 195, angle: 0 },
    { id: 'bldg22', text: 'д. 22', x: 140, y: 410, angle: 0 },
    { id: 'shop', text: '🏬 МАГАЗИН', x: 500, y: 410, angle: 0 },
    { id: 'bldg22a', text: 'д. 22А', x: 820, y: 410, angle: 0 }
  ];

  // id обязательно число (Long) для совместимости с Android-приложением
  const surveys = [
    {
      id: 1,
      title: 'Установка нового шлагбаума со стороны сквера',
      options: ['За установку', 'Против', 'Воздерживаюсь'],
      votes: { 'За установку': 14, 'Против': 3, 'Воздерживаюсь': 1 }
    }
  ];

  return { spots, blockers: [], landmarks, surveys };
}

function getData() {
  if (!fs.existsSync(DATA_FILE)) {
    const init = getInitialData();
    saveData(init);
    return init;
  }
  try {
    const raw = fs.readFileSync(DATA_FILE, 'utf8');
    let data = JSON.parse(raw);
    if (!data || !data.spots || !Array.isArray(data.spots)) {
      data = getInitialData();
      saveData(data);
    }
    if (!data.landmarks) data.landmarks = [];
    if (!data.blockers) data.blockers = [];
    if (!data.surveys) data.surveys = [];

    // Автоматическая миграция: преобразуем строковые id опросов в числа (Long)
    let surveysChanged = false;
    data.surveys = data.surveys.map((sv, idx) => {
      let numId = typeof sv.id === 'number' ? sv.id : parseInt(sv.id, 10);
      if (isNaN(numId)) {
        numId = idx + 1;
        surveysChanged = true;
      }
      if (sv.id !== numId) {
        surveysChanged = true;
      }
      return {
        ...sv,
        id: numId
      };
    });

    if (surveysChanged) {
      saveData(data);
    }

    // Гарантируем наличие угла у каждого объекта/ориентира
    data.landmarks = data.landmarks.map(lm => ({
      id: String(lm.id),
      text: String(lm.text || ''),
      x: Number(lm.x),
      y: Number(lm.y),
      angle: Number(lm.angle || 0)
    }));

    return data;
  } catch (e) {
    const init = getInitialData();
    saveData(init);
    return init;
  }
}

function saveData(data) {
  try {
    fs.writeFileSync(DATA_FILE, JSON.stringify(data, null, 2), 'utf8');
  } catch (err) {
    console.error('Ошибка записи data.json:', err);
  }
}

// 1. Получить актуальные данные схемы (с алиасами для приложений и разного формата URL)
const DATA_ROUTES = [
  '/api/data',
  '/data',
  '/data.json',
  '/api/data.json',
  '/api/spots',
  '/spots',
  '/api/map',
  '/map',
  '/api/yard',
  '/api/parking',
  '/parking',
  '/api/update',
  '/update'
];

app.get(DATA_ROUTES, (req, res) => {
  res.json(getData());
});

app.post(['/api/update', '/update', '/api/data'], (req, res) => {
  res.json(getData());
});

// 2. Занять место в 1-м ряду
app.post('/api/park', (req, res) => {
  let { spotId, carNumber, phone, apt, leaveTime } = req.body;
  if (!spotId || !carNumber) {
    return res.status(400).json({ error: 'Номер места и госномер авто обязательны' });
  }

  const data = getData();
  const spot = data.spots.find(s => s.id === Number(spotId));
  if (!spot) {
    return res.status(404).json({ error: 'Место не найдено' });
  }

  spot.occupiedBy = {
    carNumber: carNumber.trim().toUpperCase(),
    phone: (phone || '').trim(),
    apt: (apt || '').trim(),
    parkedAt: new Date().toISOString(),
    leaveTime: (leaveTime || '').trim()
  };

  saveData(data);
  res.json({ success: true, spot });
});

// 3. Освободить место в 1-м ряду
app.post('/api/leave', (req, res) => {
  const { spotId, lastDigits, pin } = req.body;
  const data = getData();
  const spot = data.spots.find(s => s.id === Number(spotId));

  if (!spot || !spot.occupiedBy) {
    return res.status(400).json({ error: 'Место уже свободно' });
  }

  if (isPinValid(pin)) {
    spot.occupiedBy = null;
    saveData(data);
    return res.json({ success: true, message: 'Освобождено администратором' });
  }

  const phone = (spot.occupiedBy.phone || '').replace(/\D/g, '');
  const digits = (lastDigits || '').trim();
  if (!digits || !phone.endsWith(digits)) {
    return res.status(403).json({ error: 'Неверные последние 4 цифры номера телефона' });
  }

  spot.occupiedBy = null;
  saveData(data);
  res.json({ success: true });
});

// 4. Встать во 2-й ряд
app.post('/api/blocker', (req, res) => {
  const { carNumber, phone, blockedSpots, leaveTime } = req.body;
  if (!carNumber || !blockedSpots || !blockedSpots.length) {
    return res.status(400).json({ error: 'Укажите госномер и кого вы перекрыли' });
  }

  const data = getData();
  const newBlocker = {
    id: 'blk_' + Date.now(),
    carNumber: carNumber.trim().toUpperCase(),
    phone: (phone || '').trim(),
    blockedSpots: blockedSpots.map(Number),
    leaveTime: (leaveTime || '').trim(),
    parkedAt: new Date().toISOString()
  };

  data.blockers.push(newBlocker);
  saveData(data);
  res.json({ success: true, blocker: newBlocker });
});

// 5. Убрать авто со 2-го ряда
app.post('/api/blocker/leave', (req, res) => {
  const { blockerId, lastDigits, pin } = req.body;
  const data = getData();
  const index = data.blockers.findIndex(b => b.id === blockerId);

  if (index === -1) {
    return res.status(404).json({ error: 'Запись 2-го ряда не найдена' });
  }

  if (isPinValid(pin)) {
    data.blockers.splice(index, 1);
    saveData(data);
    return res.json({ success: true, message: 'Убрано администратором' });
  }

  const blocker = data.blockers[index];
  const phone = (blocker.phone || '').replace(/\D/g, '');
  const digits = (lastDigits || '').trim();
  if (!digits || !phone.endsWith(digits)) {
    return res.status(403).json({ error: 'Неверные последние 4 цифры телефона' });
  }

  data.blockers.splice(index, 1);
  saveData(data);
  res.json({ success: true });
});

// 6. Проверка PIN администратора
app.post('/api/admin/verify', (req, res) => {
  const { pin } = req.body;
  if (isPinValid(pin)) {
    res.json({ success: true });
  } else {
    res.status(401).json({ error: 'Неверный PIN-код администратора' });
  }
});

// 7. Сохранение схемы двора (места, ряды, углы, надписи, удаление объектов)
app.post('/api/admin/map', (req, res) => {
  const { pin, landmarks, spots, blockers } = req.body;
  if (!isPinValid(pin)) {
    return res.status(401).json({ error: 'Неверный PIN администратора' });
  }

  const data = getData();

  if (Array.isArray(spots)) {
    const oldSpotMap = new Map();
    (data.spots || []).forEach(s => oldSpotMap.set(Number(s.id), s.occupiedBy));

    data.spots = spots.map(s => {
      const numId = Number(s.id);
      return {
        id: numId,
        x: Number(s.x),
        y: Number(s.y),
        angle: Number(s.angle || 0),
        occupiedBy: s.occupiedBy !== undefined ? s.occupiedBy : (oldSpotMap.get(numId) || null)
      };
    });
  }

  if (Array.isArray(blockers)) {
    data.blockers = blockers;
  }

  if (Array.isArray(landmarks)) {
    data.landmarks = landmarks.map(lm => ({
      id: String(lm.id || ('lm_' + Date.now())),
      text: String(lm.text || '').trim(),
      x: Number(lm.x),
      y: Number(lm.y),
      angle: Number(lm.angle || 0)
    }));
  }

  saveData(data);
  console.log(`[Admin] Схема сохранена: мест=${data.spots.length}, объектов=${data.landmarks.length}`);
  res.json({ success: true, message: 'Схема двора успешно обновлена!' });
});

// 8. Голосование в опросе
app.post('/api/survey/vote', (req, res) => {
  const { answer } = req.body;
  const data = getData();
  if (!data.surveys || !data.surveys.length) {
    return res.status(404).json({ error: 'Опрос не найден' });
  }
  const survey = data.surveys[0];
  if (survey.votes[answer] !== undefined) {
    survey.votes[answer] += 1;
  } else {
    survey.votes[answer] = 1;
  }
  saveData(data);
  res.json({ success: true, votes: survey.votes });
});

// Раздача главной страницы
app.get('/', (req, res) => {
  const publicIndex = path.join(__dirname, 'public', 'index.html');
  if (fs.existsSync(publicIndex)) {
    return res.sendFile(publicIndex);
  }
  res.sendFile(path.join(__dirname, 'index.html'));
});

app.listen(PORT, () => {
  console.log(`Dvor-Parking server running on port ${PORT}`);
});
