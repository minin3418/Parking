let currentData = { spots: [], blockers: [], landmarks: [], surveys: [] };
let isAdmin = false;
let adminPin = localStorage.getItem('dvor_admin_pin') || '';
let isEditMapMode = false;
let isRow2SelectMode = false;
let selectedBlockSpots = new Set();
let editingSpotId = null;
let editingLandmarkId = null;

let draggingItem = null; // { type: 'spot' | 'landmark', id, startMouseX, startMouseY, origX, origY, hasMoved }

// Если ранее админ уже авторизовался — восстанавливаем сессию
if (adminPin) {
    isAdmin = true;
}

// Загрузка данных с сервера
async function loadData() {
    if (isEditMapMode) return; // не затирать фоновым опросом при редактировании

    try {
        const res = await fetch('/api/data');
        if (!res.ok) {
            console.warn(`Сервер вернул статус HTTP ${res.status}`);
            return;
        }
        currentData = await res.json();
        renderMap();
        updateStats();
    } catch (e) {
        console.error('Ошибка загрузки данных:', e);
    }
}

function updateStats() {
    const free = (currentData.spots || []).filter(s => !s.occupiedBy).length;
    const total = (currentData.spots || []).length;
    const freeEl = document.getElementById('freeCount');
    if (freeEl) freeEl.textContent = `${free} из ${total}`;

    const blockers = (currentData.blockers || []).length;
    const blockersEl = document.getElementById('blockersCount');
    if (blockersEl) blockersEl.textContent = `${blockers} авто`;
}

// Всплывающее уведомление
function showToast(msg) {
    let toast = document.getElementById('toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'toast';
        toast.style.position = 'fixed';
        toast.style.bottom = '80px';
        toast.style.left = '50%';
        toast.style.transform = 'translateX(-50%)';
        toast.style.background = '#2e7d32';
        toast.style.color = '#fff';
        toast.style.padding = '10px 18px';
        toast.style.borderRadius = '8px';
        toast.style.fontWeight = 'bold';
        toast.style.zIndex = '999';
        toast.style.boxShadow = '0 4px 14px rgba(0,0,0,0.5)';
        document.body.appendChild(toast);
    }
    toast.textContent = msg;
    toast.style.display = 'block';
    setTimeout(() => { toast.style.display = 'none'; }, 2200);
}

// Перевод координат экрана в систему координат SVG (960x440)
function getSvgCoords(evt) {
    const svg = document.getElementById('yardMap');
    const pt = svg.createSVGPoint();
    pt.x = evt.clientX;
    pt.y = evt.clientY;
    return pt.matrixTransform(svg.getScreenCTM().inverse());
}

