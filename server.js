const express = require('express');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 8100;
const DATA_FILE = path.join(__dirname, 'data.json');
const ADMIN_PIN = process.env.ADMIN_PIN || '2200';

app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// Начальная схема двора
function getInitialData() {
  const spots = [];
  // Ряд 1: 15 мест (1 - 15) вдоль дома Карбышева 22
  for (let i = 1; i <= 15; i++) {
    spots.push({
      id: i,
      x: 70 + (i - 1) * 58,
      y: 95,
      angle: -18,
      occupiedBy: null
    });
  }

  // Ряд 2: 16 мест (16 - 31) напротив
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
    { id: 'barrier', text: '🚧 ШЛАГБАУМ', x: 45, y: 195 },
    { id: 'bldg22', text: 'д. 22', x: 140, y: 410 },
    { id: 'shop', text: 'МАГАЗИН', x: 500, y: 410 },
    { id: 'bldg22a', text: 'д. 22А', x: 820, y: 410 }
  ];

  const surveys = [
    {
      id: 'barrier_survey',
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
    // ВАЖНО: проверяем только наличие массива spots, чтобы не сбрасывать схему при добавлении мест
    if (!data || !data.spots || !Array.isArray(data.spots)) {
      data = getInitialData();
      saveData(data);
    }
    if (!data.landmarks) data.landmarks = [];
    if (!data.blockers) data.blockers = [];
    if (!data.surveys) data.surveys = [];
    return data;
  } catch (e) {
    const init = getInitialData();
    saveData(init);
    return init;
  }
}

function saveData(data) {
  fs.writeFileSync(DATA_FILE, JSON.stringify(data, null, 2), 'utf8');
}

// 1. Получить состояние парковки
app.get('/api/data', (req, res) => {
  res.json(getData());
});

// 2. Занять место в 1-м ряду (с указанием времени отъезда leaveTime)
app.post('/api/park', (req, res) => {
  let { spotId, carNumber, phone, apt, leaveTime } = req.body;
  if (!spotId || !carNumber) {
    return res.status(400).json({ error: 'Номер места и авто обязательны' });
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

  // Проверка прав (админ по PIN или житель по последним 4 цифрам телефона)
  if (pin && pin.trim() === ADMIN_PIN) {
    spot.occupiedBy = null;
    saveData(data);
    return res.json({ success: true, message: 'Освобождено администратором' });
  }

  const phone = spot.occupiedBy.phone.replace(/\D/g, '');
  const digits = (lastDigits || '').trim();
  if (!digits || !phone.endsWith(digits)) {
    return res.status(403).json({ error: 'Неверные последние 4 цифры номера телефона' });
  }

  spot.occupiedBy = null;
  saveData(data);
  res.json({ success: true });
});

// 4. Встать во 2-й ряд (подпереть)
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

  const blocker = data.blockers[index];

  if (pin && pin.trim() === ADMIN_PIN) {
    data.blockers.splice(index, 1);
    saveData(data);
    return res.json({ success: true, message: 'Убрано администратором' });
  }

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
  if (pin && pin.trim() === ADMIN_PIN) {
    res.json({ success: true });
  } else {
    res.status(401).json({ error: 'Неверный PIN-код' });
  }
});

// 7. Сохранение схемы двора (места, ряды, углы, надписи)
app.post('/api/admin/map', (req, res) => {
  const { pin, landmarks, spots } = req.body;
  if (!pin || pin.trim() !== ADMIN_PIN) {
    return res.status(401).json({ error: 'Неверный PIN администратора' });
  }

  const data = getData();

  if (Array.isArray(spots)) {
    // Сохраняем статус занятости существующих мест, обновляя координаты и углы
    const oldSpotMap = new Map();
    data.spots.forEach(s => oldSpotMap.set(s.id, s.occupiedBy));

    data.spots = spots.map(s => ({
      id: Number(s.id),
      x: Number(s.x),
      y: Number(s.y),
      angle: Number(s.angle || 0),
      occupiedBy: oldSpotMap.has(Number(s.id)) ? oldSpotMap.get(Number(s.id)) : (s.occupiedBy || null)
    }));
  }

  if (Array.isArray(landmarks)) {
    data.landmarks = landmarks.map(lm => ({
      id: String(lm.id || ('lm_' + Date.now())),
      text: String(lm.text || '').trim(),
      x: Number(lm.x),
      y: Number(lm.y)
    }));
  }

  saveData(data);
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

app.listen(PORT, () => {
  console.log(`Dvor-Parking server running on port ${PORT}`);
});
