let parkingData = { spots: [], blockers: [] };
let isAdmin = false;
let adminPin = "";
let r2Selected = [];

const YARD_SPOTS = [
    // ВЕРХНИЙ РЯД У ПЛОЩАДКИ (1-14)
    ...Array.from({ length: 14 }, (_, i) => ({
        id: i + 1,
        x: 90 + i * 58,
        y: 85,
        w: 42,
        h: 68,
        angle: -18,
        area: 'Верхний ряд (у площадки)'
    })),
    // ЛЕВЫЙ КАРМАН СНИЗУ (15-20)
    ...Array.from({ length: 6 }, (_, i) => ({
        id: i + 15,
        x: 75 + i * 44,
        y: 285,
        w: 38,
        h: 70,
        angle: 0,
        area: 'Левый карман (у 1 подъезда)'
    })),
    // ЦЕНТРАЛЬНЫЙ КАРМАН СНИЗУ (21-24)
    ...Array.from({ length: 4 }, (_, i) => ({
        id: i + 21,
        x: 355 + i * 44,
        y: 285,
        w: 38,
        h: 70,
        angle: 0,
        area: 'Центральный карман'
    })),
    // ПРАВЫЙ КАРМАН У ПОВОРОТА (25-31)
    ...Array.from({ length: 7 }, (_, i) => ({
        id: i + 25,
        x: 555 + i * 54,
        y: 295 - i * 9,
        w: 40,
        h: 70,
        angle: 22,
        area: 'Карман у поворота'
    }))
];

async function loadData() {
    try {
        const res = await fetch('/api/parking');
        parkingData = await res.json();
        render();
    } catch(e) {
        console.error("Ошибка загрузки", e);
    }
}

