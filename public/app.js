let parkingData = { spots: [], blockers: [] };
let isAdmin = false;
let adminPin = "";
let isRow2Mode = false;
let selectedSpots = [];
let deferredPrompt = null;

window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    deferredPrompt = e;
});

function installApp() {
    if (deferredPrompt) {
        deferredPrompt.prompt();
        deferredPrompt.userChoice.then(() => { deferredPrompt = null; });
    } else {
        alert('Нажмите на 3 точки ⋮ в браузере (или значок "Поделиться" на iPhone) и выберите «Добавить на главный экран»');
    }
}

async function loadData() {
    try {
        const res = await fetch('/api/parking');
        parkingData = await res.json();
        render();
    } catch(e) {
        console.error("Ошибка загрузки данных", e);
    }
}

function render() {
    const spots = parkingData.spots || [];
    const blockers = parkingData.blockers || [];
    const row1El = document.getElementById('row1');
    row1El.innerHTML = '';
    let occupied = 0;

    spots.forEach(spot => {
        if (spot.occupiedBy) occupied++;
        const div = document.createElement('div');
        const isSelected = selectedSpots.includes(spot.id);
        const isBlocked = blockers.some(b => b.blockedSpots && b.blockedSpots.includes(spot.id));

        div.className = `spot ${spot.occupiedBy ? 'busy' : ''} ${isSelected ? 'selected' : ''}`;
        if (isBlocked) div.style.border = '2px dashed #ff9800';

        const label = spot.occupiedBy ? (spot.occupiedBy.carNumber || 'Занято') : 'Свободно';
        div.innerHTML = `<b>${spot.id} ${isBlocked ? '⚠️' : ''}</b><small>${label}</small>`;

        div.onclick = () => {
            if (isRow2Mode) {
                if (selectedSpots.includes(spot.id)) {
                    selectedSpots = selectedSpots.filter(id => id !== spot.id);
                } else {
                    selectedSpots.push(spot.id);
                }
                render();
            } else {
                openSpotDetails(spot);
            }
        };
        row1El.appendChild(div);
    });

    document.getElementById('statOccupied').innerText = `${occupied} / ${spots.length}`;
    document.getElementById('statBlocked').innerText = blockers.length;

    const bList = document.getElementById('blockersList');
    if (blockers.length === 0) {
        bList.innerHTML = '<p style="color:#666;">Второй ряд свободен</p>';
    } else {
        bList.innerHTML = blockers.map(b => `
            <div class="block-card">
                <b>🚗 ${b.carNumber}</b> (до ~${b.leaveTime || '?'})<br>
                Подпёр места: <b>${(b.blockedSpots || []).join(', ')}</b><br>
                <a href="tel:${b.phone}" style="color:#4CAF50; font-weight:bold; text-decoration:none; display:inline-block; margin-top:5px;">📞 Позвонить (${b.phone})</a><br>
                <button onclick="leaveRow2(${b.id})" style="background:#333; border:1px solid #555; color:#ccc; padding:6px 10px; border-radius:4px; margin-top:6px; font-size:12px;">Освободить проезд</button>
            </div>
        `).join('');
    }
}

function openSpotDetails(spot) {
    const modal = document.getElementById('modalRow1');
    const title = document.getElementById('row1ModalTitle');
    const actions = document.getElementById('spotActions');
    title.innerText = `Место №${spot.id}`;

    if (spot.occupiedBy) {
        actions.innerHTML = `
            <p>Занято: <b>${spot.occupiedBy.carNumber}</b></p>
            <p>Кв: ${spot.occupiedBy.apt || '—'} | Тел: ${spot.occupiedBy.phone || '—'}</p>
            <button onclick="leaveRow1(${spot.id})" style="width:100%; padding:10px; background:#d32f2f; color:#fff; border:none; border-radius:6px; font-weight:bold;">
                ${isAdmin ? '★ Принудительно освободить' : 'Освободить место'}
            </button>
            <button onclick="closeModal()" style="width:100%; padding:10px; background:#444; color:#fff; border:none; border-radius:6px; margin-top:8px;">Закрыть</button>
        `;
    } else {
        actions.innerHTML = `
            <input type="text" id="r1Car" placeholder="Номер авто (А123АА)">
            <input type="text" id="r1Apt" placeholder="Номер квартиры">
            <input type="tel" id="r1Phone" placeholder="Телефон">
            <button onclick="occupyRow1(${spot.id})" style="width:100%; padding:10px; background:#2e7d32; color:#fff; border:none; border-radius:6px; font-weight:bold; margin-top:6px;">Занять место</button>
            <button onclick="closeModal()" style="width:100%; padding:10px; background:#444; color:#fff; border:none; border-radius:6px; margin-top:8px;">Закрыть</button>
        `;
    }
    modal.style.display = 'flex';
}