// Отрисовка карты в SVG
function renderMap() {
    const spotsLayer = document.getElementById('spotsLayer');
    const blockersLayer = document.getElementById('blockersLayer');
    const landmarksLayer = document.getElementById('landmarksLayer');

    if (!spotsLayer || !blockersLayer || !landmarksLayer) return;

    spotsLayer.innerHTML = '';
    blockersLayer.innerHTML = '';
    landmarksLayer.innerHTML = '';

    // 1. Отрисовка объектов/надписей с поддержкой ориентации (угла) и Drag-and-Drop
    (currentData.landmarks || []).forEach(lm => {
        const g = document.createElementNS('http://www.w3.org/2000/svg', 'g');
        const angle = lm.angle || 0;
        g.setAttribute('transform', `translate(${lm.x}, ${lm.y}) rotate(${angle})`);
        g.style.cursor = isEditMapMode ? 'grab' : 'pointer';
        g.style.touchAction = 'none';

        const textLen = (lm.text || '').length;
        const boxWidth = Math.max(64, textLen * 8.5 + 18);
        const boxHeight = 26;

        // Рамка-подложка: кликабельная область и визуальная подсветка
        const box = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
        box.setAttribute('x', -boxWidth / 2);
        box.setAttribute('y', -boxHeight / 2);
        box.setAttribute('width', boxWidth);
        box.setAttribute('height', boxHeight);
        box.setAttribute('rx', '6');

        if (isEditMapMode) {
            box.setAttribute('fill', 'rgba(255, 213, 79, 0.18)');
            box.setAttribute('stroke', '#ffd54f');
            box.setAttribute('stroke-width', '1.5');
            box.setAttribute('stroke-dasharray', '4,3');
        } else {
            box.setAttribute('fill', 'rgba(26, 32, 38, 0.65)');
            box.setAttribute('stroke', '#37474f');
            box.setAttribute('stroke-width', '1');
        }
        g.appendChild(box);

        const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
        text.setAttribute('x', '0');
        text.setAttribute('y', '0');
        text.setAttribute('dominant-baseline', 'central');
        text.setAttribute('text-anchor', 'middle');
        text.setAttribute('fill', isEditMapMode ? '#ffd54f' : '#b0bec5');
        text.setAttribute('font-size', '12');
        text.setAttribute('font-weight', 'bold');
        text.style.userSelect = 'none';
        text.textContent = lm.text;
        g.appendChild(text);

        if (isEditMapMode) {
            g.onpointerdown = (e) => {
                e.preventDefault();
                e.stopPropagation();
                const coords = getSvgCoords(e);
                draggingItem = {
                    type: 'landmark',
                    id: lm.id,
                    startMouseX: coords.x,
                    startMouseY: coords.y,
                    origX: lm.x,
                    origY: lm.y,
                    hasMoved: false
                };
                g.style.cursor = 'grabbing';
                g.setPointerCapture(e.pointerId);
            };

            g.onpointermove = (e) => {
                if (draggingItem && draggingItem.type === 'landmark' && draggingItem.id === lm.id) {
                    const coords = getSvgCoords(e);
                    const dx = coords.x - draggingItem.startMouseX;
                    const dy = coords.y - draggingItem.startMouseY;
                    if (Math.hypot(dx, dy) > 3) {
                        draggingItem.hasMoved = true;
                        lm.x = Math.round(Math.max(15, Math.min(945, draggingItem.origX + dx)));
                        lm.y = Math.round(Math.max(15, Math.min(425, draggingItem.origY + dy)));
                        g.setAttribute('transform', `translate(${lm.x}, ${lm.y}) rotate(${lm.angle || 0})`);
                    }
                }
            };

            g.onpointerup = (e) => {
                if (draggingItem && draggingItem.type === 'landmark' && draggingItem.id === lm.id) {
                    try { g.releasePointerCapture(e.pointerId); } catch (_) {}
                    g.style.cursor = 'grab';
                    if (!draggingItem.hasMoved) {
                        openEditLandmark(lm.id);
                    } else {
                        saveMapToServer(true); // автосохранение новой позиции
                    }
                    draggingItem = null;
                }
            };
        } else {
            g.onclick = () => {
                showToast(`Объект: ${lm.text}`);
            };
        }

        landmarksLayer.appendChild(g);
    });

    // 2. Отрисовка парковочных мест с поддержкой Drag-and-Drop
    (currentData.spots || []).forEach(spot => {
        const isFree = !spot.occupiedBy;
        const isBlocked = (currentData.blockers || []).some(b => b.blockedSpots.includes(spot.id));
        const isSelectedRow2 = selectedBlockSpots.has(spot.id);

        const g = document.createElementNS('http://www.w3.org/2000/svg', 'g');
        const angle = spot.angle || 0;
        g.setAttribute('transform', `translate(${spot.x}, ${spot.y}) rotate(${angle})`);
        g.style.cursor = isEditMapMode ? 'grab' : 'pointer';
        g.style.touchAction = 'none';

        if (isEditMapMode) {
            g.onpointerdown = (e) => {
                e.preventDefault();
                e.stopPropagation();
                const coords = getSvgCoords(e);
                draggingItem = {
                    type: 'spot',
                    id: spot.id,
                    startMouseX: coords.x,
                    startMouseY: coords.y,
                    origX: spot.x,
                    origY: spot.y,
                    hasMoved: false
                };
                g.style.cursor = 'grabbing';
                g.setPointerCapture(e.pointerId);
            };

            g.onpointermove = (e) => {
                if (draggingItem && draggingItem.type === 'spot' && draggingItem.id === spot.id) {
                    const coords = getSvgCoords(e);
                    const dx = coords.x - draggingItem.startMouseX;
                    const dy = coords.y - draggingItem.startMouseY;
                    if (Math.hypot(dx, dy) > 3) {
                        draggingItem.hasMoved = true;
                        spot.x = Math.round(Math.max(20, Math.min(940, draggingItem.origX + dx)));
                        spot.y = Math.round(Math.max(20, Math.min(420, draggingItem.origY + dy)));
                        g.setAttribute('transform', `translate(${spot.x}, ${spot.y}) rotate(${spot.angle || 0})`);
                    }
                }
            };

            g.onpointerup = (e) => {
                if (draggingItem && draggingItem.type === 'spot' && draggingItem.id === spot.id) {
                    try { g.releasePointerCapture(e.pointerId); } catch (_) {}
                    g.style.cursor = 'grab';
                    if (!draggingItem.hasMoved) {
                        openEditSpot(spot.id);
                    } else {
                        saveMapToServer(true); // автосохранение новой позиции
                    }
                    draggingItem = null;
                }
            };
        } else {
            g.onclick = () => onSpotClicked(spot);
        }

        if (isFree) {
            const rect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
            rect.setAttribute('x', '-18');
            rect.setAttribute('y', '-32');
            rect.setAttribute('width', '36');
            rect.setAttribute('height', '64');
            rect.setAttribute('rx', '6');
            rect.setAttribute('fill', isSelectedRow2 ? 'rgba(255, 179, 0, 0.4)' : 'rgba(76, 175, 80, 0.2)');
            rect.setAttribute('stroke', isSelectedRow2 ? '#ffb300' : (isBlocked ? '#ffb300' : '#4caf50'));
            rect.setAttribute('stroke-width', isSelectedRow2 ? '3' : '1.8');
            if (isBlocked && !isSelectedRow2) rect.setAttribute('stroke-dasharray', '5,4');
            g.appendChild(rect);

            const num = document.createElementNS('http://www.w3.org/2000/svg', 'text');
            num.setAttribute('x', '0');
            num.setAttribute('y', '5');
            num.setAttribute('text-anchor', 'middle');
            num.setAttribute('fill', isSelectedRow2 ? '#fff59d' : '#81c784');
            num.setAttribute('font-size', '12');
            num.setAttribute('font-weight', 'bold');
            num.textContent = `№${spot.id}${isBlocked ? '⚠️' : ''}${isSelectedRow2 ? '✓' : ''}`;
            g.appendChild(num);
        } else {
            const car = spot.occupiedBy;
            const rect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
            rect.setAttribute('x', '-16');
            rect.setAttribute('y', '-30');
            rect.setAttribute('width', '32');
            rect.setAttribute('height', '60');
            rect.setAttribute('rx', '7');
            rect.setAttribute('fill', isSelectedRow2 ? '#e65100' : '#c62828');
            rect.setAttribute('stroke', isSelectedRow2 ? '#ffb300' : (isBlocked ? '#ffb300' : '#ef5350'));
            rect.setAttribute('stroke-width', isSelectedRow2 || isBlocked ? '2.5' : '1.5');
            g.appendChild(rect);

            const glass = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
            glass.setAttribute('x', '-10');
            glass.setAttribute('y', '-20');
            glass.setAttribute('width', '20');
            glass.setAttribute('height', '10');
            glass.setAttribute('rx', '2');
            glass.setAttribute('fill', '#111');
            g.appendChild(glass);

            const plate = document.createElementNS('http://www.w3.org/2000/svg', 'text');
            plate.setAttribute('x', '0');
            plate.setAttribute('y', '2');
            plate.setAttribute('text-anchor', 'middle');
            plate.setAttribute('fill', '#ffffff');
            plate.setAttribute('font-size', '7.5');
            plate.setAttribute('font-weight', 'bold');
            plate.textContent = car.carNumber || `№${spot.id}`;
            g.appendChild(plate);

            if (car.leaveTime) {
                const leave = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                leave.setAttribute('x', '0');
                leave.setAttribute('y', '14');
                leave.setAttribute('text-anchor', 'middle');
                leave.setAttribute('fill', '#81c784');
                leave.setAttribute('font-size', '6.5');
                leave.textContent = car.leaveTime.slice(0, 9);
                g.appendChild(leave);
            }
        }

        spotsLayer.appendChild(g);
    });

    // 3. Отрисовка авто во 2-м ряду
    (currentData.blockers || []).forEach(blocker => {
        const targets = (currentData.spots || []).filter(s => blocker.blockedSpots.includes(s.id));
        const avgX = targets.length ? targets.reduce((a, b) => a + b.x, 0) / targets.length : 480;
        const avgY = 195;

        const g = document.createElementNS('http://www.w3.org/2000/svg', 'g');
        g.setAttribute('transform', `translate(${avgX}, ${avgY}) rotate(90)`);
        g.style.cursor = 'pointer';
        g.onclick = () => openBlockerDetails(blocker);

        const rect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
        rect.setAttribute('x', '-16');
        rect.setAttribute('y', '-30');
        rect.setAttribute('width', '32');
        rect.setAttribute('height', '60');
        rect.setAttribute('rx', '7');
        rect.setAttribute('fill', '#d84315');
        rect.setAttribute('stroke', '#ffb300');
        rect.setAttribute('stroke-width', '2.5');
        g.appendChild(rect);

        const plate = document.createElementNS('http://www.w3.org/2000/svg', 'text');
        plate.setAttribute('x', '0');
        plate.setAttribute('y', '0');
        plate.setAttribute('text-anchor', 'middle');
        plate.setAttribute('fill', '#fff');
        plate.setAttribute('font-size', '8');
        plate.setAttribute('font-weight', 'bold');
        plate.textContent = blocker.carNumber;
        g.appendChild(plate);

        if (blocker.leaveTime) {
            const leave = document.createElementNS('http://www.w3.org/2000/svg', 'text');
            leave.setAttribute('x', '0');
            leave.setAttribute('y', '12');
            leave.setAttribute('text-anchor', 'middle');
            leave.setAttribute('fill', '#ffd54f');
            leave.setAttribute('font-size', '6.5');
            leave.textContent = blocker.leaveTime.slice(0, 8);
            g.appendChild(leave);
        }

        blockersLayer.appendChild(g);
    });
}

