const express = require('express');
const fs = require('fs');
const app = express();
const PORT = process.env.PORT || 7860;

app.use(express.json());
app.use(express.static('public'));

const DB_FILE = './database.json';
const ADMIN_PIN = "3418";

function getData() { 
    let data = { spots: [], blockers: [], surveys: [] };
    if (fs.existsSync(DB_FILE)) {
        try { data = JSON.parse(fs.readFileSync(DB_FILE, 'utf-8')); } catch(e){}
    }
    // Если мест нет — создаем 8 мест по умолчанию
    if (!data.spots || data.spots.length === 0) {
        data.spots = Array.from({ length: 8 }, (_, i) => ({ id: i + 1, occupiedBy: null }));
    }
    if (!data.blockers) data.blockers = [];
    if (!data.surveys) data.surveys = [];
    return data;
}

function saveData(data) { 
    fs.writeFileSync(DB_FILE, JSON.stringify(data, null, 2)); 
}

// Инициализация при старте
saveData(getData());

app.get('/api/parking', (req, res) => res.json(getData()));

app.post('/api/admin/login', (req, res) => {
    const { pin } = req.body;
    res.json({ success: pin === ADMIN_PIN });
});

app.post('/api/admin/spot/add', (req, res) => {
    const { pin } = req.body;
    if (pin !== ADMIN_PIN) return res.status(403).json({ error: 'Запрещено' });
    const data = getData();
    const newId = data.spots.length > 0 ? Math.max(...data.spots.map(s => s.id)) + 1 : 1;
    data.spots.push({ id: newId, occupiedBy: null });
    saveData(data);
    res.json({ success: true, newId });
});

app.post('/api/admin/spot/remove', (req, res) => {
    const { pin } = req.body;
    if (pin !== ADMIN_PIN) return res.status(403).json({ error: 'Запрещено' });
    const data = getData();
    if (data.spots.length > 0) {
        data.spots.pop();
        saveData(data);
    }
    res.json({ success: true });
});

app.post('/api/park/row1', (req, res) => {
    const { spotId, carNumber, phone, apt } = req.body;
    const data = getData();
    const spot = data.spots.find(s => s.id === spotId);
    if (spot) {
        spot.occupiedBy = { carNumber, phone, apt, parkedAt: new Date().toISOString() };
        saveData(data);
        res.json({ success: true });
    } else {
        res.status(404).json({ error: 'Место не найдено' });
    }
});

app.post('/api/leave/row1', (req, res) => {
    const { spotId } = req.body;
    const data = getData();
    const spot = data.spots.find(s => s.id === spotId);
    if (spot) {
        spot.occupiedBy = null;
        saveData(data);
        res.json({ success: true });
    } else {
        res.status(404).json({ error: 'Место не найдено' });
    }
});

app.post('/api/park/row2', (req, res) => {
    const { carNumber, phone, apt, blockedSpots, leaveTime } = req.body;
    const data = getData();
    data.blockers.push({
        id: Date.now(),
        carNumber,
        phone,
        apt,
        blockedSpots: Array.isArray(blockedSpots) ? blockedSpots : [blockedSpots],
        leaveTime
    });
    saveData(data);
    res.json({ success: true });
});

app.post('/api/leave/row2', (req, res) => {
    const { blockerId } = req.body;
    const data = getData();
    data.blockers = data.blockers.filter(b => b.id !== blockerId);
    saveData(data);
    res.json({ success: true });
});

app.post('/api/survey', (req, res) => {
    const { answer, comment } = req.body;
    const data = getData();
    data.surveys.push({
        id: Date.now(),
        answer,
        comment: comment || '',
        date: new Date().toLocaleString('ru-RU', { timeZone: 'Europe/Moscow' })
    });
    saveData(data);
    res.json({ success: true });
});

app.get('/api/admin/surveys', (req, res) => {
    const data = getData();
    res.json(data.surveys || []);
});

app.listen(PORT, '0.0.0.0', '0.0.0.0', () => {
    console.log(`Сервер готов на порту ${PORT}`);
});