function render() {
    const spots = parkingData.spots || [];
    const blockers = parkingData.blockers || [];
    const slotsLayer = document.getElementById('slotsLayer');
    const blockersLayer = document.getElementById('blockersLayer');
    slotsLayer.innerHTML = '';
    blockersLayer.innerHTML = '';

    let occupied = 0;

    YARD_SPOTS.forEach(def => {
        const spot = spots.find(s => s.id === def.id) || { id: def.id, occupiedBy: null };
        if (spot.occupiedBy) occupied++;

        const isBlocked = blockers.some(b => b.blockedSpots && b.blockedSpots.includes(spot.id));
        const statusClass = spot.occupiedBy ? 'busy' : 'free';
        const blockedClass = isBlocked ? 'blocked' : '';

        const g = document.createElementNS('http://www.w3.org/2000/svg', 'g');
        g.setAttribute('class', `stall-node ${statusClass} ${blockedClass}`);
        g.setAttribute('transform', `translate(${def.x}, ${def.y}) rotate(${def.angle})`);
        g.onclick = () => openSpotDetails(spot, def.area);

        const rect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
        rect.setAttribute('x', -def.w / 2);
        rect.setAttribute('y', -def.h / 2);
        rect.setAttribute('width', def.w);
        rect.setAttribute('height', def.h);
        g.appendChild(rect);

        const textNum = document.createElementNS('http://www.w3.org/2000/svg', 'text');
        textNum.setAttribute('y', -16);
        textNum.setAttribute('class', 'stall-text');
        textNum.textContent = `№${def.id}${isBlocked ? '⚠️' : ''}`;
        g.appendChild(textNum);

        if (spot.occupiedBy) {
            const textCar = document.createElementNS('http://www.w3.org/2000/svg', 'text');
            textCar.setAttribute('y', 14);
            textCar.setAttribute('class', 'stall-car-text');
            textCar.textContent = spot.occupiedBy.carNumber;
            g.appendChild(textCar);
        }

        slotsLayer.appendChild(g);
    });

    document.getElementById('statFree').innerText = `${spots.length - occupied} из ${spots.length}`;
    document.getElementById('statBlocked').innerText = blockers.length;

    // Машины 2-го ряда в проезде
    blockers.forEach(b => {
        const targetDefs = YARD_SPOTS.filter(s => b.blockedSpots && b.blockedSpots.includes(s.id));
        if (targetDefs.length > 0) {
            const avgX = targetDefs.reduce((acc, cur) => acc + cur.x, 0) / targetDefs.length;
            const avgY = 195;

            const bg = document.createElementNS('http://www.w3.org/2000/svg', 'g');
            bg.setAttribute('transform', `translate(${avgX}, ${avgY})`);
            bg.onclick = () => requestLeaveRow2(b.id);

            const r = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
            r.setAttribute('x', -35);
            r.setAttribute('y', -13);
            r.setAttribute('width', 70);
            r.setAttribute('height', 26);
            r.setAttribute('rx', 6);
            r.setAttribute('fill', '#e65100');
            r.setAttribute('stroke', '#ffeb3b');
            r.setAttribute('stroke-width', 2);
            r.setAttribute('filter', 'drop-shadow(0 2px 5px rgba(0,0,0,0.8))');
            bg.appendChild(r);

            const t = document.createElementNS('http://www.w3.org/2000/svg', 'text');
            t.setAttribute('class', 'stall-text');
            t.setAttribute('font-size', '9');
            t.textContent = `🚗 ${b.carNumber}`;
            bg.appendChild(t);

            blockersLayer.appendChild(bg);
        }
    });

    const bList = document.getElementById('blockersList');
    if (blockers.length === 0) {
        bList.innerHTML = '<p style="color:#666; font-size:14px;">Второй ряд свободен</p>';
    } else {
        bList.innerHTML = blockers.map(b => `
            <div class="block-card">
                <b>🚗 ${b.carNumber}</b> (до ~${b.leaveTime || '?'})<br>
                Подпёр места: <b>${(b.blockedSpots || []).join(', ')}</b><br>
                <div>
                    <a href="tel:${b.phone}" class="action-btn" style="background:#2e7d32; color:#fff;">📞 Набрать</a>
                    <a href="sms:${b.phone}?body=${encodeURIComponent('Здравствуйте! Вы перекрыли выезд во дворе. Пожалуйста, выпустите мою машину.')}" class="action-btn" style="background:#0288d1; color:#fff;">✉️ SMS</a>
                </div>
                <button onclick="requestLeaveRow2(${b.id})" style="background:#333; border:1px solid #555; color:#ccc; padding:6px 10px; border-radius:4px; margin-top:8px; font-size:12px; cursor:pointer;">Освободить проезд</button>
            </div>
        `).join('');
    }
}