function onSpotClicked(spot) {
    if (isRow2SelectMode) {
        if (selectedBlockSpots.has(spot.id)) {
            selectedBlockSpots.delete(spot.id);
        } else {
            selectedBlockSpots.add(spot.id);
        }
        renderMap();
        return;
    }

    openSpotModal(spot);
}

// Модальное окно места 1-го ряда
function openSpotModal(spot) {
    const body = document.getElementById('spotModalBody');
    document.getElementById('modalSpotTitle').textContent = `Место №${spot.id} (${spot.occupiedBy ? 'Занято' : 'Свободно'})`;

    const blocker = (currentData.blockers || []).find(b => b.blockedSpots.includes(spot.id));
    let blockerHtml = '';
    if (blocker) {
        blockerHtml = `
            <div style="background:#331414; border:1px solid #ffb300; padding:12px; border-radius:10px; margin-bottom:12px;">
                <strong style="color:#ffb300;">⚠️ ВЫЕЗД ПЕРЕКРЫТ!</strong>
                <p style="margin:4px 0;">Авто во 2-м ряду: <b>${blocker.carNumber}</b> (до ${blocker.leaveTime || 'утра'})</p>
                ${blocker.phone ? `<a href="tel:${blocker.phone}" class="btn btn-green" style="margin-top:6px; text-decoration:none;">📞 Позвонить ${blocker.phone}</a>` : ''}
            </div>
        `;
    }

    if (!spot.occupiedBy) {
        body.innerHTML = `
            ${blockerHtml}
            <div class="input-group">
                <label>Госномер авто (напр. А123АА):</label>
                <input type="text" id="parkCarInput" placeholder="А123АА">
            </div>
            <div style="display:flex; gap:8px;">
                <div class="input-group" style="flex:1;">
                    <label>Квартира:</label>
                    <input type="text" id="parkAptInput" placeholder="42">
                </div>
                <div class="input-group" style="flex:1.5;">
                    <label>Телефон:</label>
                    <input type="tel" id="parkPhoneInput" placeholder="+7 9...">
                </div>
            </div>
            <div class="input-group">
                <label>Уеду в... (напр. до 19:00 или до утра):</label>
                <input type="text" id="parkLeaveTimeInput" value="08:30 утра">
            </div>
            <button class="btn btn-green" style="width:100%; justify-content:center; padding:12px;" onclick="submitPark(${spot.id})">
                Занять парковочное место
            </button>
        `;
    } else {
        const car = spot.occupiedBy;
        body.innerHTML = `
            ${blockerHtml}
            <div class="number-plate">${car.carNumber}</div>
            <p style="margin-bottom:6px;">Квартира: <b>${car.apt || '—'}</b></p>
            <p style="margin-bottom:6px;">Телефон: <b>${car.phone || 'Не указан'}</b></p>
            ${car.leaveTime ? `<p style="color:#81c784; margin-bottom:8px;">Планирует уехать: <b>${car.leaveTime}</b></p>` : ''}
            
            <div style="display:flex; gap:8px; margin:14px 0;">
                ${car.phone ? `<a href="tel:${car.phone}" class="btn btn-green" style="flex:1; justify-content:center; text-decoration:none;">📞 Позвонить</a>` : ''}
                ${car.phone ? `<a href="sms:${car.phone}?body=Здравствуйте! Освободите, пожалуйста, выезд на парковке." class="btn btn-cyan" style="flex:1; justify-content:center; text-decoration:none;">💬 SMS</a>` : ''}
            </div>

            <button class="btn btn-red" style="width:100%; justify-content:center; padding:10px;" onclick="submitLeave(${spot.id})">
                Освободить место
            </button>
        `;
    }

    document.getElementById('spotModal').classList.add('active');
}