async function occupyRow1(spotId) {
    const carNumber = document.getElementById('r1Car').value.trim();
    if (!carNumber) return alert('Укажите номер машины!');
    await fetch('/api/park/row1', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({
            spotId,
            carNumber,
            apt: document.getElementById('r1Apt').value.trim(),
            phone: document.getElementById('r1Phone').value.trim()
        })
    });
    closeModal();
    loadData();
}

async function leaveRow1(spotId) {
    await fetch('/api/leave/row1', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ spotId })
    });
    closeModal();
    loadData();
}

function startRow2Mode() {
    isRow2Mode = true;
    selectedSpots = [];
    const car = prompt('Номер вашего авто (напр. А123АА):');
    if (!car) return;
    const phone = prompt('Ваш телефон для связи:');
    if (!phone) return;
    const time = prompt('Во сколько уедете (напр. 08:30 утра):') || 'Утром';
    const spotsStr = prompt('Какие места перекрыли через запятую (напр. 1, 2):');
    if (!spotsStr) return;

    const spotsArr = spotsStr.split(',').map(s => parseInt(s.trim())).filter(n => !isNaN(n));

    fetch('/api/park/row2', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ carNumber: car, phone, leaveTime: time, blockedSpots: spotsArr })
    }).then(() => loadData());
}

async function leaveRow2(blockerId) {
    await fetch('/api/leave/row2', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ blockerId })
    });
    loadData();
}

async function toggleAdmin() {
    if (isAdmin) {
        isAdmin = false;
        document.getElementById('roleBadge').innerText = 'Режим: Житель';
        document.getElementById('roleBadge').style.background = '#e91e63';
        document.getElementById('adminPanel').style.display = 'none';
        render();
        return;
    }
    const pin = prompt('Введите PIN-код:');
    if (!pin) return;
    const res = await fetch('/api/admin/login', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ pin })
    });
    const d = await res.json();
    if (d.success) {
        isAdmin = true;
        adminPin = pin;
        document.getElementById('roleBadge').innerText = '★ Админ';
        document.getElementById('roleBadge').style.background = '#4CAF50';
        document.getElementById('adminPanel').style.display = 'block';
        render();
    } else {
        alert('Неверный PIN!');
    }
}

async function addSpot() {
    await fetch('/api/admin/spot/add', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ pin: adminPin })
    });
    loadData();
}

async function removeSpot() {
    if (confirm('Удалить последнее место?')) {
        await fetch('/api/admin/spot/remove', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ pin: adminPin })
        });
        loadData();
    }
}

async function viewSurveyResults() {
    const res = await fetch('/api/admin/surveys');
    const list = await res.json();
    const c = document.getElementById('surveyAnswersList');
    if (!list || list.length === 0) {
        c.innerHTML = '<p>Пока нет ответов.</p>';
    } else {
        c.innerHTML = list.map(item => `
            <div style="border-bottom:1px solid #444; padding:6px 0;">
                <b style="color:#ffeb3b;">${item.answer}</b><br>
                ${item.comment ? `💬 <i>"${item.comment}"</i><br>` : ''}
                <small style="color:#777;">${item.date}</small>
            </div>
        `).join('');
    }
    document.getElementById('modalSurveys').style.display = 'flex';
}

async function submitSurvey() {
    const choice = document.querySelector('input[name="poll"]:checked');
    if (!choice) return alert('Выберите вариант ответа!');
    await fetch('/api/survey', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            answer: choice.value,
            comment: document.getElementById('surveyComment').value.trim()
        })
    });
    localStorage.setItem('parking_voted', '1');
    document.getElementById('surveyForm').style.display = 'none';
    document.getElementById('surveySuccess').style.display = 'block';
}

function closeModal() {
    document.getElementById('modalRow1').style.display = 'none';
}

if (localStorage.getItem('parking_voted')) {
    document.getElementById('surveyForm').style.display = 'none';
    document.getElementById('surveySuccess').style.display = 'block';
}

if ("serviceWorker" in navigator) {
    navigator.serviceWorker.register("/sw.js");
}

loadData();
setInterval(loadData, 3000);
