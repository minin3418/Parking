const express = require('express');
const fs = require('fs');
const app = express();

const PORT = process.env.PORT || 8100;
const IP = process.env.IP || '::';
const DB_FILE = './database.json';
const ADMIN_PIN = process.env.ADMIN_PIN || "3418";

app.use(express.json());
app.use(express.static('public'));

function getData() { 
    let data = { spots: [], blockers: [], surveys: [] };
    if (fs.existsSync(DB_FILE)) {
        try { data = JSON.parse(fs.readFileSync(DB_FILE, 'utf-8')); } catch(e){}
    }
    // 31 реальное место двора
    if (!data.spots || data.spots.length !== 31) {
        data.spots = Array.from({ length: 31 }, (_, i) => ({ id: i + 1, occupiedBy: null }));
    }
    if (!data.blockers) data.blockers = [];
    if (!data.surveys) data.surveys = [];
    return data;
}

function saveData(data) { fs.writeFileSync(DB_FILE, JSON.stringify(data, null, 2)); }
saveData(getData());

app.get('/api/parking', (req, res) => res.json(getData()));

app.post('/api/admin/login', (req, res) => {
    res.json({ success: req.body.pin === ADMIN_PIN });
});

app.post('/api/park/row1', (req, res) => {
    let { spotId, carNumber, phone, apt } = req.body;
    if (!carNumber) return res.status(400).json({ error: 'Номер обязателен' });
    const data = getData();
    const spot = data.spots.find(s => s.id === spotId);
    if (spot) {
        spot.occupiedBy = {
            carNumber: carNumber.toUpperCase().trim(),
            phone: (phone || '').trim(),
            apt: (apt || '').trim(),
            parkedAt: new Date().toISOString()
        };
        saveData(data);
        res.json({ success: true });
    } else {
        res.status(404).json({ error: 'Место не найдено' });
    }
});

app.post('/api/leave/row1', (req, res) => {
    const { spotId, phoneDigits, pin } = req.body;
    const data = getData();
    const spot = data.spots.find(s => s.id === spotId);
    if (!spot || !spot.occupiedBy) return res.status(404).json({ error: 'Место свободно' });

    if (pin === ADMIN_PIN) {
        spot.occupiedBy = null;
        saveData(data);
        return res.json({ success: true });
    }

    const cleanStored = (spot.occupiedBy.phone || '').replace(/\D/g, '');
    const cleanEntered = (phoneDigits || '').replace(/\D/g, '');
    if (cleanStored.length >= 4 && !cleanStored.endsWith(cleanEntered)) {
        return res.status(403).json({ error: 'Неверные 4 цифры телефона!' });
    }

    spot.occupiedBy = null;
    saveData(data);
    res.json({ success: true });
});

app.post('/api/park/row2', (req, res) => {
    let { carNumber, phone, leaveTime, blockedSpots } = req.body;
    if (!carNumber || !phone) return res.status(400).json({ error: 'Номер и телефон обязательны' });
    const data = getData();
    data.blockers.push({
        id: Date.now(),
        carNumber: carNumber.toUpperCase().trim(),
        phone: phone.trim(),
        leaveTime: (leaveTime || 'Утром').trim(),
        blockedSpots: Array.isArray(blockedSpots) ? blockedSpots : [blockedSpots]
    });
    saveData(data);
    res.json({ success: true });
});

app.post('/api/leave/row2', (req, res) => {
    const { blockerId, phoneDigits, pin } = req.body;
    const data = getData();
    const blocker = data.blockers.find(b => b.id === blockerId);
    if (!blocker) return res.json({ success: true });

    if (pin === ADMIN_PIN) {
        data.blockers = data.blockers.filter(b => b.id !== blockerId);
        saveData(data);
        return res.json({ success: true });
    }

    const cleanStored = (blocker.phone || '').replace(/\D/g, '');
    const cleanEntered = (phoneDigits || '').replace(/\D/g, '');
    if (cleanStored.length >= 4 && !cleanStored.endsWith(cleanEntered)) {
        return res.status(403).json({ error: 'Неверные 4 цифры телефона!' });
    }

    data.blockers = data.blockers.filter(b => b.id !== blockerId);
    saveData(data);
    res.json({ success: true });
});

app.post('/api/survey', (req, res) => {
    const data = getData();
    data.surveys.push({
        id: Date.now(),
        answer: req.body.answer,
        comment: (req.body.comment || '').trim(),
        date: new Date().toLocaleString('ru-RU', { timeZone: 'Europe/Moscow' })
    });
    saveData(data);
    res.json({ success: true });
});

app.post('/api/admin/surveys', (req, res) => {
    if (req.body.pin !== ADMIN_PIN) return res.status(403).json({ error: 'Запрещено' });
    const data = getData();
    res.json(data.surveys || []);
});

app.listen(PORT, IP, () => {
    console.log(`Сервер слушает на [${IP}]:${PORT}`);
});