// Занять место
async function submitPark(spotId) {
    const carNumber = document.getElementById('parkCarInput').value;
    const phone = document.getElementById('parkPhoneInput').value;
    const apt = document.getElementById('parkAptInput').value;
    const leaveTime = document.getElementById('parkLeaveTimeInput').value;

    if (!carNumber) return alert('Введите номер авто');

    try {
        const res = await fetch('/api/park', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ spotId, carNumber, phone, apt, leaveTime })
        });
        const ans = await res.json();
        if (ans.error) return alert(ans.error);

        closeModal();
        loadData();
    } catch (e) {
        alert('Ошибка связи с сервером: ' + e.message);
    }
}

// Освободить место
async function submitLeave(spotId) {
    let digits = '';
    let pin = '';
    if (isAdmin) {
        pin = adminPin;
    } else {
        digits = prompt('Введите последние 4 цифры вашего номера телефона:');
        if (digits === null) return;
    }

    try {
        const res = await fetch('/api/leave', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ spotId, lastDigits: digits, pin })
        });
        const ans = await res.json();
        if (ans.error) return alert(ans.error);

        closeModal();
        loadData();
    } catch (e) {
        alert('Ошибка связи с сервером: ' + e.message);
    }
}

// Режим 2-го ряда
function startRow2Mode() {
    isRow2SelectMode = true;
    selectedBlockSpots.clear();
    document.getElementById('instructionBar').innerHTML = '👉 <b>НАЖМИТЕ НА КАРТЕ</b> на места/машины, которые вы перекрываете, затем нажмите «Подтвердить» ниже:';
    
    const modal = document.getElementById('spotModal');
    const body = document.getElementById('spotModalBody');
    document.getElementById('modalSpotTitle').textContent = 'Встать во 2-й ряд';
    body.innerHTML = `
        <div class="input-group">
            <label>Ваш госномер авто:</label>
            <input type="text" id="row2Car" placeholder="А123АА">
        </div>
        <div class="input-group">
            <label>Телефон для связи (обязательно):</label>
            <input type="tel" id="row2Phone" placeholder="+7 9...">
        </div>
        <div class="input-group">
            <label>Уеду в... (напр. до 08:30 утра):</label>
            <input type="text" id="row2Time" value="08:30 утра">
        </div>
        <button class="btn btn-amber" style="width:100%; justify-content:center; padding:12px;" onclick="submitRow2()">
            Подтвердить постановку во 2-й ряд
        </button>
    `;
    modal.classList.add('active');
}

