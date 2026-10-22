let parkingData = { spots: [], blockers: [], landmarks: [] };
let isAdmin = false;
let adminPin = "";
let isRow2Mode = false;
let isEditMode = false;
let r2Selected = [];

function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.style.display = 'none';
}

function formatTime(iso) {
    if (!iso) return 'Нет данных';
    const d = new Date(iso);
    const h = d.getHours().toString().padStart(2, '0');
    const m = d.getMinutes().toString().padStart(2, '0');
    const diffMin = Math.floor((Date.now() - d.getTime()) / 60000);
    if (diffMin < 60) return `${h}:${m} (${diffMin} мин назад)`;
    return `${h}:${m} (~${Math.floor(diffMin / 60)} ч назад)`;
}

function getShortTime(iso) {
    if (!iso) return '';
    const d = new Date(iso);
    const h = d.getHours().toString().padStart(2, '0');
    const m = d.getMinutes().toString().padStart(2, '0');
    return `${h}:${m}`;
}

async function loadData() {
    try {
        const res = await fetch('/api/parking');
        parkingData = await res.json();
        render();
    } catch(e) {
        console.error("Ошибка загрузки", e);
    }
}

function createCarSvg(isFree, carNumber, spotId, isBlocked, isSelected, parkedAtStr, leaveTimeStr) {
    const g = document.createElementNS('http://www.w3.org/2000/svg', 'g');
    if (isFree) {
        const stall = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
        stall.setAttribute('x', -18); stall.setAttribute('y', -32);
        stall.setAttribute('width', 36); stall.setAttribute('height', 64);
        stall.setAttribute('rx', 6);
        stall.setAttribute('fill', 'rgba(46, 125, 50, 0.25)');
        stall.setAttribute('stroke', isSelected ? '#ffeb3b' : (isBlocked ? '#ff9800' : '#4caf50'));
        stall.setAttribute('stroke-width', isSelected ? '3' : '1.8');
        if (isBlocked && !isSelected) stall.setAttribute('stroke-dasharray', '4,3');
        g.appendChild(stall);

        const textNum = document.createElementNS('http://www.w3.org/2000/svg', 'text');
        textNum.setAttribute('y', 4);
        textNum.setAttribute('fill', '#81c784');
        textNum.setAttribute('font-size', '13'); textNum.setAttribute('font-weight', 'bold');
        textNum.setAttribute('text-anchor', 'middle'); textNum.setAttribute('dominant-baseline', 'middle');
        textNum.textContent = `№${spotId}${isBlocked ? '⚠️' : ''}${isSelected ? ' ✅' : ''}`;
        g.appendChild(textNum);
    } else {
        const body = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
        body.setAttribute('x', -16); body.setAttribute('y', -30);
        body.setAttribute('width', 32); body.setAttribute('height', 60);
        body.setAttribute('rx', 7);
        body.setAttribute('fill', isSelected ? '#e65100' : '#c62828');
        body.setAttribute('stroke', isSelected ? '#ffeb3b' : (isBlocked ? '#ffeb3b' : '#ff7961'));
        body.setAttribute('stroke-width', isSelected ? '3' : (isBlocked ? '2.5' : '1.5'));
        if (isBlocked && !isSelected) body.setAttribute('stroke-dasharray', '4,3');
        g.appendChild(body);

        const m1 = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
        m1.setAttribute('x', -19); m1.setAttribute('y', -18); m1.setAttribute('width', 3); m1.setAttribute('height', 6); m1.setAttribute('fill', '#888');
        const m2 = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
        m2.setAttribute('x', 16); m2.setAttribute('y', -18); m2.setAttribute('width', 3); m2.setAttribute('height', 6); m2.setAttribute('fill', '#888');
        g.appendChild(m1); g.appendChild(m2);

        const frontGlass = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        frontGlass.setAttribute('d', 'M -11 -12 L -9 -21 Q 0 -23 9 -21 L 11 -12 Z');
        frontGlass.setAttribute('fill', '#111'); frontGlass.setAttribute('opacity', '0.8');
        g.appendChild(frontGlass);

        const rearGlass = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        rearGlass.setAttribute('d', 'M -11 15 L -9 22 Q 0 23 9 22 L 11 15 Z');
        rearGlass.setAttribute('fill', '#111'); rearGlass.setAttribute('opacity', '0.8');
        g.appendChild(rearGlass);

        const l1 = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
        l1.setAttribute('cx', -10); l1.setAttribute('cy', -28); l1.setAttribute('r', 2); l1.setAttribute('fill', '#fff9c4');
        const l2 = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
        l2.setAttribute('cx', 10); l2.setAttribute('cy', -28); l2.setAttribute('r', 2); l2.setAttribute('fill', '#fff9c4');
        g.appendChild(l1); g.appendChild(l2);

        const textCar = document.createElementNS('http://www.w3.org/2000/svg', 'text');
        textCar.setAttribute('y', -2); textCar.setAttribute('fill', '#ffffff');
        textCar.setAttribute('font-size', '8'); textCar.setAttribute('font-weight', 'bold');
        textCar.setAttribute('text-anchor', 'middle'); textCar.setAttribute('dominant-baseline', 'middle');
        textCar.textContent = carNumber || `№${spotId}`;
        g.appendChild(textCar);

        if (leaveTimeStr) {
            const textTime = document.createElementNS('http://www.w3.org/2000/svg', 'text');
            textTime.setAttribute('y', 8); textTime.setAttribute('fill', '#81c784');
            textTime.setAttribute('font-size', '6'); textTime.setAttribute('font-weight', 'bold');
            textTime.setAttribute('text-anchor', 'middle'); textTime.setAttribute('dominant-baseline', 'middle');
            textTime.textContent = leaveTimeStr.slice(0, 9);
            g.appendChild(textTime);
        } else if (parkedAtStr) {
            const textTime = document.createElementNS('http://www.w3.org/2000/svg', 'text');
            textTime.setAttribute('y', 8); textTime.setAttribute('fill', '#ffeb3b');
            textTime.setAttribute('font-size', '6'); textTime.setAttribute('text-anchor', 'middle');
            textTime.setAttribute('dominant-baseline', 'middle');
            textTime.textContent = getShortTime(parkedAtStr);
            g.appendChild(textTime);
        }
    }
    return g;
}