function openSpotDetails(spot, areaName) {
    const modal = document.getElementById('modalRow1');
    const title = document.getElementById('row1ModalTitle');
    const actions = document.getElementById('spotActions');
    title.innerText = `Место №${spot.id} (${areaName || 'Двор'})`;

    const blocker = (parkingData.blockers || []).find(b => b.blockedSpots && b.blockedSpots.includes(spot.id));
    let blockerHtml = blocker ? `
        <div style="background:#b71c1c; padding:10px; border-radius:6px; margin-bottom:12px;">
            ⚠️ <b>ВАС ПОДПЕРЛИ В ПРОЕЗДЕ!</b><br>
            Авто: <b>${blocker.carNumber}</b> (до ${blocker.leaveTime})<br>
            <div style="margin-top:6px;">
                <a href="tel:${blocker.phone}" class="action-btn" style="background:#4CAF50; color:#fff;">📞 Набрать</a>
                <a href="sms:${blocker.phone}?body=${encodeURIComponent('Здравствуйте! Вы перекрыли мою машину на месте №' + spot.id + '. Пожалуйста, выпустите!')}" class="action-btn" style="background:#00bcd4; color:#000;">✉️ Отправить SMS</a>
            </div>
        </div>
    ` : '';

    if (spot.occupiedBy) {
        actions.innerHTML = `
            ${blockerHtml}
            <p style="margin:4px 0;">Занято: <b>${spot.occupiedBy.carNumber}</b></p>
            <p style="margin:4px 0; color:#aaa; font-size:13px;">Кв: ${spot.occupiedBy.apt || '—'} | Тел: ${spot.occupiedBy.phone || '—'}</p>
            <button onclick="requestLeaveRow1(${spot.id})" style="width:100%; padding:10px; background:#d32f2f; color:#fff; border:none; border-radius:6px; font-weight:bold; cursor:pointer; margin-top:8px;">
                ${isAdmin ? '★ Освободить (Админ)' : 'Освободить место'}
            </button>
            <button onclick="closeModal('modalRow1')" style="width:100%; padding:10px; background:#444; color:#fff; border:none; border-radius:6px; margin-top:8px; cursor:pointer;">Закрыть</button>
        `;
    } else {
        actions.innerHTML = `
            ${blockerHtml}
            <input type="text" id="r1Car" placeholder="Номер авто (А123АА)" value="${localStorage.getItem('my_car') || ''}">
            <input type="text" id="r1Apt" placeholder="Номер квартиры" value="${localStorage.getItem('my_apt') || ''}">
            <input type="tel" id="r1Phone" placeholder="Телефон для связи" value="${localStorage.getItem('my_phone') || ''}">
            <button onclick="occupyRow1(${spot.id})" style="width:100%; padding:10px; background:#2e7d32; color:#fff; border:none; border-radius:6px; font-weight:bold; margin-top:6px; cursor:pointer;">Занять место</button>
            <button onclick="closeModal('modalRow1')" style="width:100%; padding:10px; background:#444; color:#fff; border:none; border-radius:6px; margin-top:8px; cursor:pointer;">Закрыть</button>
        `;
    }
    modal.style.display = 'flex';
}

async function occupyRow1(spotId) {
    const carNumber = document.getElementById('r1Car').value.trim().toUpperCase();
    const phone = document.getElementById('r1Phone').value.trim();
    const apt = document.getElementById('r1Apt').value.trim();
    if (!carNumber) return alert('Укажите номер машины!');

    localStorage.setItem('my_car', carNumber);
    localStorage.setItem('my_phone', phone);
    localStorage.setItem('my_apt', apt);

    await fetch('/api/park/row1', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ spotId, carNumber, apt, phone })
    });
    closeModal('modalRow1');
    loadData();
}

function requestLeaveRow1(spotId) {
    closeModal('modalRow1');
    if (isAdmin) return leaveRow1(spotId, '', adminPin);
    document.getElementById('confirmDigits').value = '';
    document.getElementById('confirmError').style.display = 'none';
    document.getElementById('btnConfirmLeave').onclick = () => {
        const digits = document.getElementById('confirmDigits').value.trim();
        if (digits.length !== 4) {
            document.getElementById('confirmError').innerText = 'Введите ровно 4 цифры!';
            document.getElementById('confirmError').style.display = 'block';
            return;
        }
        leaveRow1(spotId, digits, '');
    };
    document.getElementById('modalConfirmLeave').style.display = 'flex';
}

async function leaveRow1(spotId, phoneDigits, pin) {
    const res = await fetch('/api/leave/row1', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ spotId, phoneDigits, pin })
    });
    const d = await res.json();
    if (d.error) {
        document.getElementById('confirmError').innerText = d.error;
        document.getElementById('confirmError').style.display = 'block';
    } else {
        closeModal('modalConfirmLeave');
        loadData();
    }
}