async function submitRow2() {
    const carNumber = document.getElementById('row2Car').value;
    const phone = document.getElementById('row2Phone').value;
    const leaveTime = document.getElementById('row2Time').value;

    if (!carNumber || !phone) return alert('Укажите авто и телефон');
    if (selectedBlockSpots.size === 0) return alert('Выберите на карте хотя бы одно место, которое перекрыли');

    try {
        const res = await fetch('/api/blocker', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                carNumber,
                phone,
                blockedSpots: Array.from(selectedBlockSpots),
                leaveTime
            })
        });
        const ans = await res.json();
        if (ans.error) return alert(ans.error);

        isRow2SelectMode = false;
        selectedBlockSpots.clear();
        closeModal();
        loadData();
    } catch (e) {
        alert('Ошибка связи с сервером: ' + e.message);
    }
}

// Просмотр деталей авто 2-го ряда
function openBlockerDetails(blocker) {
    const modal = document.getElementById('spotModal');
    const body = document.getElementById('spotModalBody');
    document.getElementById('modalSpotTitle').textContent = `Авто 2-го ряда: ${blocker.carNumber}`;

    const spotsList = (blocker.blockedSpots || []).join(', ');

    body.innerHTML = `
        <div class="number-plate" style="background:#ffb300; border-color:#e65100;">${blocker.carNumber}</div>
        <p style="margin-bottom:6px;">Перекрыл места: <b>№${spotsList}</b></p>
        <p style="margin-bottom:6px;">Телефон: <b>${blocker.phone || 'Не указан'}</b></p>
        ${blocker.leaveTime ? `<p style="color:#ffd54f; margin-bottom:8px;">Планирует уехать: <b>${blocker.leaveTime}</b></p>` : ''}

        <div style="display:flex; gap:8px; margin:14px 0;">
            ${blocker.phone ? `<a href="tel:${blocker.phone}" class="btn btn-green" style="flex:1; justify-content:center; text-decoration:none;">📞 Позвонить</a>` : ''}
            ${blocker.phone ? `<a href="sms:${blocker.phone}?body=Здравствуйте! Освободите, пожалуйста, выезд." class="btn btn-cyan" style="flex:1; justify-content:center; text-decoration:none;">💬 SMS</a>` : ''}
        </div>

        <button class="btn btn-red" style="width:100%; justify-content:center; padding:10px;" onclick="submitLeaveBlocker('${blocker.id}')">
            Убрать машину со 2-го ряда
        </button>
    `;
    modal.classList.add('active');
}