function render() {
    const spots = parkingData.spots || [];
    const blockers = parkingData.blockers || [];
    const landmarks = parkingData.landmarks || [];
    const slotsLayer = document.getElementById('slotsLayer');
    const blockersLayer = document.getElementById('blockersLayer');
    const landmarksLayer = document.getElementById('landmarksLayer');

    slotsLayer.innerHTML = '';
    blockersLayer.innerHTML = '';
    landmarksLayer.innerHTML = '';
    let occupied = 0;

    landmarks.forEach(lm => {
        const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
        text.setAttribute('x', lm.x);
        text.setAttribute('y', lm.y);
        text.setAttribute('fill', '#aaa');
        text.setAttribute('font-size', '11');
        text.setAttribute('font-weight', 'bold');
        text.setAttribute('text-anchor', 'middle');
        text.setAttribute('class', `landmark-text ${isEditMode ? 'editable-landmark' : ''}`);
        text.textContent = lm.text;
        text.onclick = () => {
            if (!isEditMode) return;
            const newText = prompt(`Изменить надпись "${lm.text}":`, lm.text);
            if (newText && newText.trim()) {
                lm.text = newText.trim();
                render();
            }
        };
        landmarksLayer.appendChild(text);
    });

    spots.forEach(spot => {
        if (spot.occupiedBy) occupied++;
        const isBlocked = blockers.some(b => b.blockedSpots && b.blockedSpots.includes(spot.id));
        const isSelected = r2Selected.includes(spot.id);
        const g = document.createElementNS('http://www.w3.org/2000/svg', 'g');
        g.setAttribute('class', 'stall-node');
        g.setAttribute('transform', `translate(${spot.x}, ${spot.y}) rotate(${spot.angle || 0})`);
        g.onclick = () => {
            if (isRow2Mode) {
                if (r2Selected.includes(spot.id)) {
                    r2Selected = r2Selected.filter(id => id !== spot.id);
                } else {
                    r2Selected.push(spot.id);
                }
                updateRow2SheetText();
                render();
            } else {
                openSpotDetails(spot);
            }
        };
        const parkedAtStr = spot.occupiedBy ? spot.occupiedBy.parkedAt : null;
        const leaveTimeStr = spot.occupiedBy ? spot.occupiedBy.leaveTime : null;
        const carSvg = createCarSvg(!spot.occupiedBy, spot.occupiedBy ? spot.occupiedBy.carNumber : '', spot.id, isBlocked, isSelected, parkedAtStr, leaveTimeStr);
        g.appendChild(carSvg);
        slotsLayer.appendChild(g);
    });

    document.getElementById('statFree').innerText = `${spots.length - occupied} из ${spots.length}`;
    document.getElementById('statBlocked').innerText = blockers.length;

    blockers.forEach(b => {
        const targetDefs = spots.filter(s => b.blockedSpots && b.blockedSpots.includes(s.id));
        if (targetDefs.length > 0) {
            const avgX = targetDefs.reduce((acc, cur) => acc + cur.x, 0) / targetDefs.length;
            const avgY = 195;
            const bg = document.createElementNS('http://www.w3.org/2000/svg', 'g');
            bg.setAttribute('transform', `translate(${avgX}, ${avgY}) rotate(90)`);
            bg.onclick = () => requestLeaveRow2(b.id);
            const carSvg = createCarSvg(false, b.carNumber, '', false, false, null, null);
            bg.appendChild(carSvg);
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

function toggleMapEdit() {
    isEditMode = !isEditMode;
    document.getElementById('btnSaveMap').style.display = isEditMode ? 'block' : 'none';
    document.getElementById('editHint').style.display = isEditMode ? 'block' : 'none';
    document.getElementById('btnMapEdit').innerText = isEditMode ? 'Отмена' : '🛠 Редактировать схему';
    render();
}

async function saveMapConfig() {
    await fetch('/api/admin/map-config', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pin: adminPin, landmarks: parkingData.landmarks, spots: parkingData.spots })
    });
    alert('Схема и надписи сохранены!');
    toggleMapEdit();
    loadData();
}

function startRow2Selection() {
    isRow2Mode = true;
    r2Selected = [];
    document.getElementById('btnStartRow2').style.display = 'none';
    document.getElementById('sheetRow2').style.display = 'block';
    document.getElementById('r2Car').value = localStorage.getItem('my_car') || '';
    document.getElementById('r2Phone').value = localStorage.getItem('my_phone') || '';
    document.getElementById('r2Time').value = '08:30 утра';
    const inst = document.getElementById('instructionText');
    inst.className = 'instruction-bar select-mode';
    inst.innerHTML = '👉 <b>НАЖМИТЕ НА КАРТЕ</b> на машины, которые вы перекрыли:';
    updateRow2SheetText();
    render();
}

function cancelRow2Selection() {
    isRow2Mode = false;
    r2Selected = [];
    document.getElementById('btnStartRow2').style.display = 'block';
    document.getElementById('sheetRow2').style.display = 'none';
    const inst = document.getElementById('instructionText');
    inst.className = 'instruction-bar';
    inst.innerHTML = '👉 Листайте схему влево-вправо ⇄ Нажмите на машинку, чтобы занять.';
    render();
}

function updateRow2SheetText() {
    const textEl = document.getElementById('sheetSelectedSpots');
    if (r2Selected.length === 0) {
        textEl.innerText = 'Нажмите на карте';
    } else {
        textEl.innerText = '№' + r2Selected.sort((a,b)=>a-b).join(', №');
    }
}

async function submitRow2() {
    if (r2Selected.length === 0) return alert('Нажмите на карте хотя бы на одну машину, которую перекрыли!');
    const carNumber = document.getElementById('r2Car').value.trim().toUpperCase();
    const phone = document.getElementById('r2Phone').value.trim();
    const leaveTime = document.getElementById('r2Time').value.trim();
    if (!carNumber || !phone) return alert('Укажите номер и телефон!');
    localStorage.setItem('my_car', carNumber);
    localStorage.setItem('my_phone', phone);
    await fetch('/api/park/row2', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ carNumber, phone, leaveTime, blockedSpots: r2Selected })
    });
    cancelRow2Selection();
    loadData();
}

