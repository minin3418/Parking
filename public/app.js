let parkingData = { spots: [], blockers: [], config: { topCount: 16, bottomCount: 14 } };
let isAdmin = false;
let adminPin = "";
let r2Selected = [];

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
    const topContainer = document.getElementById('stallsTop');
    const bottomContainer = document.getElementById('stallsBottom');
    topContainer.innerHTML = '';
    bottomContainer.innerHTML = '';

    let occupied = 0;

    spots.forEach(spot => {
        if (spot.occupiedBy) occupied++;
        const isBlocked = blockers.some(b => b.blockedSpots && b.blockedSpots.includes(spot.id));
        const div = document.createElement('div');
        div.className = `stall ${spot.occupiedBy ? 'busy' : 'free'} ${isBlocked ? 'blocked' : ''}`;

        const carLabel = spot.occupiedBy ? spot.occupiedBy.carNumber : 'Свободно';
        div.innerHTML = `
            <div class="stall-num">№${spot.id} ${isBlocked ? '⚠️' : ''}</div>
            <div class="stall-car">${spot.occupiedBy ? spot.occupiedBy.carNumber : ''}</div>
        `;

        div.onclick = () => openSpotDetails(spot);

        if (spot.row === 'top' || spot.id <= (parkingData.config ? parkingData.config.topCount : 16)) {
            topContainer.appendChild(div);
        } else {
            bottomContainer.appendChild(div);
        }
    });

    document.getElementById('statFree').innerText = `${spots.length - occupied} из ${spots.length}`;
    document.getElementById('statBlocked').innerText = blockers.length;

    // Машины в проезде
    const drivewayChips = document.getElementById('drivewayChips');
    const drivewayEmpty = document.getElementById('drivewayEmpty');
    drivewayChips.innerHTML = '';

    if (blockers.length === 0) {
        drivewayEmpty.style.display = 'block';
    } else {
        drivewayEmpty.style.display = 'none';
        blockers.forEach(b => {
            const chip = document.createElement('div');
            chip.className = 'blocker-car-chip';
            chip.innerHTML = `🚗 ${b.carNumber} ➔ перекрыл №${(b.blockedSpots || []).join(', ')}`;
            drivewayChips.appendChild(chip);
        });
    }

    // Список 2 ряда внизу
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

function openSpotDetails(spot) {
    const modal = document.getElementById('modalRow1');
    const title = document.getElementById('row1ModalTitle');
    const actions = document.getElementById('spotActions');
    title.innerText = `Место №${spot.id}`;

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

// 2-й ряд
function openRow2Modal() {
    r2Selected = [];
    document.getElementById('r2Car').value = localStorage.getItem('my_car') || '';
    document.getElementById('r2Phone').value = localStorage.getItem('my_phone') || '';
    document.getElementById('r2Time').value = '08:30 утра';

    const checklist = document.getElementById('r2SpotsChecklist');
    checklist.innerHTML = (parkingData.spots || []).map(s => `
        <span class="spot-badge" id="badge_${s.id}" onclick="toggleSpotBadge(${s.id})">№${s.id}</span>
    `).join('');

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

// Админка
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
        if (parkingData.config) {
            document.getElementById('cfgTop').value = parkingData.config.topCount;
            document.getElementById('cfgBottom').value = parkingData.config.bottomCount;
        }
        render();
    } else {
        alert('Неверный PIN!');
    }
}

async function saveRowConfig() {
    const topCount = document.getElementById('cfgTop').value;
    const bottomCount = document.getElementById('cfgBottom').value;
    await fetch('/api/admin/config-rows', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pin: adminPin, topCount, bottomCount })
    });
    alert('Схема рядов обновлена!');
    loadData();
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