// Список всех авто 2-го ряда
function openBlockersList() {
    const modal = document.getElementById('blockersModal');
    const content = document.getElementById('blockersListContent');
    const blockers = currentData.blockers || [];

    if (blockers.length === 0) {
        content.innerHTML = `
            <div style="text-align:center; padding:24px 0; color:#81c784;">
                <p style="font-size:28px; margin-bottom:8px;">✓</p>
                <b>Сейчас во 2-м ряду машин нет</b>
                <p style="font-size:12px; color:#90a4ae; margin-top:4px;">Все выезды свободны</p>
            </div>
        `;
    } else {
        let html = '';
        blockers.forEach(b => {
            const spotsList = (b.blockedSpots || []).join(', ');
            html += `
                <div style="background:#242c35; border:1px solid #ffb300; border-radius:10px; padding:12px; margin-bottom:10px;">
                    <div style="display:flex; justify-content:space-between; align-items:center;">
                        <b style="font-size:16px; color:#ffb300;">${b.carNumber}</b>
                        <span style="font-size:11px; color:#90a4ae;">до ${b.leaveTime || 'утра'}</span>
                    </div>
                    <p style="font-size:12px; color:#cfd8dc; margin:6px 0;">Перекрыл места: <b>№${spotsList}</b></p>
                    <div style="display:flex; gap:8px; margin-top:8px;">
                        ${b.phone ? `<a href="tel:${b.phone}" class="btn btn-green" style="padding:4px 10px; font-size:12px; text-decoration:none;">📞 ${b.phone}</a>` : ''}
                        <button class="btn btn-outline" style="padding:4px 10px; font-size:12px;" onclick="closeBlockersModal(); openBlockerDetails(${JSON.stringify(b).replace(/"/g, '&quot;')})">Подробнее</button>
                    </div>
                </div>
            `;
        });
        content.innerHTML = html;
    }

    modal.classList.add('active');
}

function closeBlockersModal() {
    const modal = document.getElementById('blockersModal');
    if (modal) modal.classList.remove('active');
}

// Освободить место во 2-м ряду
async function submitLeaveBlocker(blockerId) {
    let digits = '';
    let pin = '';
    if (isAdmin) {
        pin = adminPin;
    } else {
        digits = prompt('Введите последние 4 цифры вашего номера телефона:');
        if (digits === null) return;
    }

    try {
        const res = await fetch('/api/blocker/leave', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ blockerId, lastDigits: digits, pin })
        });
        const ans = await res.json();
        if (ans.error) return alert(ans.error);

        closeModal();
        closeBlockersModal();
        loadData();
    } catch (e) {
        alert('Ошибка связи с сервером: ' + e.message);
    }
}

// ----------------- РЕДАКТОР СХЕМЫ ДЛЯ АДМИНИСТРАТОРА -----------------

async function toggleAdmin() {
    if (!isAdmin) {
        const pin = prompt('Введите PIN администратора:');
        if (!pin || !pin.trim()) return;

        try {
            const res = await fetch('/api/admin/verify', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ pin: pin.trim() })
            });
            const ans = await res.json();
            if (ans.success) {
                isAdmin = true;
                adminPin = pin.trim();
                localStorage.setItem('dvor_admin_pin', adminPin);
                document.getElementById('roleBadge').textContent = '★ Админ';
                document.getElementById('roleBadge').classList.add('active');
                document.getElementById('adminEditorBar').style.display = 'block';
                isEditMapMode = true;
                renderMap();
                showToast('Вход в режим администратора выполнен');
            } else {
                alert(ans.error || 'Неверный PIN администратора');
            }
        } catch (e) {
            alert('Ошибка связи с сервером: ' + e.message);
        }
    } else {
        isAdmin = false;
        isEditMapMode = false;
        adminPin = '';
        localStorage.removeItem('dvor_admin_pin');
        document.getElementById('roleBadge').textContent = 'Житель';
        document.getElementById('roleBadge').classList.remove('active');
        document.getElementById('adminEditorBar').style.display = 'none';
        renderMap();
        showToast('Выход из режима администратора');
    }
}