// 2-й ряд: УДОБНАЯ ГРУППИРОВКА (15-31 сверху!)
function openRow2Modal() {
    r2Selected = [];
    document.getElementById('r2Car').value = localStorage.getItem('my_car') || '';
    document.getElementById('r2Phone').value = localStorage.getItem('my_phone') || '';
    document.getElementById('r2Time').value = '08:30 утра';

    const checklist = document.getElementById('r2SpotsChecklist');
    
    // Сначала показываем места 15-31 (которые обычно подпирают!), а затем 1-14
    const bottomSpots = YARD_SPOTS.filter(s => s.id >= 15);
    const topSpots = YARD_SPOTS.filter(s => s.id < 15);

    checklist.innerHTML = `
        <div style="color:#ff9800; font-weight:bold; font-size:12px; margin:4px 0;">🏢 У подъездов и карманов (15–31) — обычно здесь:</div>
        <div>
            ${bottomSpots.map(s => `<span class="spot-badge" id="badge_${s.id}" onclick="toggleSpotBadge(${s.id})">№${s.id}</span>`).join('')}
        </div>
        <div style="color:#81c784; font-weight:bold; font-size:12px; margin:8px 0 4px 0;">🛝 Вдоль площадки (1–14):</div>
        <div>
            ${topSpots.map(s => `<span class="spot-badge" id="badge_${s.id}" onclick="toggleSpotBadge(${s.id})">№${s.id}</span>`).join('')}
        </div>
    `;

    document.getElementById('modalRow2').style.display = 'flex';
}

function toggleSpotBadge(id) {
    const el = document.getElementById('badge_' + id);
    if (r2Selected.includes(id)) {
        r2Selected = r2Selected.filter(x => x !== id);
        el.classList.remove('active');
    } else {
        r2Selected.push(id);
        el.classList.add('active');
    }
}

async function submitRow2Modal() {
    const carNumber = document.getElementById('r2Car').value.trim().toUpperCase();
    const phone = document.getElementById('r2Phone').value.trim();
    const leaveTime = document.getElementById('r2Time').value.trim();
    if (!carNumber || !phone) return alert('Укажите номер и телефон!');
    if (r2Selected.length === 0) return alert('Выберите хотя бы одно перекрытое место!');

    localStorage.setItem('my_car', carNumber);
    localStorage.setItem('my_phone', phone);

    await fetch('/api/park/row2', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ carNumber, phone, leaveTime, blockedSpots: r2Selected })
    });
    closeModal('modalRow2');
    loadData();
}

function requestLeaveRow2(blockerId) {
    if (isAdmin) return leaveRow2(blockerId, '', adminPin);
    document.getElementById('confirmDigits').value = '';
    document.getElementById('confirmError').style.display = 'none';
    document.getElementById('btnConfirmLeave').onclick = () => {
        const digits = document.getElementById('confirmDigits').value.trim();
        if (digits.length !== 4) {
            document.getElementById('confirmError').innerText = 'Введите ровно 4 цифры!';
            document.getElementById('confirmError').style.display = 'block';
            return;
        }
        leaveRow2(blockerId, digits, '');
    };
    document.getElementById('modalConfirmLeave').style.display = 'flex';
}

async function leaveRow2(blockerId, phoneDigits, pin) {
    const res = await fetch('/api/leave/row2', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ blockerId, phoneDigits, pin })
    });
    const d = await res.json();
    if (d.error) {
        document.getElementById('confirmError').innerText = d.error;
        document.getElementById('confirmError').style.display = 'block';
    } else {
        closeModal('modalConfirmLeave');
        loadData();
    }
}

async function toggleAdmin() {
    if (isAdmin) {
        isAdmin = false;
        document.getElementById('roleBadge').innerText = 'Режим: Житель';
        document.getElementById('roleBadge').style.background = '#333';
        document.getElementById('adminPanel').style.display = 'none';
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

async function viewSurveyResults() {
    const res = await fetch('/api/admin/surveys', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ pin: adminPin })
    });
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

function closeModal(id) { document.getElementById(id).style.display = 'none'; }

if (localStorage.getItem('parking_voted')) {
    document.getElementById('surveyForm').style.display = 'none';
    document.getElementById('surveySuccess').style.display = 'block';
}

loadData();
setInterval(loadData, 3000);