function openSpotDetails(spot) {
    const modal = document.getElementById('modalRow1');
    const title = document.getElementById('row1ModalTitle');
    const actions = document.getElementById('spotActions');
    title.innerText = `Место №${spot.id}`;

    const blocker = (parkingData.blockers || []).find(b => b.blockedSpots && b.blockedSpots.includes(spot.id));
    let blockerHtml = blocker ? `
        <div style="background:#b71c1c; padding:10px; border-radius:6px; margin-bottom:12px;">
            ⚠️ <b>Выезд перекрыт машиной во 2-м ряду!</b><br>
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
            ${spot.occupiedBy.leaveTime ? `<p style="margin:4px 0; color:#81c784; font-size:13px;">🕑 Уедет в: <b>${spot.occupiedBy.leaveTime}</b></p>` : ''}
            <p style="margin:8px 0 12px 0; color:#ffeb3b; font-size:13px;">🕑 Время парковки: <b>${formatTime(spot.occupiedBy.parkedAt)}</b></p>
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
            <input type="text" id="r1Time" placeholder="Уеду в... (напр. 08:30 утра)" value="${localStorage.getItem('my_time') || ''}">
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
    const leaveTime = document.getElementById('r1Time').value.trim();
    if (!carNumber) return alert('Укажите номер машины!');
    localStorage.setItem('my_car', carNumber);
    localStorage.setItem('my_phone', phone);
    localStorage.setItem('my_apt', apt);
    if (leaveTime) localStorage.setItem('my_time', leaveTime);

    await fetch('/api/park/row1', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ spotId, carNumber, apt, phone, leaveTime })
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
        isEditMode = false;
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
    document.getElementById('surveyForm').style.display = 'none';
    document.getElementById('surveySuccess').style.display = 'block';
}

window.onload = loadData;