function closeAdminEdit() {
    isEditMapMode = false;
    document.getElementById('adminEditorBar').style.display = 'none';
    renderMap();
}

// Добавить место
function addNewSpot() {
    const nextId = ((currentData.spots || []).reduce((max, s) => Math.max(max, s.id), 0) || 0) + 1;
    currentData.spots.push({
        id: nextId,
        x: 480,
        y: 200,
        angle: 0,
        occupiedBy: null
    });
    renderMap();
    openEditSpot(nextId);
    saveMapToServer(true);
}

// Открыть настройку места (номер, угол и удаление)
function openEditSpot(spotId) {
    editingSpotId = spotId;
    const spot = (currentData.spots || []).find(s => s.id === spotId);
    if (!spot) return;

    document.getElementById('editSpotTitle').textContent = `Настройка места №${spot.id}`;
    const idInput = document.getElementById('spotIdInput');
    if (idInput) idInput.value = spot.id;

    const angle = spot.angle || 0;
    document.getElementById('spotAngleInput').value = angle;
    document.getElementById('spotAngleNumInput').value = angle;

    document.getElementById('editSpotModal').classList.add('active');
}

// Изменение угла места вручную
function onAngleNumInput(val) {
    const angle = parseFloat(val) || 0;
    document.getElementById('spotAngleInput').value = angle;
    const spot = (currentData.spots || []).find(s => s.id === editingSpotId);
    if (spot) {
        spot.angle = angle;
        renderMap();
    }
}

// Изменение угла места ползунком
function onAngleSliderChange(val) {
    document.getElementById('spotAngleNumInput').value = val;
    const spot = (currentData.spots || []).find(s => s.id === editingSpotId);
    if (spot) {
        spot.angle = Number(val);
        renderMap();
    }
}

// Быстрые пресеты угла места
function setAnglePreset(deg) {
    document.getElementById('spotAngleNumInput').value = deg;
    document.getElementById('spotAngleInput').value = deg;
    const spot = (currentData.spots || []).find(s => s.id === editingSpotId);
    if (spot) {
        spot.angle = deg;
        renderMap();
    }
}

// Сохранение настроек места (включая смену номера места)
function saveSpotEdit() {
    const spot = (currentData.spots || []).find(s => s.id === editingSpotId);
    if (!spot) return;

    const idInput = document.getElementById('spotIdInput');
    if (idInput) {
        const newId = parseInt(idInput.value, 10);
        if (isNaN(newId) || newId <= 0) {
            alert('Пожалуйста, введите корректный номер места (положительное число)');
            return;
        }

        if (newId !== editingSpotId) {
            const exists = (currentData.spots || []).some(s => s.id === newId);
            if (exists) {
                alert(`Место с номером №${newId} уже существует! Выберите другой номер.`);
                return;
            }

            const oldId = spot.id;
            spot.id = newId;

            // Обновляем связи блокировок во 2-м ряду
            (currentData.blockers || []).forEach(b => {
                if (Array.isArray(b.blockedSpots)) {
                    b.blockedSpots = b.blockedSpots.map(id => (id === oldId ? newId : id));
                }
            });

            if (selectedBlockSpots.has(oldId)) {
                selectedBlockSpots.delete(oldId);
                selectedBlockSpots.add(newId);
            }

            editingSpotId = newId;
        }
    }

    const angleInput = document.getElementById('spotAngleNumInput');
    if (angleInput) {
        spot.angle = parseFloat(angleInput.value) || 0;
    }

    document.getElementById('editSpotModal').classList.remove('active');
    renderMap();
    updateStats();
    saveMapToServer(true);
}

function deleteCurrentSpot() {
    if (confirm(`Удалить место №${editingSpotId}?`)) {
        currentData.spots = (currentData.spots || []).filter(s => s.id !== editingSpotId);
        (currentData.blockers || []).forEach(b => {
            if (Array.isArray(b.blockedSpots)) {
                b.blockedSpots = b.blockedSpots.filter(id => id !== editingSpotId);
            }
        });
        document.getElementById('editSpotModal').classList.remove('active');
        renderMap();
        updateStats();
        saveMapToServer(true);
    }
}

// Добавить ориентир/объект
function addNewLandmark() {
    const text = prompt('Введите название объекта (напр. 🚧 Шлагбаум, 🏢 Корпус 3, 🏬 МАГАЗИН, 🗑 Мусорка):', '🚧 ШЛАГБАУМ');
    if (!text || !text.trim()) return;

    const newId = 'lm_' + Date.now();
    currentData.landmarks.push({
        id: newId,
        text: text.trim(),
        x: 480,
        y: 220,
        angle: 0
    });
    renderMap();
    openEditLandmark(newId);
    saveMapToServer(true);
}

// Открыть редактирование ориентира
function openEditLandmark(id) {
    editingLandmarkId = id;
    const lm = (currentData.landmarks || []).find(l => l.id === id);
    if (!lm) return;

    const titleEl = document.getElementById('editLmTitle');
    if (titleEl) titleEl.textContent = `Редактировать: ${lm.text}`;

    document.getElementById('lmTextInput').value = lm.text || '';
    const angle = lm.angle || 0;
    document.getElementById('lmAngleInput').value = angle;
    document.getElementById('lmAngleNumInput').value = angle;

    document.getElementById('editLandmarkModal').classList.add('active');
}

// Изменение угла объекта вводом цифр
function onLmAngleNumInput(val) {
    const angle = parseFloat(val) || 0;
    document.getElementById('lmAngleInput').value = angle;
    const lm = (currentData.landmarks || []).find(l => l.id === editingLandmarkId);
    if (lm) {
        lm.angle = angle;
        renderMap();
    }
}

// Изменение угла объекта ползунком
function onLmAngleSliderChange(val) {
    document.getElementById('lmAngleNumInput').value = val;
    const lm = (currentData.landmarks || []).find(l => l.id === editingLandmarkId);
    if (lm) {
        lm.angle = Number(val);
        renderMap();
    }
}

// Быстрые пресеты угла объекта
function setLmAnglePreset(deg) {
    document.getElementById('lmAngleNumInput').value = deg;
    document.getElementById('lmAngleInput').value = deg;
    const lm = (currentData.landmarks || []).find(l => l.id === editingLandmarkId);
    if (lm) {
        lm.angle = deg;
        renderMap();
    }
}

function saveLmEdit() {
    const lm = (currentData.landmarks || []).find(l => l.id === editingLandmarkId);
    if (lm) {
        lm.text = document.getElementById('lmTextInput').value.trim();
        lm.angle = parseFloat(document.getElementById('lmAngleNumInput').value) || 0;
        renderMap();
    }
    document.getElementById('editLandmarkModal').classList.remove('active');
    saveMapToServer(true);
}

function deleteCurrentLm() {
    if (confirm('Удалить этот объект со схемы?')) {
        currentData.landmarks = (currentData.landmarks || []).filter(l => l.id !== editingLandmarkId);
        document.getElementById('editLandmarkModal').classList.remove('active');
        renderMap();
        saveMapToServer(true);
    }
}

// Сохранение схемы на сервере
async function saveMapToServer(silent = false) {
    const pin = adminPin;

    try {
        const res = await fetch('/api/admin/map', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                pin: pin,
                landmarks: currentData.landmarks,
                spots: currentData.spots,
                blockers: currentData.blockers
            })
        });
        const ans = await res.json();
        if (ans.error) {
            alert('Ошибка сохранения: ' + ans.error);
            return;
        }
        showToast('✓ Схема сохранена на сервере');
        if (!silent) {
            alert('Схема двора успешно сохранена!');
        }
    } catch (e) {
        console.error('Ошибка сохранения на сервере:', e);
        if (!silent) alert('Ошибка связи с сервером: ' + e.message);
    }
}

function closeModal() {
    const modal = document.getElementById('spotModal');
    if (modal) modal.classList.remove('active');
}

// Инициализация при открытии
if (isAdmin) {
    const badge = document.getElementById('roleBadge');
    if (badge) {
        badge.textContent = '★ Админ';
        badge.classList.add('active');
    }
    const adminBar = document.getElementById('adminEditorBar');
    if (adminBar) adminBar.style.display = 'block';
    isEditMapMode = true;
}

loadData();
setInterval(loadData, 10000);
